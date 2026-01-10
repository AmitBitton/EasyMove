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
import java.util.List;
import java.util.Map;

public class MyDeliveriesViewModel extends ViewModel {

    private final MoveRepository moveRepository = new MoveRepository();

    private final MutableLiveData<List<MoveRequest>> deliveries = new MutableLiveData<>();
    private final MutableLiveData<Map<String, MatchRequest>> activeRequestsMap = new MutableLiveData<>(new HashMap<>());

    private ListenerRegistration listenerRegistration;
    private ListenerRegistration requestsListener;

    public LiveData<List<MoveRequest>> getDeliveries() { return deliveries; }
    public LiveData<Map<String, MatchRequest>> getActiveRequestsMap() { return activeRequestsMap; }

    public void loadMyDeliveries() {
        String currentUserId = moveRepository.getCurrentUserId();
        if (currentUserId == null) {
            deliveries.setValue(null);
            return;
        }

        if (listenerRegistration != null) listenerRegistration.remove();

        listenerRegistration = moveRepository.listenToMoverConfirmedMoves(currentUserId, (value, error) -> {
            if (error != null) return;
            if (value != null) {
                List<MoveRequest> list = value.toObjects(MoveRequest.class);
                deliveries.setValue(list);
                listenForPartnerRequests(list);
            }
        });
    }

    private void listenForPartnerRequests(List<MoveRequest> moves) {
        if (requestsListener != null) requestsListener.remove();
        if (moves.isEmpty()) return;

        requestsListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("status", "waiting_for_mover")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        Map<String, MatchRequest> requestsMap = new HashMap<>();
                        for (QueryDocumentSnapshot doc : value) {
                            MatchRequest req = doc.toObject(MatchRequest.class);
                            req.setRequestId(doc.getId());
                            for (MoveRequest move : moves) {
                                if (move.getId().equals(req.getMoveId())) {
                                    requestsMap.put(move.getId(), req);
                                    break;
                                }
                            }
                        }
                        activeRequestsMap.setValue(requestsMap);
                    }
                });
    }

    public void approvePartner(MatchRequest req) {
        // התיקון: השותף הוא הנמען (toUserId) כי הבעלים (fromUserId) הוא זה שיצר את הבקשה
        String partnerId = req.getToUserId();

        moveRepository.finalizePartnerMatch(
                req.getRequestId(),
                req.getMoveId(),
                partnerId, // <--- הנה התיקון!
                req.getPartnerAddress()
        );
    }

    public void rejectPartner(MatchRequest req) {
        moveRepository.rejectAndDeleteRequest(req.getRequestId());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (listenerRegistration != null) listenerRegistration.remove();
        if (requestsListener != null) requestsListener.remove();
    }
}