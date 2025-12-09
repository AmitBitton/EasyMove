package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.view.fragments.MyDeliveriesFragment;
import com.example.easymove.view.fragments.MyMoveFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNav;

    // "customer" or "mover"
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        bottomNav = findViewById(R.id.bottom_navigation);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // קודם כל בודקים את הפרופיל כדי לדעת איזה תפריט להציג
        checkUserProfile(currentUser.getUid());

        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
    }

    private void checkUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        Toast.makeText(this, "אין פרופיל למשתמש, התחברי מחדש", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        startActivity(new Intent(this, AuthActivity.class));
                        finish();
                        return;
                    }

                    UserProfile profile = document.toObject(UserProfile.class);
                    if (profile == null) {
                        Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        startActivity(new Intent(this, AuthActivity.class));
                        finish();
                        return;
                    }

                    userType = profile.getUserType();
                    if (userType == null || userType.isEmpty()) {
                        // אם תרצי – כאן אפשר לפתוח מסך "השלמת פרופיל"
                        userType = "customer"; // ברירת מחדל
                    }

                    setupBottomNav(userType);

                    // מסך ה"בית" אחרי התחברות:
                    if ("customer".equals(userType)) {
                        // "המעבר שלי" הוא הבית של הלקוח
                        replaceFragment(new MyMoveFragment());
                    } else if ("mover".equals(userType)) {
                        // "ההובלות שלי" הוא הבית של המוביל
                        replaceFragment(new MyDeliveriesFragment());
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת פרופיל: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    auth.signOut();
                    startActivity(new Intent(this, AuthActivity.class));
                    finish();
                });
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear();

        if ("customer".equals(userType)) {
            // תפריט תחתון ללקוח
            bottomNav.inflateMenu(R.menu.bottom_nav_customer);
        } else if ("mover".equals(userType)) {
            // תפריט תחתון למוביל
            bottomNav.inflateMenu(R.menu.bottom_nav_mover);
        }
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        if ("customer".equals(userType)) {

            if (id == R.id.nav_my_move) {
                // "המעבר שלי" – זה הבית של הלקוח
                fragment = new MyMoveFragment();

            } else if (id == R.id.nav_partner) {
                // TODO: ליצור PartnerSearchFragment – מסך חיפוש שותף למעבר
                // fragment = new PartnerSearchFragment();

            } else if (id == R.id.nav_movers) {
                // TODO: ליצור MoversSearchFragment – מסך חיפוש מובילים
                // fragment = new MoversSearchFragment();

            } else if (id == R.id.nav_chats) {
                // TODO: ליצור ChatsFragment – רשימת צ'אטים של הלקוח
                // fragment = new ChatsFragment();
            }

        } else if ("mover".equals(userType)) {

            if (id == R.id.nav_my_deliveries) {
                // "ההובלות שלי" – זה הבית של המוביל
                fragment = new MyDeliveriesFragment();

            } else if (id == R.id.nav_chats) {
                // TODO: ליצור ChatsFragment – רשימת צ'אטים של המוביל
                // fragment = new ChatsFragment();
            }
        }

        if (fragment != null) {
            replaceFragment(fragment);
            return true;
        }

        return false;
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
