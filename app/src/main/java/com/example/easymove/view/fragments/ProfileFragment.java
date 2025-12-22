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
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository;
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

    private final MoveRepository moveRepository = new MoveRepository();

    private NestedScrollView profileScroll;

    private TextView labelFromAddress, labelToAddress;

    private ImageView imageProfile;
    private Button buttonChangeImage;
    private EditText editName;
    private EditText editPhone;
    private TextView textUserType;

    // כתובות לקוח
    private TextView tvFromAddress;
    private TextView tvToAddress;
    private Button btnPickFromAddress;
    private Button btnPickToAddress;

    // תאריך הובלה (לקוח)
    private TextView labelMoveDate, tvMoveDate;
    private Button btnPickMoveDate;

    // לקוח בלבד: קומה + דירה
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

    // מוביל בלבד - מצב עריכה
    private LinearLayout layoutMoverFields;
    private TextView tvSelectedMoverAddress;
    private Button btnPickMoverLocation;
    private TextView tvRadiusLabel;
    private Slider sliderRadius;

    // מוביל בלבד - מצב תצוגה
    private TextView labelMoverBaseAddress;
    private TextView tvMoverBaseAddress;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private enum AddressPickType { FROM, TO, MOVER }
    private AddressPickType currentPickType = AddressPickType.MOVER;

    private boolean isEditMode = false;

    // שמירת ערכים ישנים לביטול
    private String oldName, oldPhone, oldAbout, oldFromAddress, oldToAddress;
    private String oldFloorStr, oldApartmentStr;

    // תאריך הובלה לביטול
    private long oldMoveDate = 0;
    private long selectedMoveDate = 0;

    // מוביל - שמירת ערכים לביטול
    private String oldMoverBaseAddress, oldGeohash;
    private double oldLat, oldLng;
    private int oldRadius;

    // ההובלה הפעילה הנוכחית (כדי לעדכן אותה)
    private MoveRequest activeMove;

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
            saveProfileFromUi();   // כולל עדכון ההובלה הפעילה
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

                // בנוסף: טוענים הובלה פעילה כדי להציג תאריך + לאפשר עדכון
                loadActiveMoveForCustomerIfNeeded();
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

    private void loadActiveMoveForCustomerIfNeeded() {
        if (currentUserProfile == null) return;
        if (!"customer".equals(currentUserProfile.getUserType())) return;
        if (currentUserProfile.getUserId() == null) return;

        moveRepository.getCurrentActiveMove(currentUserProfile.getUserId())
                .addOnSuccessListener(move -> {
                    activeMove = move;
                    if (activeMove != null) {
                        selectedMoveDate = activeMove.getMoveDate();
                        updateMoveDateText();
                    } else {
                        selectedMoveDate = 0;
                        tvMoveDate.setText("טרם נקבע תאריך");
                    }
                });
    }

    private void fillUiFromProfile(@NonNull UserProfile profile) {
        if (profile.getName() != null) editName.setText(profile.getName());
        if (profile.getPhone() != null) editPhone.setText(profile.getPhone());

        String type = profile.getUserType();
        if (type == null) type = "customer";
        textUserType.setText(type);

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
            // מוביל: מסתירים שדות לקוח
            labelFromAddress.setVisibility(View.GONE);
            tvFromAddress.setVisibility(View.GONE);
            btnPickFromAddress.setVisibility(View.GONE);

            labelToAddress.setVisibility(View.GONE);
            tvToAddress.setVisibility(View.GONE);
            btnPickToAddress.setVisibility(View.GONE);

            // תאריך הובלה (לקוח) לא רלוונטי
            labelMoveDate.setVisibility(View.GONE);
            tvMoveDate.setVisibility(View.GONE);
            btnPickMoveDate.setVisibility(View.GONE);

            labelFloor.setVisibility(View.GONE);
            editFloor.setVisibility(View.GONE);
            labelApartment.setVisibility(View.GONE);
            editApartment.setVisibility(View.GONE);

            String baseAddress =
                    (profile.getDefaultFromAddress() != null && !profile.getDefaultFromAddress().isEmpty())
                            ? profile.getDefaultFromAddress()
                            : "טרם הוגדר בסיס יציאה";

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

        } else {
            // לקוח: מסתירים מוביל
            layoutMoverFields.setVisibility(View.GONE);
            labelMoverBaseAddress.setVisibility(View.GONE);
            tvMoverBaseAddress.setVisibility(View.GONE);

            // כתובות לקוח
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

            // תאריך הובלה
            labelMoveDate.setVisibility(View.VISIBLE);
            tvMoveDate.setVisibility(View.VISIBLE);
            btnPickMoveDate.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            updateMoveDateText();

            // כפתורי בחירה לכתובות רק בעריכה
            btnPickFromAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            btnPickToAddress.setVisibility(isEditMode ? View.VISIBLE : View.GONE);

            // קומה/דירה
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

        if (currentPickType != AddressPickType.MOVER) {
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

        double lat = place.getLatLng().latitude;
        double lng = place.getLatLng().longitude;

        tvSelectedMoverAddress.setText(address);

        String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));

        currentUserProfile.setLat(lat);
        currentUserProfile.setLng(lng);
        currentUserProfile.setGeohash(hash);

        currentUserProfile.setDefaultFromAddress(address);
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
        if (tvMoveDate == null) return;

        if (selectedMoveDate > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvMoveDate.setText(sdf.format(new Date(selectedMoveDate)));
        } else {
            tvMoveDate.setText("טרם נקבע תאריך");
        }
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
        } else {
            Integer floor = parseOptionalInt(editFloor.getText().toString());
            Integer apt = parseOptionalInt(editApartment.getText().toString());
            currentUserProfile.setFloor(floor);
            currentUserProfile.setApartment(apt);
        }

        viewModel.saveMyProfile(currentUserProfile);

        // עדכון ההובלה הפעילה מהאזור האישי (כתובות + תאריך)
        if ("customer".equals(currentUserProfile.getUserType()) && currentUserProfile.getUserId() != null) {
            moveRepository.getCurrentActiveMove(currentUserProfile.getUserId())
                    .addOnSuccessListener(move -> {
                        if (move == null) return;

                        String from = currentUserProfile.getDefaultFromAddress();
                        String to = currentUserProfile.getDefaultToAddress();
                        long date = selectedMoveDate;

                        moveRepository.updateMoveDetails(move.getId(), from, to, date)
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "שגיאה בעדכון ההובלה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    });
        }

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

        oldMoveDate = selectedMoveDate;

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
            btnPickMoveDate.setVisibility(View.VISIBLE);
        } else {
            btnPickFromAddress.setVisibility(View.GONE);
            btnPickToAddress.setVisibility(View.GONE);
            btnPickMoveDate.setVisibility(View.GONE);

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
        btnPickMoveDate.setVisibility(View.GONE);

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

        selectedMoveDate = oldMoveDate;
        updateMoveDateText();

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
                currentUserProfile.setFloor(parseOptionalInt(oldFloorStr));
                currentUserProfile.setApartment(parseOptionalInt(oldApartmentStr));
            }
        }

        exitEditMode();
    }
}
