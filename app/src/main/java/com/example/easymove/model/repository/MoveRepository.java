package com.example.easymove.model.repository;

import com.example.easymove.model.MoveRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.Arrays;
import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String COLLECTION = "moves";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
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

    /**
     * סיום הובלה
     */
    public Task<Void> completeMove(String moveId) {
        return db.collection(COLLECTION).document(moveId).update("status", "COMPLETED");
    }

    /**
     * אישור ע"י לקוח (נשאר עם טרנזקציה כי כאן אנחנו עובדים על ID ספציפי וזה חוקי ומצוין!)
     */
    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId) {
        // שולפים קודם את ההובלה הפעילה כדי להשיג את ה-moveId
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest activeMove = task.getResult();
            if (activeMove == null) {
                throw new FirebaseFirestoreException("לא נמצאה הובלה פעילה לאישור",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            String moveId = activeMove.getId();
            DocumentReference moveRef = db.collection(COLLECTION).document(moveId);
            DocumentReference chatRef = db.collection("chats").document(chatId);

            return db.runTransaction(transaction -> {
                DocumentSnapshot chatSnap = transaction.get(chatRef);
                if (!chatSnap.exists()) {
                    throw new FirebaseFirestoreException("הצ'אט לא קיים", FirebaseFirestoreException.Code.ABORTED);
                }

                Boolean moverConfirmed = chatSnap.getBoolean("moverConfirmed");
                if (moverConfirmed == null || !moverConfirmed) {
                    throw new FirebaseFirestoreException("המוביל טרם אישר", FirebaseFirestoreException.Code.ABORTED);
                }

                // עדכון הצ'אט
                transaction.update(chatRef,
                        "customerConfirmed", true,
                        "customerConfirmedAt", System.currentTimeMillis()
                );

                // עדכון ההובלה - הופכת ל-CONFIRMED
                transaction.update(moveRef,
                        "status", "CONFIRMED",
                        "confirmed", true,
                        "moverId", moverId,
                        "chatId", chatId
                );

                return null;
            });
        });
    }

    public Task<List<MoveRequest>> getMoverConfirmedMoves(String moverId) {
        return db.collection(COLLECTION)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<MoveRequest> moves = new java.util.ArrayList<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
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
    public Task<Void> updateMoveDetails(String moveId, String fromAddress, String toAddress, long moveDate) {
        return db.collection(COLLECTION).document(moveId).update(
                "sourceAddress", fromAddress,
                "destAddress", toAddress,
                "moveDate", moveDate
        );
    }
}