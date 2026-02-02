package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.UserRepository;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class AuthViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final UserRepository userRepository = new UserRepository();

    private final MutableLiveData<Boolean> navigateToMain = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // ✅ משתנה חדש: מחזיק את חשבון הגוגל אם המשתמש חדש וצריך להשלים פרטים
    private final MutableLiveData<GoogleSignInAccount> googleUserIncomplete = new MutableLiveData<>();

    public LiveData<Boolean> getNavigateToMain() { return navigateToMain; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<GoogleSignInAccount> getGoogleUserIncomplete() { return googleUserIncomplete; }

    // חשיפת מופע ה-Auth למקרה הצורך ב-Activity
    public FirebaseAuth getAuthInstance() { return auth; }

    // --- התחברות רגילה ---
    public void login(String email, String password) {
        isLoading.setValue(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    isLoading.setValue(false);
                    navigateToMain.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("התחברות נכשלה: " + e.getMessage());
                });
    }

    // --- הרשמה רגילה (כולל כל הכתובות) ---
    public void register(String email, String password, String name, String phone,
                         boolean isCustomer,
                         String address, Double lat, Double lng,
                         String destAddress, Double destLat, Double destLng) {

        isLoading.setValue(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // יצירת פרופיל מלא
                    UserProfile user = createProfileObject(uid, name, phone, isCustomer ? "customer" : "mover",
                            address, lat, lng, destAddress, destLat, destLng);

                    // שמירה
                    userRepository.saveMyProfile(user)
                            .addOnSuccessListener(unused -> {
                                isLoading.setValue(false);
                                navigateToMain.setValue(true);
                            })
                            .addOnFailureListener(e -> {
                                isLoading.setValue(false);
                                errorMessage.setValue("שגיאה בשמירת פרופיל: " + e.getMessage());
                            });

                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("הרשמה נכשלה: " + e.getMessage());
                });
    }

    // --- טיפול בכניסה עם גוגל ---
    public void handleGoogleSignIn(GoogleSignInAccount account) {
        isLoading.setValue(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    // בדיקה האם המשתמש כבר קיים במערכת (עם פרופיל מלא)
                    userRepository.getUserById(uid).addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            // משתמש קיים -> כניסה רגילה
                            isLoading.setValue(false);
                            navigateToMain.setValue(true);
                        } else {
                            // משתמש חדש -> מעבירים ל-UI להשלמת פרטים
                            isLoading.setValue(false);
                            googleUserIncomplete.setValue(account);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Google Sign-In Failed: " + e.getMessage());
                });
    }

    // --- ✅ השלמת הרשמה מגוגל (כעת מקבלת את כל הכתובות) ---
    public void completeGoogleRegistration(String uid, String userType, String phone,
                                           String address, Double lat, Double lng,
                                           String destAddress, Double destLat, Double destLng) {
        isLoading.setValue(true);

        // השם נלקח מהחשבון הנוכחי של גוגל
        String name = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getDisplayName() : "User";

        // שימוש באותה פונקציית עזר ליצירת הפרופיל
        UserProfile user = createProfileObject(uid, name, phone, userType,
                address, lat, lng, destAddress, destLat, destLng);

        userRepository.saveMyProfile(user)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    navigateToMain.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שמירת נתונים נכשלה: " + e.getMessage());
                });
    }

    // --- פונקציית עזר למניעת שכפול קוד ביצירת הפרופיל ---
    private UserProfile createProfileObject(String uid, String name, String phone, String userType,
                                            String address, Double lat, Double lng,
                                            String destAddress, Double destLat, Double destLng) {
        UserProfile user = new UserProfile();
        user.setUserId(uid);
        user.setName(name);
        user.setPhone(phone);
        user.setUserType(userType);

        if ("customer".equals(userType)) {
            // --- הגדרות לקוח ---

            // מוצא
            user.setDefaultFromAddress(address);
            user.setFromLat(lat);
            user.setFromLng(lng);

            // יעד (חשוב!)
            user.setDefaultToAddress(destAddress);
            user.setToLat(destLat);
            user.setToLng(destLng);

        } else {
            // --- הגדרות מוביל ---
            user.setLat(lat != null ? lat : 0);
            user.setLng(lng != null ? lng : 0);

            // יצירת GeoHash לחיפוש מבוסס מיקום
            if (lat != null && lng != null) {
                String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));
                user.setGeohash(hash);
            }

            // הוספת הכתובת לרשימת אזורי השירות
            List<String> areas = new ArrayList<>();
            if (address != null) areas.add(address);
            user.setServiceAreas(areas);

            // רדיוס ברירת מחדל
            user.setServiceRadiusKm(30);
        }

        return user;
    }
}