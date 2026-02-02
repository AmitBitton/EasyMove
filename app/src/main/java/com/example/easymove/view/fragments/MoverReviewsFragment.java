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

import java.util.ArrayList;
import java.util.List;

public class MoverReviewsFragment extends Fragment {

    private static final String ARG_MOVER_ID = "moverId";
    private static final String ARG_MOVER_NAME = "moverName";

    public MoverReviewsFragment() {
        super(R.layout.fragment_mover_reviews);
    }

    public static MoverReviewsFragment newInstance(String moverId, String moverName) {
        MoverReviewsFragment fragment = new MoverReviewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MOVER_ID, moverId);
        args.putString(ARG_MOVER_NAME, moverName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tvReviewsTitle);
        ProgressBar progressBar = view.findViewById(R.id.progressBarReviews);

        TextView tvEmpty = view.findViewById(R.id.tvEmptyReviews);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerReviews);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ReviewsAdapter adapter = new ReviewsAdapter();
        recyclerView.setAdapter(adapter);

        Bundle args = getArguments();
        String moverId = args != null ? args.getString(ARG_MOVER_ID, "") : "";
        String moverName = args != null ? args.getString(ARG_MOVER_NAME, "") : "";

        if (moverName != null && !moverName.trim().isEmpty()) {
            tvTitle.setText("ביקורות על " + moverName);
        } else {
            tvTitle.setText("ביקורות");
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        new ReviewRepository()
                .getReviewsForMover(moverId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return;

                    progressBar.setVisibility(View.GONE);

                    List<Review> reviews = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Review r = doc.toObject(Review.class);
                            if (r != null) reviews.add(r);
                        }

                    }

                    if (reviews.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setReviews(reviews);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("שגיאה בטעינת ביקורות");
                    recyclerView.setVisibility(View.GONE);
                });
    }
}
