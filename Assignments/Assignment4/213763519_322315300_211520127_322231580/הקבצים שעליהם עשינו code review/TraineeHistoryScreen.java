package com.example.stepbuystep.ActivityTrainee.TraineeHistoryScreen;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityTrainee.BaseTraineeActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.HistoryAdapter;
import com.example.stepbuystep.model.HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import java.text.ParseException;
import java.text. SimpleDateFormat;
import java. util.Calendar;
import java.util.Date;
import java.util. Locale;

public class HistoryTraineeActivity extends BaseTraineeActivity {
    private TextView tvTotalWorkouts, tvTotalDistance;
    private TextView tvTotalCalories, tvTotalPoints;
    private RecyclerView rvHistory;
    private View cardEmpty;

    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainee_history);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupNavigationBar(NavItem.HISTORY);
        setupRecyclerView();
        fetchHistory();
    }

    private void initViews() {
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTotalPoints = findViewById(R.id.tvTotalPoints);
        rvHistory = findViewById(R.id.rvHistory);
        cardEmpty = findViewById(R.id.cardEmpty);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private boolean isPastWorkout(String dateStr) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date workoutDate = dateFormat. parse(dateStr);

            if (workoutDate == null) return false;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar. SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            // Past if before today
            return workoutDate. getTime() < today.getTimeInMillis();
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Replace the fetchHistory() method
    private void fetchHistory() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("training_history")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction. DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HistoryItem> items = new ArrayList<>();
                    double totalDist = 0;
                    int count = 0;
                    int totalCalories = 0;
                    int totalPoints = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        Double dist = doc.getDouble("distance");
                        Long steps = doc.getLong("steps");
                        String date = doc.getString("date");
                        Long timestamp = doc. getLong("timestamp");

                        if (type == null) type = "Workout";
                        if (dist == null) dist = 0.0;
                        if (steps == null) steps = 0L;
                        if (timestamp == null) timestamp = 0L;

                        totalDist += dist;
                        count++;

                        totalCalories += (int)(dist * 60);
                        totalPoints += (int)(dist * 100);

                        String subtitle = String.format("%.2f km • %d steps", dist, steps);
                        String iconType = type.toLowerCase().contains("run") ? "run" : "walk";

                        items. add(new HistoryItem(doc.getId(), type, subtitle, date, timestamp, iconType));
                    }

                    fetchPastScheduledWorkouts(uid, items, count, totalDist, totalCalories, totalPoints);
                });
    }

    private void fetchPastScheduledWorkouts(String uid, List<HistoryItem> existingItems,
                                            int count, double totalDist, int totalCalories, int totalPoints) {
        db.collection("workouts")
                .whereArrayContains("traineeIds", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String location = doc.getString("location");

                        // Only add if it's a past workout
                        if (date != null && isPastWorkout(date)) {
                            String subtitle = String.format("%s at %s", time != null ? time : "", location != null ? location : "");
                            String iconType = type != null && type.toLowerCase().contains("run") ? "run" : "walk";

                            // Use 0 as timestamp for scheduled workouts (or you could parse date to timestamp)
                            existingItems.add(new HistoryItem(doc.getId(),
                                    type != null ?  type + " (Scheduled)" : "Scheduled Workout",
                                    subtitle, date, 0L, iconType));
                        }
                    }

                    updateUI(existingItems, count, totalDist, totalCalories, totalPoints);
                })
                .addOnFailureListener(e -> {
                    // If scheduled workouts fetch fails, still show tracking history
                    updateUI(existingItems, count, totalDist, totalCalories, totalPoints);
                });
    }

    private void updateUI(List<HistoryItem> items, int count, double totalDist, int calories, int points) {
        tvTotalWorkouts.setText(String.valueOf(count));
        tvTotalDistance.setText(String.format("%.1f", totalDist));

        if (tvTotalCalories != null) tvTotalCalories.setText(String.valueOf(calories));
        if (tvTotalPoints != null) tvTotalPoints.setText(String.valueOf(points));

        if (items.isEmpty()) {
            cardEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            cardEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
    }
}