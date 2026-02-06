package com.example.easymove.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ViewModel responsible for managing User Data.
 * Handles:
 * 1. Loading and Saving User Profile.
 * 2. Uploading Profile Images.
 * 3. Searching for Movers based on location (GeoFire).
 * 4. Synchronizing Profile changes with Active Moves (Drafts).
 */
public class UserViewModel extends ViewModel {

    private static final String TAG = "UserViewModel";
    private static final double SEARCH_RADIUS_M = 50 * 1000; // 50 km Search Radius

    private final UserRepository userRepository = new UserRepository();
    private final MoveRepository moveRepository = new MoveRepository(); // Used for syncing moves

    // --- LiveData Fields ---
    private final MutableLiveData<UserProfile> myProfile = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> moversList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> uploadedImageUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Signal to UI that move details have been successfully saved (e.g., ready to start chat)
    private final MutableLiveData<Boolean> moveDetailsSaved = new MutableLiveData<>();

    // --- Getters ---
    public LiveData<UserProfile> getMyProfileLiveData() { return myProfile; }
    public LiveData<List<UserProfile>> getMoversListLiveData() { return moversList; }
    public LiveData<String> getUploadedImageUrlLiveData() { return uploadedImageUrl; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoading; }
    public LiveData<String> getErrorMessageLiveData() { return errorMessage; }
    public LiveData<Boolean> getMoveDetailsSaved() { return moveDetailsSaved; }

    // =================================================================
    //  Profile Management
    // =================================================================

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
     * Saves the user profile and synchronizes changes with any active "Draft" moves.
     * Use case: User updates "Default Move Date" in profile -> Update active move request.
     */
    public void saveMyProfile(UserProfile profile) {
        if (profile == null) {
            errorMessage.setValue("Profile is null");
            return;
        }

        isLoading.setValue(true);

        // 1. Save to 'users' collection
        userRepository.saveMyProfile(profile)
                .addOnSuccessListener(unused -> {
                    myProfile.setValue(profile);

                    // 2. Sync: Update Active Move (Draft) if it exists
                    if ("customer".equals(profile.getUserType())) {
                        long date = (profile.getDefaultMoveDate() != null) ? profile.getDefaultMoveDate() : 0;
                        String src = (profile.getDefaultFromAddress() != null) ? profile.getDefaultFromAddress() : "";
                        String dst = (profile.getDefaultToAddress() != null) ? profile.getDefaultToAddress() : "";

                        // Call MoveRepository to update the move document
                        moveRepository.updateMoveDraftDetails(profile.getUserId(), src, dst, date)
                                .addOnCompleteListener(task -> {
                                    // Finish process (success even if no move existed to update)
                                    isLoading.setValue(false);
                                    moveDetailsSaved.setValue(true);
                                });
                    } else {
                        // Mover logic (No move drafts to sync)
                        isLoading.setValue(false);
                        moveDetailsSaved.setValue(true);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שמירה נכשלה: " + e.getMessage());
                });
    }

    public void uploadProfileImage(Uri imageUri) {
        if (imageUri == null) return;
        isLoading.setValue(true);

        userRepository.uploadProfileImage(imageUri)
                .addOnSuccessListener(url -> {
                    isLoading.setValue(false);
                    uploadedImageUrl.setValue(url);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    // =================================================================
    //  Search Logic (GeoFire)
    // =================================================================

    /**
     * Searches for Movers within a fixed radius of a given location.
     */
    public void searchMoversByLocation(LatLng centerLatLng) {
        if (centerLatLng == null) {
            errorMessage.setValue("מיקום לא תקין");
            return;
        }

        isLoading.setValue(true);

        final GeoLocation center = new GeoLocation(centerLatLng.latitude, centerLatLng.longitude);
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, SEARCH_RADIUS_M);
        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds b : bounds) {
            tasks.add(userRepository.getMoversByGeoHash(b.startHash, b.endHash));
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(t -> {
                    List<UserProfile> matchingMovers = new ArrayList<>();

                    for (Task<QuerySnapshot> task : tasks) {
                        QuerySnapshot snap = task.getResult();
                        if (snap != null) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                UserProfile mover = doc.toObject(UserProfile.class);

                                // Filter results by exact distance (GeoHash is approximate box)
                                if (mover != null && mover.getLat() != 0 && mover.getLng() != 0) {
                                    GeoLocation docLocation = new GeoLocation(mover.getLat(), mover.getLng());
                                    double distance = GeoFireUtils.getDistanceBetween(docLocation, center);

                                    if (distance <= SEARCH_RADIUS_M) {
                                        mover.setDistanceFromUser(distance);
                                        mover.setUserId(doc.getId());
                                        matchingMovers.add(mover);
                                    }
                                }
                            }
                        }
                    }

                    // Sort by Distance
                    matchingMovers.sort(Comparator.comparingDouble(UserProfile::getDistanceFromUser));

                    isLoading.setValue(false);
                    moversList.setValue(matchingMovers);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("חיפוש נכשל");
                });
    }

    /**
     * Shortcut to quickly save move details (from Search Screen) before starting a chat.
     * This ensures the chat context has the correct move details.
     */
    public void saveMoveDetails(String customerId, String source, String dest, long date) {
        isLoading.setValue(true);

        // 1. Fetch current profile to avoid overwriting other fields
        userRepository.getUserById(customerId).addOnSuccessListener(profile -> {
            if (profile == null) {
                // Edge case: Create new profile object if missing
                profile = new UserProfile();
                profile.setUserId(customerId);
                profile.setUserType("customer");
            }

            Log.d("DEBUG_SAVE", "Saving move details. Date: " + date);

            // 2. Update fields
            profile.setDefaultFromAddress(source);
            profile.setDefaultToAddress(dest);
            profile.setDefaultMoveDate(date);

            // 3. Save & Sync (reusing main logic)
            saveMyProfile(profile);

        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            errorMessage.setValue("שגיאה בטעינת פרופיל לשמירה: " + e.getMessage());
        });
    }
}