package com.example.easymove.model.repository;

import androidx.annotation.NonNull;

import com.example.easymove.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ReviewRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_REVIEWS = "reviews";
    private static final String COLLECTION_USERS = "users";

    // מוסיף ביקורת + מעדכן שדות אצל המוביל (users/{moverId})
    public Task<Void> addReviewAndUpdateMoverStats(@NonNull Review review) {

        DocumentReference reviewRef = db.collection(COLLECTION_REVIEWS).document();
        DocumentReference moverRef = db.collection(COLLECTION_USERS).document(review.getMoverId());

        return db.runTransaction(transaction -> {

            // 1) קריאת הנתונים הנוכחיים
            DocumentSnapshot snap = transaction.get(moverRef);

            long count = 0;
            long sum = 0;

            if (snap.exists()) {
                // ✅ תיקון: קוראים קודם את ratingCount (בו ה-UI משתמש)
                Long c = snap.getLong("ratingCount");
                // גיבוי: אם אין ratingCount, ננסה את reviewsCount
                if (c == null) c = snap.getLong("reviewsCount");

                Long s = snap.getLong("ratingSum");

                if (c != null) count = c;
                if (s != null) sum = s;
            }

            // חישוב הערכים החדשים
            long newCount = count + 1;
            long newSum = sum + review.getStars();
            double newAvg = (count == 0 && sum == 0) ? review.getStars() : (double) newSum / (double) newCount;

            // 2) כתיבת הביקורת
            transaction.set(reviewRef, review);

            // 3) עדכון הסטטיסטיקות
            Map<String, Object> updates = new HashMap<>();

            // שומרים את שני השמות ליתר ביטחון, אבל מתבססים על החישוב הנכון
            updates.put("reviewsCount", newCount);
            updates.put("ratingCount", (int) newCount); // זה השדה שהכרטיס מציג!

            updates.put("ratingSum", newSum);
            updates.put("ratingAvg", newAvg);
            updates.put("rating", (float) newAvg); // זה השדה של הכוכבים ב-UI

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

    /**
     * 🛠️ פונקציית תיקון (Repair Tool)
     * מריצה חישוב מחדש למוביל שהנתונים שלו השתבשו.
     * היא סופרת את כל הביקורות בפועל ומעדכנת את הפרופיל במספר הנכון.
     */
    public Task<Void> recalculateMoverStats(String moverId) {
        return getReviewsForMover(moverId).continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return Tasks.forException(new Exception("Failed to fetch reviews"));
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