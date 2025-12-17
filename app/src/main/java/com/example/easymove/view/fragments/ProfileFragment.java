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
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.slider.Slider;

import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;
    private UserProfile currentUserProfile; // אובייקט עזר לשמירת השינויים לפני השמירה בשרת

    private TextView labelFromAddress, labelToAddress;

    // רכיבי UI כלליים
    private ImageView imageProfile;
    private Button buttonChangeImage;
    private EditText editName;
    private EditText editPhone;
    private TextView textUserType;
    private EditText editFromAddress;
    private EditText editToAddress;
    private EditText editAbout;
    private Button buttonSave;
    private TextView textError;
    private ProgressBar progressBar;

    // --- חדש: רכיבים למוביל בלבד ---
    private LinearLayout layoutMoverFields;
    private TextView tvSelectedMoverAddress;
    private Button btnPickMoverLocation;
    private TextView tvRadiusLabel;
    private Slider sliderRadius;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    // משגר לבחירת כתובת (Google Places)
    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    updateMoverLocation(place);
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

        // אתחול Google Places (אם לא אותחל)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        initViews(view);
        setupListeners();
        observeViewModel();

        viewModel.loadMyProfile();
    }

    private void initViews(View view) {
        imageProfile = view.findViewById(R.id.image_profile);
        buttonChangeImage = view.findViewById(R.id.button_change_image);
        editName = view.findViewById(R.id.edit_name);
        editPhone = view.findViewById(R.id.edit_phone);
        textUserType = view.findViewById(R.id.text_user_type);
        editFromAddress = view.findViewById(R.id.edit_from_address);
        editToAddress = view.findViewById(R.id.edit_to_address);
        editAbout = view.findViewById(R.id.edit_about);
        buttonSave = view.findViewById(R.id.button_save_profile);
        textError = view.findViewById(R.id.text_error);
        progressBar = view.findViewById(R.id.progress_loading);
        labelFromAddress = view.findViewById(R.id.label_from_address);
        labelToAddress = view.findViewById(R.id.label_to_address);

        // רכיבי מוביל
        layoutMoverFields = view.findViewById(R.id.layoutMoverFields);
        tvSelectedMoverAddress = view.findViewById(R.id.tvSelectedMoverAddress);
        btnPickMoverLocation = view.findViewById(R.id.btnPickMoverLocation);
        tvRadiusLabel = view.findViewById(R.id.tvRadiusLabel);
        sliderRadius = view.findViewById(R.id.sliderRadius);
    }

    private void setupListeners() {
        // משגר תמונה
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
        buttonSave.setOnClickListener(v -> saveProfileFromUi());

        // כפתור בחירת מיקום למוביל
        btnPickMoverLocation.setOnClickListener(v -> openPlacePicker());

        // האזנה לשינוי ברדיוס
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
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
                this.currentUserProfile = profile; // שמירת רפרנס מקומי
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

        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
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
        textUserType.setText(type != null ? type : "לא מוגדר");

        if (profile.getAbout() != null) editAbout.setText(profile.getAbout());

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(profile.getProfileImageUrl()).placeholder(R.drawable.placeholder_image).into(imageProfile);
        }

        if ("mover".equals(type)) {
            // --- מצב מוביל ---
            layoutMoverFields.setVisibility(View.VISIBLE);

            // הסתרת שדות של לקוח
            labelFromAddress.setVisibility(View.GONE);
            editFromAddress.setVisibility(View.GONE);
            labelToAddress.setVisibility(View.GONE);
            editToAddress.setVisibility(View.GONE);

            // מילוי שדות מוביל
            if (profile.getDefaultFromAddress() != null && !profile.getDefaultFromAddress().isEmpty()) {
                tvSelectedMoverAddress.setText(profile.getDefaultFromAddress());
            } else {
                tvSelectedMoverAddress.setText("טרם הוגדר בסיס יציאה");
            }
            int radius = profile.getServiceRadiusKm() > 0 ? profile.getServiceRadiusKm() : 30;
            sliderRadius.setValue(radius);
            tvRadiusLabel.setText("רדיוס שירות: " + radius + " ק״מ");

        } else {
            // --- מצב לקוח ---
            layoutMoverFields.setVisibility(View.GONE);

            // הצגת שדות של לקוח
            labelFromAddress.setVisibility(View.VISIBLE);
            editFromAddress.setVisibility(View.VISIBLE);
            labelToAddress.setVisibility(View.VISIBLE);
            editToAddress.setVisibility(View.VISIBLE);

            if (profile.getDefaultFromAddress() != null) editFromAddress.setText(profile.getDefaultFromAddress());
            if (profile.getDefaultToAddress() != null) editToAddress.setText(profile.getDefaultToAddress());
        }
    }

    private void openPlacePicker() {
        // בחירת מיקום עם Lat/Lng וכתובת
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL") // אופציונלי: הגבלה לישראל
                .build(requireContext());
        placePickerLauncher.launch(intent);
    }

    private void updateMoverLocation(Place place) {
        if (place.getLatLng() == null || currentUserProfile == null) return;

        double lat = place.getLatLng().latitude;
        double lng = place.getLatLng().longitude;
        String address = place.getAddress();

        // עדכון UI
        tvSelectedMoverAddress.setText(address);
        editFromAddress.setText(address); // מעדכן גם את ה-EditText הראשי שיהיה נוח

        // 1. חישוב GeoHash
        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));

        // 2. עדכון המודל המקומי
        currentUserProfile.setLat(lat);
        currentUserProfile.setLng(lng);
        currentUserProfile.setGeohash(hash);
        currentUserProfile.setDefaultFromAddress(address);
    }

    private void saveProfileFromUi() {
        if (currentUserProfile == null) currentUserProfile = new UserProfile();

        currentUserProfile.setName(editName.getText().toString().trim());
        currentUserProfile.setPhone(editPhone.getText().toString().trim());
        currentUserProfile.setDefaultFromAddress(editFromAddress.getText().toString().trim());
        currentUserProfile.setDefaultToAddress(editToAddress.getText().toString().trim());
        currentUserProfile.setAbout(editAbout.getText().toString().trim());

        // הרדיוס כבר מתעדכן בזמן אמת דרך ה-Listener של הסליידר,
        // המיקום מתעדכן בפונקציה updateMoverLocation.

        viewModel.saveMyProfile(currentUserProfile);
        Toast.makeText(getContext(), "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
    }
}