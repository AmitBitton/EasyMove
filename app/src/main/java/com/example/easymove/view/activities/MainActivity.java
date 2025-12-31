package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.easymove.R;
import com.example.easymove.model.UserSession;
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

    // UI Components
    private BottomNavigationView bottomNav;
    private DrawerLayout drawerLayout;

    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Init Firebase
        auth = FirebaseAuth.getInstance();

        // Dark Mode
        android.content.SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        if (sharedPref.getBoolean("isDarkMode", false)) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        // 2. Check Login - If not logged in, go to Auth immediately
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startAuth();
            return;
        }

        handleNotificationIntent(getIntent());

        // Setup UI
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

        // 3. Start Loading Profile (We wait for this before handling notifications)
        checkUserProfile(currentUser.getUid());

        setupPushNotifications();
    }

    // --- SIDEBAR MENU HANDLER ---
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_drawer_profile) {
            replaceFragment(new ProfileFragment());
        }
        else if (id == R.id.nav_drawer_history) {
            // --- ADD THIS LINE ---
            replaceFragment(new com.example.easymove.view.fragments.MoveHistoryFragment());
        }
        else if (id == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new SettingsFragment()).addToBackStack(null).commit();
        }
        else if (id == R.id.nav_notifications) {
            replaceFragment(new NotificationsFragment());
        }
        else if (id == R.id.nav_drawer_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // --- BOTTOM NAVIGATION HANDLER (Where Search Goes!) ---
    private boolean onBottomNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String title = "";

        if ("customer".equals(userType)) {
            if (id == R.id.nav_my_move) { fragment = new MyMoveFragment(); title = "המעבר שלי"; }
            else if (id == R.id.nav_search_move) { fragment = new SearchMoverFragment(); title = "חיפוש מוביל"; }
            else if (id == R.id.nav_chats) { fragment = new com.example.easymove.view.fragments.ChatsFragment(); title = "הצ'אטים שלי"; }
        } else if ("mover".equals(userType)) {
            if (id == R.id.nav_my_deliveries) { fragment = new MyDeliveriesFragment(); title = "הובלות"; }
            else if (id == R.id.nav_chats) { fragment = new com.example.easymove.view.fragments.ChatsFragment(); title = "הצ'אטים שלי"; }
        }

        if (fragment != null) {
            replaceFragment(fragment);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(title);
            return true;
        }
        return false;
    }

    // --- PUSH NOTIFICATION HANDLER ---
    private void setupPushNotifications() {

        // 1. Check local preference FIRST
        android.content.SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isEnabled = sharedPref.getBoolean("notifications_enabled", true); // Default is true (ON)

        // If user turned it OFF in settings, STOP here. Do not send token to server.
        if (!isEnabled) {
            return;
        }

        // 1. Ask for Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // 2. Get Token and Save to Firestore
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        return;
                    }
                    // Get new FCM registration token
                    String token = task.getResult();

                    // Save to current user's profile
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null) {
                        FirebaseFirestore.getInstance().collection("users")
                                .document(user.getUid())
                                .update("fcmToken", token);
                    }
                });
    }

    // Handle notifications if the app is ALREADY open
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleNotificationIntent(getIntent());

        // If app is already running, UserSession is definitely loaded, so we can go straight there.
        if (intent.hasExtra("chatId")) {
            String chatId = intent.getStringExtra("chatId");
            Intent chatIntent = new Intent(this, com.example.easymove.view.activities.ChatActivity.class);
            chatIntent.putExtra("CHAT_ID", chatId);
            startActivity(chatIntent);
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) return;

        // 1. Check for Chat Notification
        if (intent.hasExtra("chatId")) {
            String chatId = intent.getStringExtra("chatId");

            Intent chatIntent = new Intent(this, com.example.easymove.view.activities.ChatActivity.class);
            chatIntent.putExtra("CHAT_ID", chatId);
            startActivity(chatIntent);
        }
        // 2. Check for Order Update Notification
        else if (intent.hasExtra("type") && "order_update".equals(intent.getStringExtra("type"))) {
            // Navigate to the relevant fragment (MyMoveFragment for customers)
            replaceFragment(new MyMoveFragment());
            // Optional: Set title for clarity
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("המעבר שלי");
        }
    }

    private void checkUserProfile(String uid) {
        UserSession.getInstance().ensureStarted().addOnSuccessListener(profile -> {
            if (isFinishing() || isDestroyed()) return;

            if (profile != null) {
                userType = profile.getUserType();
                if (userType == null) userType = "customer";

                setupBottomNav(userType);

                // --- CRITICAL FIX START ---
                // We are now sure the UserSession is ready.
                // NOW we check if we need to open a specific chat.
                if (getIntent().hasExtra("chatId")) {
                    String chatId = getIntent().getStringExtra("chatId");

                    // Consume the extra so we don't reopen it on rotation
                    getIntent().removeExtra("chatId");

                    Intent chatIntent = new Intent(MainActivity.this, com.example.easymove.view.activities.ChatActivity.class);
                    chatIntent.putExtra("CHAT_ID", chatId);
                    startActivity(chatIntent);
                }
                else {
                    // Normal startup - load default fragment
                    if ("customer".equals(userType)) {
                        replaceFragment(new MyMoveFragment());
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle("המעבר שלי");
                    } else {
                        replaceFragment(new MyDeliveriesFragment());
                        if (getSupportActionBar() != null) getSupportActionBar().setTitle("ההובלות שלי");
                    }
                }
                // --- CRITICAL FIX END ---
            }
        }).addOnFailureListener(e -> {
            if (isFinishing() || isDestroyed()) return;
            // Never logout automatically here. Just warn.
            Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 101 is the request code we used in setupPushNotifications()
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_DENIED) {

                // --- USER CLICKED "DON'T ALLOW" ---

                // 1. Update Local "Memory" so we don't ask again
                getSharedPreferences("AppSettings", MODE_PRIVATE)
                        .edit()
                        .putBoolean("notifications_enabled", false) // Mark as disabled
                        .apply();

                // 2. Remove Token from Firestore
                // This ensures that when they open Settings later, the switch will be OFF.
                if (auth.getCurrentUser() != null) {
                    FirebaseFirestore.getInstance().collection("users")
                            .document(auth.getCurrentUser().getUid())
                            .update("fcmToken", com.google.firebase.firestore.FieldValue.delete());
                }

                Toast.makeText(this, "התראות כובו", Toast.LENGTH_SHORT).show();
            }
        }
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

    public void replaceFragment(Fragment fragment) {
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