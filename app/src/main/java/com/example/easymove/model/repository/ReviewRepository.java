package com.example.easymove.model.repository;

import androidx.annotation.NonNull;

import com.example.easymove.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository class responsible for handling Reviews in Firestore.
 * It manages adding new reviews and atomically updating the Mover's rating statistics
 * (average rating, review count) to ensure data consistency.
 */
public class ReviewRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_REVIEWS = "reviews";
    private static final String COLLECTION_USERS = "users";

    /**
     * Adds a new review and updates the Mover's statistics atomically.
     * Uses a Firestore Transaction to ensure the review is added AND the rating count/sum
     * are updated simultaneously.
     *
     * @param review The Review object to add.
     * @return A Task representing the transaction result.
     */
    public Task<Void> addReviewAndUpdateMoverStats(@NonNull Review review) {

        DocumentReference reviewRef = db.collection(COLLECTION_REVIEWS).document();
        DocumentReference moverRef = db.collection(COLLECTION_USERS).document(review.getMoverId());

        return db.runTransaction(transaction -> {

            // 1) Read current Mover statistics
            DocumentSnapshot snap = transaction.get(moverRef);

            long count = 0;
            long sum = 0;

            if (snap.exists()) {
                // Try reading 'ratingCount' first (standard field for UI)
                Long c = snap.getLong("ratingCount");
                // Fallback: If 'ratingCount' is missing, try 'reviewsCount' (legacy)
                if (c == null) {
                    c = snap.getLong("reviewsCount");
                }

                Long s = snap.getLong("ratingSum");

                if (c != null) count = c;
                if (s != null) sum = s;
            }

            // 2) Calculate new values
            long newCount = count + 1;
            long newSum = sum + review.getStars();
            // Calculate new average (Handle division by zero for safety)
            double newAvg = (count == 0 && sum == 0) ? review.getStars() : (double) newSum / (double) newCount;

            // 3) Write the new Review document
            transaction.set(reviewRef, review);

            // 4) Update the Mover's profile with new stats
            Map<String, Object> updates = new HashMap<>();

            // Update both fields for backward compatibility
            updates.put("reviewsCount", newCount);
            updates.put("ratingCount", (int) newCount); // Field used by UI

            updates.put("ratingSum", newSum);
            updates.put("ratingAvg", newAvg);
            updates.put("rating", (float) newAvg); // Field used by RatingBar in UI

            transaction.update(moverRef, updates);

            return null;
        });
    }

    /**
     * Fetches all reviews written for a specific Mover.
     *
     * @param moverId The ID of the mover.
     * @return A Task containing the QuerySnapshot of reviews.
     */
    public Task<QuerySnapshot> getReviewsForMover(String moverId) {
        return db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("moverId", moverId)
                .get();
    }

    /**
     * Repair Tool: Recalculates stats for a mover.
     * Useful if the aggregated data (rating count/sum) gets out of sync with actual reviews.
     * It fetches all real reviews, sums them up, and overwrites the user profile stats.
     *
     * @param moverId The ID of the mover to repair.
     * @return A Task representing the update operation.
     */
    public Task<Void> recalculateMoverStats(String moverId) {
        return getReviewsForMover(moverId).continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to fetch reviews for recalculation"));
            }

            long realCount = 0;
            long realSum = 0;

            for (DocumentSnapshot doc : task.getResult()) {
                Review r = doc.toObject(Review.class);
                if (r != null) {
                    realCount++;
                    realSum += r.getStars();
                }
            }

            double realAvg = (realCount > 0) ? (double) realSum / realCount : 0.0;

            Map<String, Object> updates = new HashMap<>();
            updates.put("reviewsCount", realCount);
            updates.put("ratingCount", (int) realCount);
            updates.put("ratingSum", realSum);
            updates.put("ratingAvg", realAvg);
            updates.put("rating", (float) realAvg);

            return db.collection(COLLECTION_USERS).document(moverId).update(updates);
        });
    }
}