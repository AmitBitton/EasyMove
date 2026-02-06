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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * UserRepository (EasyMove)
 * -------------------------
 * Single source of truth for all user-related operations in EasyMove.
 * Handles Authentication, User Profiles, Partner Matching, and Notification Tokens.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_MOVES = "moves";
    private static final String COLLECTION_MATCH_REQUESTS = "match_requests";

    /* ---------- Firebase instances ---------- */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private String currentUserName;

    /* ---------------------------------------------------------
     * Auth Helpers
     * --------------------------------------------------------- */

    /**
     * @return The current authenticated User ID, or null if not logged in.
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;
        Log.d(TAG, "getCurrentUserId: user authenticated = " + (user != null) + ", uid = " + uid);
        return uid;
    }

    /**
     * Helper to get the current UID or throw an exception if not authenticated.
     */
    private String uidOrThrow() {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.e(TAG, "uidOrThrow: No authenticated user");
            throw new IllegalStateException("No authenticated user");
        }
        return uid;
    }

    /* ---------------------------------------------------------
     * User Name Management
     * --------------------------------------------------------- */

    /**
     * Preloads the current user's name from Firestore for quick access.
     */
    public void loadCurrentUserName() {
        String uid = getCurrentUserId();
        if (uid == null) {
            Log.w(TAG, "loadCurrentUserName: user not logged in, abort");
            return;
        }

        db.collection(COLLECTION_USERS).document(uid).get()
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

    /**
     * Fetches a user's name by their ID asynchronously.
     */
    public Task<String> getUserNameById(String userId) {
        return getUserById(userId).continueWith(task -> {
            UserProfile profile = task.getResult();
            return (profile != null && profile.getName() != null)
                    ? profile.getName()
                    : "Unknown User";
        });
    }

    /* ---------------------------------------------------------
     * Profile Read / Write
     * --------------------------------------------------------- */

    /**
     * Fetches the full profile of the currently logged-in user.
     */
    public Task<UserProfile> getMyProfile() {
        String uid = getCurrentUserId();
        Log.d(TAG, "getMyProfile: fetching profile for uid = " + uid);

        if (uid == null) {
            Log.e(TAG, "getMyProfile: user not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        return db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "getMyProfile: Firestore get failed", task.getException());
                        throw Objects.requireNonNull(task.getException());
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

    /**
     * Fetches the profile of any user by their ID.
     */
    public Task<UserProfile> getUserById(String userId) {
        if (userId == null) {
            Log.e(TAG, "getUserById: userId is null");
            return Tasks.forException(new IllegalArgumentException("userId is null"));
        }

        Log.d(TAG, "getUserById: fetching user with id = " + userId);

        return db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "getUserById: Firestore get failed", task.getException());
                        throw Objects.requireNonNull(task.getException());
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

    /**
     * Saves or updates the current user's profile.
     */
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

        return db.collection(COLLECTION_USERS)
                .document(uid)
                .set(profile)
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveMyProfile: failed to save profile", e)
                );
    }

    /* ---------------------------------------------------------
     * Profile Image Upload
     * --------------------------------------------------------- */

    /**
     * Uploads a profile image to Firebase Storage and returns the download URL.
     */
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

        StorageReference ref = storage.getReference().child("profile_images/" + fileName);

        return ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: upload failed", task.getException());
                        throw Objects.requireNonNull(task.getException());
                    }
                    return ref.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: failed to get download URL", task.getException());
                        throw Objects.requireNonNull(task.getException());
                    }
                    String url = task.getResult().toString();
                    Log.d(TAG, "uploadProfileImage: download URL = " + url);
                    return url;
                });
    }

    /* ---------------------------------------------------------
     * Movers Queries
     * --------------------------------------------------------- */

    public Task<QuerySnapshot> getAllMovers() {
        Log.d(TAG, "getAllMovers: fetching all movers");
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "mover")
                .get()
                .addOnFailureListener(e -> Log.e(TAG, "getAllMovers: failed", e));
    }

    public Task<QuerySnapshot> getMoversByAreas(@Nullable List<String> areas) {
        if (areas == null || areas.isEmpty()) {
            return getAllMovers();
        }
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "mover")
                .whereArrayContainsAny("serviceAreas", areas)
                .get()
                .addOnFailureListener(e -> Log.e(TAG, "getMoversByAreas: failed", e));
    }

    public Task<QuerySnapshot> getMoversByArea(String area) {
        if (area == null || area.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Area is empty"));
        }
        return getMoversByAreas(List.of(area));
    }

    public Task<QuerySnapshot> getMoversByGeoHash(String startHash, String endHash) {
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "mover")
                .orderBy("geohash")
                .startAt(startHash)
                .endAt(endHash)
                .get();
    }

    /* ---------------------------------------------------------
     * Customer Queries
     * --------------------------------------------------------- */

    public Task<QuerySnapshot> getAllCustomers() {
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "customer")
                .get();
    }

    public Task<QuerySnapshot> getAllCustomersExceptMe() {
        // Note: Firestore does not support 'notEqualTo' efficiently in all cases.
        // It's often better to fetch all and filter client-side, but standard query works for now.
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "customer")
                .get();
    }

    /* ---------------------------------------------------------
     * Partner Matchmaking Logic
     * --------------------------------------------------------- */

    /**
     * Fetches all potential partners (customers) for the matchmaking screen.
     */
    public Task<QuerySnapshot> getAllPotentialPartners() {
        return db.collection(COLLECTION_USERS)
                .whereEqualTo("userType", "customer")
                .get();
    }

    /**
     * Sends a partnership request to another user.
     * Requires the current user to have an Active Move (OPEN or CONFIRMED).
     */
    public Task<Void> sendMatchRequest(String targetUserId, String targetUserName) {
        String myUid = uidOrThrow();

        // 1. Fetch current active move to get IDs and addresses
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("customerId", myUid)
                .whereIn("status", Arrays.asList("OPEN", "CONFIRMED"))
                .limit(1)
                .get()
                .continueWithTask(moveTask -> {
                    if (!moveTask.isSuccessful() || moveTask.getResult().isEmpty()) {
                        throw new Exception("No active move found. Cannot send partner request.");
                    }

                    DocumentSnapshot moveDoc = moveTask.getResult().getDocuments().get(0);
                    String moveId = moveDoc.getId();
                    String source = moveDoc.getString("sourceAddress");
                    String dest = moveDoc.getString("destAddress");

                    // 2. Fetch my profile to get my name
                    return getUserById(myUid).continueWithTask(profileTask -> {
                        UserProfile myProfile = profileTask.getResult();
                        String myName = (myProfile != null && myProfile.getName() != null)
                                ? myProfile.getName() : "Unknown User";

                        // 3. Create Request Object
                        MatchRequest request = new MatchRequest(
                                myUid,
                                myName,
                                targetUserId,
                                targetUserName,
                                moveId,
                                source,
                                dest
                        );

                        return db.collection(COLLECTION_MATCH_REQUESTS).add(request).continueWith(t -> null);
                    });
                });
    }

    /**
     * Fetches all pending partnership requests sent TO the current user.
     */
    public Task<QuerySnapshot> getIncomingRequests() {
        String myUid = uidOrThrow();
        return db.collection(COLLECTION_MATCH_REQUESTS)
                .whereEqualTo("toUserId", myUid)
                .whereEqualTo("status", "pending")
                .get();
    }

    /**
     * Updates the status of a match request (e.g., 'approved', 'rejected').
     */
    public Task<Void> updateMatchRequestStatus(String requestId, String newStatus) {
        return db.collection(COLLECTION_MATCH_REQUESTS)
                .document(requestId)
                .update("status", newStatus);
    }

    /* ---------------------------------------------------------
     * Notifications & Settings
     * --------------------------------------------------------- */

    /**
     * Checks if the user has Push Notifications enabled (i.e., has an FCM token).
     */
    public Task<Boolean> isNotificationEnabled(String userId) {
        return db.collection(COLLECTION_USERS).document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult().getString("fcmToken");
                        return token != null && !token.isEmpty();
                    }
                    return false;
                });
    }

    /**
     * Saves the FCM Token to the user's profile to enable notifications.
     */
    public Task<Void> updateFcmToken(String userId, String token) {
        return db.collection(COLLECTION_USERS).document(userId)
                .update("fcmToken", token);
    }

    /**
     * Removes the FCM Token to disable notifications.
     */
    public Task<Void> removeFcmToken(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", FieldValue.delete());
        return db.collection(COLLECTION_USERS).document(userId).update(updates);
    }
}