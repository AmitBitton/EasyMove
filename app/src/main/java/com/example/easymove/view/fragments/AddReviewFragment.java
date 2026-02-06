package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.easymove.R;
import com.example.easymove.model.Review;
import com.example.easymove.model.repository.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Fragment responsible for allowing a Customer to review a Mover.
 * It handles:
 * 1. Displaying the Mover's name.
 * 2. Collecting Star Rating and Text.
 * 3. Fetching the current user's name (to tag the review).
 * 4. Saving the review via {@link ReviewRepository} which also updates the Mover's average rating.
 */
public class AddReviewFragment extends Fragment {

    private static final String ARG_MOVER_ID = "moverId";
    private static final String ARG_MOVER_NAME = "moverName";
    private static final String ARG_MOVE_ID = "moveId";

    private final ReviewRepository reviewRepository = new ReviewRepository();

    // UI Components
    private TextView tvMoverName;
    private RatingBar ratingBar;
    private EditText etReviewText;
    private Button btnSubmitReview;

    public AddReviewFragment() {
        super(R.layout.fragment_add_review);
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param moverId   The ID of the mover being reviewed.
     * @param moverName The name of the mover.
     * @param moveId    The ID of the move (context).
     * @return A new instance of AddReviewFragment.
     */
    public static AddReviewFragment newInstance(String moverId, String moverName, String moveId) {
        AddReviewFragment f = new AddReviewFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MOVER_ID, moverId);
        b.putString(ARG_MOVER_NAME, moverName);
        b.putString(ARG_MOVE_ID, moveId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupData();

        btnSubmitReview.setOnClickListener(v -> submitReview());
    }

    private void initViews(View view) {
        tvMoverName = view.findViewById(R.id.tvMoverName);
        ratingBar = view.findViewById(R.id.ratingBar);
        etReviewText = view.findViewById(R.id.etReviewText);
        btnSubmitReview = view.findViewById(R.id.btnSubmitReview);
    }

    private void setupData() {
        Bundle args = getArguments();
        String moverName = args != null ? args.getString(ARG_MOVER_NAME, "") : "";

        if (!TextUtils.isEmpty(moverName)) {
            tvMoverName.setText("מוביל: " + moverName);
        } else {
            tvMoverName.setText("מוביל");
        }
    }

    /**
     * Validates input and initiates the review submission process.
     */
    private void submitReview() {
        int stars = (int) ratingBar.getRating();
        String text = etReviewText.getText().toString().trim();

        // 1. Validation
        if (stars == 0) {
            Toast.makeText(requireContext(), "בחרי דירוג כוכבים", Toast.LENGTH_SHORT).show();
            return;
        }

        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "אנא כתבי ביקורת", Toast.LENGTH_SHORT).show();
            return;
        }

        String reviewerUid = FirebaseAuth.getInstance().getUid();
        if (reviewerUid == null) {
            Toast.makeText(requireContext(), "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Disable button to prevent double submission
        btnSubmitReview.setEnabled(false);

        // 3. Fetch User Name (The Reviewer)
        // Ideally, this should be in a ViewModel/Repository, but kept here per current structure.
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(reviewerUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return; // Safety check

                    String reviewerName = doc.getString("name");
                    if (reviewerName == null || reviewerName.trim().isEmpty()) {
                        reviewerName = "לקוח";
                    }

                    saveReviewToRepository(reviewerUid, reviewerName, stars, text);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(requireContext(), "שגיאה בשליפת פרטי משתמש", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sends the review object to the repository.
     */
    private void saveReviewToRepository(String reviewerUid, String reviewerName, int stars, String text) {
        Bundle args = getArguments();
        String moverId = args != null ? args.getString(ARG_MOVER_ID, "") : "";

        if (TextUtils.isEmpty(moverId)) {
            Toast.makeText(getContext(), "שגיאה: מזהה מוביל חסר", Toast.LENGTH_SHORT).show();
            btnSubmitReview.setEnabled(true);
            return;
        }

        Review review = new Review(
                moverId,
                reviewerUid,
                reviewerName,
                stars,
                text
        );

        reviewRepository.addReviewAndUpdateMoverStats(review)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "הביקורת נוספה בהצלחה", Toast.LENGTH_SHORT).show();

                    // Close the fragment and go back
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    btnSubmitReview.setEnabled(true);
                    Toast.makeText(requireContext(), "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}