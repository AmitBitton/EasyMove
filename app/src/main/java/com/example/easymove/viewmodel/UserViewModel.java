package com.example.easymove.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * UserViewModel
 * -------------
 * ViewModel responsible for:
 *  - Loading & saving the current user's profile
 *  - Uploading profile image
 *  - Loading movers for the "search move" flow
 *  - Loading customers for "shared move" (future / optional)
 *
 * UI (Fragments / Activities) should observe the LiveData exposed here.
 * This ViewModel talks to UserRepository, which talks to Firebase.
 */
public class UserViewModel extends ViewModel {

    private static final String TAG = "UserViewModel";

    private final UserRepository userRepository = new UserRepository();

    /* -------------------- LiveData fields -------------------- */

    // פרופיל של המשתמש המחובר
    private final MutableLiveData<UserProfile> myProfile = new MutableLiveData<>();

    // רשימת מובילים (למסך חיפוש הובלה)
    private final MutableLiveData<List<UserProfile>> moversList = new MutableLiveData<>(new ArrayList<>());

    // רשימת לקוחות (למסך חיפוש שותף – עתידי)
    private final MutableLiveData<List<UserProfile>> customersList = new MutableLiveData<>(new ArrayList<>());

    // URL של תמונת פרופיל שהועלתה
    private final MutableLiveData<String> uploadedImageUrl = new MutableLiveData<>();

    // טעינה / שגיאה כלליים
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /* -------------------- Getters for LiveData -------------------- */

    public LiveData<UserProfile> getMyProfileLiveData() {
        return myProfile;
    }

    public LiveData<List<UserProfile>> getMoversListLiveData() {
        return moversList;
    }

    public LiveData<List<UserProfile>> getCustomersListLiveData() {
        return customersList;
    }

    public LiveData<String> getUploadedImageUrlLiveData() {
        return uploadedImageUrl;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoading;
    }

    public LiveData<String> getErrorMessageLiveData() {
        return errorMessage;
    }

    /* -------------------- Profile: load & save -------------------- */

    /**
     * Loads the current user's profile from Firestore and posts it to myProfile LiveData.
     * Used by both customer & mover "Profile" screens.
     */
    public void loadMyProfile() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getMyProfile()
                .addOnSuccessListener(profile -> {
                    isLoading.setValue(false);
                    myProfile.setValue(profile);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadMyProfile: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /**
     * Saves the given UserProfile as the current user's profile.
     * After success, myProfile LiveData is updated.
     */
    public void saveMyProfile(UserProfile profile) {
        if (profile == null) {
            errorMessage.setValue("Profile is null");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.saveMyProfile(profile)
                .addOnSuccessListener(unused -> {
                    isLoading.setValue(false);
                    myProfile.setValue(profile);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "saveMyProfile: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /* -------------------- Profile image upload -------------------- */

    /**
     * Uploads a profile image and updates uploadedImageUrl LiveData with the download URL.
     * You can then update the profile's profileImageUrl field and call saveMyProfile().
     */
    public void uploadProfileImage(Uri imageUri) {
        if (imageUri == null) {
            errorMessage.setValue("No image selected");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.uploadProfileImage(imageUri)
                .addOnSuccessListener(url -> {
                    isLoading.setValue(false);
                    uploadedImageUrl.setValue(url);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "uploadProfileImage: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /* -------------------- Movers: search for move -------------------- */

    /**
     * Loads movers that work in at least one of the given service areas.
     * Connected to the "Search Move" screen (customer).
     */
    public void loadMoversByAreas(List<String> areas) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getMoversByAreas(areas)
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadMoversByAreas: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /**
     * Convenience method for a single area – can be used when you map the route to a single region.
     */
    public void loadMoversByArea(String area) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getMoversByArea(area)
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadMoversByArea: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /**
     * Loads all movers (fallback or initial state).
     */
    public void loadAllMovers() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getAllMovers()
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadAllMovers: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    private void mapMoversSnapshotToProfiles(QuerySnapshot snapshot) {
        isLoading.setValue(false);
        List<UserProfile> list = new ArrayList<>();

        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                UserProfile profile = doc.toObject(UserProfile.class);
                if (profile != null) {
                    profile.setUserId(doc.getId());
                    list.add(profile);
                }
            }
        }
        moversList.setValue(list);
    }

    /* -------------------- Customers: shared move (future) -------------------- */

    /**
     * Loads all customers (for future "Find partner" / shared move feature).
     */
    public void loadAllCustomers() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getAllCustomers()
                .addOnSuccessListener(this::mapCustomersSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadAllCustomers: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    /**
     * Loads all customers (ViewModel can filter out current user if needed).
     */
    public void loadAllCustomersExceptMe() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        userRepository.getAllCustomersExceptMe()
                .addOnSuccessListener(this::mapCustomersSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    Log.e(TAG, "loadAllCustomersExceptMe: failed", e);
                    errorMessage.setValue(e.getMessage());
                });
    }

    private void mapCustomersSnapshotToProfiles(QuerySnapshot snapshot) {
        isLoading.setValue(false);
        List<UserProfile> list = new ArrayList<>();

        if (snapshot != null) {
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                UserProfile profile = doc.toObject(UserProfile.class);
                if (profile != null) {
                    profile.setUserId(doc.getId());
                    list.add(profile);
                }
            }
        }
        customersList.setValue(list);
    }
}
