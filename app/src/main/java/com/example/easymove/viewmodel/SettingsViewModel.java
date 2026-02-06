package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * ViewModel responsible for Application Settings.
 * Handles:
 * 1. Checking if Push Notifications are currently enabled for the user.
 * 2. Enabling Notifications (Fetching FCM Token -> Saving to DB).
 * 3. Disabling Notifications (Removing FCM Token from DB).
 */
public class SettingsViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();

    // UI State
    private final MutableLiveData<Boolean> isNotificationEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // --- Getters ---
    public LiveData<Boolean> getIsNotificationEnabled() { return isNotificationEnabled; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /**
     * Checks the database to see if the current user has a valid FCM token registered.
     */
    public void checkNotificationStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRepository.isNotificationEnabled(uid)
                .addOnSuccessListener(isNotificationEnabled::setValue)
                .addOnFailureListener(e -> isNotificationEnabled.setValue(false));
    }

    /**
     * Toggles notifications on or off.
     * * @param enabled True to enable (fetch and save token), False to disable (delete token).
     */
    public void setNotificationsEnabled(boolean enabled) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (enabled) {
            enableNotifications(uid);
        } else {
            disableNotifications(uid);
        }
    }

    private void enableNotifications(String uid) {
        // 1. Get a fresh FCM Token from the device
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    // 2. Save token to Firestore via Repository
                    userRepository.updateFcmToken(uid, token)
                            .addOnSuccessListener(aVoid -> {
                                toastMessage.setValue("התראות הופעלו");
                                isNotificationEnabled.setValue(true);
                            })
                            .addOnFailureListener(e -> {
                                toastMessage.setValue("שגיאה בשמירת הגדרות");
                                // Revert switch visually
                                isNotificationEnabled.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בקבלת מזהה מכשיר");
                    // Revert switch visually
                    isNotificationEnabled.setValue(false);
                });
    }

    private void disableNotifications(String uid) {
        // Remove the token field from the user document
        userRepository.removeFcmToken(uid)
                .addOnSuccessListener(aVoid -> {
                    toastMessage.setValue("התראות כובו");
                    isNotificationEnabled.setValue(false);
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בכיבוי התראות");
                    // Revert switch visually (it failed to turn off)
                    isNotificationEnabled.setValue(true);
                });
    }
}