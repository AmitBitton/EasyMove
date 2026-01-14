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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
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
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private UserProfile currentUserProfile;


    private NestedScrollView profileScroll;
    private TextView labelFromAddress, labelToAddress;
    private ImageView imageProfile;
    private Button buttonChangeImage;
    private EditText editName, editPhone;
    private TextView textUserType;

    // כתובות לקוח
    private TextView tvFromAddress, tvToAddress;
    private Button btnPickFromAddress, btnPickToAddress;

    // תאריך הובלה (לקוח)
    private TextView labelMoveDate, tvMoveDate;
    private Button btnPickMoveDate;

    // קומה + דירה
    private TextView labelFloor, labelApartment;
    private EditText editFloor, editApartment;

    // "אודות" (מוביל)
    private TextView labelAbout;
    private EditText editAbout;

    // עריכה/שמירה
    private Button buttonEdit, buttonSave, buttonCancel;
    private LinearLayout layoutSaveCancel;

    private TextView textError;
    private ProgressBar progressBar;

    // מוביל
    private LinearLayout layoutMoverFields;
    private TextView tvSelectedMoverAddress, labelMoverBaseAddress, tvMoverBaseAddress, tvRadiusLabel;
    private Button btnPickMoverLocation;
    private Slider sliderRadius;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private enum AddressPickType { FROM, TO, MOVER }
    private AddressPickType currentPickType = AddressPickType.MOVER;

    private boolean isEditMode = false;

    // משתנים לשמירת מצב קודם (ביטול)
    private String oldName, oldPhone, oldAbout, oldFromAddress, oldToAddress;
    private String oldFloorStr, oldApartmentStr;
    private long oldMoveDate = 0;

    // התאריך שנבחר כרגע ב-UI
    private long selectedMoveDate = 0;

    // מוביל - ביטול
    private String oldMoverBaseAddress, oldGeohash;
    private double oldLat, oldLng;
    private int oldRadius;

    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place == null) return;

                    if (currentPickType == AddressPickType.MOVER) updateMoverLocation(place);
                    else if (currentPickType == AddressPickType.FROM) updateCustomerFromAddress(place);
                    else if (currentPickType == AddressPickType.TO) updateCustomerToAddress(place);
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
        profileScroll = view.findViewById(R.id.profile_scroll);
        imageProfile = view.findViewById(R.id.image_profile);
        buttonChangeImage = view.findViewById(R.id.button_change_image);
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        textUserType = view.findViewById(R.id.text_user_type);

        tvFromAddress = view.findViewById(R.id.tv_from_address);
        tvToAddress = view.findViewById(R.id.tv_to_address);
        btnPickFromAddress = view.findViewById(R.id.btnPickFromAddress);
        btnPickToAddress = view.findViewById(R.id.btnPickToAddress);

        labelMoveDate = view.findViewById(R.id.label_move_date);
        tvMoveDate = view.findViewById(R.id.tv_move_date);
        btnPickMoveDate = view.findViewById(R.id.btnPickMoveDate);

        labelFloor = view.findViewById(R.id.label_floor);
        labelApartment = view.findViewById(R.id.label_apartment);
        editFloor = view.findViewById(R.id.edit_floor);
        editApartment = view.findViewById(R.id.edit_apartment);

        labelAbout = view.findViewById(R.id.label_about);
        editAbout = view.findViewById(R.id.edit_about);

        buttonEdit = view.findViewById(R.id.button_edit_profile);
        layoutSaveCancel = view.findViewById(R.id.layout_save_cancel);
        buttonSave = view.findViewById(R.id.button_save_profile);
        buttonCancel = view.findViewById(R.id.button_cancel_edit);

        textError = view.findViewById(R.id.text_error);
        progressBar = view.findViewById(R.id.progress_loading);

        labelFromAddress = view.findViewById(R.id.label_from_address);
        labelToAddress = view.findViewById(R.id.label_to_address);

        layoutMoverFields = view.findViewById(R.id.layoutMoverFields);
        tvSelectedMoverAddress = view.findViewById(R.id.tvSelectedMoverAddress);
        btnPickMoverLocation = view.findViewById(R.id.btnPickMoverLocation);
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel);
        sliderRadius = view.findViewById(R.id.sliderRadius);

        labelMoverBaseAddress = view.findViewById(R.id.label_mover_base_address);
        tvMoverBaseAddress = view.findViewById(R.id.tvMoverBaseAddress);
    }

    private void setupListeners() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageProfile.setImageURI(uri);
                        viewModel.uploadProfileImage(uri);
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

        btnPickFromAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.FROM;
            openPlacePicker();
        });

        btnPickToAddress.setOnClickListener(v -> {
            currentPickType = AddressPickType.TO;
            openPlacePicker();
        });

        btnPickMoverLocation.setOnClickListener(v -> {
            currentPickType = AddressPickType.MOVER;
            openPlacePicker();
        });

        btnPickMoveDate.setOnClickListener(v -> openDatePicker());

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            currentUserProfile.setServiceRadiusKm((int) value);
            tvRadiusLabel.setText("רדיוס שירות: " + (int) value + " ק״מ");
        });
    }

    private void observeViewModel() {
        viewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                currentUserProfile = profile;
                isEditMode = false;
                fillUiFromProfile(profile);
                exitEditMode();
            }
        });

        // האזנה להצלחה של שמירת פרטי ההובלה (הסינכרון)
        viewModel.getMoveDetailsSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved != null && saved) {
                // אופציונלי: אפשר להציג טוסט מיוחד כאן אם רוצים
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
        if (profile.getName() != null) editName.setText(profile.getName());
        if (profile.getPhone() != null) editPhone.setText(profile.getPhone());

        String type = profile.getUserType();
        if (type == null) type = "customer";
        textUserType.setText(type);

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.placeholder_image).into(imageProfile);
        } else {
            imageProfile.setImageResource(R.drawable.placeholder_image);
        }

        if ("mover".equals(type)) {
            setupMoverUi(profile);
        } else {
            setupCustomerUi(profile);
        }
    }

    private void setupMoverUi(UserProfile profile) {
        labelAbout.setVisibility(View.VISIBLE);
        editAbout.setVisibility(View.VISIBLE);
        if (profile.getAbout() != null) editAbout.setText(profile.getAbout());

        // הסתרת שדות לקוח
        labelFromAddress.setVisibility(View.GONE); tvFromAddress.setVisibility(View.GONE); btnPickFromAddress.setVisibility(View.GONE);
        labelToAddress.setVisibility(View.GONE); tvToAddress.setVisibility(View.GONE); btnPickToAddress.setVisibility(View.GONE);
        labelMoveDate.setVisibility(View.GONE); tvMoveDate.setVisibility(View.GONE); btnPickMoveDate.setVisibility(View.GONE);
        labelFloor.setVisibility(View.GONE); editFloor.setVisibility(View.GONE);
        labelApartment.setVisibility(View.GONE); editApartment.setVisibility(View.GONE);

        String baseAddress = (profile.getDefaultFromAddress() != null) ? profile.getDefaultFromAddress() : "טרם הוגדר בסיס יציאה";

        labelMoverBaseAddress.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        tvMoverBaseAddress.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        tvMoverBaseAddress.setText(baseAddress);

        layoutMoverFields.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        tvSelectedMoverAddress.setText(baseAddress);

        int radius = profile.getServiceRadiusKm() > 0 ? profile.getServiceRadiusKm() : 30;
        sliderRadius.setValue(radius);
        tvRadiusLabel.setText("רדיוס שירות: " + radius + " ק״מ");

        btnPickMoverLocation.setEnabled(isEditMode);
        sliderRadius.setEnabled(isEditMode);
    }

    private void setupCustomerUi(UserProfile profile) {
        layoutMoverFields.setVisibility(View.GONE);
        labelMoverBaseAddress.setVisibility(View.GONE);
        tvMoverBaseAddress.setVisibility(View.GONE);
        labelAbout.setVisibility(View.GONE);
        editAbout.setVisibility(View.GONE);

        // כתובות
        labelFromAddress.setVisibility(View.VISIBLE); tvFromAddress.setVisibility(View.VISIBLE);
        labelToAddress.setVisibility(View.VISIBLE); tvToAddress.setVisibility(View.VISIBLE);

        tvFromAddress.setText(profile.getDefaultFromAddress() != null ? profile.getDefaultFromAddress() : "לא נבחרה כתובת");
        tvToAddress.setText(profile.getDefaultToAddress() != null ? profile.getDefaultToAddress() : "לא נבחרה כתובת");

        // תאריך הובלה
        labelMoveDate.setVisibility(View.VISIBLE); tvMoveDate.setVisibility(View.VISIBLE);

        // טעינת התאריך מהפרופיל למשתנה המקומי
        if (profile.getDefaultMoveDate() != null && profile.getDefaultMoveDate() > 0) {
            selectedMoveDate = profile.getDefaultMoveDate();
        } else {
            selectedMoveDate = 0;
        }
        updateMoveDateText();

        btnPickFromAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnPickToAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnPickMoveDate.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        labelFloor.setVisibility(View.VISIBLE); editFloor.setVisibility(View.VISIBLE);
        labelApartment.setVisibility(View.VISIBLE); editApartment.setVisibility(View.VISIBLE);

        editFloor.setEnabled(isEditMode);
        editApartment.setEnabled(isEditMode);

        editFloor.setText(profile.getFloor() != null ? String.valueOf(profile.getFloor()) : "");
        editApartment.setText(profile.getApartment() != null ? String.valueOf(profile.getApartment()) : "");
    }

    // --- לוגיקת בחירת מקומות ותאריך ---

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
        tvSelectedMoverAddress.setText(place.getAddress());
        currentUserProfile.setLat(place.getLatLng().latitude);
        currentUserProfile.setLng(place.getLatLng().longitude);
        currentUserProfile.setGeohash(GeoFireUtils.getGeoHashForLocation(new GeoLocation(place.getLatLng().latitude, place.getLatLng().longitude)));
        currentUserProfile.setDefaultFromAddress(place.getAddress());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedMoveDate > 0) c.setTimeInMillis(selectedMoveDate);

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            c.set(year, month, dayOfMonth, 0, 0, 0);
            selectedMoveDate = c.getTimeInMillis();
            updateMoveDateText();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateMoveDateText() {
        if (selectedMoveDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvMoveDate.setText(sdf.format(new Date(selectedMoveDate)));
        } else {
            tvMoveDate.setText("טרם נקבע תאריך");
        }
    }

    private Integer parseOptionalInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    // --- שמירה וביטול ---

    private void saveProfileFromUi() {
        if (currentUserProfile == null) currentUserProfile = new UserProfile();

        currentUserProfile.setName(editName.getText().toString().trim());
        currentUserProfile.setPhone(editPhone.getText().toString().trim());

        String type = currentUserProfile.getUserType() != null ? currentUserProfile.getUserType() : "customer";

        if ("mover".equals(type)) {
            currentUserProfile.setAbout(editAbout.getText().toString().trim());
        } else {
            currentUserProfile.setFloor(parseOptionalInt(editFloor.getText().toString()));
            currentUserProfile.setApartment(parseOptionalInt(editApartment.getText().toString()));

            // ✅ סנכרון: שמירת התאריך בפרופיל כדי שה-ViewModel יוכל לקחת אותו ולעדכן את ההובלה
            currentUserProfile.setDefaultMoveDate(selectedMoveDate);
        }

        // ✅ קריאה ל-ViewModel שדואג גם לשמירת הפרופיל וגם לעדכון ההובלה הפעילה!
        viewModel.saveMyProfile(currentUserProfile);

        Toast.makeText(getContext(), "הפרופיל נשמר", Toast.LENGTH_SHORT).show();
    }

    private void enterEditMode() {
        isEditMode = true;
        // שמירת ערכים ישנים לביטול
        oldName = editName.getText().toString();
        oldPhone = editPhone.getText().toString();
        oldAbout = editAbout.getText().toString();
        oldFromAddress = tvFromAddress.getText().toString();
        oldToAddress = tvToAddress.getText().toString();
        oldFloorStr = editFloor.getText().toString();
        oldApartmentStr = editApartment.getText().toString();
        oldMoveDate = selectedMoveDate;

        if (currentUserProfile != null && "mover".equals(currentUserProfile.getUserType())) {
            oldMoverBaseAddress = currentUserProfile.getDefaultFromAddress();
            oldLat = currentUserProfile.getLat();
            oldLng = currentUserProfile.getLng();
            oldGeohash = currentUserProfile.getGeohash();
            oldRadius = currentUserProfile.getServiceRadiusKm();
        }

        updateUiMode();
    }

    private void exitEditMode() {
        isEditMode = false;
        updateUiMode();
    }

    private void cancelEditMode() {
        isEditMode = false;
        // שחזור ערכים
        editName.setText(oldName);
        editPhone.setText(oldPhone);

        if (currentUserProfile != null) {
            String type = currentUserProfile.getUserType();
            if ("mover".equals(type)) {
                editAbout.setText(oldAbout);
                currentUserProfile.setDefaultFromAddress(oldMoverBaseAddress);
                currentUserProfile.setLat(oldLat); currentUserProfile.setLng(oldLng);
                currentUserProfile.setGeohash(oldGeohash);
                currentUserProfile.setServiceRadiusKm(oldRadius);
            } else {
                tvFromAddress.setText(oldFromAddress);
                tvToAddress.setText(oldToAddress);
                editFloor.setText(oldFloorStr);
                editApartment.setText(oldApartmentStr);

                selectedMoveDate = oldMoveDate;

                // חשוב: משחזרים גם לאובייקט הפרופיל כדי שלא יישמר מידע שגוי אם ילחצו שוב
                currentUserProfile.setDefaultFromAddress(oldFromAddress);
                currentUserProfile.setDefaultToAddress(oldToAddress);
                currentUserProfile.setDefaultMoveDate(oldMoveDate);
            }
        }

        fillUiFromProfile(currentUserProfile);
        updateUiMode();
    }

    private void updateUiMode() {
        // רענון נראות כפתורים ושדות
        editName.setEnabled(isEditMode);
        editPhone.setEnabled(isEditMode);

        buttonEdit.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
        layoutSaveCancel.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

        if (currentUserProfile != null) {
            fillUiFromProfile(currentUserProfile);
        }
    }
}