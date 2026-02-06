package com.example.easymove.model;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Singleton class responsible for managing the current user's session state.
 * <p>
 * Capabilities:
 * 1. Holds the currently logged-in user's profile in memory (Cache).
 * 2. Keeps the profile in sync with Firestore using a Real-time Snapshot Listener.
 * 3. Exposes {@link LiveData} so UI components (Activities/Fragments) can observe changes automatically.
 */
public class UserSession {

    private static volatile UserSession INSTANCE;
    private static final String COLLECTION_USERS = "users";

    /* ---------- Firebase Instances ---------- */
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /* ---------- State Management ---------- */

    // Fast in-memory access (volatile ensures visibility across threads)
    private volatile UserProfile cachedProfile;

    // LiveData for UI observation
    private final MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();

    // Firestore listener registration (to remove listener on logout)
    private ListenerRegistration registration;

    // Atomic flag to ensure we start the session initialization only once per login
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private UserSession() {
    }

    /**
     * Returns the global singleton instance of UserSession.
     */
    public static UserSession getInstance() {
        if (INSTANCE == null) {
            synchronized (UserSession.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserSession();
                }
            }
        }
        return INSTANCE;
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Returns the currently cached profile.
     * Warning: This may be null if the session hasn't finished initializing.
     */
    @Nullable
    public UserProfile getCachedProfile() {
        return cachedProfile;
    }

    /**
     * Exposes LiveData so UI components can observe profile changes.
     */
    public LiveData<UserProfile> getProfileLiveData() {
        return profileLiveData;
    }

    /**
     * Initializes the user session.
     * 1. Checks for a valid UID.
     * 2. Performs a one-time fetch (Get) to return a Task for the caller (e.g., Splash Screen).
     * 3. Attaches a persistent SnapshotListener to keep data fresh.
     * <p>
     * This method is thread-safe and will only execute once. Subsequent calls return the cached data.
     *
     * @return A Task containing the UserProfile (from the one-time fetch).
     */
    public Task<UserProfile> ensureStarted() {
        // "Compare and Set": If started is false, set to true and proceed. Else, skip.
        if (started.compareAndSet(false, true)) {
            String uid = getUidOrNull();

            // If no user is logged in, reset flag and fail.
            if (uid == null) {
                started.set(false);
                return Tasks.forException(new IllegalStateException("No authenticated user found."));
            }

            DocumentReference docRef = db.collection(COLLECTION_USERS).document(uid);

            // 1. One-time initial load (so the caller has a Task to await)
            Task<UserProfile> firstLoad = docRef.get().continueWith(task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                DocumentSnapshot snap = task.getResult();
                if (snap != null && snap.exists()) {
                    UserProfile profile = snap.toObject(UserProfile.class);
                    if (profile != null) {
                        profile.setUserId(snap.getId());
                        updateCache(profile);
                        return profile;
                    }
                }
                return null;
            });

            // 2. Real-time listener (updates cache silently in the background)
            registration = docRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) return; // Ignore errors in background listener
                if (snapshot != null && snapshot.exists()) {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile != null) {
                        profile.setUserId(snapshot.getId());
                        updateCache(profile);
                    }
                }
            });

            return firstLoad;
        } else {
            // Already started – just return the current cache wrapped in a Task
            return Tasks.forResult(cachedProfile);
        }
    }

    /**
     * Updates the user's profile in Firestore AND updates the local cache immediately.
     * This provides a responsive UI ("Optimistic Update").
     *
     * @param newProfile The updated profile object.
     */
    public Task<Void> updateMyProfile(UserProfile newProfile) {
        String uid = getUidOrNull();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("No authenticated user"));
        }

        return db.collection(COLLECTION_USERS).document(uid)
                .set(newProfile)
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                    // Update local cache immediately after successful save
                    newProfile.setUserId(uid);
                    updateCache(newProfile);
                    return null;
                });
    }

    /**
     * Cleans up the session. Called on Logout.
     * Removes the Firestore listener and clears the cache.
     */
    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
        started.set(false);
        cachedProfile = null;
        profileLiveData.postValue(null);
    }

    // ========================================================================
    // Internal Helpers
    // ========================================================================

    /**
     * Updates the internal cache and notifies observers.
     * Uses postValue() to be safe for background threads.
     */
    private void updateCache(UserProfile p) {
        cachedProfile = p;
        profileLiveData.postValue(p);
    }

    @Nullable
    private String getUidOrNull() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}