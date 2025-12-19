package com.example.easymove.model.repository;

import com.example.easymove.model.MoveRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String COLLECTION = "moves";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * מביא את ההובלה ה"פעילה" של הלקוח.
     * הנחה: ללקוח יש רק הובלה אחת פעילה בסטטוס OPEN בו זמנית.
     */
    public Task<MoveRequest> getCurrentActiveMove() {
        String uid = getCurrentUserId();
        if (uid == null) return Tasks.forException(new Exception("No user logged in"));

        return db.collection(COLLECTION)
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "OPEN") // רק הובלות פתוחות
                .limit(1) // לוקחים את הראשונה
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        MoveRequest move = snapshot.getDocuments().get(0).toObject(MoveRequest.class);
                        if (move != null) move.setId(snapshot.getDocuments().get(0).getId());
                        return move;
                    }
                    return null; // אין הובלה פעילה
                });
    }

    // ביטול הובלה
    public Task<Void> cancelMove(String customerId) {
        DocumentReference moveRef = db.collection(COLLECTION).document(customerId);

        return moveRef.update(
                "status", "OPEN",
                "confirmed", false,
                "moverId", null,
                "chatId", null
        );
    }


    public Task<Void> createConfirmedMove(String chatId, String moverId, String customerId) {
        MoveRequest move = new MoveRequest();
        move.setId(chatId); // אצלנו ה-id יהיה chatId
        move.setChatId(chatId);
        move.setMoverId(moverId);
        move.setCustomerId(customerId);
        move.setConfirmed(true);
        move.setCreatedAt(System.currentTimeMillis());
        move.setStatus("CONFIRMED"); // בלי תאריך כרגע

        return db.collection(COLLECTION).document(chatId).set(move);
    }


    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId) {
        DocumentReference moveRef =
                db.collection(COLLECTION).document(customerId);
        DocumentReference chatRef =
                db.collection("chats").document(chatId);

        return db.runTransaction(transaction -> {

            DocumentSnapshot chatSnap = transaction.get(chatRef);
            if (!chatSnap.exists()) {
                throw new FirebaseFirestoreException("הצ'אט לא קיים", FirebaseFirestoreException.Code.ABORTED);
            }
            Boolean moverConfirmed = chatSnap.getBoolean("moverConfirmed");

            if (moverConfirmed == null || !moverConfirmed) {
                throw new FirebaseFirestoreException(
                        "המוביל טרם אישר",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            transaction.update(chatRef,
                    "customerConfirmed", true,
                    "customerConfirmedAt", System.currentTimeMillis()
            );

            // 3) לעדכן/ליצור הובלה (אם לא קיימת - תיווצר)
            MoveRequest patch = new MoveRequest();
            patch.setCustomerId(customerId);
            patch.setStatus("CONFIRMED");
            patch.setConfirmed(true);
            patch.setMoverId(moverId);
            patch.setChatId(chatId);
            patch.setCreatedAt(System.currentTimeMillis()); // אופציונלי

            transaction.set(moveRef, patch, SetOptions.merge());

            return null;
        });
    }

    public Task<List<MoveRequest>> getMoverConfirmedMoves(String moverId) {
        return db.collection(COLLECTION)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("confirmed", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    List<MoveRequest> moves = new java.util.ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            MoveRequest move = doc.toObject(MoveRequest.class);
                            if (move != null) {
                                move.setId(doc.getId());
                                moves.add(move);
                            }
                        }
                    }
                    return moves;
                });
    }
    public Task<Void> ensureOpenMoveForCustomer(String customerId) {
        DocumentReference moveRef = db.collection(COLLECTION).document(customerId);

        return moveRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            DocumentSnapshot snap = task.getResult();

            // אם כבר יש מסמך (בין אם OPEN / CONFIRMED / כל דבר) — לא נוגעים בו
            if (snap != null && snap.exists()) {
                return Tasks.forResult(null);
            }

            // אין מסמך בכלל -> יוצרים "טיוטת הובלה" פתוחה
            MoveRequest move = new MoveRequest();
            move.setCustomerId(customerId);
            move.setStatus("OPEN");
            move.setConfirmed(false);
            move.setCreatedAt(System.currentTimeMillis());
            move.setMoverId(null);
            move.setChatId(null);

            return moveRef.set(move);
        });
    }

}