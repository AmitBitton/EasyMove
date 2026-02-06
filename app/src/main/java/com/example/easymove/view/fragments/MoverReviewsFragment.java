package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
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

/**
 * Fragment responsible for displaying a list of reviews for a specific Mover.
 * Features:
 * 1. Fetches reviews from Firestore based on Mover ID.
 * 2. Displays reviews in a RecyclerView.
 * 3. Handles "Empty State" if no reviews exist.
 * 4. Includes a custom back button navigation.
 */
public class MoverReviewsFragment extends Fragment {

    private static final String ARG_MOVER_ID = "moverId";
    private static final String ARG_MOVER_NAME = "moverName";

    private final ReviewRepository reviewRepository = new ReviewRepository();

    public MoverReviewsFragment() {
        super(R.layout.fragment_mover_reviews);
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param moverId   The ID of the mover whose reviews we want to see.
     * @param moverName The name of the mover (for the title).
     * @return A new instance of MoverReviewsFragment.
     */
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

        // 1. Initialize UI Components
        TextView tvTitle = view.findViewById(R.id.tvReviewsTitle);
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        ProgressBar progressBar = view.findViewById(R.id.progressBarReviews);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyReviews);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerReviews);

        // 2. Setup Back Button Logic
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack(); // Go back in fragment history
            } else {
                requireActivity().getOnBackPressedDispatcher().onBackPressed(); // Default activity back action
            }
        });

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ReviewsAdapter adapter = new ReviewsAdapter();
        recyclerView.setAdapter(adapter);

        // 4. Handle Arguments & Title
        Bundle args = getArguments();
        String moverId = args != null ? args.getString(ARG_MOVER_ID, "") : "";
        String moverName = args != null ? args.getString(ARG_MOVER_NAME, "") : "";

        if (!TextUtils.isEmpty(moverName)) {
            tvTitle.setText("ביקורות על " + moverName);
        } else {
            tvTitle.setText("ביקורות");
        }

        // 5. Load Data
        loadReviews(moverId, adapter, progressBar, tvEmpty, recyclerView);
    }

    /**
     * Fetches reviews from the repository and updates the UI.
     */
    private void loadReviews(String moverId, ReviewsAdapter adapter, ProgressBar progressBar, TextView tvEmpty, RecyclerView recyclerView) {
        if (TextUtils.isEmpty(moverId)) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("שגיאה: מזהה מוביל חסר");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        reviewRepository.getReviewsForMover(moverId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) return; // Safety check

                    progressBar.setVisibility(View.GONE);

                    List<Review> reviews = new ArrayList<>();
                    if (querySnapshot != null) {
                        // Optimized: Map documents directly to objects
                        reviews = querySnapshot.toObjects(Review.class);
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