package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final MutableLiveData<Boolean> isNotificationEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsNotificationEnabled() { return isNotificationEnabled; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void checkNotificationStatus() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        userRepository.isNotificationEnabled(uid)
                .addOnSuccessListener(isNotificationEnabled::setValue)
                .addOnFailureListener(e -> isNotificationEnabled.setValue(false));
    }

    public void setNotificationsEnabled(boolean enabled) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (enabled) {
            // 1. קבלת הטוקן מהמכשיר
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                // 2. שמירה ב-DB דרך הריפוזיטורי
                userRepository.updateFcmToken(uid, token)
                        .addOnSuccessListener(aVoid -> toastMessage.setValue("התראות הופעלו"))
                        .addOnFailureListener(e -> {
                            toastMessage.setValue("שגיאה בהפעלת התראות");
                            isNotificationEnabled.setValue(false); // החזרת המתג למצב כבוי
                        });
            });
        } else {
            // מחיקת הטוקן
            userRepository.removeFcmToken(uid)
                    .addOnFailureListener(e -> toastMessage.setValue("שגיאה בכיבוי התראות"));
        }
    }
}