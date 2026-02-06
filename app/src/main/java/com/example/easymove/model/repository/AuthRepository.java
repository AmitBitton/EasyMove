package com.example.easymove.model.repository;

import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository class responsible for handling all Authentication related operations.
 * It serves as a single entry point for Firebase Auth (Login, Register, Google Sign-In)
 * and basic User Profile management in Firestore.
 */
public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_USERS = "users";

    /**
     * @return The currently signed-in FirebaseUser, or null if not signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // ------------------------------------------------------------------------
    // Email / Password Authentication
    // ------------------------------------------------------------------------

    /**
     * Signs in a user with email and password.
     *
     * @param email    User's email address.
     * @param password User's password.
     * @return A Task representing the authentication result.
     */
    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Creates a new user account with email and password.
     *
     * @param email    User's email address.
     * @param password User's password.
     * @return A Task representing the registration result.
     */
    public Task<AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    // ------------------------------------------------------------------------
    // Google Authentication
    // ------------------------------------------------------------------------

    /**
     * Signs in a user using Google credentials.
     *
     * @param idToken The ID token retrieved from the Google Sign-In flow.
     * @return A Task representing the authentication result.
     */
    public Task<AuthResult> loginWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return auth.signInWithCredential(credential);
    }

    // ------------------------------------------------------------------------
    // User Profile Management (Firestore)
    // ------------------------------------------------------------------------

    /**
     * Saves or updates the user's profile data in Firestore.
     *
     * @param profile The UserProfile object containing user details.
     * @return A Task representing the save operation, or null if userId is missing.
     */
    public Task<Void> saveUserProfile(UserProfile profile) {
        if (profile.getUserId() == null) return null;
        return db.collection(COLLECTION_USERS)
                .document(profile.getUserId())
                .set(profile);
    }

    /**
     * Fetches the user's profile from Firestore based on their UID.
     * Useful for checking if a Google Sign-In user already has a profile in the DB.
     *
     * @param uid The user's unique ID.
     * @return A Task containing the DocumentSnapshot.
     */
    public Task<DocumentSnapshot> getUserProfile(String uid) {
        return db.collection(COLLECTION_USERS)
                .document(uid)
                .get();
    }
}