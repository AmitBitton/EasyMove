package com.example.easymove.view.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.R;
import com.example.easymove.viewmodel.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Fragment responsible for Application Settings.
 * Features:
 * 1. Notification Toggle (integrates with Android 13+ Permissions).
 * 2. Contact Support (Email Intent).
 * 3. Privacy Policy (Placeholder).
 */
public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private SwitchMaterial switchNotifications;

    /**
     * Launcher for requesting Notification Permission (Android 13 / API 33+).
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.setNotificationsEnabled(true);
                } else {
                    showSettingsSnackbar();
                    // Revert switch visually without triggering listener
                    setSwitchCheckedSilent(switchNotifications, false);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // 2. Initialize UI Views
        switchNotifications = view.findViewById(R.id.switchNotifications);
        View btnContact = view.findViewById(R.id.btnContactSupport);
        View btnPrivacy = view.findViewById(R.id.btnPrivacyPolicy);

        // 3. Check Initial Status (Syncs DB state with System Permission state)
        viewModel.checkNotificationStatus();

        // 4. Setup Observers & Listeners
        observeViewModel();
        setupListeners(btnContact, btnPrivacy);
    }

    private void observeViewModel() {
        // Observer: Update Switch UI when DB state changes
        viewModel.getIsNotificationEnabled().observe(getViewLifecycleOwner(), isEnabled ->
                setSwitchCheckedSilent(switchNotifications, isEnabled));

        // Observer: Show Toast messages
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners(View btnContact, View btnPrivacy) {
        // Notification Switch Listener
        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener(this::onNotificationSwitchChanged);
        }

        // "Contact Support" Button
        if (btnContact != null) {
            btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:support@easymove.com"));
                intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה בנושא EasyMove");
                try {
                    startActivity(Intent.createChooser(intent, "שלח מייל..."));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // "Privacy Policy" Button
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v ->
                    Toast.makeText(requireContext(), "מדיניות פרטיות תוצג כאן בעתיד", Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * Handles user interaction with the Notification Switch.
     */
    private void onNotificationSwitchChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            checkPermissionAndEnable();
        } else {
            viewModel.setNotificationsEnabled(false);
        }
    }

    /**
     * Checks for runtime permissions (Android 13+) before enabling notifications in the DB.
     */
    private void checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                viewModel.setNotificationsEnabled(true);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android 12 and below do not require runtime permission for notifications
            viewModel.setNotificationsEnabled(true);
        }
    }

    /**
     * Helper to programmatically change the Switch state WITHOUT triggering the OnCheckedChangeListener.
     * This prevents infinite loops between the LiveData Observer and the UI Listener.
     */
    private void setSwitchCheckedSilent(SwitchMaterial switchView, boolean isChecked) {
        if (switchView == null) return;

        // Detach listener
        switchView.setOnCheckedChangeListener(null);

        // Change state
        switchView.setChecked(isChecked);

        // Re-attach listener
        if (switchView == switchNotifications) {
            switchView.setOnCheckedChangeListener(this::onNotificationSwitchChanged);
        }
    }

    /**
     * Shows a Snackbar directing the user to System Settings if they permanently denied permissions.
     */
    private void showSettingsSnackbar() {
        Snackbar.make(requireView(), "ההתראות חסומות. יש לאפשר אותן בהגדרות.", Snackbar.LENGTH_LONG)
                .setAction("הגדרות", v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-check status when returning to the app (e.g., after changing settings in OS)
        if (viewModel != null) {
            viewModel.checkNotificationStatus();
        }
    }
}