package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.MoveHistoryAdapter;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.UserSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.List;

/**
 * MoveHistoryFragment
 * -------------------
 * Displays the move history of the currently authenticated user.
 *
 * Included move statuses:
 * - CONFIRMED
 * - COMPLETED
 * - CANCELED
 *
 * The query logic depends on the user's role:
 * - Customer → filtered by customerId
 * - Mover → filtered by moverId
 *
 * Data is fetched from Firebase Firestore and displayed in a RecyclerView.
 */
public class MoveHistoryFragment extends Fragment {

    /** RecyclerView that displays the move history list */
    private RecyclerView recyclerView;

    /** TextView displayed when no history exists */
    private TextView tvEmpty;

    /** ProgressBar shown while data is loading */
    private ProgressBar progressBar;

    /** Adapter that binds move history data */
    private MoveHistoryAdapter adapter;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** UID of the currently logged-in user */
    private String currentUid;

    /**
     * Required empty public constructor
     */
    public MoveHistoryFragment() { }

    /**
     * Inflates the fragment layout.
     *
     * @param inflater LayoutInflater to inflate views
     * @param container Parent view group
     * @param savedInstanceState Saved fragment state
     * @return Inflated fragment view
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_move_history, container, false);
    }

    /**
     * Called after the fragment view has been created.
     * Initializes UI components, Firebase instances, and loads data.
     *
     * @param view Fragment root view
     * @param savedInstanceState Saved fragment state
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Firebase Initialization ----
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        // ---- View Binding ----
        recyclerView = view.findViewById(R.id.recyclerMoveHistory);
        tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        progressBar = view.findViewById(R.id.progressBarHistory);

        // ---- RecyclerView Configuration ----
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        // ---- Load History Data ----
        loadHistory();
    }

    /**
     * Loads the user's move history from Firestore.
     *
     * Workflow:
     * 1. Show loading indicator
     * 2. Retrieve current user's profile
     * 3. Determine query field based on user type
     * 4. Fetch move history from Firestore
     * 5. Display results or empty state
     */
    private void loadHistory() {

        // Show loading indicator and hide empty state
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Ensure user session and profile are available
        UserSession.getInstance()
                .ensureStarted()
                .addOnSuccessListener(profile -> {

                    // ---- Determine Query Field ----
                    String userType = profile.getUserType();
                    String fieldName =
                            "customer".equals(userType)
                                    ? "customerId"
                                    : "moverId";

                    // ---- Firestore Query ----
                    db.collection("moves")
                            .whereEqualTo(fieldName, currentUid)
                            .whereIn(
                                    "status",
                                    Arrays.asList(
                                            "CONFIRMED",
                                            "COMPLETED",
                                            "CANCELED"
                                    )
                            )
                            .orderBy(
                                    "moveDate",
                                    Query.Direction.DESCENDING
                            )
                            .get()
                            .addOnSuccessListener(querySnapshot -> {

                                // Ensure fragment is still attached
                                if (!isAdded()) return;

                                progressBar.setVisibility(View.GONE);

                                // ---- Empty State ----
                                if (querySnapshot.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    return;
                                }

                                // ---- Convert Documents to Models ----
                                List<MoveRequest> historyList =
                                        querySnapshot.toObjects(
                                                MoveRequest.class
                                        );

                                // ---- Bind Data to RecyclerView ----
                                adapter = new MoveHistoryAdapter(historyList);
                                recyclerView.setAdapter(adapter);
                            })
                            .addOnFailureListener(e -> {

                                // Ensure fragment is still attached
                                if (!isAdded()) return;

                                progressBar.setVisibility(View.GONE);

                                Toast.makeText(
                                        getContext(),
                                        "שגיאה בטעינת היסטוריה",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                });
    }
}
