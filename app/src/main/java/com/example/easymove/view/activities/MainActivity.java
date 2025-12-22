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
import com.example.easymove.view.fragments.ChatsFragment;
import com.example.easymove.view.fragments.MoveHistoryFragment;
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

        // אתחול פיירבייס
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור ל-XML
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);

        // הגדרת כפתור ההמבורגר (Drawer Toggle)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // הגדרת מאזינים לתפריטים
        navigationView.setNavigationItemSelectedListener(this);
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);

        // בדיקה אם המשתמש מחובר
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        // טעינת סוג המשתמש (לקוח או מוביל) והגדרת המסך בהתאם
        checkUserProfile(currentUser.getUid());
    }

    /**
     * טיפול בלחיצה על תפריט הצד (המבורגר)
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_drawer_profile) {
            replaceFragment(new ProfileFragment());
            getSupportActionBar().setTitle("אזור אישי");

        } else if (id == R.id.nav_drawer_history) {
            // --- הוספה חדשה: מעבר להיסטוריה ---
            replaceFragment(new MoveHistoryFragment());
            getSupportActionBar().setTitle("היסטוריית הובלות");

        } else if (id == R.id.nav_drawer_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * טיפול בלחיצה על הסרגל התחתון
     */
    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "";

        // --- לוגיקה ללקוח ---
        if ("customer".equals(userType)) {
            if (id == R.id.nav_my_move) {
                fragment = new MyMoveFragment();


            } else if (id == R.id.nav_search_move) {
                fragment = new SearchMoverFragment();
                title = "חיפוש מוביל";
            } else if (id == R.id.nav_chats) {
                fragment = new ChatsFragment();
                title = "הצ'אטים שלי";
            }
        }
        // --- לוגיקה למוביל ---
        else if ("mover".equals(userType)) {
            if (id == R.id.nav_my_deliveries) {
                fragment = new MyDeliveriesFragment();
                title = "הובלות פתוחות";
            }
            else if (id == R.id.nav_chats) {
                fragment = new ChatsFragment();
                title = "הצ'אטים שלי";
            }
        }

        if (fragment != null) {
            replaceFragment(fragment);
            getSupportActionBar().setTitle(title);
            return true;
        }
        return false;
    }

    /**
     * טעינת פרופיל המשתמש ובניית התפריט התחתון בהתאם
     */
    private void checkUserProfile(String uid) {
        // משתמשים ב-UserSession כדי למנוע קריאות מיותרות ל-DB
        UserSession.getInstance().ensureStarted().addOnSuccessListener(profile -> {
            if (profile != null) {
                userType = profile.getUserType();
                if (userType == null) userType = "customer"; // ברירת מחדל

                setupBottomNav(userType);

                // ניווט למסך הבית (ברירת מחדל בכניסה הראשונה)
                // אם אנחנו כבר במסך כלשהו (למשל אחרי סיבוב מסך), לא נחליף אותו
                if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
                    if ("customer".equals(userType)) {
                        replaceFragment(new MyMoveFragment());
                        getSupportActionBar().setTitle("המעבר שלי");
                    } else {
                        replaceFragment(new MyDeliveriesFragment());
                        getSupportActionBar().setTitle("הובלות");
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
            logout(); // אם אי אפשר לטעון פרופיל, מתנתקים
        });
    }

    /**
     * טעינת קובץ התפריט הנכון לסרגל התחתון
     */
    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear(); // ניקוי תפריט קודם אם היה

        if ("customer".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_customer);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_mover);
        }

        // הצגת הסרגל רק לאחר שהוחלט איזה תפריט להציג
        bottomNav.setVisibility(View.VISIBLE);
    }

    /**
     * פונקציית עזר להחלפת פרגמנטים
     */
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void logout() {
        auth.signOut();
        UserSession.getInstance().stop(); // ניקוי ה-Session המקומי
        startAuth();
    }

    private void startAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    // סגירת התפריט הצדדי בלחיצה על "חזור"
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}