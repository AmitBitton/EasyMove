package com.example.easymove.model.repository;

import com.example.easymove.model.MoveRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener; // חשוב!
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration; // חשוב!
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private static final String COLLECTION_MOVES = "moves";
    private static final String COLLECTION_CHATS = "chats";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // --- הפונקציה החדשה להאזנה להובלות של מוביל ---
    /**
     * מאזין בזמן אמת להובלות מאושרות של המוביל.
     * @param moverId ה-ID של המוביל
     * @param listener פונקציית ה-callback שתופעל כשיש שינוי בנתונים
     * @return ListenerRegistration כדי שנוכל לעצור את ההאזנה כשהמסך נסגר
     */
    public ListenerRegistration listenToMoverConfirmedMoves(String moverId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }
    // ----------------------------------------------------

    public Task<MoveRequest> getCurrentActiveMove(String customerId) {
        if (customerId == null) return Tasks.forException(new Exception("Customer ID is null"));

        List<String> activeStatuses = Arrays.asList("OPEN", "CONFIRMED");

        return db.collection(COLLECTION_MOVES)
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

    public Task<MoveRequest> ensureActiveMoveForCustomer(String customerId) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest existingMove = task.getResult();
            if (existingMove != null) {
                return Tasks.forResult(existingMove);
            }

            DocumentReference newRef = db.collection(COLLECTION_MOVES).document();
            MoveRequest move = new MoveRequest();
            move.setId(newRef.getId());
            move.setCustomerId(customerId);
            move.setStatus("OPEN");
            move.setConfirmed(false);
            move.setCreatedAt(System.currentTimeMillis());

            return newRef.set(move).continueWith(t -> move);
        });
    }

    public Task<Void> updateMoveDraftDetails(String customerId, String source, String dest, long date) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest move = task.getResult();
            if (move == null) {
                return ensureActiveMoveForCustomer(customerId).continueWithTask(t -> {
                    return updateMoveFieldsInternal(t.getResult().getId(), source, dest, date);
                });
            }
            return updateMoveFieldsInternal(move.getId(), source, dest, date);
        });
    }

    private Task<Void> updateMoveFieldsInternal(String moveId, String source, String dest, long date) {
        return db.collection(COLLECTION_MOVES).document(moveId)
                .update(
                        "sourceAddress", source,
                        "destAddress", dest,
                        "moveDate", date
                );
    }

