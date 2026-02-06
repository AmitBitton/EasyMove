package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.View;
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
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment responsible for displaying the history of completed or past moves.
 * Features:
 * 1. Fetches move history based on User Type (Customer vs Mover).
 * 2. Displays the list in a RecyclerView.
 * 3. Enriching data: Fetches Mover names for display if missing.
 * 4. Navigation: Allows adding a review for a completed move.
 */
public class MoveHistoryFragment extends Fragment {

    public MoveHistoryFragment() {
        super(R.layout.fragment_move_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI
        RecyclerView recyclerView = view.findViewById(R.id.recyclerHistory);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        ProgressBar progressBar = view.findViewById(R.id.progressBarHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. Setup Adapter
        MoveHistoryAdapter adapter = new MoveHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // 3. Setup Click Listener for "Add Review" button
        adapter.setOnAddReviewClickListener(move -> {
            String moverId = move.getMoverId();
            String moverName = move.getMoverName();
            String moveId = move.getId();

            if (moverId == null || moverId.trim().isEmpty()) {
                Toast.makeText(getContext(), "אין מוביל להובלה הזו", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigate to AddReviewFragment
            AddReviewFragment fragment = AddReviewFragment.newInstance(moverId, moverName, moveId);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 4. Start Loading Data
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        loadData(adapter, progressBar, tvEmpty, recyclerView);
    }

    /**
     * Fetches user profile, determines type, and loads the relevant move history.
     */
    private void loadData(MoveHistoryAdapter adapter, ProgressBar progressBar, TextView tvEmpty, RecyclerView recyclerView) {
        UserSession.getInstance().ensureStarted()
                .addOnSuccessListener(profile -> {
                    if (profile == null) return;

                    String uid = profile.getUserId();
                    String userType = profile.getUserType();

                    // Fetch History from Repository
                    new MoveRepository().getMoveHistory(uid, userType)
                            .addOnSuccessListener(moves -> {
                                if (!isAdded()) return;

                                progressBar.setVisibility(View.GONE);

                                if (moves.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    adapter.setMoves(moves);

                                    // 5. Client-side Join: Fetch Mover names for better UX
                                    fetchMoverNames(moves, adapter);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "שגיאה בטעינת היסטוריה", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Iterates through moves and fetches the Mover's name from the 'users' collection
     * if it wasn't stored directly on the move document.
     * Note: In a production app, this should ideally be handled by a backend function or a joined query.
     */
    private void fetchMoverNames(java.util.List<MoveRequest> moves, MoveHistoryAdapter adapter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (MoveRequest m : moves) {
            String mid = m.getMoverId();
            // Skip if no mover ID or if name is already present (optimization)
            if (mid == null || mid.trim().isEmpty()) continue;

            db.collection("users").document(mid).get()
                    .addOnSuccessListener(doc -> {
                        if (!isAdded()) return;
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.trim().isEmpty()) {
                                m.setMoverName(name);
                                // Refresh list to show the new name
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }
}