package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the "Partner Match" screen.
 * Handles:
 * 1. Fetching potential partners (Customers).
 * 2. Searching/Filtering the partner list locally.
 * 3. Listening for incoming partnership requests.
 * 4. Sending, Approving, and Rejecting requests.
 */
public class PartnerMatchViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final MoveRepository moveRepository = new MoveRepository();

    // UI State LiveData
    private final MutableLiveData<List<UserProfile>> potentialPartners = new MutableLiveData<>();
    private final MutableLiveData<List<MatchRequest>> incomingRequests = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // Local cache for search functionality
    private List<UserProfile> allUsersCache = new ArrayList<>();

    // Firestore Listener
    private ListenerRegistration requestsListener;

    // --- Getters ---
    public LiveData<List<UserProfile>> getPotentialPartners() { return potentialPartners; }
    public LiveData<List<MatchRequest>> getIncomingRequests() { return incomingRequests; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    /**
     * Initializes data loading (Users list and Incoming Requests listener).
     */
    public void loadData() {
        loadPotentialPartners();
        startListeningToRequests();
    }

    /**
     * Fetches all potential partners (Users) from Firestore, excluding the current user.
     * Caches the result for local search.
     */
    private void loadPotentialPartners() {
        String myUid = userRepository.getCurrentUserId();
        if (myUid == null) return;

        userRepository.getAllPotentialPartners()
                .addOnSuccessListener(snapshot -> {
                    List<UserProfile> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // Skip myself
                        if (doc.getId().equals(myUid)) continue;

                        UserProfile p = doc.toObject(UserProfile.class);
                        if (p != null) {
                            p.setUserId(doc.getId());
                            list.add(p);
                        }
                    }
                    // Update Cache and LiveData
                    allUsersCache = list;
                    potentialPartners.setValue(list);
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת משתמשים"));
    }

    /**
     * Filters the cached list of partners based on the search query.
     */
    public void searchPartners(String query) {
        if (query == null || query.trim().isEmpty()) {
            potentialPartners.setValue(allUsersCache);
            return;
        }

        List<UserProfile> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (UserProfile p : allUsersCache) {
            if (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) {
                filtered.add(p);
            }
        }
        potentialPartners.setValue(filtered);
    }

    /**
     * Listens for real-time incoming "Match Requests" where the current user is the target.
     */
    public void startListeningToRequests() {
        String myUid = userRepository.getCurrentUserId();
        if (myUid == null) return;

        // Prevent duplicate listeners
        if (requestsListener != null) requestsListener.remove();

        requestsListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("toUserId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        List<MatchRequest> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            MatchRequest req = doc.toObject(MatchRequest.class);
                            if (req != null) {
                                req.setRequestId(doc.getId());
                                list.add(req);
                            }
                        }
                        incomingRequests.setValue(list);
                    }
                });
    }

    // =================================================================
    //  Actions (Send, Approve, Reject)
    // =================================================================

    /**
     * Sends a partnership request to another user.
     */
    public void sendRequest(UserProfile toUser) {
        // Validation handled in Repository, but good to have UI feedback
        userRepository.sendMatchRequest(toUser.getUserId(), toUser.getName())
                .addOnSuccessListener(v -> toastMessage.setValue("בקשה נשלחה ל-" + toUser.getName()))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    /**
     * Approves an incoming request.
     * This links the two users in the MoveRequest logic (Waiting for Mover).
     */
    public void approveRequest(MatchRequest request) {
        String myUid = userRepository.getCurrentUserId();
        if (myUid == null) return;

        moveRepository.approveMatchByPartner(request.getRequestId(), myUid)
                .addOnSuccessListener(v -> toastMessage.setValue("הבקשה אושרה! ממתין לאישור המוביל."))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה באישור"));
    }

    /**
     * Rejects (deletes) an incoming request.
     */
    public void rejectRequest(MatchRequest request) {
        moveRepository.rejectAndDeleteRequest(request.getRequestId())
                .addOnSuccessListener(v -> toastMessage.setValue("הבקשה נדחתה ונמחקה"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בדחייה"));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}