//    public Task<Void> cancelMoveAndResetChat(String moveId, String chatId, String customerId) {
//        WriteBatch batch = db.batch();
//
//        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
//        batch.update(moveRef,
//                "status", "CANCELED",
//                "confirmed", false
//        );
//
//        if (chatId != null && !chatId.isEmpty()) {
//            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
//            batch.update(chatRef,
//                    "moverConfirmed", false,
//                    "customerConfirmed", false,
//                    "moverConfirmedAt", null,
//                    "customerConfirmedAt", null
//            );
//        }
//
//        if (customerId != null) {
//            DocumentReference userRef = db.collection("users").document(customerId);
//            batch.update(userRef,
//                    "defaultFromAddress", null,
//                    "defaultToAddress", null,
//                    "defaultMoveDate", 0,
//                    "fromLat", null,
//                    "fromLng", null,
//                    "toLat", null,
//                    "toLng", null
//            );
//        }
//
//        return batch.commit();
//    }

    /**
     * ביטול הובלה + איפוס צ'אט (בטוח יותר)
     */
    public Task<Void> cancelMoveAndResetChat(String moveId, String chatId, String customerId) {
        // אנחנו מעדיפים לסמוך על chatId שהגיע מהקריאה, אבל ליתר ביטחון נבדוק אותו
        if (chatId == null || chatId.isEmpty()) {
            // אם אין לנו chatId, ננסה לשלוף אותו מהמסד קודם
            return db.collection(COLLECTION_MOVES).document(moveId).get().continueWithTask(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String realChatId = task.getResult().getString("chatId");
                    return performCancel(moveId, realChatId, customerId);
                }
                return performCancel(moveId, null, customerId);
            });
        }
        return performCancel(moveId, chatId, customerId);
    }

    private Task<Void> performCancel(String moveId, String chatId, String customerId) {
        WriteBatch batch = db.batch();

        // 1. ביטול ההובלה
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
        batch.update(moveRef,
                "status", "CANCELED",
                "confirmed", false
        );

        // 2. איפוס הצ'אט
        if (chatId != null && !chatId.isEmpty()) {
            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
            batch.update(chatRef,
                    "moverConfirmed", false,
                    "customerConfirmed", false,
                    "moverConfirmedAt", null,
                    "customerConfirmedAt", null
            );
        }

        // 3. איפוס פרופיל המשתמש
        if (customerId != null) {
            DocumentReference userRef = db.collection("users").document(customerId);
            batch.update(userRef,
                    "defaultFromAddress", null,
                    "defaultToAddress", null,
                    "defaultMoveDate", 0,
                    "fromLat", null,
                    "fromLng", null,
                    "toLat", null,
                    "toLng", null
            );
        }

        return batch.commit();
    }

    /**
     * סיום הובלה (ארכיון) + שחרור הצ'אט
     */
    public Task<Void> completeMove(String moveId) { // הסרתי את chatId מהחתימה כי נשלוף אותו מבפנים
        return db.collection(COLLECTION_MOVES).document(moveId).get().continueWithTask(task -> {
            String chatId = null;
            if (task.isSuccessful() && task.getResult() != null) {
                chatId = task.getResult().getString("chatId");
            }

            WriteBatch batch = db.batch();

            // 1. סגירת ההובלה
            DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
            batch.update(moveRef, "status", "COMPLETED");

            // 2. שחרור הצ'אט
            if (chatId != null && !chatId.isEmpty()) {
                DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
                batch.update(chatRef,
                        "moverConfirmed", false,
                        "customerConfirmed", false,
                        "moverConfirmedAt", null,
                        "customerConfirmedAt", null
                );
            }

            return batch.commit();
        });
    }

    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest activeMove = task.getResult();
            if (activeMove == null) {
                throw new FirebaseFirestoreException("לא נמצאה הובלה פעילה לאישור",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            String moveId = activeMove.getId();
            DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
            DocumentReference userRef = db.collection("users").document(customerId);

            return db.runTransaction(transaction -> {
                transaction.update(chatRef,
                        "customerConfirmed", true,
                        "customerConfirmedAt", System.currentTimeMillis()
                );

                transaction.update(moveRef,
                        "status", "CONFIRMED",
                        "confirmed", true,
                        "moverId", moverId,
                        "chatId", chatId
                );

                transaction.update(userRef,
                        "defaultFromAddress", activeMove.getSourceAddress(),
                        "defaultToAddress", activeMove.getDestAddress(),
                        "defaultMoveDate", activeMove.getMoveDate(),
                        "fromLat", activeMove.getSourceLat(),
                        "fromLng", activeMove.getSourceLng(),
                        "toLat", activeMove.getDestLat(),
                        "toLng", activeMove.getDestLng()
                );

                return null;
            });
        });
    }

    public Task<Boolean> hasActiveConfirmedMove(String customerId) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("status", "CONFIRMED")
                .limit(1)
                .get()
                .continueWith(task -> !task.getResult().isEmpty());
    }

    public Task<List<MoveRequest>> getMoveHistory(String uid, String userType) {
        String fieldName = "mover".equals(userType) ? "moverId" : "customerId";
        List<String> historyStatuses = Arrays.asList("COMPLETED", "CANCELED");

        return db.collection(COLLECTION_MOVES)
                .whereEqualTo(fieldName, uid)
                .whereIn("status", historyStatuses)
                .orderBy("moveDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);
    }

    // הוספתי פונקציה זו לתמיכה בשימושים ישנים שאולי קיימים במקומות אחרים,
    // אבל ב-MyDeliveriesViewModel החדש נשתמש ב-listenToMoverConfirmedMoves
    public Task<List<MoveRequest>> getMoverConfirmedMoves(String moverId) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);
    }

    private List<MoveRequest> mapQueryToMoves(Task<QuerySnapshot> task) throws Exception {
        if (!task.isSuccessful()) throw task.getException();
        List<MoveRequest> moves = new ArrayList<>();
        if (task.getResult() != null) {
            for (DocumentSnapshot doc : task.getResult()) {
                MoveRequest move = doc.toObject(MoveRequest.class);
                if (move != null) {
                    move.setId(doc.getId());
                    moves.add(move);
                }
            }
        }
        return moves;
    }

    // פונקציית עזר לעדכון ישיר (אופציונלי)
    public Task<Void> updateMoveDetails(String moveId, String fromAddress, String toAddress, long moveDate) {
        return db.collection(COLLECTION_MOVES).document(moveId).update(
                "sourceAddress", fromAddress,
                "destAddress", toAddress,
                "moveDate", moveDate
        );
    }
}