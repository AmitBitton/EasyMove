package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.ReviewsAdapter;
import com.example.easymove.model.Review;
import com.example.easymove.model.repository.ReviewRepository;

import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ReviewsFragment extends Fragment {

    private static final String ARG_MOVER_ID = "moverId";
    private static final String ARG_MOVER_NAME = "moverName";

    public ReviewsFragment() {
        super(R.layout.fragment_reviews);
    }

    public static ReviewsFragment newInstance(String moverId, String moverName) {
        ReviewsFragment f = new ReviewsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MOVER_ID, moverId);
        b.putString(ARG_MOVER_NAME, moverName);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvReviewsTitle);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyReviews);
        ProgressBar progress = view.findViewById(R.id.progressBarReviews);
        RecyclerView recycler = view.findViewById(R.id.recyclerReviews);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        ReviewsAdapter adapter = new ReviewsAdapter();
        recycler.setAdapter(adapter);

        Bundle args = getArguments();
        String moverId = (args != null) ? args.getString(ARG_MOVER_ID, "") : "";
        String moverName = (args != null) ? args.getString(ARG_MOVER_NAME, "") : "";

        tvTitle.setText("ביקורות על: " + (moverName != null && !moverName.isEmpty() ? moverName : moverId));

        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        recycler.setVisibility(View.GONE);

        new ReviewRepository().getReviewsForMover(moverId)
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;

                    progress.setVisibility(View.GONE);

                    List<Review> list = new ArrayList<>();
                    if (snap != null) {
                        for (var doc : snap.getDocuments()) {
                            Review r = doc.toObject(Review.class);
                            if (r != null) list.add(r);
                        }
                    }

                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recycler.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recycler.setVisibility(View.VISIBLE);
                        adapter.setReviews(list);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progress.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                    tvEmpty.setText("שגיאה בטעינת ביקורות");
                });
    }
}
