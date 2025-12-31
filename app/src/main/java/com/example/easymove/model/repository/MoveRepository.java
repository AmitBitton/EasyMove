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

import java.util.Arrays;
import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String COLLECTION = "moves";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // --- UPDATED: Confirm Move with Address & Date ---
    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId,
                                            String sourceAddress, String destAddress, long moveDate) {

        // CRITICAL CHANGE: Use 'chatId' as the document ID, not 'customerId'.
        // This allows the customer to have multiple moves in their history (one per chat).
        DocumentReference moveRef = db.collection(COLLECTION).document(chatId);
        DocumentReference chatRef = db.collection("chats").document(chatId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot chatSnap = transaction.get(chatRef);
            if (!chatSnap.exists()) {
                throw new FirebaseFirestoreException("Chat not found", FirebaseFirestoreException.Code.ABORTED);
            }

            Boolean moverConfirmed = chatSnap.getBoolean("moverConfirmed");
            if (moverConfirmed == null || !moverConfirmed) {
                throw new FirebaseFirestoreException("Mover has not confirmed yet", FirebaseFirestoreException.Code.ABORTED);
            }

            // 1. Update Chat Status
            transaction.update(chatRef,
                    "customerConfirmed", true,
                    "customerConfirmedAt", System.currentTimeMillis()
            );

            // 2. Create/Update the Move Document
            MoveRequest move = new MoveRequest();
            move.setId(chatId);
            move.setChatId(chatId);
            move.setCustomerId(customerId);
            move.setMoverId(moverId);
            move.setStatus("CONFIRMED"); // Initial status
            move.setConfirmed(true);
            move.setCreatedAt(System.currentTimeMillis());

            // --- SAVE THE NEW DATA ---
            move.setSourceAddress(sourceAddress);
            move.setDestAddress(destAddress);
            move.setMoveDate(moveDate); // Saves as Number (Long)
            // -------------------------

            // Use 'set' with merge to avoid overwriting unrelated fields if they exist
            transaction.set(moveRef, move, SetOptions.merge());

            return null;
        });
    }

    /**
     * מביא את ההובלה הפעילה של הלקוח הספציפי.
     * מקבל את ה-customerId כפרמטר ליציבות.
     */
    public Task<MoveRequest> getCurrentActiveMove(String customerId) {
        if (customerId == null) return Tasks.forException(new Exception("Customer ID is null"));

        List<String> activeStatuses = Arrays.asList("OPEN", "CONFIRMED");

        return db.collection(COLLECTION)
                .whereEqualTo("customerId", customerId)
                .whereIn("status", activeStatuses)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        MoveRequest move = doc.toObject(MoveRequest.class);
                        if (move != null) move.setId(doc.getId());
                        return move;
                    }
                    return null;
                });
    }

    /**
     * הפונקציה החכמה ליצירת הובלה.
     * מחליפה את הטרנזקציה הלא-חוקית בשרשרת פעולות (Chain).
     */
    public Task<MoveRequest> ensureActiveMoveForCustomer(String customerId) {
        // שלב 1: בודקים אם קיימת הובלה פעילה
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest existingMove = task.getResult();

            // אם מצאנו פעילה -> מחזירים אותה וזהו
            if (existingMove != null) {
                return Tasks.forResult(existingMove);
            }

            // שלב 2: אם לא מצאנו -> יוצרים חדשה
            DocumentReference newRef = db.collection(COLLECTION).document();

            MoveRequest move = new MoveRequest();
            move.setId(newRef.getId());
            move.setCustomerId(customerId);
            move.setStatus("OPEN");
            move.setConfirmed(false);
            move.setCreatedAt(System.currentTimeMillis());
            move.setMoverId(null);
            move.setChatId(null);

            return newRef.set(move).continueWith(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return move; // מחזירים את האובייקט החדש שנוצר
            });
        });
    }

    /**
     * ביטול הובלה
     */
    public Task<Void> cancelMove(String moveId) {
        return db.collection(COLLECTION).document(moveId).update(
                "status", "CANCELED",
                "confirmed", false
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

    /**
     * סיום הובלה
     */
    public Task<Void> completeMove(String moveId) {
        return db.collection(COLLECTION).document(moveId).update("status", "COMPLETED");
    }



}