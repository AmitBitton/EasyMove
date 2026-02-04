package com.example.easymove.model.repository;

import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private static final String COLLECTION_MOVES = "moves";
    private static final String COLLECTION_CHATS = "chats";
    private static final String COLLECTION_MATCH_REQUESTS = "match_requests";
    private static final String COLLECTION_USERS = "users";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // --- שותפים והובלות ---

    public Task<Void> approveMatchByPartner(String requestId, String partnerUid) {
        return db.collection(COLLECTION_USERS).document(partnerUid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw new Exception("שגיאה בשליפת פרטי שותף");
                    }
                    UserProfile profile = task.getResult().toObject(UserProfile.class);
                    String address = (profile != null && profile.getDefaultFromAddress() != null)
                            ? profile.getDefaultFromAddress() : "כתובת לא מעודכנת";

                    return db.collection(COLLECTION_MATCH_REQUESTS).document(requestId)
                            .update(
                                    "status", "waiting_for_mover",
                                    "partnerAddress", address
                            );
                });
    }

    public Task<Void> finalizePartnerMatch(String requestId, String moveId, String partnerId, String partnerAddress) {
        WriteBatch batch = db.batch();

        DocumentReference requestRef = db.collection(COLLECTION_MATCH_REQUESTS).document(requestId);
        batch.update(requestRef, "status", "approved");

        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
        batch.update(moveRef,
                "partnerId", partnerId,
                "intermediateAddress", partnerAddress
        );

        return batch.commit();
    }

    public Task<Void> rejectAndDeleteRequest(String requestId) {
        return db.collection(COLLECTION_MATCH_REQUESTS).document(requestId).delete();
    }

    public ListenerRegistration listenToMoverConfirmedMoves(String moverId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

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
                .update("sourceAddress", source, "destAddress", dest, "moveDate", date);
    }

    public Task<Void> cancelMoveAndResetChat(String moveId, String chatId, String customerId) {
        if (chatId == null || chatId.isEmpty()) {
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
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);

        batch.update(moveRef, "status", "CANCELED", "confirmed", false);

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
    }

    public Task<Void> completeMove(String moveId) {
        return db.collection(COLLECTION_MOVES).document(moveId).get().continueWithTask(task -> {
            String chatId = null;
            if (task.isSuccessful() && task.getResult() != null) {
                chatId = task.getResult().getString("chatId");
            }
            WriteBatch batch = db.batch();
            DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
            batch.update(moveRef, "status", "COMPLETED");
            if (chatId != null && !chatId.isEmpty()) {
                DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
                batch.update(chatRef, "moverConfirmed", false, "customerConfirmed", false, "moverConfirmedAt", null, "customerConfirmedAt", null);
            }
            return batch.commit();
        });
    }

    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest activeMove = task.getResult();
            if (activeMove == null) throw new FirebaseFirestoreException("No active move", FirebaseFirestoreException.Code.ABORTED);

            String moveId = activeMove.getId();
            DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
            DocumentReference userRef = db.collection("users").document(customerId);

            return db.runTransaction(transaction -> {
                transaction.update(chatRef, "customerConfirmed", true, "customerConfirmedAt", System.currentTimeMillis());
                transaction.update(moveRef, "status", "CONFIRMED", "confirmed", true, "moverId", moverId, "chatId", chatId);
                transaction.update(userRef, "defaultFromAddress", activeMove.getSourceAddress(), "defaultToAddress", activeMove.getDestAddress(), "defaultMoveDate", activeMove.getMoveDate(), "fromLat", activeMove.getSourceLat(), "fromLng", activeMove.getSourceLng(), "toLat", activeMove.getDestLat(), "toLng", activeMove.getDestLng());
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

    // ✅✅✅ התיקון הגדול להיסטוריית שותפים ✅✅✅
    public Task<List<MoveRequest>> getMoveHistory(String uid, String userType) {
        List<String> historyStatuses = Arrays.asList("COMPLETED", "CANCELED");

        // אם המשתמש הוא מוביל - מחפשים רגיל
        if ("mover".equals(userType)) {
            return db.collection(COLLECTION_MOVES)
                    .whereEqualTo("moverId", uid)
                    .whereIn("status", historyStatuses)
                    .orderBy("moveDate", Query.Direction.DESCENDING)
                    .get()
                    .continueWith(this::mapQueryToMoves);
        }

        // אם המשתמש הוא לקוח - הוא יכול להיות גם "בעלים" וגם "שותף"
        // מכיוון שאי אפשר לעשות OR ב-Firestore בין שדות שונים באותה שאילתה, נעשה שתי שאילתות ונאחד אותן

        // 1. שאילתה כבעל ההובלה (customerId)
        Task<List<MoveRequest>> taskAsCustomer = db.collection(COLLECTION_MOVES)
                .whereEqualTo("customerId", uid)
                .whereIn("status", historyStatuses)
                .orderBy("moveDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);

        // 2. שאילתה כשותף (partnerId)
        Task<List<MoveRequest>> taskAsPartner = db.collection(COLLECTION_MOVES)
                .whereEqualTo("partnerId", uid)
                .whereIn("status", historyStatuses)
                .orderBy("moveDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);

        // 3. איחוד התוצאות
        return Tasks.whenAllSuccess(taskAsCustomer, taskAsPartner).continueWith(task -> {
            List<Object> results = task.getResult(); // מחזיר רשימה של התוצאות של כל טאסק
            List<MoveRequest> combinedList = new ArrayList<>();

            // מוסיפים תוצאות מהשאילתה הראשונה
            if (results.size() > 0) {
                combinedList.addAll((List<MoveRequest>) results.get(0));
            }
            // מוסיפים תוצאות מהשאילתה השנייה
            if (results.size() > 1) {
                combinedList.addAll((List<MoveRequest>) results.get(1));
            }

            // מיון ידני מחדש לפי תאריך (כי האיחוד משבש את הסדר)
            Collections.sort(combinedList, new Comparator<MoveRequest>() {
                @Override
                public int compare(MoveRequest m1, MoveRequest m2) {
                    return Long.compare(m2.getMoveDate(), m1.getMoveDate()); // סדר יורד (הכי חדש למעלה)
                }
            });

            return combinedList;
        });
    }

    public Task<List<MoveRequest>> getMoverConfirmedMoves(String moverId) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);
    }

    // פונקציית עזר להמרת תוצאות שאילתה לרשימת הובלות
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

    public Task<Void> requestCancelMoveByCustomer(String moveId, String customerId) {
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
        return moveRef.update(
                "cancelRequestPending", true,
                "cancelRequestedAt", System.currentTimeMillis(),
                "cancelRequestedBy", customerId
        );
    }

    public Task<Void> approveCancelMoveByMover(String moveId, String chatId, String customerId, String moverId) {
        WriteBatch batch = db.batch();
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);

        batch.update(moveRef,
                "cancelRequestPending", false,
                "cancelApprovedAt", System.currentTimeMillis(),
                "cancelApprovedBy", moverId
        );

        return batch.commit().continueWithTask(t -> performCancel(moveId, chatId, customerId));
    }

    public Task<Void> cancelPartnerParticipation(String moveId) {
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
        return moveRef.update(
                "partnerId", null,
                "intermediateAddress", null,
                "intermediateLat", null,
                "intermediateLng", null
        );
    }
}