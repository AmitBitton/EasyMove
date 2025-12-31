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

import com.example.easymove.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SettingsFragment
 * ----------------
 * Manages application settings related to:
 * - Push notifications (Firebase Cloud Messaging)
 * - Dark mode (Day/Night theme)
 * <p>
 * Features:
 * - Runtime notification permission handling (Android 13+)
 * - Persisted preferences using SharedPreferences
 * - FCM token synchronization with Firestore
 * - Redirect to system settings when permission is permanently denied
 */
public class SettingsFragment extends Fragment {

    /** Switch for enabling/disabling notifications */
    private SwitchMaterial switchNotifications;

    /** Switch for enabling/disabling dark mode */
    private SwitchMaterial switchDarkMode;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** UID of the currently logged-in user */
    private String currentUserId;

    /** SharedPreferences for storing local app settings */
    private SharedPreferences sharedPref;

    /**
     * Activity Result launcher for requesting POST_NOTIFICATIONS permission.
     * Handles user response asynchronously.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            // Permission granted → enable notifications
                            uploadTokenToDatabase();
                        } else {
                            // Permission denied → show explanation and revert switch
                            showSettingsSnackbar();
                            if (switchNotifications != null) {
                                switchNotifications.post(() ->
                                        setSwitchCheckedSilent(
                                                switchNotifications,
                                                false
                                        )
                                );
                            }
                        }
                    }
            );

    /**
     * Inflates the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_settings,
                container,
                false
        );
    }

    /**
     * Called after the fragment view is created.
     * Initializes Firebase, views, stored preferences, and listeners.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Firebase Initialization ----
        db = FirebaseFirestore.getInstance();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance()
                        .getCurrentUser())
                .getUid();

        // ---- SharedPreferences ----
        sharedPref = requireContext()
                .getSharedPreferences("AppSettings", Context.MODE_PRIVATE);

        // ---- View Binding ----
        switchNotifications = view.findViewById(R.id.switchNotifications);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);

        // ---- Load Saved States ----
        loadNightModeState();
        loadNotificationState();

        // ---- Dark Mode Switch Listener ----
        switchDarkMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    if (isChecked) {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_YES
                        );
                        sharedPref.edit()
                                .putBoolean("isDarkMode", true)
                                .apply();
                    } else {
                        AppCompatDelegate.setDefaultNightMode(
                                AppCompatDelegate.MODE_NIGHT_NO
                        );
                        sharedPref.edit()
                                .putBoolean("isDarkMode", false)
                                .apply();
                    }
                }
        );

        // ---- Notifications Switch Listener ----
        switchNotifications.setOnCheckedChangeListener(
                this::onNotificationSwitchChanged
        );
    }

    /**
     * Reload notification state when returning from system settings.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadNotificationState();
    }

    // ------------------------------------------------------------------------
    // Night Mode Logic
    // ------------------------------------------------------------------------

    /**
     * Loads and applies the saved dark mode preference.
     */
    private void loadNightModeState() {
        if (switchDarkMode == null) return;

        boolean isDark =
                sharedPref.getBoolean("isDarkMode", false);
        switchDarkMode.setChecked(isDark);
    }

    // ------------------------------------------------------------------------
    // Notification Logic
    // ------------------------------------------------------------------------

    /**
     * Loads the notification state based on:
     * 1. System permission (Android 13+)
     * 2. Presence of FCM token in Firestore
     */
    private void loadNotificationState() {
        if (switchNotifications == null) return;

        // ---- System Permission Check ----
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                setSwitchCheckedSilent(
                        switchNotifications,
                        false
                );
                disableNotifications();
                return;
            }
        }

        // ---- Firestore Token Check ----
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String token =
                                doc.getString("fcmToken");
                        boolean enabled =
                                token != null && !token.isEmpty();
                        setSwitchCheckedSilent(
                                switchNotifications,
                                enabled
                        );
                    }
                });
    }

    /**
     * Updates a switch state without triggering its listener.
     * Prevents infinite loops when changing switch programmatically.
     */
    private void setSwitchCheckedSilent(
            SwitchMaterial switchView,
            boolean isChecked
    ) {
        if (switchView == null) return;

        if (switchView == switchNotifications) {
            switchView.setOnCheckedChangeListener(null);
            switchView.setChecked(isChecked);
            switchView.setOnCheckedChangeListener(
                    this::onNotificationSwitchChanged
            );
        } else {
            switchView.setChecked(isChecked);
        }
    }

    /**
     * Main listener for notification switch changes.
     */
    private void onNotificationSwitchChanged(
            CompoundButton buttonView,
            boolean isChecked
    ) {
        sharedPref.edit()
                .putBoolean("notifications_enabled", isChecked)
                .apply();

        if (isChecked) {
            checkPermissionAndEnable();
        } else {
            disableNotifications();
        }
    }

    /**
     * Checks notification permission and enables notifications if allowed.
     */
    private void checkPermissionAndEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {

                uploadTokenToDatabase();
            } else {
                requestPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        } else {
            uploadTokenToDatabase();
        }
    }

    /**
     * Retrieves FCM token and uploads it to Firestore.
     */
    private void uploadTokenToDatabase() {
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token ->
                        db.collection("users")
                                .document(currentUserId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(
                                                getContext(),
                                                "התראות הופעלו",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                )
                                .addOnFailureListener(e -> {
                                    Toast.makeText(
                                            getContext(),
                                            "שגיאה",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    setSwitchCheckedSilent(
                                            switchNotifications,
                                            false
                                    );
                                })
                );
    }

    /**
     * Disables notifications by removing the FCM token from Firestore.
     */
    private void disableNotifications() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", FieldValue.delete());

        db.collection("users")
                .document(currentUserId)
                .update(updates);
    }

    // ------------------------------------------------------------------------
    // Permission Denied Handling
    // ------------------------------------------------------------------------

    /**
     * Shows a Snackbar guiding the user to system settings
     * when notification permission is permanently denied.
     */
    private void showSettingsSnackbar() {
        Snackbar.make(
                        requireView(),
                        "ההתראות חסומות. יש לאפשר אותן בהגדרות.",
                        Snackbar.LENGTH_LONG
                )
                .setAction(
                        "הגדרות",
                        v -> {
                            Intent intent =
                                    new Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    );
                            Uri uri = Uri.fromParts(
                                    "package",
                                    requireContext().getPackageName(),
                                    null
                            );
                            intent.setData(uri);
                            startActivity(intent);
                        }
                )
                .show();
    }
}
