package com.example.easymove.view.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private UserProfile currentUserProfile;

    // רכיבי UI ראשיים
    private ImageView imageProfile;
    private Button buttonChangeImage;
    private TextInputEditText editName, editPhone;
    private TextView textUserType;

    // קונטיינרים (מחזיקים את כל השדות לכל סוג משתמש)
    private LinearLayout containerCustomerInfo;
    private LinearLayout containerMoverInfo;

    // --- שדות לקוח ---
    private TextView tvFromAddress, tvToAddress, tvMoveDate;
    private Button btnPickFromAddress, btnPickToAddress, btnPickMoveDate;
    private TextInputEditText editFloor, editApartment;

    // --- שדות מוביל ---
    private TextView tvMoverBaseAddress, tvRadiusLabel;
    private Button btnPickMoverLocation;
    private Slider sliderRadius;
    private TextInputEditText editAbout;

    // --- כפתורים כלליים ---
    private Button buttonEdit, buttonSave, buttonCancel;
    private LinearLayout layoutSaveCancel;
    private TextView textError;
    private ProgressBar progressBar;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private enum AddressPickType { FROM, TO, MOVER }
    private AddressPickType currentPickType = AddressPickType.MOVER;

    private boolean isEditMode = false;

    // גיבוי לביטול עריכה
    private String oldName, oldPhone, oldAbout, oldFromAddress, oldToAddress;
    private String oldFloorStr, oldApartmentStr;
    private long oldMoveDate = 0;

    // גיבוי למוביל
    private String oldMoverBaseAddress, oldGeohash;
    private double oldLat, oldLng;
    private int oldRadius;

    private long selectedMoveDate = 0;

    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place != null) {
                        if (currentPickType == AddressPickType.MOVER) updateMoverLocation(place);
                        else if (currentPickType == AddressPickType.FROM) updateCustomerFromAddress(place);
                        else if (currentPickType == AddressPickType.TO) updateCustomerToAddress(place);
                    }
                }
            }
    );

    public ProfileFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        initViews(view);
        setupListeners();
        observeViewModel();

        viewModel.loadMyProfile();
    }

    private void initViews(View view) {
        // ראש ופרטים אישיים
        imageProfile = view.findViewById(R.id.image_profile);
        buttonChangeImage = view.findViewById(R.id.button_change_image);
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        textUserType = view.findViewById(R.id.text_user_type);

        // קונטיינרים
        containerCustomerInfo = view.findViewById(R.id.container_customer_info);
        containerMoverInfo = view.findViewById(R.id.container_mover_info);

        // לקוח
        tvFromAddress = view.findViewById(R.id.tv_from_address);
        btnPickFromAddress = view.findViewById(R.id.btnPickFromAddress);
        tvToAddress = view.findViewById(R.id.tv_to_address);
        btnPickToAddress = view.findViewById(R.id.btnPickToAddress);
        tvMoveDate = view.findViewById(R.id.tv_move_date);
        btnPickMoveDate = view.findViewById(R.id.btnPickMoveDate);
        editFloor = view.findViewById(R.id.edit_floor);
        editApartment = view.findViewById(R.id.edit_apartment);

        // מוביל
        tvMoverBaseAddress = view.findViewById(R.id.tvMoverBaseAddress);
        btnPickMoverLocation = view.findViewById(R.id.btnPickMoverLocation);
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel);
        sliderRadius = view.findViewById(R.id.sliderRadius);
        editAbout = view.findViewById(R.id.edit_about);

        // כללי
        buttonEdit = view.findViewById(R.id.button_edit_profile);
        layoutSaveCancel = view.findViewById(R.id.layout_save_cancel);
        buttonSave = view.findViewById(R.id.button_save_profile);
        buttonCancel = view.findViewById(R.id.button_cancel_edit);
        textError = view.findViewById(R.id.text_error);
        progressBar = view.findViewById(R.id.progress_loading);
    }

    private void setupListeners() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        // ✅ תיקון: שימוש ב-Glide גם כאן כדי לחתוך לעיגול מיד בבחירה
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(imageProfile);

                        viewModel.uploadProfileImage(uri); // העלאה מיידית
                    }
                }
        );

        buttonChangeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        buttonEdit.setOnClickListener(v -> enterEditMode());

        buttonSave.setOnClickListener(v -> {
            saveProfileFromUi();
            exitEditMode();
        });

        buttonCancel.setOnClickListener(v -> cancelEditMode());

        // כפתורי לקוח
        btnPickFromAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.FROM;
            openPlacePicker();
        });
        btnPickToAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.TO;
            openPlacePicker();
        });
        btnPickMoveDate.setOnClickListener(v -> openDatePicker());

        // כפתורי מוביל
        btnPickMoverLocation.setOnClickListener(v -> {
            currentPickType = AddressPickType.MOVER;
            openPlacePicker();
        });

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && currentUserProfile != null) {
                currentUserProfile.setServiceRadiusKm((int) value);
                tvRadiusLabel.setText("רדיוס שירות: " + (int) value + " ק״מ");
            }
        });
    }

    private void observeViewModel() {
        viewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentUserProfile = profile;
                fillUiFromProfile(profile);
            }
        });

        viewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                textError.setText(msg);
                textError.setVisibility(View.VISIBLE);
            } else {
                textError.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(),
                isLoading -> progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        viewModel.getUploadedImageUrlLiveData().observe(getViewLifecycleOwner(), url -> {
            if (url != null && currentUserProfile != null) {
                currentUserProfile.setProfileImageUrl(url);
                viewModel.saveMyProfile(currentUserProfile);
            }
        });
    }

    private void fillUiFromProfile(@NonNull UserProfile profile) {
        editName.setText(profile.getName());
        editPhone.setText(profile.getPhone());

        String type = profile.getUserType();
        if (type == null) type = "customer";
        textUserType.setText(type);

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            // ✅ תיקון: הוספת circleCrop() כדי לחתוך את התמונה לעיגול
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(imageProfile);
        } else {
            imageProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // בחירת הקונטיינר הנכון להצגה
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

        // שליפת כתובת בסיס מהרשימה
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

    // --- Places & Date Picker ---

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS);
        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).setCountry("IL");
        if (currentPickType != AddressPickType.MOVER) builder.setTypeFilter(TypeFilter.ADDRESS);
        placePickerLauncher.launch(builder.build(requireContext()));
    }

    private boolean hasStreetNumber(@Nullable Place place) {
        if (place == null || place.getAddressComponents() == null) return false;
        for (AddressComponent c : place.getAddressComponents().asList()) {
            if (c.getTypes() != null && c.getTypes().contains("street_number")) return true;
        }
        return false;
    }

    private void updateCustomerFromAddress(Place place) {
        if (currentUserProfile == null || place.getAddress() == null) return;
        if (!hasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }
        tvFromAddress.setText(place.getAddress());
        currentUserProfile.setDefaultFromAddress(place.getAddress());
        currentUserProfile.setFromLat(place.getLatLng().latitude);
        currentUserProfile.setFromLng(place.getLatLng().longitude);
    }

    private void updateCustomerToAddress(Place place) {
        if (currentUserProfile == null || place.getAddress() == null) return;
        if (!hasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }
        tvToAddress.setText(place.getAddress());
        currentUserProfile.setDefaultToAddress(place.getAddress());
        currentUserProfile.setToLat(place.getLatLng().latitude);
        currentUserProfile.setToLng(place.getLatLng().longitude);
    }

    private void updateMoverLocation(Place place) {
        if (place.getLatLng() == null || currentUserProfile == null || place.getAddress() == null) return;

        tvMoverBaseAddress.setText(place.getAddress()); // עדכון ויזואלי מיידי

        currentUserProfile.setLat(place.getLatLng().latitude);
        currentUserProfile.setLng(place.getLatLng().longitude);
        currentUserProfile.setGeohash(GeoFireUtils.getGeoHashForLocation(new GeoLocation(place.getLatLng().latitude, place.getLatLng().longitude)));

        // עדכון רשימת אזורי השירות (האיבר הראשון)
        List<String> areas = currentUserProfile.getServiceAreas();
        if (areas == null) areas = new java.util.ArrayList<>();
        if (!areas.isEmpty()) areas.set(0, place.getAddress());
        else areas.add(place.getAddress());
        currentUserProfile.setServiceAreas(areas);

        // גיבוי גם לכתובת מוצא רגילה (ליתר ביטחון)
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
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    // --- Edit Mode Logic ---

    private void enterEditMode() {
        isEditMode = true;
        backupCurrentData();
        updateUiMode();
    }

    private void saveProfileFromUi() {
        if (currentUserProfile == null) currentUserProfile = new UserProfile();

        currentUserProfile.setName(editName.getText().toString().trim());
        currentUserProfile.setPhone(editPhone.getText().toString().trim());

        if ("mover".equals(currentUserProfile.getUserType())) {
            currentUserProfile.setAbout(editAbout.getText().toString().trim());
        } else {
            currentUserProfile.setFloor(parseOptionalInt(editFloor.getText().toString()));
            currentUserProfile.setApartment(parseOptionalInt(editApartment.getText().toString()));
            currentUserProfile.setDefaultMoveDate(selectedMoveDate);
        }

        viewModel.saveMyProfile(currentUserProfile);
        Toast.makeText(getContext(), "הפרופיל נשמר", Toast.LENGTH_SHORT).show();
    }

    private void cancelEditMode() {
        isEditMode = false;
        restoreData();
        fillUiFromProfile(currentUserProfile); // רענון ה-UI מהאובייקט המשוחזר
        updateUiMode();
    }

    private void exitEditMode() {
        isEditMode = false;
        updateUiMode();
    }

    private void updateUiMode() {
        // שדות טקסט
        editName.setEnabled(isEditMode);
        editPhone.setEnabled(isEditMode);
        editFloor.setEnabled(isEditMode);
        editApartment.setEnabled(isEditMode);
        editAbout.setEnabled(isEditMode);

        // כפתורים
        buttonEdit.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        layoutSaveCancel.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        buttonChangeImage.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // כפתורי בחירה (לקוח)
        btnPickFromAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnPickToAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnPickMoveDate.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        // כפתורי בחירה (מוביל)
        btnPickMoverLocation.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        sliderRadius.setEnabled(isEditMode);
    }

    private void backupCurrentData() {
        oldName = editName.getText().toString();
        oldPhone = editPhone.getText().toString();

        if (currentUserProfile != null) {
            if ("mover".equals(currentUserProfile.getUserType())) {
                oldAbout = editAbout.getText().toString();
                oldRadius = currentUserProfile.getServiceRadiusKm();
                // גיבוי מיקום
                List<String> areas = currentUserProfile.getServiceAreas();
                oldMoverBaseAddress = (areas != null && !areas.isEmpty()) ? areas.get(0) : "";
                oldLat = currentUserProfile.getLat();
                oldLng = currentUserProfile.getLng();
                oldGeohash = currentUserProfile.getGeohash();
            } else {
                oldFloorStr = editFloor.getText().toString();
                oldApartmentStr = editApartment.getText().toString();
                oldFromAddress = currentUserProfile.getDefaultFromAddress();
                oldToAddress = currentUserProfile.getDefaultToAddress();
                oldMoveDate = selectedMoveDate;
            }
        }
    }

    private void restoreData() {
        if (currentUserProfile == null) return;

        editName.setText(oldName);
        editPhone.setText(oldPhone);

        if ("mover".equals(currentUserProfile.getUserType())) {
            editAbout.setText(oldAbout);
            currentUserProfile.setServiceRadiusKm(oldRadius);

            // שחזור מיקום מוביל
            currentUserProfile.setLat(oldLat);
            currentUserProfile.setLng(oldLng);
            currentUserProfile.setGeohash(oldGeohash);
            List<String> areas = new java.util.ArrayList<>();
            if (oldMoverBaseAddress != null && !oldMoverBaseAddress.isEmpty()) areas.add(oldMoverBaseAddress);
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