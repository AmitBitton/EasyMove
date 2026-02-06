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
import java.util.List;
import java.util.Objects;

/**
 * Repository class responsible for managing all Move-related data in Firestore.
 * Handles the lifecycle of a move: Creation (Draft), Matching with Partners,
 * Confirmation with Mover, Cancellation, and Completion.
 */
public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private static final String COLLECTION_MOVES = "moves";
    private static final String COLLECTION_CHATS = "chats";
    private static final String COLLECTION_MATCH_REQUESTS = "match_requests";
    private static final String COLLECTION_USERS = "users";

    /**
     * @return The current user's UID or null if not logged in.
     */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // ------------------------------------------------------------------------
    // Partner Matching Logic
    // ------------------------------------------------------------------------

    /**
     * Approves a partner match request (Step 1: Partner approves the request).
     * Updates the request status to 'waiting_for_mover' and fetches the partner's address.
     */
    public Task<Void> approveMatchByPartner(String requestId, String partnerUid) {
        return db.collection(COLLECTION_USERS).document(partnerUid).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw new Exception("Error fetching partner profile details.");
                    }
                    UserProfile profile = task.getResult().toObject(UserProfile.class);
                    String address = (profile != null && profile.getDefaultFromAddress() != null)
                            ? profile.getDefaultFromAddress() : "Address not updated";

                    return db.collection(COLLECTION_MATCH_REQUESTS).document(requestId)
                            .update(
                                    "status", "waiting_for_mover",
                                    "partnerAddress", address
                            );
                });
    }

    /**
     * Finalizes the partner match (Step 2: Mover approves the request).
     * Updates the move document with the partner's ID and address.
     */
    public Task<Void> finalizePartnerMatch(String requestId, String moveId, String partnerId, String partnerAddress) {
        WriteBatch batch = db.batch();

        // Mark request as approved
        DocumentReference requestRef = db.collection(COLLECTION_MATCH_REQUESTS).document(requestId);
        batch.update(requestRef, "status", "approved");

        // Update the actual move with partner details
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
        batch.update(moveRef,
                "partnerId", partnerId,
                "intermediateAddress", partnerAddress
        );

        return batch.commit();
    }

    /**
     * Rejects a partner request and deletes it from the database.
     */
    public Task<Void> rejectAndDeleteRequest(String requestId) {
        return db.collection(COLLECTION_MATCH_REQUESTS).document(requestId).delete();
    }

    // ------------------------------------------------------------------------
    // Active Move Management (Customer Side)
    // ------------------------------------------------------------------------

    /**
     * Retrieves the current active move for a customer (OPEN or CONFIRMED).
     * Returns the most recent one created.
     */
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
                    if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
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
     * Ensures an active move document exists for the customer.
     * If one exists, returns it. If not, creates a new "OPEN" move.
     */
    public Task<MoveRequest> ensureActiveMoveForCustomer(String customerId) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest existingMove = task.getResult();
            if (existingMove != null) {
                return Tasks.forResult(existingMove);
            }
            // Create new move
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

    /**
     * Updates the draft details (source, dest, date) of the current active move.
     */
    public Task<Void> updateMoveDraftDetails(String customerId, String source, String dest, long date) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest move = task.getResult();
            if (move == null) {
                // If no move exists, create one and then update it
                return ensureActiveMoveForCustomer(customerId).continueWithTask(t ->
                        updateMoveFieldsInternal(t.getResult().getId(), source, dest, date));
            }
            return updateMoveFieldsInternal(move.getId(), source, dest, date);
        });
    }

    private Task<Void> updateMoveFieldsInternal(String moveId, String source, String dest, long date) {
        return db.collection(COLLECTION_MOVES).document(moveId)
                .update("sourceAddress", source, "destAddress", dest, "moveDate", date);
    }

    // ------------------------------------------------------------------------
    // Move Lifecycle Actions
    // ------------------------------------------------------------------------

    /**
     * Confirms a move (Customer accepts Mover's offer).
     * Updates Move status, Chat confirmation, and User's default preferences.
     */
    public Task<Void> confirmMoveByCustomer(String chatId, String moverId, String customerId) {
        return getCurrentActiveMove(customerId).continueWithTask(task -> {
            MoveRequest activeMove = task.getResult();
            if (activeMove == null)
                throw new FirebaseFirestoreException("No active move found", FirebaseFirestoreException.Code.ABORTED);

            String moveId = activeMove.getId();
            DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);
            DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
            DocumentReference userRef = db.collection(COLLECTION_USERS).document(customerId);

            return db.runTransaction(transaction -> {
                // 1. Update Chat
                transaction.update(chatRef,
                        "customerConfirmed", true,
                        "customerConfirmedAt", System.currentTimeMillis());

                // 2. Update Move
                transaction.update(moveRef,
                        "status", "CONFIRMED",
                        "confirmed", true,
                        "moverId", moverId,
                        "chatId", chatId);

                // 3. Update User Defaults
                transaction.update(userRef,
                        "defaultFromAddress", activeMove.getSourceAddress(),
                        "defaultToAddress", activeMove.getDestAddress(),
                        "defaultMoveDate", activeMove.getMoveDate(),
                        "fromLat", activeMove.getSourceLat(),
                        "fromLng", activeMove.getSourceLng(),
                        "toLat", activeMove.getDestLat(),
                        "toLng", activeMove.getDestLng());
                return null;
            });
        });
    }

    /**
     * Cancels a move and resets the associated chat confirmations.
     */
    public Task<Void> cancelMoveAndResetChat(String moveId, String chatId, String customerId) {
        if (chatId == null || chatId.isEmpty()) {
            // Try to find chatId from the move document if not provided
            return db.collection(COLLECTION_MOVES).document(moveId).get().continueWithTask(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String realChatId = task.getResult().getString("chatId");
                    return performCancel(moveId, realChatId);
                }
                return performCancel(moveId, null);
            });
        }
        return performCancel(moveId, chatId);
    }

    private Task<Void> performCancel(String moveId, String chatId) {
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

    /**
     * Marks a move as COMPLETED.
     */
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
                batch.update(chatRef,
                        "moverConfirmed", false,
                        "customerConfirmed", false,
                        "moverConfirmedAt", null,
                        "customerConfirmedAt", null);
            }
            return batch.commit();
        });
    }

    // ------------------------------------------------------------------------
    // Move History (Complex Queries)
    // ------------------------------------------------------------------------

    /**
     * Retrieves the move history (COMPLETED or CANCELED) for a user.
     * <p>
     * If userType is "mover": Simple query by moverId.
     * If userType is "customer": Performs two queries (Owner + Partner) and merges them.
     */
    public Task<List<MoveRequest>> getMoveHistory(String uid, String userType) {
        List<String> historyStatuses = Arrays.asList("COMPLETED", "CANCELED");

        // 1. Mover Logic
        if ("mover".equals(userType)) {
            return db.collection(COLLECTION_MOVES)
                    .whereEqualTo("moverId", uid)
                    .whereIn("status", historyStatuses)
                    .orderBy("moveDate", Query.Direction.DESCENDING)
                    .get()
                    .continueWith(this::mapQueryToMoves);
        }

        // 2. Customer Logic (Owner OR Partner)
        // Firestore does not support logical OR across different fields in one query.
        // We run two parallel queries and merge the results.

        Task<List<MoveRequest>> taskAsCustomer = db.collection(COLLECTION_MOVES)
                .whereEqualTo("customerId", uid)
                .whereIn("status", historyStatuses)
                .orderBy("moveDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);

        Task<List<MoveRequest>> taskAsPartner = db.collection(COLLECTION_MOVES)
                .whereEqualTo("partnerId", uid)
                .whereIn("status", historyStatuses)
                .orderBy("moveDate", Query.Direction.DESCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);

        return Tasks.whenAllSuccess(taskAsCustomer, taskAsPartner).continueWith(task -> {
            List<Object> results = task.getResult();
            List<MoveRequest> combinedList = new ArrayList<>();

            if (!results.isEmpty()) {
                combinedList.addAll((List<MoveRequest>) results.get(0));
            }
            if (results.size() > 1) {
                combinedList.addAll((List<MoveRequest>) results.get(1));
            }

            // Re-sort manually because merging destroys order
            combinedList.sort((m1, m2) -> {
                return Long.compare(m2.getMoveDate(), m1.getMoveDate()); // Descending
            });

            return combinedList;
        });
    }

    /**
     * Helper to map Firestore QuerySnapshot to List<MoveRequest>.
     */
    private List<MoveRequest> mapQueryToMoves(Task<QuerySnapshot> task) throws Exception {
        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
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

    // ------------------------------------------------------------------------
    // Mover Specific
    // ------------------------------------------------------------------------

    public ListenerRegistration listenToMoverConfirmedMoves(String moverId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public Task<List<MoveRequest>> getMoverConfirmedMoves(String moverId) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("moverId", moverId)
                .whereEqualTo("status", "CONFIRMED")
                .orderBy("moveDate", Query.Direction.ASCENDING)
                .get()
                .continueWith(this::mapQueryToMoves);
    }

    // ------------------------------------------------------------------------
    // Cancellation Requests (Pending Approval)
    // ------------------------------------------------------------------------

    public Task<Void> requestCancelMoveByCustomer(String moveId, String customerId) {
        return db.collection(COLLECTION_MOVES).document(moveId).update(
                "cancelRequestPending", true,
                "cancelRequestedAt", System.currentTimeMillis(),
                "cancelRequestedBy", customerId
        );
    }

    public Task<Void> approveCancelMoveByMover(String moveId, String chatId, String customerId, String moverId) {
        WriteBatch batch = db.batch();
        DocumentReference moveRef = db.collection(COLLECTION_MOVES).document(moveId);

        // Mark cancellation as approved
        batch.update(moveRef,
                "cancelRequestPending", false,
                "cancelApprovedAt", System.currentTimeMillis(),
                "cancelApprovedBy", moverId
        );

        // Chain the actual cancellation logic after batch update
        return batch.commit().continueWithTask(t -> performCancel(moveId, chatId));
    }

    /**
     * Checks if the customer has any currently active CONFIRMED move.
     */
    public Task<Boolean> hasActiveConfirmedMove(String customerId) {
        return db.collection(COLLECTION_MOVES)
                .whereEqualTo("customerId", customerId)
                .whereEqualTo("status", "CONFIRMED")
                .limit(1)
                .get()
                .continueWith(task -> !task.getResult().isEmpty());
    }

    /**
     * Removes a partner from a move (effectively cancelling their participation).
     */
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