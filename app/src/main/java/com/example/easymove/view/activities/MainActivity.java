package com.example.easymove.view.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.easymove.R;
import com.example.easymove.model.UserSession;
import com.example.easymove.view.fragments.ChatsFragment;
import com.example.easymove.view.fragments.MoveHistoryFragment;
import com.example.easymove.view.fragments.MyDeliveriesFragment;
import com.example.easymove.view.fragments.MyMoveFragment;
import com.example.easymove.view.fragments.NotificationsFragment;
import com.example.easymove.view.fragments.ProfileFragment;
import com.example.easymove.view.fragments.SearchMoverFragment;
import com.example.easymove.view.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // רכיבי UI
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private String userType;
    private static final int PERMISSION_REQUEST_CODE = 101;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        updateFcmToken();
        checkUserProfile(currentUser.getUid());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_drawer_profile) {
            replaceFragment(new ProfileFragment());
            getSupportActionBar().setTitle("אזור אישי");

        } else if (id == R.id.nav_drawer_history) {
            replaceFragment(new MoveHistoryFragment());
            getSupportActionBar().setTitle("היסטוריית הובלות");

        }else if (id == R.id.nav_settings) {
            replaceFragment(new SettingsFragment());
            getSupportActionBar().setTitle("הגדרות");

        } else if (id == R.id.nav_notifications) {
            replaceFragment(new NotificationsFragment());
            getSupportActionBar().setTitle("התראות");

        } else if (id == R.id.nav_drawer_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "";

        if ("customer".equals(userType)) {
            if (id == R.id.nav_my_move) {
                fragment = new MyMoveFragment();
                title = "המעבר שלי";
            } else if (id == R.id.nav_search_move) {
                fragment = new SearchMoverFragment();
                title = "חיפוש מוביל";
            } else if (id == R.id.nav_chats) {
                fragment = new ChatsFragment();
                title = "הצ'אטים שלי";
            }
        }
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
            if (!title.isEmpty()) {
                getSupportActionBar().setTitle(title);
            }
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

                // ✅ אם המשתמש כבר נמצא במסך כלשהו (למשל אחרי סיבוב), לא מחליפים
                if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {

                    // ✅ ניתוב חכם לפי התראות
                    if (checkIntentForNotifications()) {
                        return; // טופל ע"י הפונקציה, לא טוענים מסך ברירת מחדל
                    }

                    // מסך ברירת מחדל רגיל
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
            logout();
        });
    }

    // ✅ הפונקציה המשודרגת שמטפלת בכל סוגי ההתראות
    private boolean checkIntentForNotifications() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();

            // 1. טיפול בצ'אט
            String chatId = extras.getString("chatId");
            if (chatId != null) {
                Intent chatIntent = new Intent(this, com.example.easymove.view.activities.ChatActivity.class);
                chatIntent.putExtra("CHAT_ID", chatId);
                startActivity(chatIntent);
                return true;
            }

            // 2. טיפול בבקשות והודעות מערכת
            String type = extras.getString("type");

            if (type != null) {
                if ("partner_request".equals(type) && "customer".equals(userType)) {
                    // לקוח קיבל בקשת שותפות -> "המעבר שלי"
                    replaceFragment(new MyMoveFragment());
                    getSupportActionBar().setTitle("המעבר שלי");
                    return true;
                }
                else if ("mover_partner_approval".equals(type) && "mover".equals(userType)) {
                    // מוביל קיבל אישור שותף -> "הובלות פתוחות"
                    replaceFragment(new MyDeliveriesFragment());
                    getSupportActionBar().setTitle("הובלות פתוחות");
                    return true;
                }
            }
        }
        return false;
    }

    // כאשר לוחצים על התראה והאפליקציה כבר פתוחה ברקע (SingleTop)
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // מעדכנים את האינטנט הנוכחי
        // כאן אפשר לקרוא שוב ל-checkIntentForNotifications אם רוצים ריענון מיידי
        // אבל לרוב ה-Activity יאותחל מחדש דרך onCreate אם הוא לא היה בפורגראונד
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear();

        if ("customer".equals(userType)) {
            bottomNav.inflateMenu(R.menu.bottom_nav_customer);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_mover);
        }
        bottomNav.setVisibility(View.VISIBLE);
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

    private void updateFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    String token = task.getResult();
                    String uid = FirebaseAuth.getInstance().getUid();

                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("users")
                                .document(uid)
                                .update("fcmToken", token);
                    }
                });
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