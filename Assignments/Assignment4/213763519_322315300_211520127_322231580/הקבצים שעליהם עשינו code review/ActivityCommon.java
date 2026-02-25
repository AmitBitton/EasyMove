package com.example.stepbuystep.ActivityCommon;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityTrainee.TraineeHistoryScreen.HistoryTraineeActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen.TraineeHomeActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeStoreScreen.ShopActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.LeaderboardAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LeaderBoardActivity extends AppCompatActivity{

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView rvAthletes;
    private LeaderboardAdapter adapter;
    private View btnBack;
    private String currentUserRole = "";
    private LinearLayout bottomNavigationBar;
    private LinearLayout navDashboard, navHistory, navShoeStore, navLeaderboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leader_board);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        rvAthletes = findViewById(R.id.rv_athletes);
        btnBack = findViewById(R.id.btn_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        initNavigationViews();


        setupRecyclerView();
        loadUserData();
    }

    private void initNavigationViews() {
        bottomNavigationBar = findViewById(R.id.bottomNavigationBar);
        navDashboard = findViewById(R.id.navDashboard);
        navHistory = findViewById(R.id.navHistory);
        navShoeStore = findViewById(R.id.navShoeStore);
        navLeaderboard = findViewById(R.id.navLeaderboard);
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        adapter.setCurrentUserId(auth.getUid());
        rvAthletes.setLayoutManager(new LinearLayoutManager(this));
        rvAthletes.setAdapter(adapter);

        adapter.setListener(item -> {
            if ("coach".equals(currentUserRole)) {
                showMarkAttendanceDialog(item.id, item.name);
            }
        });
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long coachId = doc.getLong("coachID");
                currentUserRole = doc.getString("role");

                setupNavigationBasedOnRole(currentUserRole);


                if (coachId != null) {
                    fetchTrainees(coachId);
                } else if ("coach".equals(currentUserRole)) {
                    fetchTrainees(doc.getLong("coachID"));
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void setupNavigationBasedOnRole(String role) {
        if (bottomNavigationBar == null) return;

        if ("trainee".equals(role)) {
            bottomNavigationBar.setVisibility(View.VISIBLE);
            btnBack.setVisibility(View.GONE);
            setupTraineeNavigation();
        } else {
            bottomNavigationBar.setVisibility(View.GONE);
            btnBack.setVisibility(View.VISIBLE);

        }
    }

    private void setupTraineeNavigation() {
        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> navigateToTraineePage("dashboard"));
        }
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> navigateToTraineePage("history"));
        }
        if (navShoeStore != null) {
            navShoeStore.setOnClickListener(v -> navigateToTraineePage("shop"));
        }
        if (navLeaderboard != null) {
            navLeaderboard.setOnClickListener(v -> {
                rvAthletes.smoothScrollToPosition(0);
            });
        }

        // Highlight the current page (Leaderboard)
        highlightNavigationItem("leaderboard");
    }

    private void navigateToTraineePage(String page) {
        android.content.Intent intent = null;

        switch (page) {
            case "dashboard":
                intent = new android.content.Intent(this, TraineeHomeActivity.class);
                break;
            case "history":
                intent = new android. content.Intent(this, HistoryTraineeActivity.class);
                break;
            case "shop":
                intent = new android.content.Intent(this, ShopActivity.class);
                break;
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private void highlightNavigationItem(String currentPage) {
        // Reset all nav items to default state
        if (navDashboard != null) resetNavItem(navDashboard);
        if (navHistory != null) resetNavItem(navHistory);
        if (navShoeStore != null) resetNavItem(navShoeStore);
        if (navLeaderboard != null) resetNavItem(navLeaderboard);

        // Highlight the current page
        LinearLayout activeNav = null;
        if ("leaderboard".equals(currentPage)) {
            activeNav = navLeaderboard;
        }

        if (activeNav != null) {
            setNavItemActive(activeNav);
        }
    }

    private void resetNavItem(LinearLayout navItem) {
        ImageView icon = null;
        TextView label = null;
        LinearLayout backgroundContainer = null;

        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                icon = (ImageView) child;
            } else if (child instanceof TextView) {
                label = (TextView) child;
            } else if (child instanceof LinearLayout) {
                backgroundContainer = (LinearLayout) child;
                backgroundContainer.setBackgroundResource(0);
                for (int j = 0; j < backgroundContainer.getChildCount(); j++) {
                    if (backgroundContainer.getChildAt(j) instanceof ImageView) {
                        icon = (ImageView) backgroundContainer.getChildAt(j);
                    }
                }
            }
        }

        if (icon != null) {
            icon.setColorFilter(ContextCompat.getColor(this, R. color.text_secondary));
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this, R. color.text_secondary));
            label.setTypeface(null, android.graphics.Typeface. NORMAL);
        }
    }

    private void setNavItemActive(LinearLayout navItem) {
        ImageView icon = null;
        TextView label = null;
        LinearLayout backgroundContainer = null;

        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                icon = (ImageView) child;
            } else if (child instanceof TextView) {
                label = (TextView) child;
            } else if (child instanceof LinearLayout) {
                backgroundContainer = (LinearLayout) child;
                backgroundContainer.setBackgroundResource(R.drawable.ic_launcher_background);
                backgroundContainer.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_50));
                for (int j = 0; j < backgroundContainer. getChildCount(); j++) {
                    if (backgroundContainer.getChildAt(j) instanceof ImageView) {
                        icon = (ImageView) backgroundContainer.getChildAt(j);
                    }
                }
            }
        }

        if (icon != null) {
            icon.setColorFilter(ContextCompat.getColor(this, R.color. brand_blue));
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void fetchTrainees(Long coachId) {
        if (coachId == null) return;

        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LeaderboardAdapter.LeaderboardItem> tempItems = new ArrayList<>();
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String email = doc.getString("email");
                        String name = (email != null) ? email.split("@")[0] : "Trainee";
                        if (name.length() > 0) name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        String userId = doc.getId();
                        String city = "Unknown";

                        // Create placeholder item
                        LeaderboardAdapter.LeaderboardItem item = new LeaderboardAdapter.LeaderboardItem(userId, name, 0, 1, 1.0, city);
                        tempItems.add(item);

                        // Fetch inventory for this user to find best shoe
                        Task<QuerySnapshot> task = db.collection("users").document(userId).collection("inventory")
                                .orderBy("multiplier", Query.Direction.DESCENDING)
                                .limit(1)
                                .get();
                        tasks.add(task);
                    }

                    if (tasks.isEmpty()) {
                        updateAdapter(new ArrayList<>());
                        return;
                    }

                    Tasks.whenAllComplete(tasks).addOnSuccessListener(results -> {
                        for (int i = 0; i < tasks.size(); i++) {
                            Task<QuerySnapshot> t = tasks.get(i);
                            if (t.isSuccessful() && !t.getResult().isEmpty()) {
                                QueryDocumentSnapshot shoeDoc = (QueryDocumentSnapshot) t.getResult().getDocuments().get(0);
                                Double m = shoeDoc.getDouble("multiplier");
                                Long l = shoeDoc.getLong("tier");
                                if (m != null) tempItems.get(i).multiplier = m;
                                if (l != null) tempItems.get(i).level = l.intValue();
                            }
                        }

                        // Sort by Multiplier Descending
                        Collections.sort(tempItems, (o1, o2) -> Double.compare(o2.multiplier, o1.multiplier));

                        // Assign Ranks
                        for (int i = 0; i < tempItems.size(); i++) {
                            tempItems.get(i).rank = i + 1;
                        }

                        updateAdapter(tempItems);
                    });
                });
    }

    private void updateAdapter(List<LeaderboardAdapter.LeaderboardItem> items) {
        adapter.setItems(items);
        View emptyState = findViewById(R.id.card_empty_state);
        if (emptyState != null) {
            emptyState.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showMarkAttendanceDialog(String traineeId, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance for " + name)
                .setMessage("Award coins for attendance?")
                .setPositiveButton("Award 50 Coins", (dialog, which) -> {
                    awardCoins(traineeId, 50);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void awardCoins(String traineeId, int amount) {
        db.collection("users").document(traineeId)
                .update("coin_balance", com.google.firebase.firestore.FieldValue.increment(amount))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Coins awarded!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}