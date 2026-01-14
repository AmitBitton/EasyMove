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
    private final MutableLiveData<String> showGoogleTypeDialog = new MutableLiveData<>();

    public LiveData<Boolean> getNavigateToMain() { return navigateToMain; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getShowGoogleTypeDialog() { return showGoogleTypeDialog; }

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

    // ✅ הפונקציה המעודכנת - מקבלת עכשיו גם את נתוני היעד (Destination)
    public void register(String email, String password, String name, String phone,
                         boolean isCustomer,
                         String address, Double lat, Double lng,
                         String destAddress, Double destLat, Double destLng) {

        isLoading.setValue(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    UserProfile user = new UserProfile();
                    user.setUserId(uid);
                    user.setName(name);
                    user.setPhone(phone);
                    user.setUserType(isCustomer ? "customer" : "mover");

                    if (isCustomer) {
                        // --- לקוח: שומרים גם מוצא וגם יעד ---

                        // כתובת מוצא
                        user.setDefaultFromAddress(address);
                        user.setFromLat(lat);
                        user.setFromLng(lng);

                        // ✅ כתובת יעד (החדש)
                        user.setDefaultToAddress(destAddress);
                        user.setToLat(destLat);
                        user.setToLng(destLng);

                    } else {
                        // --- מוביל: שומרים כתובת בסיס ו-GeoHash ---
                        user.setLat(lat != null ? lat : 0);
                        user.setLng(lng != null ? lng : 0);

                        // יצירת GeoHash לחיפושים
                        if (lat != null && lng != null) {
                            String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));
                            user.setGeohash(hash);
                        }

                        // שומרים את הכתובת כאיזור שירות ראשוני
                        List<String> areas = new ArrayList<>();
                        if (address != null) areas.add(address);
                        user.setServiceAreas(areas);
                    }

                    // שמירה במסד הנתונים
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

    public void handleGoogleSignIn(GoogleSignInAccount account) {
        isLoading.setValue(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    // בודקים אם המשתמש קיים ב-Firestore
                    userRepository.getUserById(uid).addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            // משתמש קיים - כניסה רגילה
                            isLoading.setValue(false);
                            navigateToMain.setValue(true);
                        } else {
                            // משתמש חדש - צריך לבחור סוג
                            isLoading.setValue(false);
                            showGoogleTypeDialog.setValue(uid);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Google Sign-In Failed");
                });
    }

    public void completeGoogleRegistration(String uid, String userType) {
        isLoading.setValue(true);
        UserProfile user = new UserProfile();
        user.setUserId(uid);
        user.setName(auth.getCurrentUser().getDisplayName());
        user.setUserType(userType);
        // בהרשמה דרך גוגל אין לנו כרגע כתובת, המשתמש ישלים בפרופיל

        userRepository.saveMyProfile(user)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    navigateToMain.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שמירת נתונים נכשלה");
                });
    }
}