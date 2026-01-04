package com.example.easymove.viewmodel;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.UserRepository;
import com.example.easymove.model.repository.MoveRepository;
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

public class UserViewModel extends ViewModel {

    private static final String TAG = "UserViewModel";

    private final UserRepository userRepository = new UserRepository();
    private final MoveRepository moveRepository = new MoveRepository(); // לסנכרון מול הובלות

    /* -------------------- LiveData fields -------------------- */
    private final MutableLiveData<UserProfile> myProfile = new MutableLiveData<>();
    private final MutableLiveData<List<UserProfile>> moversList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<UserProfile>> customersList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> uploadedImageUrl = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> moveDetailsSaved = new MutableLiveData<>(); // אינדיקציה לסיום שמירה

    private static final double SEARCH_RADIUS_M = 50 * 1000; // 50 ק"מ

    /* -------------------- Getters -------------------- */
    public LiveData<UserProfile> getMyProfileLiveData() { return myProfile; }
    public LiveData<List<UserProfile>> getMoversListLiveData() { return moversList; }
    public LiveData<List<UserProfile>> getCustomersListLiveData() { return customersList; }
    public LiveData<String> getUploadedImageUrlLiveData() { return uploadedImageUrl; }
    public LiveData<Boolean> getIsLoadingLiveData() { return isLoading; }
    public LiveData<String> getErrorMessageLiveData() { return errorMessage; }
    public LiveData<Boolean> getMoveDetailsSaved() { return moveDetailsSaved; }

    /* -------------------- Profile Logic -------------------- */

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
     * שמירת פרופיל + סנכרון להובלה פעילה
     */
    public void saveMyProfile(UserProfile profile) {
        if (profile == null) {
            errorMessage.setValue("Profile is null");
            return;
        }

        isLoading.setValue(true);

        // 1. שמירה ב-users
        userRepository.saveMyProfile(profile)
                .addOnSuccessListener(unused -> {
                    myProfile.setValue(profile);

                    // 2. סנכרון: עדכון ההובלה הפעילה (Moves)
                    // רק אם זה לקוח ושינה תאריך/כתובות
                    if ("customer".equals(profile.getUserType())) {

                        long date = (profile.getDefaultMoveDate() != null) ? profile.getDefaultMoveDate() : 0;
                        String src = (profile.getDefaultFromAddress() != null) ? profile.getDefaultFromAddress() : "";
                        String dst = (profile.getDefaultToAddress() != null) ? profile.getDefaultToAddress() : "";

                        // קריאה ל-MoveRepository לעדכון ה-Move
                        moveRepository.updateMoveDraftDetails(profile.getUserId(), src, dst, date)
                                .addOnCompleteListener(task -> {
                                    // סיום התהליך (גם אם לא הייתה הובלה לעדכן, זה נחשב הצלחה)
                                    isLoading.setValue(false);
                                    moveDetailsSaved.setValue(true);
                                });
                    } else {
                        // מוביל - אין מה לסנכרן מול moves
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

    /* -------------------- Search Movers (GeoFire) -------------------- */

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
                    Collections.sort(matchingMovers, Comparator.comparingDouble(UserProfile::getDistanceFromUser));
                    isLoading.setValue(false);
                    moversList.setValue(matchingMovers);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("חיפוש נכשל");
                });
    }

    /**
     * שמירת פרטים מהירה (מתוך מסך החיפוש)
     */
    public void saveMoveDetails(String customerId, String source, String dest, long date) {
        isLoading.setValue(true);
        moveRepository.updateMoveDraftDetails(customerId, source, dest, date)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    moveDetailsSaved.setValue(true);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שמירה נכשלה: " + e.getMessage());
                });
    }
}