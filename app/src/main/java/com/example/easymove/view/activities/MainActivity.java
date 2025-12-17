package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.easymove.R;
import com.example.easymove.model.UserSession;
import com.example.easymove.view.fragments.MyDeliveriesFragment;
import com.example.easymove.view.fragments.MyMoveFragment;
import com.example.easymove.view.fragments.ProfileFragment;
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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        checkUserProfile(currentUser.getUid());
    }

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

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;

        // לוגיקה ללקוח
        if ("customer".equals(userType)) {
            if (id == R.id.nav_my_move) {
                fragment = new MyMoveFragment();
                getSupportActionBar().setTitle("המעבר שלי");
            } else if (id == R.id.nav_search_move) {
                fragment = new SearchMoverFragment();
                getSupportActionBar().setTitle("חיפוש מוביל");
            } else if (id == R.id.nav_chats) {
                fragment = new com.example.easymove.view.fragments.ChatsFragment();
                getSupportActionBar().setTitle("הצ'אטים שלי");
            }
        }
        // לוגיקה למוביל
        else if ("mover".equals(userType)) {
            if (id == R.id.nav_my_deliveries) {
                fragment = new MyDeliveriesFragment();
                getSupportActionBar().setTitle("הובלות");
            }
            else if (id == R.id.nav_chats) { // וודא שזה ה-ID בקובץ menu של המוביל
                fragment = new com.example.easymove.view.fragments.ChatsFragment();
                getSupportActionBar().setTitle("הצ'אטים שלי");
            }
        }

        if (fragment != null) {
            replaceFragment(fragment);
            return true;
        }
        return false;
    }

    private void checkUserProfile(String uid) {
        UserSession.getInstance().ensureStarted().addOnSuccessListener(profile -> {
            if (profile != null) {
                userType = profile.getUserType();
                if (userType == null) userType = "customer";

                setupBottomNav(userType);

                // ניווט ברירת מחדל במסך הבית
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
            logout();
        });
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear(); // ניקוי הקיים

        if ("customer".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_customer);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_mover);
        }

        // --- התיקון: הצגת הסרגל רק עכשיו ---
        bottomNav.setVisibility(View.VISIBLE);
        // -----------------------------------
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void logout() {
        auth.signOut();
        UserSession.getInstance().stop();
        startAuth();
    }

    private void startAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}