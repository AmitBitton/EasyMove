package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.easymove.R;
import com.example.easymove.model.Review;
import com.example.easymove.model.repository.ReviewRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddReviewFragment extends Fragment {

    private static final String ARG_MOVER_ID = "moverId";
    private static final String ARG_MOVER_NAME = "moverName";
    private static final String ARG_MOVE_ID = "moveId";

    public AddReviewFragment() {
        super(R.layout.fragment_add_review);
    }

    public static AddReviewFragment newInstance(String moverId, String moverName, String moveId) {
        AddReviewFragment f = new AddReviewFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MOVER_ID, moverId);
        b.putString(ARG_MOVER_NAME, moverName);
        b.putString(ARG_MOVE_ID, moveId);
        f.setArguments(b);
        return f;
    }

    private final ReviewRepository reviewRepository = new ReviewRepository();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvMoverName = view.findViewById(R.id.tvMoverName);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        EditText etReviewText = view.findViewById(R.id.etReviewText);
        Button btnSubmitReview = view.findViewById(R.id.btnSubmitReview);

        Bundle args = getArguments();
        String moverName = args != null ? args.getString(ARG_MOVER_NAME, "") : "";
        String moverId = args != null ? args.getString(ARG_MOVER_ID, "") : "";

        // ברירת מחדל
        final String[] reviewerNameHolder = new String[]{"לקוח"};

        String reviewerId = FirebaseAuth.getInstance().getUid();
        if (reviewerId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(reviewerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        if (name != null && !name.trim().isEmpty()) {
                            reviewerNameHolder[0] = name;
                        }
                    });
        }

        tvMoverName.setText(
                moverName != null && !moverName.trim().isEmpty()
                        ? "מוביל: " + moverName
                        : "מוביל"
        );

        btnSubmitReview.setOnClickListener(v -> {

            int stars = (int) ratingBar.getRating();
            String text = etReviewText.getText().toString().trim();

            if (stars == 0) {
                android.widget.Toast.makeText(requireContext(), "בחרי דירוג כוכבים", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (text.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "אנא כתבי ביקורת", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String reviewerUid = FirebaseAuth.getInstance().getUid();
            if (reviewerUid == null) {
                android.widget.Toast.makeText(requireContext(), "שגיאה: משתמש לא מחובר", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // כדי למנוע לחיצות כפולות
            btnSubmitReview.setEnabled(false);

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(reviewerUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        android.util.Log.d("AddReviewFragment", "USER DOC = " + doc.getData());

                        String reviewerName = doc.getString("name");
                        if (reviewerName == null || reviewerName.trim().isEmpty()) {
                            reviewerName = "לקוח";
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
                                    android.widget.Toast.makeText(requireContext(), "הביקורת נוספה בהצלחה", android.widget.Toast.LENGTH_SHORT).show();
                                    requireActivity().getSupportFragmentManager().popBackStack();
                                })
                                .addOnFailureListener(e -> {
                                    btnSubmitReview.setEnabled(true);
                                    android.widget.Toast.makeText(
                                            requireContext(),
                                            "שגיאה: " + (e.getMessage() != null ? e.getMessage() : e.toString()),
                                            android.widget.Toast.LENGTH_LONG
                                    ).show();
                                    android.util.Log.e("AddReviewFragment", "addReview failed", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        btnSubmitReview.setEnabled(true);
                        android.widget.Toast.makeText(requireContext(), "שגיאה בשליפת שם משתמש", android.widget.Toast.LENGTH_SHORT).show();
                        android.util.Log.e("AddReviewFragment", "get reviewer name failed", e);
                    });
        });
    }
}
