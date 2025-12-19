package com.example.easymove.view.fragments;

import android.app.Activity;
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

import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private UserProfile currentUserProfile;

    private NestedScrollView profileScroll;

    private TextView labelFromAddress, labelToAddress;

    private ImageView imageProfile;
    private Button buttonChangeImage;
    private EditText editName;
    private EditText editPhone;
    private TextView textUserType;

    // כתובות לקוח (TextView) + כפתורי בחירה
    private TextView tvFromAddress;
    private TextView tvToAddress;
    private Button btnPickFromAddress;
    private Button btnPickToAddress;

    // לקוח בלבד: קומה + דירה (אופציונלי)
    private TextView labelFloor, labelApartment;
    private EditText editFloor, editApartment;

    // "אודות" (רק למוביל)
    private TextView labelAbout;
    private EditText editAbout;

    // מצב עריכה/שמירה/ביטול
    private Button buttonEdit;
    private LinearLayout layoutSaveCancel;
    private Button buttonSave;
    private Button buttonCancel;

    private TextView textError;
    private ProgressBar progressBar;

    // מוביל בלבד - מצב עריכה (הריבוע הירוק)
    private LinearLayout layoutMoverFields;
    private TextView tvSelectedMoverAddress;
    private Button btnPickMoverLocation;
    private TextView tvRadiusLabel;
    private Slider sliderRadius;

    // מוביל בלבד - מצב תצוגה (שדה רגיל)
    private TextView labelMoverBaseAddress;
    private TextView tvMoverBaseAddress;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private enum AddressPickType { FROM, TO, MOVER }
    private AddressPickType currentPickType = AddressPickType.MOVER;

    // מצב עריכה
    private boolean isEditMode = false;

    // שמירת ערכים ישנים בשביל "ביטול"
    private String oldName, oldPhone, oldAbout, oldFromAddress, oldToAddress;

    // לקוח - שמירת ערכים לביטול
    private String oldFloorStr, oldApartmentStr;

    // מוביל - שמירת ערכים לביטול (בסיס + מיקום + רדיוס)
    private String oldMoverBaseAddress, oldGeohash;
    private double oldLat, oldLng;
    private int oldRadius;

    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place == null) return;

                    if (currentPickType == AddressPickType.MOVER) {
                        updateMoverLocation(place);
                    } else if (currentPickType == AddressPickType.FROM) {
                        updateCustomerFromAddress(place);
                    } else if (currentPickType == AddressPickType.TO) {
                        updateCustomerToAddress(place);
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

        // לקוח: קומה + דירה
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

        // מצב תצוגה למוביל (שדה רגיל)
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

        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser) return;
            int radius = (int) value;
            tvRadiusLabel.setText("רדיוס שירות: " + radius + " ק״מ");
            if (currentUserProfile != null) {
                currentUserProfile.setServiceRadiusKm(radius);
            }
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

        // אודות - רק למוביל
        if ("mover".equals(type)) {
            labelAbout.setVisibility(View.VISIBLE);
            editAbout.setVisibility(View.VISIBLE);
            if (profile.getAbout() != null) editAbout.setText(profile.getAbout());
        } else {
            labelAbout.setVisibility(View.GONE);
            editAbout.setVisibility(View.GONE);
        }

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(imageProfile);
        } else {
            imageProfile.setImageResource(R.drawable.placeholder_image);
        }

        if ("mover".equals(type)) {
            // --- מוביל ---

            // לקוח - שדות לא רלוונטיים
            labelFromAddress.setVisibility(View.GONE);
            tvFromAddress.setVisibility(View.GONE);
            btnPickFromAddress.setVisibility(View.GONE);

            labelToAddress.setVisibility(View.GONE);
            tvToAddress.setVisibility(View.GONE);
            btnPickToAddress.setVisibility(View.GONE);

            // קומה/דירה - לא רלוונטי
            labelFloor.setVisibility(View.GONE);
            editFloor.setVisibility(View.GONE);
            labelApartment.setVisibility(View.GONE);
            editApartment.setVisibility(View.GONE);

            String baseAddress =
                    (profile.getDefaultFromAddress() != null && !profile.getDefaultFromAddress().isEmpty())
                            ? profile.getDefaultFromAddress()
                            : "טרם הוגדר בסיס יציאה";

            // מצב תצוגה (שדה רגיל)
            labelMoverBaseAddress.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
            tvMoverBaseAddress.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
            tvMoverBaseAddress.setText(baseAddress);

            // מצב עריכה (ריבוע ירוק) - רק בעריכה
            layoutMoverFields.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            tvSelectedMoverAddress.setText(baseAddress);

            int radius = profile.getServiceRadiusKm() > 0 ? profile.getServiceRadiusKm() : 30;
            sliderRadius.setValue(radius);
            tvRadiusLabel.setText("רדיוס שירות: " + radius + " ק״מ");

            btnPickMoverLocation.setEnabled(isEditMode);
            sliderRadius.setEnabled(isEditMode);

        } else {
            // --- לקוח ---

            // מוביל - לא מציגים
            layoutMoverFields.setVisibility(View.GONE);
            labelMoverBaseAddress.setVisibility(View.GONE);
            tvMoverBaseAddress.setVisibility(View.GONE);

            // כתובות לקוח - תמיד תצוגה
            labelFromAddress.setVisibility(View.VISIBLE);
            tvFromAddress.setVisibility(View.VISIBLE);

            labelToAddress.setVisibility(View.VISIBLE);
            tvToAddress.setVisibility(View.VISIBLE);

            tvFromAddress.setText(
                    profile.getDefaultFromAddress() != null && !profile.getDefaultFromAddress().isEmpty()
                            ? profile.getDefaultFromAddress()
                            : "לא נבחרה כתובת"
            );

            tvToAddress.setText(
                    profile.getDefaultToAddress() != null && !profile.getDefaultToAddress().isEmpty()
                            ? profile.getDefaultToAddress()
                            : "לא נבחרה כתובת"
            );

            // כפתורי בחירה - רק בעריכה
            btnPickFromAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            btnPickToAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            // קומה/דירה - רק ללקוח
            labelFloor.setVisibility(View.VISIBLE);
            editFloor.setVisibility(View.VISIBLE);
            labelApartment.setVisibility(View.VISIBLE);
            editApartment.setVisibility(View.VISIBLE);

            editFloor.setEnabled(isEditMode);
            editApartment.setEnabled(isEditMode);

            Integer floor = profile.getFloor();
            Integer apt = profile.getApartment();

            editFloor.setText(floor != null ? String.valueOf(floor) : "");
            editApartment.setText(apt != null ? String.valueOf(apt) : "");
        }
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
        );

        Autocomplete.IntentBuilder builder =
                new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .setCountry("IL");

        // לקוח: חייב כתובת מלאה (כולל מספר בית)
        // מוביל: לא חייב מספר בית (יכול לבחור רק עיר/אזור)
        if (currentPickType == AddressPickType.MOVER) {
            // אין TypeFilter.ADDRESS כדי לא לחסום בחירה של עיר בלבד
            // (ה־Places יאפשר גם כתובת מלאה וגם מקום כללי)
        } else {
            builder.setTypeFilter(TypeFilter.ADDRESS);
        }

        placePickerLauncher.launch(builder.build(requireContext()));
    }

    private boolean hasStreetNumber(@Nullable Place place) {
        if (place == null) return false;
        AddressComponents comps = place.getAddressComponents();
        if (comps == null) return false;

        for (AddressComponent c : comps.asList()) {
            if (c.getTypes() != null && c.getTypes().contains("street_number")) {
                return true;
            }
        }
        return false;
    }

    private void updateCustomerFromAddress(Place place) {
        if (currentUserProfile == null) return;

        String address = place.getAddress();
        if (address == null || address.isEmpty()) return;

        // לקוח: חובה מספר בית
        if (!hasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }

        tvFromAddress.setText(address);
        currentUserProfile.setDefaultFromAddress(address);
    }

    private void updateCustomerToAddress(Place place) {
        if (currentUserProfile == null) return;

        String address = place.getAddress();
        if (address == null || address.isEmpty()) return;

        // לקוח: חובה מספר בית
        if (!hasStreetNumber(place)) {
            Toast.makeText(getContext(), "בחרי כתובת מלאה עם מספר בית", Toast.LENGTH_SHORT).show();
            return;
        }

        tvToAddress.setText(address);
        currentUserProfile.setDefaultToAddress(address);
    }

    private void updateMoverLocation(Place place) {
        if (place.getLatLng() == null || currentUserProfile == null) return;

        String address = place.getAddress();
        if (address == null || address.isEmpty()) return;

        // מוביל: לא דורשים מספר בית ✅
        double lat = place.getLatLng().latitude;
        double lng = place.getLatLng().longitude;

        tvSelectedMoverAddress.setText(address);

        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));

        currentUserProfile.setLat(lat);
        currentUserProfile.setLng(lng);
        currentUserProfile.setGeohash(hash);

        // אצל מוביל: בסיס יציאה נשמר ב-defaultFromAddress
        currentUserProfile.setDefaultFromAddress(address);
    }

    private Integer parseOptionalInt(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveProfileFromUi() {
        if (currentUserProfile == null) currentUserProfile = new UserProfile();

        String type = currentUserProfile.getUserType() != null ? currentUserProfile.getUserType() : "customer";

        currentUserProfile.setName(editName.getText().toString().trim());
        currentUserProfile.setPhone(editPhone.getText().toString().trim());

        if ("mover".equals(type)) {
            currentUserProfile.setAbout(editAbout.getText().toString().trim());
            // בסיס/lat/lng/geohash כבר מתעדכנים בזמן הבחירה
            // רדיוס מתעדכן בסליידר
        } else {
            // לקוח: שדות אופציונליים
            Integer floor = parseOptionalInt(editFloor.getText().toString());
            Integer apt = parseOptionalInt(editApartment.getText().toString());
            currentUserProfile.setFloor(floor);
            currentUserProfile.setApartment(apt);
        }

        viewModel.saveMyProfile(currentUserProfile);
        Toast.makeText(getContext(), "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();

        fillUiFromProfile(currentUserProfile);
    }

    private void enterEditMode() {
        isEditMode = true;

        oldName = editName.getText().toString();
        oldPhone = editPhone.getText().toString();
        oldAbout = editAbout.getText().toString();
        oldFromAddress = tvFromAddress.getText().toString();
        oldToAddress = tvToAddress.getText().toString();
        oldFloorStr = editFloor.getText().toString();
        oldApartmentStr = editApartment.getText().toString();

        if (currentUserProfile != null && "mover".equals(currentUserProfile.getUserType())) {
            oldMoverBaseAddress = currentUserProfile.getDefaultFromAddress();
            oldLat = currentUserProfile.getLat();
            oldLng = currentUserProfile.getLng();
            oldGeohash = currentUserProfile.getGeohash();
            oldRadius = currentUserProfile.getServiceRadiusKm();
        }

        editName.setEnabled(true);
        editPhone.setEnabled(true);

        String type = currentUserProfile != null ? currentUserProfile.getUserType() : "customer";

        if ("mover".equals(type)) {
            editAbout.setEnabled(true);
        } else {
            editAbout.setEnabled(false);
            editFloor.setEnabled(true);
            editApartment.setEnabled(true);
        }

        buttonEdit.setVisibility(View.GONE);
        layoutSaveCancel.setVisibility(View.VISIBLE);

        if (!"mover".equals(type)) {
            btnPickFromAddress.setVisibility(View.VISIBLE);
            btnPickToAddress.setVisibility(View.VISIBLE);
        } else {
            btnPickFromAddress.setVisibility(View.GONE);
            btnPickToAddress.setVisibility(View.GONE);

            btnPickMoverLocation.setEnabled(true);
            sliderRadius.setEnabled(true);
        }

        if (currentUserProfile != null) {
            fillUiFromProfile(currentUserProfile);
        }
    }

    private void exitEditMode() {
        isEditMode = false;

        editName.setEnabled(false);
        editPhone.setEnabled(false);
        editAbout.setEnabled(false);

        editFloor.setEnabled(false);
        editApartment.setEnabled(false);

        buttonEdit.setVisibility(View.VISIBLE);
        layoutSaveCancel.setVisibility(View.GONE);

        btnPickFromAddress.setVisibility(View.GONE);
        btnPickToAddress.setVisibility(View.GONE);

        String type = currentUserProfile != null ? currentUserProfile.getUserType() : "customer";
        if ("mover".equals(type)) {
            btnPickMoverLocation.setEnabled(false);
            sliderRadius.setEnabled(false);
        }

        if (currentUserProfile != null) {
            fillUiFromProfile(currentUserProfile);
        }
    }

    private void cancelEditMode() {
        editName.setText(oldName);
        editPhone.setText(oldPhone);
        editAbout.setText(oldAbout);
        tvFromAddress.setText(oldFromAddress);
        tvToAddress.setText(oldToAddress);
        editFloor.setText(oldFloorStr);
        editApartment.setText(oldApartmentStr);

        if (currentUserProfile != null) {
            currentUserProfile.setName(oldName);
            currentUserProfile.setPhone(oldPhone);

            String type = currentUserProfile.getUserType() != null ? currentUserProfile.getUserType() : "customer";

            if ("mover".equals(type)) {
                currentUserProfile.setAbout(oldAbout);

                currentUserProfile.setDefaultFromAddress(oldMoverBaseAddress);
                currentUserProfile.setLat(oldLat);
                currentUserProfile.setLng(oldLng);
                currentUserProfile.setGeohash(oldGeohash);
                currentUserProfile.setServiceRadiusKm(oldRadius > 0 ? oldRadius : 30);

                String base =
                        (oldMoverBaseAddress != null && !oldMoverBaseAddress.isEmpty())
                                ? oldMoverBaseAddress
                                : "טרם הוגדר בסיס יציאה";

                tvSelectedMoverAddress.setText(base);
                tvMoverBaseAddress.setText(base);
                sliderRadius.setValue(currentUserProfile.getServiceRadiusKm());
                tvRadiusLabel.setText("רדיוס שירות: " + currentUserProfile.getServiceRadiusKm() + " ק״מ");
            } else {
                currentUserProfile.setDefaultFromAddress(oldFromAddress);
                currentUserProfile.setDefaultToAddress(oldToAddress);

                // מחזירים גם במודל את הערכים האופציונליים
                currentUserProfile.setFloor(parseOptionalInt(oldFloorStr));
                currentUserProfile.setApartment(parseOptionalInt(oldApartmentStr));
            }
        }

        exitEditMode();
    }
}
