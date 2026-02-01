package com.example.easymove.model.repository;

import androidx.annotation.NonNull;

import com.example.easymove.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class ReviewRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_REVIEWS = "reviews";

    // מוסיף ביקורת + מעדכן שדות אצל המוביל (users/{moverId})
    public Task<Void> addReviewAndUpdateMoverStats(@NonNull Review review) {

        DocumentReference reviewRef = db.collection(COLLECTION_REVIEWS).document(); // id אוטומטי
        DocumentReference moverRef = db.collection("users").document(review.getMoverId());

        return db.runTransaction(transaction -> {

            // 1) קודם כל READ (חובה לפני כל WRITE)
            DocumentSnapshot snap = transaction.get(moverRef);

            long count = 0;
            long sum = 0;

            if (snap.exists()) {
                Long c = snap.getLong("reviewsCount");
                Long s = snap.getLong("ratingSum");
                if (c != null) count = c;
                if (s != null) sum = s;
            }

            long newCount = count + 1;
            long newSum = sum + review.getStars();
            double newAvg = (double) newSum / (double) newCount;

            // 2) עכשיו WRITE של הביקורת
            transaction.set(reviewRef, review);

            // 3) עדכון הסטטיסטיקות של המוביל
            Map<String, Object> updates = new HashMap<>();

            // השדות הפנימיים שלנו (אופציונלי להשאיר)
            updates.put("reviewsCount", newCount);
            updates.put("ratingSum", newSum);
            updates.put("ratingAvg", newAvg);

            // ✅ השדות שה-UI שלך קורא (UserProfile)
            updates.put("ratingCount", (int) newCount);
            updates.put("rating", (float) newAvg);

            transaction.update(moverRef, updates);

            return null;
        });
    }

    // שולף את כל הביקורות של מוביל ספציפי
    public Task<QuerySnapshot> getReviewsForMover(String moverId) {
        return db.collection(COLLECTION_REVIEWS)
                .whereEqualTo("moverId", moverId)
                .get();
    }

}
