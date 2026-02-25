package com.example.stepbuystep.ActivityCoach.CoachHomeScreen;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget. Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.ActivityCommon.LeaderBoardActivity;
import com.example.stepbuystep.ActivityCommon.LoginActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.UpcomingWorkoutAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CoachHomeActivity extends BaseCoachActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvCoachIdShort;
    private TextView tvUserName;
    private TextView tvActiveAthletesCount;
    private TextView tvPendingRequestsCount;
    private LinearLayout cardActiveAthletes;
    private LinearLayout cardPendingRequests;
    private TextView badgeUpcomingCount;
    private LinearLayout btnCopyCoachId;
    private LinearLayout btnBroadcastMessage;
    private LinearLayout btnLogout;
    private RecyclerView rvUpcoming;
    private View cardNoUpcoming;

    private long coachIdValue = 0;
    private UpcomingWorkoutAdapter upcomingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupNavigationBar(BaseCoachActivity.NavItem.DASH_COACH);
        setupRecyclerView();
        setupListeners();
        fetchCoachData();
        fetchUpcomingWorkouts();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvCoachIdShort = findViewById(R.id.tvCoachIdShort);
        tvActiveAthletesCount = findViewById(R.id.tvActiveAthletesCount);
        cardActiveAthletes = findViewById(R.id.cardActiveAthletes);
        tvPendingRequestsCount = findViewById(R.id.tvPendingRequestsCount);
        cardPendingRequests = findViewById(R.id.cardPendingRequests);
        badgeUpcomingCount = findViewById(R.id.badgeUpcomingCount);

        btnCopyCoachId = findViewById(R.id.btnCopyCoachId);
        btnBroadcastMessage = findViewById(R.id.btnBroadcastMessage);
        btnLogout = findViewById(R. id.btnLogout);
        rvUpcoming = findViewById(R.id.rvUpcoming);
        cardNoUpcoming = findViewById(R.id.cardNoUpcoming);
    }

    private void setupRecyclerView() {
        upcomingAdapter = new UpcomingWorkoutAdapter();
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setAdapter(upcomingAdapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            auth. signOut();
            startActivity(new Intent(this, LoginActivity. class));
            finish();
        });

        btnCopyCoachId.setOnClickListener(v -> {
            if (coachIdValue != 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Coach ID", String.valueOf(coachIdValue));
                clipboard. setPrimaryClip(clip);
                Toast.makeText(this, "Copied ID: " + coachIdValue, Toast.LENGTH_SHORT).show();
            }
        });

        btnBroadcastMessage.setOnClickListener(v -> showBroadcastDialog());

        cardActiveAthletes.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderBoardActivity.class))
        );

        cardPendingRequests.setOnClickListener(v ->
                startActivity(new Intent(this, PendingRequestsActivity.class))
        );
    }

    private void showBroadcastDialog() {
        BroadcastMessageDialogActivity dialog = new BroadcastMessageDialogActivity();
        dialog.show(getSupportFragmentManager(),"BroadcastMessageDialog");
    }

    private void fetchCoachData() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long cid = doc.getLong("coachID");
                        if (cid != null) {
                            coachIdValue = cid;
                            tvCoachIdShort.setText("ID: " + coachIdValue);
                            fetchAthletesCount(coachIdValue);
                            fetchPendingRequestsCount(coachIdValue);
                        } else {
                            tvCoachIdShort.setText("ID: N/A");
                        }
                        String email = doc.getString("email");
                        String name = email != null ? email.split("@")[0] : "Trainee";
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }

                        tvUserName.setText(name);
                    }
                });
    }

    private void fetchPendingRequestsCount(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot != null) {
                        int count = querySnapshot. size();
                        tvPendingRequestsCount.setText(String.valueOf(count));
                    }
                });
    }

    private void fetchAthletesCount(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot != null) {
                        int count = querySnapshot.size();
                        tvActiveAthletesCount.setText(String.valueOf(count));
                    }
                });
    }



    private void fetchUpcomingWorkouts() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("workouts")
                .whereEqualTo("coachId", uid)
                . orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UpcomingWorkoutAdapter. WorkoutItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String location = doc.getString("location");

                        if (type == null) type = "Workout";
                        if (date == null) date = "";
                        if (time == null) time = "";
                        if (location == null) location = "";


                        items.add(new UpcomingWorkoutAdapter.WorkoutItem(doc.getId(), type, date, time, location, 0));
                    }

                    if (items.isEmpty()) {
                        rvUpcoming.setVisibility(View.GONE);
                        cardNoUpcoming. setVisibility(View.VISIBLE);
                        badgeUpcomingCount.setText("0");
                    } else {
                        rvUpcoming.setVisibility(View.VISIBLE);
                        cardNoUpcoming.setVisibility(View.GONE);
                        badgeUpcomingCount.setText(String.valueOf(items.size()));
                        upcomingAdapter.setItems(items);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (coachIdValue != 0) {
            fetchAthletesCount(coachIdValue);
            fetchPendingRequestsCount(coachIdValue);
        }
        fetchUpcomingWorkouts();
    }
}