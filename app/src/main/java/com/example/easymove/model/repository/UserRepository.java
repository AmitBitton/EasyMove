package com.example.easymove.model.repository;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.UUID;

/**
 * UserRepository (EasyMove)
 * -------------------------
 * Single source of truth for all user-related operations in EasyMove.
 *
 * Responsibilities:
 *  - Talks to FirebaseAuth  (who is the current user?)
 *  - Talks to Firestore "users" collection (read / write profiles)
 *  - Talks to Firebase Storage (upload profile images)
 *  - Provides higher-level queries for EasyMove use cases:
 *      * get all movers
 *      * get movers by service areas
 *      * get all customers (for shared move / future features)
 *
 * ViewModels should depend on this class instead of using Firebase directly.
 */
public class UserRepository {

    private static final String TAG = "UserRepository";

    /* ---------- Firebase instances ---------- */
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    // Optional: cache of current user's full name (if you want, like in Roomatch)
    private String currentUserName;

    /* ---------------------------------------------------------
     *  Auth helpers
     * --------------------------------------------------------- */

    /**
     * Returns the UID of the currently authenticated user, or null if not logged in.
     */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        String uid = (user != null) ? user.getUid() : null;
        Log.d(TAG, "getCurrentUserId: user authenticated = " + (user != null) + ", uid = " + uid);
        return uid;
    }

    /**
     * Same as getCurrentUserId(), but throws if user is not logged in.
     * Useful for internal methods that MUST have a logged-in user.
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
     *  Current user name helpers (optional, like Roomatch)
     * --------------------------------------------------------- */

    /**
     * Loads the current user's full name into a local cache (currentUserName).
     * You can call this once on app start / profile screen start.
     */
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

    /**
     * Returns the cached name of the current user (may be null if not loaded yet).
     */
    @Nullable
    public String getCurrentUserName() {
        return currentUserName;
    }

    /**
     * Convenience method: get full name of user by id.
     * Returns "אנונימי" if profile or name is missing.
     */
    public Task<String> getUserNameById(String userId) {
        return getUserById(userId).continueWith(task -> {
            UserProfile profile = task.getResult();
            return (profile != null && profile.getName() != null)
                    ? profile.getName()
                    : "אנונימי";
        });
    }

    /* ---------------------------------------------------------
     *  Profile read / write
     * --------------------------------------------------------- */

    /**
     * Loads the profile of the current logged-in user as a UserProfile object.
     * Returns Task<UserProfile>. If user not logged in, returns a failed Task.
     */
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

    /**
     * Loads a user profile by its UID.
     * Used for:
     *  - showing details of a mover
     *  - showing details of another customer (shared move, chat, etc.)
     */
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

    /**
     * Saves the profile of the current logged-in user.
     * If profile userId is null, we inject the current UID to keep it in sync.
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
            // user not logged in
            return Tasks.forException(e);
        }

        Log.d(TAG, "saveMyProfile: saving profile for uid = " + uid);

        profile.setUserId(uid); // keep in sync with Firestore document id

        return db.collection("users")
                .document(uid)
                .set(profile)
                .addOnFailureListener(e ->
                        Log.e(TAG, "saveMyProfile: failed to save profile", e)
                );
    }

    /* ---------------------------------------------------------
     *  Profile image upload
     * --------------------------------------------------------- */

    /**
     * Uploads a profile image to Firebase Storage and returns a Task<String>
     * containing the public download URL.
     *
     * If imageUri is null, we simply return a Task with null.
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

        StorageReference ref = storage.getReference()
                .child("profile_images/" + fileName);

        return ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "uploadProfileImage: upload failed", task.getException());
                        throw task.getException();
                    }
                    Log.d(TAG, "uploadProfileImage: upload success, fetching download URL");
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
     *  Movers queries
     * --------------------------------------------------------- */

    /**
     * Returns all users of type "mover".
     * Used for:
     *  - general browsing of movers
     *  - fallback if no area filter is selected
     */
    public Task<QuerySnapshot> getAllMovers() {
        Log.d(TAG, "getAllMovers: fetching all movers");

        return db.collection("users")
                .whereEqualTo("userType", "mover")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllMovers: failed to fetch movers", e)
                );
    }

    /**
     * Returns movers that work in at least one of the requested service areas.
     *
     * Firestore structure assumption:
     *  - collection: "users"
     *  - field: "userType" == "mover"
     *  - field: "serviceAreas" is an array of strings (List<String> in UserProfile),
     *    e.g. ["Tel Aviv", "Center", "South"]
     *
     * If areas is null or empty → returns all movers.
     */
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

    /**
     * Convenience method for a single area (wraps getMoversByAreas).
     */
    public Task<QuerySnapshot> getMoversByArea(String area) {
        if (area == null || area.trim().isEmpty()) {
            Log.e(TAG, "getMoversByArea: area is empty");
            return Tasks.forException(new IllegalArgumentException("area is empty"));
        }
        return getMoversByAreas(List.of(area));
    }

    /* ---------------------------------------------------------
     *  Customers queries (for shared move / future features)
     * --------------------------------------------------------- */

    /**
     * Returns all users of type "customer".
     * If you need "all customers except me", you can filter in memory in the ViewModel,
     * because Firestore limitations make complex "!=" + other filters annoying.
     */
    public Task<QuerySnapshot> getAllCustomers() {
        Log.d(TAG, "getAllCustomers: fetching all customers");

        return db.collection("users")
                .whereEqualTo("userType", "customer")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllCustomers: failed to fetch customers", e)
                );
    }

    /**
     * Returns all users of type "customer".
     * Note: does NOT exclude the current user on the Firestore side.
     * The ViewModel can filter out myUid if needed.
     */
    public Task<QuerySnapshot> getAllCustomersExceptMe() {
        String myUid = getCurrentUserId();
        Log.d(TAG, "getAllCustomersExceptMe: myUid = " + myUid);

        if (myUid == null) {
            Log.e(TAG, "getAllCustomersExceptMe: user not logged in");
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        // We can't do "whereNotEqualTo" + other complex filters easily,
        // so we simply return all customers and let the caller filter out myUid.
        return db.collection("users")
                .whereEqualTo("userType", "customer")
                .get()
                .addOnFailureListener(e ->
                        Log.e(TAG, "getAllCustomersExceptMe: failed to fetch customers", e)
                );
    }
}
