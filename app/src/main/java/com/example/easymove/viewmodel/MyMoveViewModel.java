package com.example.easymove.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

/**
 * ViewModel responsible for the "My Move" dashboard (Customer Side).
 * Handles:
 * 1. Detecting active moves (Owner or Partner).
 * 2. Falling back to "Draft" mode (Profile data) if no active move exists.
 * 3. Listening for incoming partner invites.
 * 4. Handling Move Cancellations (Policy checks).
 */
public class MyMoveViewModel extends ViewModel {

    private static final String TAG = "MyMoveViewModel";

    private final MoveRepository repository = new MoveRepository();
    private final UserRepository userRepository = new UserRepository();

    // UI State
    private final MutableLiveData<MoveRequest> currentMove = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();
    private final MutableLiveData<MatchRequest> incomingRequest = new MutableLiveData<>();

    private ListenerRegistration moveListener;
    private ListenerRegistration requestListener;

    // --- Getters ---
    public LiveData<MoveRequest> getCurrentMove() { return currentMove; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }
    public LiveData<MatchRequest> getIncomingRequest() { return incomingRequest; }

    /**
     * Main Entry Point:
     * 1. Checks if the user is the OWNER of an active move.
     * 2. If not, checks if they are a PARTNER in an active move.
     * 3. If neither, loads a DRAFT from their profile.
     */
    public void loadCurrentMove() {
        String uid = repository.getCurrentUserId();
        if (uid == null) return;

        // Cleanup old listeners
        if (moveListener != null) moveListener.remove();

        // Always listen for invites in parallel
        listenForMatchRequests(uid);

        isLoading.setValue(true);
        Log.d(TAG, "Checking for moves as OWNER for: " + uid);

        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("customerId", uid)
                .whereIn("status", Arrays.asList("OPEN", "CONFIRMED"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error checking owner moves: " + error.getMessage());
                        checkIfImPartner(uid); // Fallback on error
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d(TAG, "Found active move as OWNER.");
                        updateMoveData(value.getDocuments().get(0));
                    } else {
                        Log.d(TAG, "No owner moves found. Checking as PARTNER...");
                        checkIfImPartner(uid);
                    }
                });
    }

    private void checkIfImPartner(String uid) {
        if (moveListener != null) moveListener.remove();

        Log.d(TAG, "Checking for moves as PARTNER for: " + uid);

        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("partnerId", uid)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false); // Stop loading regardless of outcome

                    if (error != null) {
                        Log.e(TAG, "Error checking partner moves: " + error.getMessage());
                        loadDraftFromProfile();
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // Iterate to find the active one (OPEN or CONFIRMED)
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");
                            if ("OPEN".equals(status) || "CONFIRMED".equals(status)) {
                                Log.d(TAG, "Found active move as PARTNER. ID: " + doc.getId());
                                updateMoveData(doc);
                                return;
                            }
                        }
                    }

                    // No active move found at all
                    Log.d(TAG, "No active moves found. Loading DRAFT from Profile...");
                    loadDraftFromProfile();
                });
    }

    /**
     * Loads default address/date from User Profile to show as a "Draft".
     * A draft is distinguished by having a null ID.
     */
    private void loadDraftFromProfile() {
        userRepository.getMyProfile().addOnSuccessListener(profile -> {
            if (profile != null) {
                MoveRequest draftMove = new MoveRequest();
                draftMove.setId(null); // Important: Signals UI this is a Draft
                draftMove.setSourceAddress(profile.getDefaultFromAddress());
                draftMove.setDestAddress(profile.getDefaultToAddress());

                if (profile.getDefaultMoveDate() != null) {
                    draftMove.setMoveDate(profile.getDefaultMoveDate());
                }
                currentMove.setValue(draftMove);
            } else {
                currentMove.setValue(null); // Truly empty state
            }
        }).addOnFailureListener(e -> currentMove.setValue(null));
    }

    private void updateMoveData(DocumentSnapshot doc) {
        MoveRequest move = doc.toObject(MoveRequest.class);
        if (move != null) {
            move.setId(doc.getId());
            currentMove.setValue(move);
        }
        isLoading.setValue(false);
    }

    // ------------------------------------------------------------------------
    // Partner Matching Logic
    // ------------------------------------------------------------------------

    public void listenForMatchRequests(String uid) {
        if (requestListener != null) requestListener.remove();

        requestListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("toUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        MatchRequest req = doc.toObject(MatchRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            incomingRequest.setValue(req);
                        }
                    } else {
                        incomingRequest.setValue(null);
                    }
                });
    }

    public void approveMatch(MatchRequest req) {
        repository.approveMatchByPartner(req.getRequestId(), repository.getCurrentUserId());
    }

    public void rejectMatch(MatchRequest req) {
        repository.rejectAndDeleteRequest(req.getRequestId());
    }

    // ------------------------------------------------------------------------
    // Cancellation Logic
    // ------------------------------------------------------------------------

    public void cancelCurrentMove() {
        MoveRequest move = currentMove.getValue();
        if (move != null && move.getId() != null) {
            repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
        }
    }

    public void markMoveAsCompleted() {
        MoveRequest move = currentMove.getValue();
        if (move != null && move.getId() != null) {
            repository.completeMove(move.getId());
        }
    }

    /**
     * Handles cancellation based on user role and time policy.
     * - Partners can leave anytime.
     * - Customers can cancel freely > 1 week before.
     * - Customers need Mover approval < 1 week before.
     */
    public void cancelMoveWithPolicy() {
        MoveRequest move = currentMove.getValue();
        if (move == null || move.getId() == null) return;

        String myId = repository.getCurrentUserId();
        if (myId == null) return;

        boolean iAmPartner = myId.equals(move.getPartnerId());
        boolean iAmCustomer = myId.equals(move.getCustomerId());

        if (iAmPartner) {
            // Partner just leaves, move stays open for customer
            repository.cancelPartnerParticipation(move.getId());
            return;
        }

        if (iAmCustomer) {
            long now = System.currentTimeMillis();
            long weekMs = 7L * 24L * 60L * 60L * 1000L;
            long moveDate = move.getMoveDate();

            // Policy check
            if (moveDate == 0 || (moveDate - now) >= weekMs) {
                // More than a week away OR no date set -> Instant Cancel
                repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
            } else {
                // Less than a week away -> Request Approval
                repository.requestCancelMoveByCustomer(move.getId(), move.getCustomerId());
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (moveListener != null) moveListener.remove();
        if (requestListener != null) requestListener.remove();
    }
}