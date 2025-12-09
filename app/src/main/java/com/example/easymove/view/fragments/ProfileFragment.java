package com.example.easymove.view.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.viewmodel.UserViewModel;

/**
 * ProfileFragment
 * ---------------
 * מסך "האזור האישי" – מציג ועריכת פרטי הפרופיל של המשתמש (לקוח/מוביל).
 * משתמש ב-UserViewModel בשביל לטעון ולשמור את הפרופיל, ולהעלות תמונת פרופיל.
 */
public class ProfileFragment extends Fragment {

    private UserViewModel viewModel;

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

    private Uri selectedImageUri = null;

    // לבחירת תמונה מהגלריה
    private ActivityResultLauncher<String> pickImageLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) ViewModel
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // 2) Views
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

        // 3) Image picker launcher
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                (ActivityResultCallback<Uri>) uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imageProfile.setImageURI(uri);
                        // מעלים את התמונה ל-Storage דרך ה-ViewModel
                        viewModel.uploadProfileImage(uri);
                    }
                }
        );

        // 4) Observers
        observeViewModel();

        // 5) Listeners
        buttonChangeImage.setOnClickListener(v -> {
            // פתיחת גלריה
            pickImageLauncher.launch("image/*");
        });

        buttonSave.setOnClickListener(v -> saveProfileFromUi());

        // 6) טוענים את הפרופיל כשנכנסים למסך
        viewModel.loadMyProfile();
    }

    private void observeViewModel() {
        // פרופיל
        viewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                fillUiFromProfile(profile);
            }
        });

        // שגיאות
        viewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                textError.setText(msg);
                textError.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            } else {
                textError.setText("");
                textError.setVisibility(View.GONE);
            }
        });

        // טעינה
        viewModel.getIsLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        });

        // URL של תמונה שהועלתה
        viewModel.getUploadedImageUrlLiveData().observe(getViewLifecycleOwner(), url -> {
            if (url != null) {
                UserProfile current = viewModel.getMyProfileLiveData().getValue();
                if (current != null) {
                    current.setProfileImageUrl(url);
                    viewModel.saveMyProfile(current);
                }
                // אם תרצי – כאן אפשר לטעון את התמונה מהאינטרנט עם Glide/Picasso
                // Glide.with(this).load(url).into(imageProfile);
            }
        });
    }

    private void fillUiFromProfile(@NonNull UserProfile profile) {
        if (profile.getName() != null) {
            editName.setText(profile.getName());
        }
        if (profile.getPhone() != null) {
            editPhone.setText(profile.getPhone());
        }
        if (profile.getUserType() != null) {
            textUserType.setText(profile.getUserType());
        }
        if (profile.getDefaultFromAddress() != null) {
            editFromAddress.setText(profile.getDefaultFromAddress());
        }
        if (profile.getDefaultToAddress() != null) {
            editToAddress.setText(profile.getDefaultToAddress());
        }
        if (profile.getAbout() != null) {
            editAbout.setText(profile.getAbout());
        }

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            // כאן אפשר להשתמש ב-Glide/Picasso אם תוסיפי תלות
            // Glide.with(this).load(profile.getProfileImageUrl()).into(imageProfile);
        }
    }

    private void saveProfileFromUi() {
        UserProfile current = viewModel.getMyProfileLiveData().getValue();
        if (current == null) {
            current = new UserProfile();
        }

        current.setName(editName.getText().toString().trim());
        current.setPhone(editPhone.getText().toString().trim());
        current.setDefaultFromAddress(editFromAddress.getText().toString().trim());
        current.setDefaultToAddress(editToAddress.getText().toString().trim());
        current.setAbout(editAbout.getText().toString().trim());

        viewModel.saveMyProfile(current);
    }
}
