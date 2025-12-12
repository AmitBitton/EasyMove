package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.UserSession; // וודא שזה קיים
import com.example.easymove.view.fragments.MyDeliveriesFragment;
import com.example.easymove.view.fragments.MyMoveFragment;
import com.example.easymove.view.fragments.ProfileFragment;
// import com.example.easymove.view.fragments.SearchMoveFragment; // נצטרך ליצור
// import com.example.easymove.view.fragments.ChatsFragment; // נצטרך ליצור
import com.example.easymove.view.fragments.SearchMoverFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // רכיבי UI
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור לרכיבים ב-XML
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // הגדרת ה-Toolbar כסרגל הראשי

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);

        // הגדרת כפתור ההמבורגר
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // מאזינים
        navigationView.setNavigationItemSelectedListener(this);
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);

        // בדיקת משתמש
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        checkUserProfile(currentUser.getUid());
    }

    // --- טיפול בתפריט הצד (Drawer) ---
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_drawer_profile) {
            replaceFragment(new ProfileFragment());
        } else if (id == R.id.nav_drawer_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // --- טיפול בתפריט התחתון (Bottom Nav) ---
    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        // לוגיקה ללקוח (לפי הבקשה שלך: 3 כפתורים)
        if ("customer".equals(userType)) {
            if (id == R.id.nav_my_move) {
                fragment = new MyMoveFragment();
            } else if (id == R.id.nav_search_move) {
                fragment = new SearchMoverFragment();
            } else if (id == R.id.nav_chats) {
                // TODO: fragment = new ChatsFragment();
                Toast.makeText(this, "כאן יהיו הצ'אטים", Toast.LENGTH_SHORT).show();
            }
        }
        // לוגיקה למוביל
        else if ("mover".equals(userType)) {
            if (id == R.id.nav_my_deliveries) { // צריך לוודא שזה קיים ב-XML של המוביל
                fragment = new MyDeliveriesFragment();
            }
            // ... שאר הכפתורים למוביל
        }

        if (fragment != null) {
            replaceFragment(fragment);
            return true;
        }
        return false;
    }

    private void checkUserProfile(String uid) {
        // טעינה דרך UserSession (מומלץ) או ישירות
        UserSession.getInstance().ensureStarted().addOnSuccessListener(profile -> {
            if (profile != null) {
                userType = profile.getUserType();
                if (userType == null) userType = "customer";

                setupBottomNav(userType);

                // מסך הבית
                if ("customer".equals(userType)) {
                    replaceFragment(new MyMoveFragment());
                    getSupportActionBar().setTitle("המעבר שלי");
                } else {
                    replaceFragment(new MyDeliveriesFragment());
                    getSupportActionBar().setTitle("ההובלות שלי");
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
            logout(); // אם נכשל, עדיף לצאת
        });
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear();
        if ("customer".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_customer);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_mover);
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void logout() {
        auth.signOut();
        UserSession.getInstance().stop(); // ניקוי ה-Session
        startAuth();
    }

    private void startAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    // סגירת התפריט בלחיצה על 'חזור' אם הוא פתוח
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}