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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UserSession
 *
 * Holds the currently logged-in user's profile in memory (singleton),
 * keeps it in sync with Firestore using a snapshot listener,
 * and exposes LiveData so that UI layers can observe changes.
 */
public class UserSession {

    private static volatile UserSession INSTANCE;

    /* ---------- Firebase ---------- */
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /* ---------- cached profile + LiveData ---------- */

    // Fast in-memory access (may be null if not loaded yet)
    private volatile UserProfile cachedProfile;

    // For Fragments / Activities that want to observe the profile
    private final MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();

    // Firestore listener registration
    private ListenerRegistration registration;

    // Make sure we start the session only once
    private final AtomicBoolean started = new AtomicBoolean(false);

    private UserSession() {
        // private ctor – use getInstance()
    }

    /** Global singleton access */
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

    /* ===================== Public API ===================== */

    /** Returns the cached profile (may be null). */
    @Nullable
    public UserProfile getCachedProfile() {
        return cachedProfile;
    }

    /** Exposes LiveData so UI can observe changes. */
    public LiveData<UserProfile> getProfileLiveData() {
        return profileLiveData;
    }

    /**
     * Ensure the session is started:
     *  - finds the current UID
     *  - loads the profile once from Firestore
     *  - attaches a snapshot listener for future changes.
     *
     * Safe to call multiple times – it will actually start only once.
     */
    public Task<UserProfile> ensureStarted() {
        // First caller 'wins'
        if (started.compareAndSet(false, true)) {
            String uid = getUidOrNull();
            if (uid == null) {
                started.set(false);
                return Tasks.forException(
                        new IllegalStateException("No authenticated user"));
            }

            DocumentReference docRef = db.collection("users").document(uid);

            // One-time initial load (for the Task result)
            Task<UserProfile> firstLoad = docRef.get().continueWith(task -> {
                if (!task.isSuccessful()) throw task.getException();
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

            // Live listener – keeps the cache up to date
            registration = docRef.addSnapshotListener((snapshot, e) -> {
                if (e != null) return;
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
            // Already started – just return current cache as Task
            return Tasks.forResult(cachedProfile);
        }
    }

    /**
     * Write-through update:
     *  - updates Firestore
     *  - updates in-memory cache (so UI reacts immediately)
     */
    public Task<Void> updateMyProfile(UserProfile newProfile) {
        String uid = getUidOrNull();
        if (uid == null) {
            return Tasks.forException(
                    new IllegalStateException("No authenticated user"));
        }

        return db.collection("users").document(uid).set(newProfile)
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    newProfile.setUserId(uid);
                    updateCache(newProfile);
                    return null;
                });
    }

    /**
     * Called on logout.
     *  - removes Firestore listener
     *  - clears cache
     *  - resets LiveData
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

    /* ===================== Helpers ===================== */

    private void updateCache(UserProfile p) {
        cachedProfile = p;
        profileLiveData.postValue(p);
    }

    @Nullable
    private String getUidOrNull() {
        return auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : null;
    }
}
