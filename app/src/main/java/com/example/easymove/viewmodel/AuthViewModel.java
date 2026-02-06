package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.UserRepository;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsible for Authentication logic.
 * Handles:
 * 1. Email/Password Login & Registration.
 * 2. Google Sign-In (Credential exchange and Firestore profile creation).
 * 3. State management (Loading, Errors, Navigation).
 */
public class AuthViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final UserRepository userRepository;

    // UI State LiveData
    private final MutableLiveData<Boolean> navigateToMain = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Holds temporary profile data if a Google User is new and needs to complete registration
    private final MutableLiveData<UserProfile> googleUserIncomplete = new MutableLiveData<>();

    // Default Constructor
    public AuthViewModel() {
        this.auth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository();
    }

    // Constructor for Dependency Injection (Optional/Testing)
    public AuthViewModel(UserRepository userRepository) {
        this.auth = FirebaseAuth.getInstance();
        this.userRepository = userRepository;
    }

    // --- Getters ---
    public LiveData<Boolean> getNavigateToMain() { return navigateToMain; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<UserProfile> getGoogleUserIncomplete() { return googleUserIncomplete; }

    public FirebaseAuth getAuthInstance() { return auth; }

    // ------------------------------------------------------------------------
    // Email / Password Logic
    // ------------------------------------------------------------------------

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

    public void register(String email, String password, String name, String phone,
                         boolean isCustomer,
                         String address, Double lat, Double lng,
                         String destAddress, Double destLat, Double destLng) {

        isLoading.setValue(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) return;
                    String uid = result.getUser().getUid();

                    // Create full profile object
                    UserProfile user = createProfileObject(uid, name, phone, isCustomer ? "customer" : "mover",
                            address, lat, lng, destAddress, destLat, destLng);

                    saveProfileToFirestore(user);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("הרשמה נכשלה: " + e.getMessage());
                });
    }

    // ------------------------------------------------------------------------
    // Google Sign-In Logic (Updated for CredentialManager)
    // ------------------------------------------------------------------------

    /**
     * Authenticates with Firebase using the Google ID Token.
     * Checks if the user exists in Firestore:
     * - If yes: Logs in.
     * - If no: Triggers the "Complete Registration" flow.
     */
    public void handleGoogleSignIn(String idToken, String displayName) {
        isLoading.setValue(true);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Google Sign-In Error: User is null");
                        return;
                    }

                    checkIfUserExistsInFirestore(firebaseUser.getUid(), displayName);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Google Sign-In Failed: " + e.getMessage());
                });
    }

    private void checkIfUserExistsInFirestore(String uid, String displayName) {
        userRepository.getUserById(uid).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                // User exists -> Login success
                isLoading.setValue(false);
                navigateToMain.setValue(true);
            } else {
                // User is new -> Navigate to "Complete Details" screen
                isLoading.setValue(false);

                // Create a temporary object to pass the name to the UI
                UserProfile tempProfile = new UserProfile();
                tempProfile.setName(displayName);
                tempProfile.setUserId(uid);

                googleUserIncomplete.setValue(tempProfile);
            }
        });
    }

    /**
     * Saves the additional details for a Google user (User Type, Address, Phone).
     */
    public void completeGoogleRegistration(String uid, String userType, String phone,
                                           String address, Double lat, Double lng,
                                           String destAddress, Double destLat, Double destLng) {
        isLoading.setValue(true);

        // Get name from current Firebase User (since they are already signed in via Google)
        String name = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getDisplayName() : "User";

        UserProfile user = createProfileObject(uid, name, phone, userType,
                address, lat, lng, destAddress, destLat, destLng);

        saveProfileToFirestore(user);
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    private void saveProfileToFirestore(UserProfile user) {
        userRepository.saveMyProfile(user)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    navigateToMain.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בשמירת פרופיל: " + e.getMessage());
                });
    }

    /**
     * Helper to construct the UserProfile object based on User Type.
     */
    private UserProfile createProfileObject(String uid, String name, String phone, String userType,
                                            String address, Double lat, Double lng,
                                            String destAddress, Double destLat, Double destLng) {
        UserProfile user = new UserProfile();
        user.setUserId(uid);
        user.setName(name);
        user.setPhone(phone);
        user.setUserType(userType);

        // Save email if available
        if (auth.getCurrentUser() != null) {
            user.setEmail(auth.getCurrentUser().getEmail());
        }

        if ("customer".equals(userType)) {
            // --- Customer Specifics ---
            user.setDefaultFromAddress(address);
            user.setFromLat(lat);
            user.setFromLng(lng);

            user.setDefaultToAddress(destAddress);
            user.setToLat(destLat);
            user.setToLng(destLng);

        } else {
            // --- Mover Specifics ---
            user.setLat(lat != null ? lat : 0);
            user.setLng(lng != null ? lng : 0);

            // Generate GeoHash for location-based search
            if (lat != null && lng != null) {
                String hash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));
                user.setGeohash(hash);
            }

            // Initialize Service Areas with the base address
            List<String> areas = new ArrayList<>();
            if (address != null) areas.add(address);
            user.setServiceAreas(areas);

            // Default Service Radius
            user.setServiceRadiusKm(30);
        }

        return user;
    }
}