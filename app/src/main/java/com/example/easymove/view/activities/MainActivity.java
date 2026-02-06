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

/**
 * The Main Entry point of the application after authentication.
 * It manages:
 * 1. The Navigation Drawer (Side menu).
 * 2. The Bottom Navigation Bar (Context-aware based on User Type).
 * 3. Fragment navigation.
 * 4. Handling Deep Links/Notifications (e.g., opening a specific chat).
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Constants to prevent magic strings
    private static final String TYPE_CUSTOMER = "customer";
    private static final String TYPE_MOVER = "mover";
    private static final int PERMISSION_REQUEST_CODE = 101;

    // Firebase
    private FirebaseAuth auth;

    // UI Components
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;

    // State
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Firebase
        auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2. Setup UI
        initViews();

        // 3. Permission Check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }

        // 4. Check Auth Status
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        // 5. Initial Data Load
        updateFcmToken();
        checkUserProfile(currentUser.getUid());
    }

    /**
     * Initialize UI components and listeners.
     */
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        bottomNav = findViewById(R.id.bottom_navigation);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
    }

    /**
     * Handles clicks on the Side Drawer menu items.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "";

        if (id == R.id.nav_drawer_profile) {
            fragment = new ProfileFragment();
            title = "אזור אישי";
        } else if (id == R.id.nav_drawer_history) {
            fragment = new MoveHistoryFragment();
            title = "היסטוריית הובלות";
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
            title = "הגדרות";
        } else if (id == R.id.nav_notifications) {
            fragment = new NotificationsFragment();
            title = "התראות";
        } else if (id == R.id.nav_drawer_logout) {
            logout();
            return true;
        }

        if (fragment != null) {
            replaceFragment(fragment);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Handles clicks on the Bottom Navigation Bar items.
     * Logic changes based on User Type (Customer vs Mover).
     */
    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "";

        if (TYPE_CUSTOMER.equals(userType)) {
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
        } else if (TYPE_MOVER.equals(userType)) {
            if (id == R.id.nav_my_deliveries) {
                fragment = new MyDeliveriesFragment();
                title = "הובלות פתוחות";
            } else if (id == R.id.nav_chats) {
                fragment = new ChatsFragment();
                title = "הצ'אטים שלי";
            }
        }

        if (fragment != null) {
            replaceFragment(fragment);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            return true;
        }
        return false;
    }

    /**
     * Fetches the user profile from UserSession to determine User Type (Customer/Mover).
     * Sets up the correct Bottom Navigation Menu and handles Deep Linking (Notifications).
     */
    private void checkUserProfile(String uid) {
        UserSession.getInstance().ensureStarted().addOnSuccessListener(profile -> {
            if (profile != null) {
                userType = profile.getUserType();
                if (userType == null) userType = TYPE_CUSTOMER; // Default fallback

                setupBottomNav(userType);

                // Only load default fragment if we are not restoring state
                if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {

                    // 1. Check if we opened via Notification (Deep Link)
                    if (checkIntentForNotifications()) {
                        return; // Handled, skip default fragment
                    }

                    // 2. Load Default Fragment
                    if (TYPE_CUSTOMER.equals(userType)) {
                        replaceFragment(new MyMoveFragment());
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle("המעבר שלי");
                    } else {
                        replaceFragment(new MyDeliveriesFragment());
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle("הובלות");
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
            logout();
        });
    }

    /**
     * Checks if the Activity was started with specific extras (from a Notification click).
     * Handles routing to Chat or specific Request pages.
     *
     * @return true if a deep link was handled, false otherwise.
     */
    private boolean checkIntentForNotifications() {
        if (getIntent() != null && getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();

            // 1. Handle Chat Notification
            String chatId = extras.getString("chatId");
            if (chatId != null) {
                Intent chatIntent = new Intent(this, com.example.easymove.view.activities.ChatActivity.class);
                chatIntent.putExtra("CHAT_ID", chatId);
                startActivity(chatIntent);
                return true;
            }

            // 2. Handle System Requests (Partner Request / Approval)
            String type = extras.getString("type");
            if (type != null) {
                if ("partner_request".equals(type) && TYPE_CUSTOMER.equals(userType)) {
                    replaceFragment(new MyMoveFragment());
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle("המעבר שלי");
                    return true;
                } else if ("mover_partner_approval".equals(type) && TYPE_MOVER.equals(userType)) {
                    replaceFragment(new MyDeliveriesFragment());
                    if (getSupportActionBar() != null) getSupportActionBar().setTitle("הובלות פתוחות");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Called when the activity receives a new Intent (e.g., clicking a notification while app is running).
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the current intent

        // Immediately check if this new intent requires navigation
        // (Since checkUserProfile might have already run)
        if (userType != null) {
            checkIntentForNotifications();
        }
    }

    private void setupBottomNav(String userType) {
        bottomNav.getMenu().clear();

        if (TYPE_CUSTOMER.equals(userType)) {
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
        UserSession.getInstance().stop(); // Clear session cache
        startAuth();
    }

    private void startAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    /**
     * Refreshes the FCM token and updates it in Firestore.
     * Crucial for receiving Push Notifications.
     */
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