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

public class AuthRepository {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String COLLECTION_USERS = "users";

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // --- התחברות והרשמה במייל/סיסמה ---

    public Task<AuthResult> login(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    // --- התחברות עם גוגל ---

    public Task<AuthResult> loginWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        return auth.signInWithCredential(credential);
    }

    // --- ניהול פרופיל משתמש ---

    /**
     * שמירת פרופיל המשתמש ב-Firestore
     */
    public Task<Void> saveUserProfile(UserProfile profile) {
        if (profile.getUserId() == null) return null;
        return db.collection(COLLECTION_USERS).document(profile.getUserId()).set(profile);
    }

    /**
     * בדיקה אם קיים פרופיל למשתמש (עבור התחברות גוגל)
     */
    public Task<DocumentSnapshot> getUserProfile(String uid) {
        return db.collection(COLLECTION_USERS).document(uid).get();
    }
}