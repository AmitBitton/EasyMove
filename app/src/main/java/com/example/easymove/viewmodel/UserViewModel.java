package com.example.easymove.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * UserViewModel
 */
public class UserViewModel extends ViewModel {

    private static final String TAG = "UserViewModel";

    private final UserRepository userRepository = new UserRepository();

    /* -------------------- LiveData fields -------------------- */

    private final MutableLiveData<UserProfile> myProfile = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> moversList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<UserProfile>> customersList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> uploadedImageUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // קבוע לרדיוס חיפוש - 50 קילומטרים
    private static final double SEARCH_RADIUS_M = 50 * 1000;

    /* -------------------- Getters for LiveData -------------------- */

    public LiveData<UserProfile> getMyProfileLiveData() { return myProfile; }
    public LiveData<List<UserProfile>> getMoversListLiveData() { return moversList; }
    public LiveData<List<UserProfile>> getCustomersListLiveData() { return customersList; }
    public LiveData<String> getUploadedImageUrlLiveData() { return uploadedImageUrl; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoading; }
    public LiveData<String> getErrorMessageLiveData() { return errorMessage; }

    /* -------------------- Profile: load & save -------------------- */

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
     * פונקציית החיפוש החדשה והחכמה - מבוססת מיקום גיאוגרפי
     */
    public void searchMoversByLocation(LatLng centerLatLng) {
        if (centerLatLng == null) {
            errorMessage.setValue("מיקום לא תקין");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        // 1. הגדרת מרכז החיפוש והרדיוס
        final GeoLocation center = new GeoLocation(centerLatLng.latitude, centerLatLng.longitude);
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, SEARCH_RADIUS_M);

        final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        // 2. יצירת רשימת משימות (Tasks) מול ה-Repository
        for (GeoQueryBounds b : bounds) {
            tasks.add(userRepository.getMoversByGeoHash(b.startHash, b.endHash));
        }

        // 3. הרצת כל השאילתות במקביל
        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(t -> {
                    List<UserProfile> matchingMovers = new ArrayList<>();

                    for (Task<QuerySnapshot> task : tasks) {
                        QuerySnapshot snap = task.getResult();
                        if (snap != null) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {
                                UserProfile mover = doc.toObject(UserProfile.class);

                                // בדיקה שהמוביל קיים ויש לו מיקום תקין
                                if (mover != null && mover.getLat() != 0 && mover.getLng() != 0) {

                                    // חישוב מרחק מדויק
                                    GeoLocation docLocation = new GeoLocation(mover.getLat(), mover.getLng());
                                    double distanceInMeters = GeoFireUtils.getDistanceBetween(docLocation, center);

                                    if (distanceInMeters <= SEARCH_RADIUS_M) {
                                        mover.setDistanceFromUser(distanceInMeters);
                                        mover.setUserId(doc.getId());
                                        matchingMovers.add(mover);
                                    }
                                }
                            }
                        }
                    }

                    // 4. מיון לפי מרחק
                    Collections.sort(matchingMovers, Comparator.comparingDouble(UserProfile::getDistanceFromUser));

                    // 5. עדכון ה-UI
                    isLoading.setValue(false);
                    moversList.setValue(matchingMovers);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("חיפוש נכשל: " + e.getMessage());
                });
    }


    // --- שיטות ישנות (ניתן להשאיר לתמיכה לאחור או למחוק אם אין צורך) ---

    public void loadMoversByAreas(List<String> areas) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        userRepository.getMoversByAreas(areas)
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void loadMoversByArea(String area) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        userRepository.getMoversByArea(area)
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void loadAllMovers() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        userRepository.getAllMovers()
                .addOnSuccessListener(this::mapMoversSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
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

    public void loadAllCustomers() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        userRepository.getAllCustomers()
                .addOnSuccessListener(this::mapCustomersSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue(e.getMessage());
                });
    }

    public void loadAllCustomersExceptMe() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        userRepository.getAllCustomersExceptMe()
                .addOnSuccessListener(this::mapCustomersSnapshotToProfiles)
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
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