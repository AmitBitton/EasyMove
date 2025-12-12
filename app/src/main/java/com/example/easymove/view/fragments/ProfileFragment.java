package com.example.easymove.view.fragments;

import android.app.AlertDialog;
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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;

    // רכיבי UI קיימים
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
    private Button btnSelectAreas;
    private TextView textSelectedAreas;

    // רשימת האזורים האפשריים (חייב להיות תואם לחיפוש!)
    private final String[] availableAreas = {"מרכז", "השרון", "צפון", "דרום", "ירושלים", "שפלה", "יהודה ושומרון"};
    private boolean[] checkedAreas; // שומר את הבחירות בדיאלוג
    private List<String> selectedAreasList = new ArrayList<>(); // הרשימה הסופית לשמירה

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

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

        // אתחול מערך הבחירות
        checkedAreas = new boolean[availableAreas.length];

        initViews(view);

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

        // מאזינים
        buttonChangeImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        buttonSave.setOnClickListener(v -> saveProfileFromUi());

        // --- חדש: לחיצה על בחירת אזורים ---
        btnSelectAreas.setOnClickListener(v -> showAreaSelectionDialog());

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

        // רכיבי מוביל
        layoutMoverFields = view.findViewById(R.id.layoutMoverFields);
        btnSelectAreas = view.findViewById(R.id.btnSelectAreas);
        textSelectedAreas = view.findViewById(R.id.textSelectedAreas);
    }

    private void observeViewModel() {
        viewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
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
            if (url != null) {
                UserProfile current = viewModel.getMyProfileLiveData().getValue();
                if (current != null) {
                    current.setProfileImageUrl(url);
                    viewModel.saveMyProfile(current);
                }
            }
        });
    }

    private void fillUiFromProfile(@NonNull UserProfile profile) {
        if (profile.getName() != null) editName.setText(profile.getName());
        if (profile.getPhone() != null) editPhone.setText(profile.getPhone());

        String type = profile.getUserType();
        textUserType.setText(type != null ? type : "לא מוגדר");

        if (profile.getDefaultFromAddress() != null) editFromAddress.setText(profile.getDefaultFromAddress());
        if (profile.getDefaultToAddress() != null) editToAddress.setText(profile.getDefaultToAddress());
        if (profile.getAbout() != null) editAbout.setText(profile.getAbout());

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(this).load(profile.getProfileImageUrl()).placeholder(R.drawable.placeholder_image).into(imageProfile);
        }

        // --- לוגיקה ייחודית למוביל ---
        if ("mover".equals(type)) {
            layoutMoverFields.setVisibility(View.VISIBLE);

            // טעינת אזורים שכבר שמורים ב-Firebase
            if (profile.getServiceAreas() != null) {
                selectedAreasList = new ArrayList<>(profile.getServiceAreas());
                updateSelectedAreasText();

                // סנכרון הצ'קבוקסים לדיאלוג
                for (int i = 0; i < availableAreas.length; i++) {
                    checkedAreas[i] = selectedAreasList.contains(availableAreas[i]);
                }
            }
        } else {
            layoutMoverFields.setVisibility(View.GONE);
        }
    }

    // --- חדש: דיאלוג בחירת אזורים ---
    private void showAreaSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("בחר אזורי שירות");
        builder.setMultiChoiceItems(availableAreas, checkedAreas, (dialog, which, isChecked) -> {
            if (isChecked) {
                // הוספה לרשימה אם לא קיים
                if (!selectedAreasList.contains(availableAreas[which])) {
                    selectedAreasList.add(availableAreas[which]);
                }
            } else {
                // הסרה מהרשימה
                selectedAreasList.remove(availableAreas[which]);
            }
        });

        builder.setPositiveButton("אישור", (dialog, which) -> updateSelectedAreasText());
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void updateSelectedAreasText() {
        if (selectedAreasList.isEmpty()) {
            textSelectedAreas.setText("לא נבחרו אזורים");
        } else {
            textSelectedAreas.setText(String.join(", ", selectedAreasList));
        }
    }

    private void saveProfileFromUi() {
        UserProfile current = viewModel.getMyProfileLiveData().getValue();
        if (current == null) current = new UserProfile();

        current.setName(editName.getText().toString().trim());
        current.setPhone(editPhone.getText().toString().trim());
        current.setDefaultFromAddress(editFromAddress.getText().toString().trim());
        current.setDefaultToAddress(editToAddress.getText().toString().trim());
        current.setAbout(editAbout.getText().toString().trim());

        // שמירת אזורים למוביל
        if ("mover".equals(current.getUserType())) {
            current.setServiceAreas(selectedAreasList);
        }

        viewModel.saveMyProfile(current);
        Toast.makeText(getContext(), "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
    }
}