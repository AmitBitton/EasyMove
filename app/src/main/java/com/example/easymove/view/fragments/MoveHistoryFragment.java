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
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.UserSession;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.FirebaseFirestore;

public class MoveHistoryFragment extends Fragment {

    public MoveHistoryFragment() {
        super(R.layout.fragment_move_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHistory);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        ProgressBar progressBar = view.findViewById(R.id.progressBarHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        MoveHistoryAdapter adapter = new MoveHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // מאזין לכפתור "הוספת ביקורת"
        adapter.setOnAddReviewClickListener(move -> {
            String moverId = move.getMoverId();
            String moverName = move.getMoverName();
            String moveId = move.getId();

            if (moverId == null || moverId.trim().isEmpty()) {
                Toast.makeText(getContext(), "אין מוביל להובלה הזו", Toast.LENGTH_SHORT).show();
                return;
            }

            // פותחים את המסך להוספת ביקורת (כרגע בלי שם לקוח - נטפל בזה אחרי שנרוץ)
            AddReviewFragment fragment =
                    AddReviewFragment.newInstance(moverId, moverName, moveId);


            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        UserSession.getInstance().ensureStarted()
                .addOnSuccessListener(profile -> {
                    if (profile == null) return;

                    String uid = profile.getUserId();
                    String userType = profile.getUserType();

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

                                    // השלמת moverName מתוך users
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    for (MoveRequest m : moves) {
                                        String mid = m.getMoverId();
                                        if (mid == null || mid.trim().isEmpty()) continue;

                                        db.collection("users").document(mid).get()
                                                .addOnSuccessListener(doc -> {
                                                    if (doc.exists()) {
                                                        String name = doc.getString("name");
                                                        if (name != null && !name.trim().isEmpty()) {
                                                            m.setMoverName(name);
                                                            adapter.notifyDataSetChanged();
                                                        }
                                                    }
                                                });
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                progressBar.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                });
    }
}
