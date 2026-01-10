package com.example.easymove.model.repository;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UserRepository (EasyMove)
 * -------------------------
 * Single source of truth for all user-related operations in EasyMove.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    /* ---------- Firebase instances ---------- */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private String currentUserName;

    /* ---------------------------------------------------------
     * Auth helpers
     * --------------------------------------------------------- */

    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;
        Log.d(TAG, "getCurrentUserId: user authenticated = " + (user != null) + ", uid = " + uid);
        return uid;
    }

    private String uidOrThrow() {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.e(TAG, "uidOrThrow: No authenticated user");
            throw new IllegalStateException("No authenticated user");
        }
        return uid;
    }

    /* ---------------------------------------------------------
     * Current user name helpers
     * --------------------------------------------------------- */

    public void loadCurrentUserName() {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.w(TAG, "loadCurrentUserName: user not logged in, abort");
            return;
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null && profile.getName() != null) {
                        currentUserName = profile.getName();
                        Log.d(TAG, "loadCurrentUserName: loaded name = " + currentUserName);
                    } else {
                        Log.w(TAG, "loadCurrentUserName: profile or fullName is null");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "loadCurrentUserName: failed to load name", e)
                );
    }

    @Nullable
    public String getCurrentUserName() {
        return currentUserName;
    }

    public Task<String> getUserNameById(String userId) {
        return getUserById(userId).continueWith(task -> {
            UserProfile profile = task.getResult();
            return (profile != null && profile.getName() != null)
                    ? profile.getName()
                    : "אנונימי";
        });
    }

    /* ---------------------------------------------------------
     * Profile read / write
     * --------------------------------------------------------- */

    public Task<UserProfile> getMyProfile() {
        String uid = getCurrentUserId();
        Log.d(TAG, "getMyProfile: fetching profile for uid = " + uid);

        if (uid == null) {
            Log.e(TAG, "getMyProfile: user not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return db.collection("users")
                .document(uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "getMyProfile: Firestore get failed", task.getException());
                        throw task.getException();
                    }
                    DocumentSnapshot snap = task.getResult();
                    if (snap != null && snap.exists()) {
                        UserProfile profile = snap.toObject(UserProfile.class);
                        if (profile != null) {
                            profile.setUserId(snap.getId());
                        }
                        Log.d(TAG, "getMyProfile: profile loaded successfully");
                        return profile;
                    }
                    Log.w(TAG, "getMyProfile: document does not exist");
                    return null;
                });
    }

    public Task<UserProfile> getUserById(String userId) {
        if (userId == null) {
            Log.e(TAG, "getUserById: userId is null");
            return Tasks.forException(new IllegalArgumentException("userId is null"));
        }

        Log.d(TAG, "getUserById: fetching user with id = " + userId);

        return db.collection("users")
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "getUserById: Firestore get failed", task.getException());
                        throw task.getException();
                    }
                    DocumentSnapshot snap = task.getResult();
                    if (snap != null && snap.exists()) {
                        UserProfile profile = snap.toObject(UserProfile.class);
                        if (profile != null) {
                            profile.setUserId(snap.getId());
                        }
                        Log.d(TAG, "getUserById: profile loaded");
                        return profile;
                    }
                    Log.w(TAG, "getUserById: document does not exist for id = " + userId);
                    return null;
                });
    }

    public Task<Void> saveMyProfile(UserProfile profile) {
        if (profile == null) {
            Log.e(TAG, "saveMyProfile: profile is null");
            return Tasks.forException(new IllegalArgumentException("profile is null"));
        }

        String uid;
        try {
            uid = uidOrThrow();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }

        Log.d(TAG, "saveMyProfile: saving profile for uid = " + uid);

        profile.setUserId(uid);

        return db.collection("users")
                .document(uid)
                .set(profile)
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveMyProfile: failed to save profile", e)
                );
    }

    /* ---------------------------------------------------------
     * Profile image upload
     * --------------------------------------------------------- */

    public Task<String> uploadProfileImage(@Nullable Uri imageUri) {
        if (imageUri == null) {
            Log.d(TAG, "uploadProfileImage: imageUri is null, returning null URL");
            return Tasks.forResult(null);
        }

        String uid;
        try {
            uid = uidOrThrow();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }

        String fileName = "profile_" + uid + "_" + UUID.randomUUID();
        Log.d(TAG, "uploadProfileImage: uploading image fileName = " + fileName);

        StorageReference ref = storage.getReference()
                .child("profile_images/" + fileName);

        return ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: upload failed", task.getException());
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: failed to get download URL", task.getException());
                        throw task.getException();
                    }
                    String url = task.getResult().toString();
                    Log.d(TAG, "uploadProfileImage: download URL = " + url);
                    return url;
                });
    }

    /* ---------------------------------------------------------
     * Movers queries
     * --------------------------------------------------------- */

    public Task<QuerySnapshot> getAllMovers() {
        Log.d(TAG, "getAllMovers: fetching all movers");

        return db.collection("users")
                .whereEqualTo("userType", "mover")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllMovers: failed to fetch movers", e)
                );
    }

    public Task<QuerySnapshot> getMoversByAreas(@Nullable List<String> areas) {
        if (areas == null || areas.isEmpty()) {
            Log.d(TAG, "getMoversByAreas: no areas provided, returning all movers");
            return getAllMovers();
        }

        Log.d(TAG, "getMoversByAreas: fetching movers for areas = " + areas);

        return db.collection("users")
                .whereEqualTo("userType", "mover")
                .whereArrayContainsAny("serviceAreas", areas)
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getMoversByAreas: failed to fetch movers", e)
                );
    }

    public Task<QuerySnapshot> getMoversByArea(String area) {
        if (area == null || area.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("area is empty"));
        }
        return getMoversByAreas(List.of(area));
    }

    /* --- NEW METHOD: GeoSpatial Query Support --- */
    /**
     * Executes a single query for a GeoHash range.
     * This is used by the ViewModel to construct the full radius search.
     */
    public Task<QuerySnapshot> getMoversByGeoHash(String startHash, String endHash) {
        // שים לב: זה דורש ליצור Index ב-Firebase Console על השדה 'geohash'
        return db.collection("users")
                .whereEqualTo("userType", "mover")
                .orderBy("geohash")
                .startAt(startHash)
                .endAt(endHash)
                .get();
    }

    /* ---------------------------------------------------------
     * Customers queries
     * --------------------------------------------------------- */

    public Task<QuerySnapshot> getAllCustomers() {
        Log.d(TAG, "getAllCustomers: fetching all customers");

        return db.collection("users")
                .whereEqualTo("userType", "customer")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllCustomers: failed to fetch customers", e)
                );
    }

    public Task<QuerySnapshot> getAllCustomersExceptMe() {
        String myUid = getCurrentUserId();
        if (myUid == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }
        return db.collection("users")
                .whereEqualTo("userType", "customer")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllCustomersExceptMe: failed to fetch customers", e)
                );
    }





    // -----------------------------------------------------------
    //  פונקציות לשידוך שותפים (Matchmaking)
    // -----------------------------------------------------------

    /**
     * מביא את כל הלקוחות (Customers) כדי שנוכל לחפש ביניהם.
     */
    public Task<QuerySnapshot> getAllPotentialPartners() {
        return db.collection("users")
                .whereEqualTo("userType", "customer")
                .get();
    }

    /**
     * שליחת בקשת חברות למשתמש אחר (מתוקן)
     * שולף את ההובלה הפעילה, את השם שלי, ויוצר בקשה עם moveId.
     */
    public Task<Void> sendMatchRequest(String targetUserId) {
        String myUid = uidOrThrow();

        // 1. שליפת ההובלה הפעילה כדי לקבל ID וכתובות
        return db.collection("moves")
                .whereEqualTo("customerId", myUid)
                .whereIn("status", java.util.Arrays.asList("OPEN", "CONFIRMED"))
                .limit(1)
                .get()
                .continueWithTask(moveTask -> {
                    if (!moveTask.isSuccessful() || moveTask.getResult().isEmpty()) {
                        throw new Exception("לא נמצאה הובלה פעילה לצרף אליה שותף");
                    }

                    DocumentSnapshot moveDoc = moveTask.getResult().getDocuments().get(0);
                    String moveId = moveDoc.getId();
                    String source = moveDoc.getString("sourceAddress");
                    String dest = moveDoc.getString("destAddress");

                    // 2. שליפת השם שלי
                    return getUserById(myUid).continueWithTask(profileTask -> {
                        UserProfile myProfile = profileTask.getResult();
                        String myName = (myProfile != null && myProfile.getName() != null) ? myProfile.getName() : "משתמש";

                        // 3. יצירת הבקשה עם הכתובות
                        MatchRequest request = new MatchRequest(myUid, myName, targetUserId, moveId, source, dest);

                        return db.collection("match_requests").add(request).continueWith(t -> null);
                    });
                });
    }

    /**
     * מביא את כל הבקשות שממתינות לי (סטטוס pending).
     */
    public Task<QuerySnapshot> getIncomingRequests() {
        String myUid = uidOrThrow();
        return db.collection("match_requests")
                .whereEqualTo("toUserId", myUid)
                .whereEqualTo("status", "pending")
                .get();
    }

    /**
     * עדכון סטטוס בקשה (אישור/דחייה).
     */
    public Task<Void> updateMatchRequestStatus(String requestId, String newStatus) {
        return db.collection("match_requests").document(requestId)
                .update("status", newStatus);
    }

    /**
     * בדיקה אם ההתראות דלוקות (האם קיים טוקן למשתמש)
     */
    public Task<Boolean> isNotificationEnabled(String userId) {
        return db.collection("users").document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult().getString("fcmToken");
                        return token != null && !token.isEmpty();
                    }
                    return false;
                });
    }

    /**
     * שמירת טוקן (הפעלת התראות)
     */
    public Task<Void> updateFcmToken(String userId, String token) {
        return db.collection("users").document(userId)
                .update("fcmToken", token);
    }

    /**
     * מחיקת טוקן (כיבוי התראות)
     */
    public Task<Void> removeFcmToken(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", FieldValue.delete());
        return db.collection("users").document(userId).update(updates);
    }
}