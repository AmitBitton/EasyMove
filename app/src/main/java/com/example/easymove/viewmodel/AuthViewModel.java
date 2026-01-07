package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.AuthRepository;
import com.example.easymove.model.repository.MoveRepository;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository = new AuthRepository();
    private final MoveRepository moveRepository = new MoveRepository();

    // LiveData לניהול ה-UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navigateToMain = new MutableLiveData<>();
    private final MutableLiveData<String> showGoogleTypeDialog = new MutableLiveData<>(); // מחזיר UID

    // Getters
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getNavigateToMain() { return navigateToMain; }
    public LiveData<String> getShowGoogleTypeDialog() { return showGoogleTypeDialog; }

    // --- פעולות ---

    public void login(String email, String password) {
        isLoading.setValue(true);
        authRepository.login(email, password)
                .addOnSuccessListener(result -> {
                    // התחברות הצליחה -> בודקים פרופיל ועוברים לראשי
                    checkProfileAndNavigate(result.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בהתחברות: " + e.getMessage());
                });
    }

    public void register(String email, String password, String name, String phone, boolean isCustomer,
                         String address, Double lat, Double lng) {

        isLoading.setValue(true);

        authRepository.register(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    String userType = isCustomer ? "customer" : "mover";

                    UserProfile profile = new UserProfile();
                    profile.setUserId(uid);
                    profile.setName(name);
                    profile.setPhone(phone);
                    profile.setUserType(userType);

                    // ✅ עדכון: שמירת המיקום אם זה מוביל
                    if (!isCustomer) {
                        profile.setServiceRadiusKm(30);
                        if (address != null) profile.setDefaultFromAddress(address); // שומר כתובת בסיס
                        if (lat != null) profile.setLat(lat);
                        if (lng != null) profile.setLng(lng);
                        // מומלץ להוסיף כאן גם Geohash אם אתה משתמש בחיפוש מבוסס מיקום
                    }

                    saveProfileAndFinalize(profile, isCustomer);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בהרשמה: " + e.getMessage());
                });
    }

    public void handleGoogleSignIn(GoogleSignInAccount account) {
        if (account == null) {
            errorMessage.setValue("Google Sign In Failed");
            return;
        }
        isLoading.setValue(true);
        authRepository.loginWithGoogle(account.getIdToken())
                .addOnSuccessListener(result -> {
                    checkGoogleUserProfileExists(result.getUser());
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("אימות גוגל נכשל: " + e.getMessage());
                });
    }

    /**
     * נרשם דרך גוגל וצריך לבחור סוג משתמש (נקרא מהדיאלוג)
     */
    public void completeGoogleRegistration(String uid, String userType) {
        isLoading.setValue(true);
        FirebaseUser firebaseUser = authRepository.getCurrentUser();

        UserProfile profile = new UserProfile();
        profile.setUserId(uid);
        profile.setName(firebaseUser != null ? firebaseUser.getDisplayName() : "");
        profile.setPhone(firebaseUser != null ? firebaseUser.getPhoneNumber() : "");
        profile.setUserType(userType);

        if ("mover".equals(userType)) {
            profile.setServiceRadiusKm(30);
        }

        saveProfileAndFinalize(profile, "customer".equals(userType));
    }

    // --- פונקציות עזר פנימיות ---

    private void checkProfileAndNavigate(String uid) {
        // במקרה של התחברות רגילה, אנחנו מניחים שהפרופיל קיים
        // אבל אפשר להוסיף בדיקה אם רוצים להיות בטוחים
        isLoading.setValue(false);
        navigateToMain.setValue(true);
    }

    private void checkGoogleUserProfileExists(FirebaseUser user) {
        authRepository.getUserProfile(user.getUid())
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // משתמש קיים -> כניסה
                        isLoading.setValue(false);
                        navigateToMain.setValue(true);
                    } else {
                        // משתמש חדש -> צריך לבחור סוג
                        isLoading.setValue(false);
                        showGoogleTypeDialog.setValue(user.getUid());
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בבדיקת פרופיל");
                });
    }

    private void saveProfileAndFinalize(UserProfile profile, boolean isCustomer) {
        authRepository.saveUserProfile(profile)
                .addOnSuccessListener(unused -> {
                    if (isCustomer) {
                        // אם זה לקוח, יוצרים לו הובלה ריקה
                        moveRepository.ensureActiveMoveForCustomer(profile.getUserId())
                                .addOnCompleteListener(task -> {
                                    isLoading.setValue(false);
                                    navigateToMain.setValue(true);
                                });
                    } else {
                        isLoading.setValue(false);
                        navigateToMain.setValue(true);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בשמירת נתונים: " + e.getMessage());
                });
    }
}