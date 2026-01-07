package com.example.easymove.view.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.R;
import com.example.easymove.viewmodel.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel; // ✅ שימוש ב-ViewModel
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchDarkMode;
    private SharedPreferences sharedPref;

    // לאנצ'ר לבקשת הרשאה (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.setNotificationsEnabled(true);
                } else {
                    showSettingsSnackbar();
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

        // 1. אתחול ViewModel
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        sharedPref = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        // 2. חיבור ל-XML
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        View btnContact = view.findViewById(R.id.btnContactSupport);
        View btnPrivacy = view.findViewById(R.id.btnPrivacyPolicy);

        // 3. טעינת מצב קיים
        loadNightModeState();

        // בדיקת מצב התראות מה-ViewModel (הוא יבדוק מול ה-DB)
        viewModel.checkNotificationStatus();

        // 4. האזנה לשינויים מה-ViewModel
        observeViewModel();

        // 5. הגדרת מאזינים לכפתורים
        setupListeners(btnContact, btnPrivacy);
    }

    private void observeViewModel() {
        // עדכון המתג כשהמידע מגיע מהמסד
        viewModel.getIsNotificationEnabled().observe(getViewLifecycleOwner(), isEnabled -> {
            setSwitchCheckedSilent(switchNotifications, isEnabled);
        });

        // הצגת הודעות טוסט
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupListeners(View btnContact, View btnPrivacy) {
        // מצב לילה
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
            sharedPref.edit().putBoolean("isDarkMode", isChecked).apply();
        });

        // התראות
        switchNotifications.setOnCheckedChangeListener(this::onNotificationSwitchChanged);

        // צור קשר
        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@easymove.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה בנושא EasyMove");
            try {
                startActivity(Intent.createChooser(intent, "שלח מייל..."));
            } catch (Exception e) {
                Toast.makeText(getContext(), "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
            }
        });

        // מדיניות פרטיות
        btnPrivacy.setOnClickListener(v ->
                Toast.makeText(getContext(), "מדיניות פרטיות תוצג כאן בעתיד", Toast.LENGTH_SHORT).show()
        );
    }

    private void onNotificationSwitchChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            checkPermissionAndEnable();
        } else {
            viewModel.setNotificationsEnabled(false);
        }
    }

    private void checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                viewModel.setNotificationsEnabled(true);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            viewModel.setNotificationsEnabled(true);
        }
    }

    private void loadNightModeState() {
        if (switchDarkMode == null) return;
        boolean isDark = sharedPref.getBoolean("isDarkMode", false);
        switchDarkMode.setChecked(isDark);
    }

    private void setSwitchCheckedSilent(SwitchMaterial switchView, boolean isChecked) {
        if (switchView == null) return;
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(isChecked);
        if (switchView == switchNotifications) {
            switchView.setOnCheckedChangeListener(this::onNotificationSwitchChanged);
        }
    }

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
        // בדיקה חוזרת כשחוזרים למסך (אולי שינו הרשאות בהגדרות)
        viewModel.checkNotificationStatus();
    }
}