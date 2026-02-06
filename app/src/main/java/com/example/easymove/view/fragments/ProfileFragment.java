package com.example.easymove.view.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.viewmodel.UserViewModel;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Fragment responsible for displaying and editing the User Profile.
 * Supports both "Customer" and "Mover" user types with dynamic UI changes.
 * <p>
 * Features:
 * 1. View/Edit Personal Info (Name, Phone, Image).
 * 2. Customer: Manage Default Move Details (Source, Dest, Floor, Date).
 * 3. Mover: Manage Service Area (Base Location, Radius) and "About".
 * 4. Google Places Autocomplete for address selection.
 * 5. Edit Mode with "Cancel/Undo" functionality.
 */
public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private UserProfile currentUserProfile;

    // --- Main UI Components ---
    private ImageView imageProfile;
    private Button buttonChangeImage;
    private TextInputEditText editName, editPhone;
    private TextView textUserType;

    // --- Containers (Hold specific fields per user type) ---
    private LinearLayout containerCustomerInfo;
    private LinearLayout containerMoverInfo;

    // --- Customer Specific Fields ---
    private TextView tvFromAddress, tvToAddress, tvMoveDate;
    private Button btnPickFromAddress, btnPickToAddress, btnPickMoveDate;
    private TextInputEditText editFloor, editApartment;

    // --- Mover Specific Fields ---
    private TextView tvMoverBaseAddress, tvRadiusLabel;
    private Button btnPickMoverLocation;
    private Slider sliderRadius;
    private TextInputEditText editAbout;

    // --- Action Buttons & States ---
    private Button buttonEdit, buttonSave, buttonCancel;
    private LinearLayout layoutSaveCancel;
    private TextView textError;
    private ProgressBar progressBar;

    // --- Image & Place Picking ---
    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private enum AddressPickType {FROM, TO, MOVER}

    private AddressPickType currentPickType = AddressPickType.MOVER;

    // --- Edit Mode State & Backup ---
    private boolean isEditMode = false;

    private String oldFromAddress;
    private String oldToAddress;
    private String oldFloorStr, oldApartmentStr;
    private long oldMoveDate = 0;

    // Backup for Mover specifics
    private String oldMoverBaseAddress, oldGeohash;
    private double oldLat, oldLng;
    private int oldRadius;

    private long selectedMoveDate = 0;

    /**
     * Launcher for Google Places Autocomplete Activity.
     */
    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    handlePlaceSelection(place);
                }
            }
    );

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // Initialize Places API if needed
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        initViews(view);
        setupListeners();
        observeViewModel();

        viewModel.loadMyProfile();
    }

    private void initViews(View view) {
        // Header & Personal Info
        imageProfile = view.findViewById(R.id.image_profile);
        buttonChangeImage = view.findViewById(R.id.button_change_image);
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        textUserType = view.findViewById(R.id.text_user_type);

        // Containers
        containerCustomerInfo = view.findViewById(R.id.container_customer_info);
        containerMoverInfo = view.findViewById(R.id.container_mover_info);

        // Customer UI
        tvFromAddress = view.findViewById(R.id.tv_from_address);
        btnPickFromAddress = view.findViewById(R.id.btnPickFromAddress);
        tvToAddress = view.findViewById(R.id.tv_to_address);
        btnPickToAddress = view.findViewById(R.id.btnPickToAddress);
        tvMoveDate = view.findViewById(R.id.tv_move_date);
        btnPickMoveDate = view.findViewById(R.id.btnPickMoveDate);
        editFloor = view.findViewById(R.id.edit_floor);
        editApartment = view.findViewById(R.id.edit_apartment);

        // Mover UI
        tvMoverBaseAddress = view.findViewById(R.id.tvMoverBaseAddress);
        btnPickMoverLocation = view.findViewById(R.id.btnPickMoverLocation);
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel);
        sliderRadius = view.findViewById(R.id.sliderRadius);
        editAbout = view.findViewById(R.id.edit_about);

        // General UI
        buttonEdit = view.findViewById(R.id.button_edit_profile);
        layoutSaveCancel = view.findViewById(R.id.layout_save_cancel);
        buttonSave = view.findViewById(R.id.button_save_profile);
        buttonCancel = view.findViewById(R.id.button_cancel_edit);
        textError = view.findViewById(R.id.text_error);
        progressBar = view.findViewById(R.id.progress_loading);
    }

    private void setupListeners() {
        // Image Picker
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        // Load immediately into UI
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(imageProfile);
                        // Upload immediately
                        viewModel.uploadProfileImage(uri);
                    }
                }
        );

        buttonChangeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Edit Mode Controls
        buttonEdit.setOnClickListener(v -> enterEditMode());

        buttonSave.setOnClickListener(v -> {
            saveProfileFromUi();
            exitEditMode();
        });

        buttonCancel.setOnClickListener(v -> cancelEditMode());

        // Customer Buttons
        btnPickFromAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.FROM;
            openPlacePicker();
        });
        btnPickToAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.TO;
            openPlacePicker();
        });
        btnPickMoveDate.setOnClickListener(v -> openDatePicker());

        // Mover Buttons
        btnPickMoverLocation.setOnClickListener(v -> {
            currentPickType = AddressPickType.MOVER;
            openPlacePicker();
        });

        // Radius Slider
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && currentUserProfile != null) {
                currentUserProfile.setServiceRadiusKm((int) value);
                tvRadiusLabel.setText("רדיוס שירות: " + (int) value + " ק״מ");
            }
        });
    }

    private void observeViewModel() {
        // Observe Profile Data
        viewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentUserProfile = profile;
                fillUiFromProfile(profile);
            }
        });

        // Observe Errors
        viewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                textError.setText(msg);
                textError.setVisibility(View.VISIBLE);
            } else {
                textError.setVisibility(View.GONE);
            }
        });

        // Observe Loading State
        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(),
                isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        // Observe Image Upload Success
        viewModel.getUploadedImageUrlLiveData().observe(getViewLifecycleOwner(), url -> {
            if (url != null && currentUserProfile != null) {
                currentUserProfile.setProfileImageUrl(url);
                viewModel.saveMyProfile(currentUserProfile);
            }
        });
    }

    /**
     * Populates the UI fields based on the UserProfile object.
     */
    private void fillUiFromProfile(@NonNull UserProfile profile) {
        editName.setText(profile.getName());
        editPhone.setText(profile.getPhone());

        String type = profile.getUserType();
        if (type == null) type = "customer";
        textUserType.setText(type);

        // Load Profile Image
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(imageProfile);
        } else {
            imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Toggle UI based on User Type
        if ("mover".equals(type)) {
            setupMoverUi(profile);
        } else {
            setupCustomerUi(profile);
        }
    }

    private void setupMoverUi(UserProfile profile) {
        containerCustomerInfo.setVisibility(View.GONE);
        containerMoverInfo.setVisibility(View.VISIBLE);

        if (profile.getAbout() != null) editAbout.setText(profile.getAbout());

        // Get Base Address from Service Areas list
        String baseAddress = "טרם הוגדר בסיס יציאה";
        List<String> areas = profile.getServiceAreas();
        if (areas != null && !areas.isEmpty()) {
            baseAddress = areas.get(0);
        }
        tvMoverBaseAddress.setText(baseAddress);

        int radius = profile.getServiceRadiusKm() > 0 ? profile.getServiceRadiusKm() : 30;
        sliderRadius.setValue(radius);
        tvRadiusLabel.setText("רדיוס שירות: " + radius + " ק״מ");
    }

    private void setupCustomerUi(UserProfile profile) {
        containerMoverInfo.setVisibility(View.GONE);
        containerCustomerInfo.setVisibility(View.VISIBLE);

        tvFromAddress.setText(profile.getDefaultFromAddress() != null ? profile.getDefaultFromAddress() : "לא נבחרה כתובת");
        tvToAddress.setText(profile.getDefaultToAddress() != null ? profile.getDefaultToAddress() : "לא נבחרה כתובת");

        if (profile.getDefaultMoveDate() != null && profile.getDefaultMoveDate() > 0) {
            selectedMoveDate = profile.getDefaultMoveDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvMoveDate.setText(sdf.format(new Date(selectedMoveDate)));
        } else {
            selectedMoveDate = 0;
            tvMoveDate.setText("טרם נקבע תאריך");
        }

        editFloor.setText(profile.getFloor() != null ? String.valueOf(profile.getFloor()) : "");
        editApartment.setText(profile.getApartment() != null ? String.valueOf(profile.getApartment()) : "");
    }

    // ------------------------------------------------------------------------
    // Places & Date Logic
    // ------------------------------------------------------------------------

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS);
        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).setCountry("IL");

        // If picking a specific address (not a city/area), filter for addresses
        if (currentPickType != AddressPickType.MOVER) {
            builder.setTypeFilter(TypeFilter.ADDRESS);
        }
        placePickerLauncher.launch(builder.build(requireContext()));
    }

    private void handlePlaceSelection(Place place) {
        if (currentPickType == AddressPickType.MOVER) {
            updateMoverLocation(place);
        } else if (currentPickType == AddressPickType.FROM) {
            updateCustomerFromAddress(place);
        } else if (currentPickType == AddressPickType.TO) {
            updateCustomerToAddress(place);
        }
    }

    private boolean NotHasStreetNumber(@Nullable Place place) {
        if (place == null || place.getAddressComponents() == null) return true;
        for (AddressComponent c : place.getAddressComponents().asList()) {
            c.getTypes();
            if (c.getTypes().contains("street_number")) return false;
        }
        return true;
    }

    private void updateCustomerFromAddress(Place place) {
        if (currentUserProfile == null || place.getAddress() == null) return;
        if (NotHasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }
        tvFromAddress.setText(place.getAddress());
        currentUserProfile.setDefaultFromAddress(place.getAddress());
        currentUserProfile.setFromLat(Objects.requireNonNull(place.getLatLng()).latitude);
        currentUserProfile.setFromLng(place.getLatLng().longitude);
    }

    private void updateCustomerToAddress(Place place) {
        if (currentUserProfile == null || place.getAddress() == null) return;
        if (NotHasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }
        tvToAddress.setText(place.getAddress());
        currentUserProfile.setDefaultToAddress(place.getAddress());
        currentUserProfile.setToLat(Objects.requireNonNull(place.getLatLng()).latitude);
        currentUserProfile.setToLng(place.getLatLng().longitude);
    }

    private void updateMoverLocation(Place place) {
        if (place.getLatLng() == null || currentUserProfile == null || place.getAddress() == null)
            return;

        tvMoverBaseAddress.setText(place.getAddress());

        currentUserProfile.setLat(place.getLatLng().latitude);
        currentUserProfile.setLng(place.getLatLng().longitude);
        currentUserProfile.setGeohash(GeoFireUtils.getGeoHashForLocation(new GeoLocation(place.getLatLng().latitude, place.getLatLng().longitude)));

        // Update Service Areas list (Index 0 is considered the base)
        List<String> areas = currentUserProfile.getServiceAreas();
        if (areas == null) areas = new java.util.ArrayList<>();
        if (!areas.isEmpty()) areas.set(0, place.getAddress());
        else areas.add(place.getAddress());
        currentUserProfile.setServiceAreas(areas);

        // Also update default address for consistency
        currentUserProfile.setDefaultFromAddress(place.getAddress());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedMoveDate > 0) c.setTimeInMillis(selectedMoveDate);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            c.set(year, month, dayOfMonth, 0, 0, 0);
            selectedMoveDate = c.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvMoveDate.setText(sdf.format(c.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private Integer parseOptionalInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // Edit Mode Logic (Backup/Restore)
    // ------------------------------------------------------------------------

    private void enterEditMode() {
        isEditMode = true;
        backupCurrentData();
        updateUiMode();
    }

    private void saveProfileFromUi() {
        if (currentUserProfile == null) currentUserProfile = new UserProfile();

        String name = Objects.requireNonNull(editName.getText()).toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "השם לא יכול להיות ריק", Toast.LENGTH_SHORT).show();
            return;
        }

        currentUserProfile.setName(name);
        currentUserProfile.setPhone(Objects.requireNonNull(editPhone.getText()).toString().trim());

        if ("mover".equals(currentUserProfile.getUserType())) {
            currentUserProfile.setAbout(Objects.requireNonNull(editAbout.getText()).toString().trim());
        } else {
            currentUserProfile.setFloor(parseOptionalInt(Objects.requireNonNull(editFloor.getText()).toString()));
            currentUserProfile.setApartment(parseOptionalInt(Objects.requireNonNull(editApartment.getText()).toString()));
            currentUserProfile.setDefaultMoveDate(selectedMoveDate);
        }

        viewModel.saveMyProfile(currentUserProfile);
        Toast.makeText(getContext(), "הפרופיל נשמר", Toast.LENGTH_SHORT).show();
    }

    /**
     * Cancels edit mode and restores the data to its previous state.
     */
    private void cancelEditMode() {
        isEditMode = false;
        restoreData();
        // Refresh UI from the restored object
        fillUiFromProfile(currentUserProfile);
        updateUiMode();
    }

    private void exitEditMode() {
        isEditMode = false;
        updateUiMode();
    }

    private void updateUiMode() {
        // Text Fields
        editName.setEnabled(isEditMode);
        editPhone.setEnabled(isEditMode);
        editFloor.setEnabled(isEditMode);
        editApartment.setEnabled(isEditMode);
        editAbout.setEnabled(isEditMode);

        // Buttons visibility
        buttonEdit.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        layoutSaveCancel.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        buttonChangeImage.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // Pickers
        int pickerVisibility = isEditMode ? View.VISIBLE : View.GONE;
        btnPickFromAddress.setVisibility(pickerVisibility);
        btnPickToAddress.setVisibility(pickerVisibility);
        btnPickMoveDate.setVisibility(pickerVisibility);
        btnPickMoverLocation.setVisibility(pickerVisibility);
        sliderRadius.setEnabled(isEditMode);
    }

    private void backupCurrentData() {
        // Backup variables to support "Cancel Edit"
        String oldName = Objects.requireNonNull(editName.getText()).toString();
        String oldPhone = Objects.requireNonNull(editPhone.getText()).toString();

        if (currentUserProfile != null) {
            if ("mover".equals(currentUserProfile.getUserType())) {
                String oldAbout = Objects.requireNonNull(editAbout.getText()).toString();
                oldRadius = currentUserProfile.getServiceRadiusKm();
                // Backup location
                List<String> areas = currentUserProfile.getServiceAreas();
                oldMoverBaseAddress = (areas != null && !areas.isEmpty()) ? areas.get(0) : "";
                oldLat = currentUserProfile.getLat();
                oldLng = currentUserProfile.getLng();
                oldGeohash = currentUserProfile.getGeohash();
            } else {
                oldFloorStr = Objects.requireNonNull(editFloor.getText()).toString();
                oldApartmentStr = Objects.requireNonNull(editApartment.getText()).toString();
                oldFromAddress = currentUserProfile.getDefaultFromAddress();
                oldToAddress = currentUserProfile.getDefaultToAddress();
                oldMoveDate = selectedMoveDate;
            }
        }
    }

    /**
     * Reverts the fields in `currentUserProfile` to the backed-up values.
     */
    private void restoreData() {
        if (currentUserProfile == null) return;

        // Note: We only update the Object here. UI update happens in fillUiFromProfile()
        if ("mover".equals(currentUserProfile.getUserType())) {
            currentUserProfile.setServiceRadiusKm(oldRadius);
            // Restore location
            currentUserProfile.setLat(oldLat);
            currentUserProfile.setLng(oldLng);
            currentUserProfile.setGeohash(oldGeohash);

            List<String> areas = new java.util.ArrayList<>();
            if (oldMoverBaseAddress != null && !oldMoverBaseAddress.isEmpty())
                areas.add(oldMoverBaseAddress);
            currentUserProfile.setServiceAreas(areas);

        } else {
            selectedMoveDate = oldMoveDate;
            currentUserProfile.setFloor(parseOptionalInt(oldFloorStr));
            currentUserProfile.setApartment(parseOptionalInt(oldApartmentStr));
            currentUserProfile.setDefaultFromAddress(oldFromAddress);
            currentUserProfile.setDefaultToAddress(oldToAddress);
            currentUserProfile.setDefaultMoveDate(oldMoveDate);
        }
    }
}