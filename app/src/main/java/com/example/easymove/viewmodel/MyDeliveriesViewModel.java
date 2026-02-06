package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel responsible for the "My Deliveries" screen (Mover side).
 * Handles:
 * 1. Fetching assigned moves (Deliveries).
 * 2. Listening for Partner Requests associated with those moves.
 * 3. Handling Move Cancellation and Partner Approvals.
 */
public class MyDeliveriesViewModel extends ViewModel {

    private final MoveRepository moveRepository = new MoveRepository();

    // List of moves assigned to this mover
    private final MutableLiveData<List<MoveRequest>> deliveries = new MutableLiveData<>();

    // Map of MoveID -> Pending Partner Request (Used to show red dots/indicators in UI)
    private final MutableLiveData<Map<String, MatchRequest>> activeRequestsMap = new MutableLiveData<>(new HashMap<>());

    private ListenerRegistration deliveriesListener;
    private ListenerRegistration requestsListener;

    // --- Getters ---
    public LiveData<List<MoveRequest>> getDeliveries() { return deliveries; }
    public LiveData<Map<String, MatchRequest>> getActiveRequestsMap() { return activeRequestsMap; }

    /**
     * Listen to all moves where the current user is assigned as the Mover.
     */
    public void loadMyDeliveries() {
        String currentUserId = moveRepository.getCurrentUserId();
        if (currentUserId == null) {
            deliveries.setValue(null);
            return;
        }

        // Remove old listener to prevent duplicates
        if (deliveriesListener != null) deliveriesListener.remove();

        deliveriesListener = moveRepository.listenToMoverConfirmedMoves(currentUserId, (value, error) -> {
            if (error != null) return;
            if (value != null) {

                List<MoveRequest> list = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                    MoveRequest move = doc.toObject(MoveRequest.class);
                    if (move != null) {
                        // Crucial: Ensure the model ID matches the Firestore Document ID
                        move.setId(doc.getId());
                        list.add(move);
                    }
                }

                deliveries.setValue(list);

                // Once we have the moves, we listen for relevant partner requests
                listenForPartnerRequests(list);
            }
        });
    }

    /**
     * Listens for "Partner Match" requests that are waiting for Mover approval.
     * Filters them to only include requests relevant to the loaded moves.
     */
    private void listenForPartnerRequests(List<MoveRequest> moves) {
        if (requestsListener != null) requestsListener.remove();
        if (moves.isEmpty()) return;

        // Optimization: Create a Set of IDs for O(1) lookup
        Set<String> myMoveIds = new HashSet<>();
        for (MoveRequest move : moves) {
            if (move.getId() != null) myMoveIds.add(move.getId());
        }

        requestsListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("status", "waiting_for_mover")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Map<String, MatchRequest> requestsMap = new HashMap<>();

                        for (QueryDocumentSnapshot doc : value) {
                            MatchRequest req = doc.toObject(MatchRequest.class);
                            req.setRequestId(doc.getId());

                            // Only add if this request belongs to one of the Mover's active jobs
                            if (req.getMoveId() != null && myMoveIds.contains(req.getMoveId())) {
                                requestsMap.put(req.getMoveId(), req);
                            }
                        }
                        activeRequestsMap.setValue(requestsMap);
                    }
                });
    }

    /**
     * Approves a partner match request.
     * Note: The `partnerId` is derived from `toUserId` because the Customer (Owner)
     * is the `fromUserId` who initiated the invitation.
     */
    public void approvePartner(MatchRequest req) {
        String partnerId = req.getToUserId();

        moveRepository.finalizePartnerMatch(
                req.getRequestId(),
                req.getMoveId(),
                partnerId,
                req.getPartnerAddress()
        );
    }

    public void rejectPartner(MatchRequest req) {
        moveRepository.rejectAndDeleteRequest(req.getRequestId());
    }

    /**
     * Mover approves a cancellation request initiated by the Customer.
     */
    public void approveCancel(MoveRequest move, String moverId) {
        if (move == null || moverId == null) return;

        moveRepository.approveCancelMoveByMover(
                move.getId(),
                move.getChatId(),
                move.getCustomerId(),
                moverId
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (deliveriesListener != null) deliveriesListener.remove();
        if (requestsListener != null) requestsListener.remove();
    }
}