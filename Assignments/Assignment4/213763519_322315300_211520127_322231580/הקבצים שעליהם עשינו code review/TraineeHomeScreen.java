package com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.stepbuystep.R;
import com.example.stepbuystep.service.TrackingService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class TrackingActivity extends ComponentActivity {

    private TrackingService trackingService;
    private boolean isBound = false;

    private TextView tvStats, tvEquipmentInfo;
    private Button btnStart, btnStop;
    private RadioGroup rgActivityType;
    private RadioButton rbWalk, rbRun;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private double currentMultiplier = 1.0;
    private String selectedType = "walking_shoes";

    private final Handler handler = new Handler();
    private final Runnable updateStatsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && trackingService != null) {
                double dist = trackingService.getDistance();
                int steps = trackingService.getSteps();
                tvStats.setText(String.format("Distance: %.2f km\nSteps: %d", dist, steps));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            trackingService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) allGranted = false;
                }
                if (allGranted) {
                    bindService();
                } else {
                    Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvStats = findViewById(R.id.tvStats);
        tvEquipmentInfo = findViewById(R.id.tvEquipmentInfo);
        btnStart = findViewById(R.id.btnStartTracking);
        btnStop = findViewById(R.id.btnStopTracking);
        rgActivityType = findViewById(R.id.rgActivityType);
        rbWalk = findViewById(R.id.rbWalk);
        rbRun = findViewById(R.id.rbRun);

        rgActivityType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbWalk) selectedType = "walking_shoes";
            else selectedType = "running_shoes";
            fetchBestEquipment();
        });

        btnStart.setOnClickListener(v -> startSession());
        btnStop.setOnClickListener(v -> stopSession());

        checkPermissions();
        fetchBestEquipment();
    }

    private void checkPermissions() {
        java.util.List<String> permList = new java.util.ArrayList<>();
        permList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permList.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        String[] permissions = permList.toArray(new String[0]);

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            bindService();
        } else {
            requestPermissionLauncher.launch(permissions);
        }
    }

    private void bindService() {
        Intent intent = new Intent(this, TrackingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void fetchBestEquipment() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Default
        if (selectedType.equals("walking_shoes")) currentMultiplier = 2.0; // Base walk
        else currentMultiplier = 5.0; // Base run

        db.collection("users").document(uid).collection("inventory")
                .whereEqualTo("type", selectedType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double maxMult = currentMultiplier;
                    String name = "Default";
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double m = doc.getDouble("multiplier");
                        if (m != null && m > maxMult) {
                            maxMult = m;
                            name = doc.getString("name");
                        }
                    }
                    currentMultiplier = maxMult;
                    tvEquipmentInfo.setText("Equipment: " + name + " (" + currentMultiplier + "x coins/km)");
                });
    }

    private void startSession() {
        // Start Foreground Service
        Intent intent = new Intent(this, TrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        if (!isBound || trackingService == null) return;

        trackingService.startTracking();
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);
        rgActivityType.setEnabled(false);
        handler.post(updateStatsRunnable);
    }

    private void stopSession() {
        if (!isBound || trackingService == null) return;

        double distance = trackingService.getDistance();
        trackingService.stopTracking();
        handler.removeCallbacks(updateStatsRunnable);

        long earnedCoins = (long) (distance * currentMultiplier);

        // Update DB
        String uid = auth.getUid();
        if (uid != null && earnedCoins > 0) {
            // Update Coin Balance
            db.collection("users").document(uid)
                    .update("coin_balance", com.google.firebase.firestore.FieldValue.increment(earnedCoins))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Earned " + earnedCoins + " coins!", Toast.LENGTH_LONG).show();
                        saveSessionToHistory(uid, distance, trackingService.getSteps(), earnedCoins);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving coins", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            Toast.makeText(this, "Session ended. Distance too short.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveSessionToHistory(String uid, double distance, int steps, long earnedCoins) {
        String type = rbRun.isChecked() ? "Run" : "Walk";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String date = sdf.format(new java.util.Date());

        Map<String, Object> session = new java.util.HashMap<>();
        session.put("userId", uid);
        session.put("type", type);
        session.put("distance", distance);
        session.put("steps", steps);
        session.put("earnedCoins", earnedCoins);
        session.put("date", date);
        session.put("timestamp", System.currentTimeMillis());

        db.collection("training_history").add(session)
                .addOnCompleteListener(task -> finish())
                .addOnFailureListener(e -> finish()); // Ensure we finish even if history save fails
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        handler.removeCallbacks(updateStatsRunnable);
    }
}