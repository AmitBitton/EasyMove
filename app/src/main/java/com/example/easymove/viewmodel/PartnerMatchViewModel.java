package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel responsible for:
 * - Loading potential partners
 * - Loading incoming match requests
 * - Handling search (client-side filtering)
 * - Sending / approving / rejecting match requests
 * <p>
 * This class follows MVVM:
 * UI (Fragment) observes LiveData exposed here.
 */
public class PartnerMatchViewModel extends ViewModel {

    /**
     * Repository handles all Firebase / Firestore logic.
     * ViewModel should NEVER talk to Firebase directly.
     */
    private final UserRepository repository = new UserRepository();

    /**
     * LiveData exposed to the UI
     */
    private final MutableLiveData<List<UserProfile>> potentialPartners = new MutableLiveData<>();
    private final MutableLiveData<List<MatchRequest>> incomingRequests = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    /**
     * Cache of all users used for local (client-side) searching.
     * This prevents refetching data on every search query.
     */
    private List<UserProfile> allUsersCache = new ArrayList<>();

    // --- Public getters for LiveData (encapsulation best practice) ---
    public LiveData<List<UserProfile>> getPotentialPartners() {
        return potentialPartners;
    }

    public LiveData<List<MatchRequest>> getIncomingRequests() {
        return incomingRequests;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    /**
     * Entry point called by the Fragment
     * Loads all required data at once
     */
    public void loadData() {
        loadPotentialPartners();
        loadIncomingRequests();
    }

    /**
     * Loads all potential partners from Firestore
     * - Filters out the current user
     * - Converts documents into UserProfile objects
     * - Stores a local cache for searching
     */
    private void loadPotentialPartners() {
        repository.getAllPotentialPartners()
                .addOnSuccessListener(snapshot -> {
                    List<UserProfile> list = new ArrayList<>();
                    String myUid = repository.getCurrentUserId();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        // Do not include myself in the partner list
                        if (doc.getId().equals(myUid)) continue;

                        UserProfile profile = doc.toObject(UserProfile.class);
                        if (profile != null) {
                            // Firestore document ID is not auto-mapped
                            profile.setUserId(doc.getId());
                            list.add(profile);
                        }
                    }

                    // Cache full list for search functionality
                    allUsersCache = list;

                    // Notify UI
                    potentialPartners.setValue(list);
                })
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בטעינת משתמשים")
                );
    }

    /**
     * Client-side search over cached users
     * Avoids unnecessary Firestore queries
     */
    public void searchPartners(String query) {

        // Empty query → show all users
        if (query == null || query.isEmpty()) {
            potentialPartners.setValue(allUsersCache);
            return;
        }

        List<UserProfile> filtered = new ArrayList<>();

        for (UserProfile user : allUsersCache) {
            if (user.getName() != null &&
                    user.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(user);
            }
        }

        potentialPartners.setValue(filtered);
    }

    /**
     * Loads incoming match requests for the current user
     * Used to populate the "Incoming Requests" RecyclerView
     */
    private void loadIncomingRequests() {
        repository.getIncomingRequests()
                .addOnSuccessListener(snapshot -> {
                    List<MatchRequest> list = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        MatchRequest request = doc.toObject(MatchRequest.class);
                        if (request != null) {
                            // Store Firestore document ID
                            request.setRequestId(doc.getId());
                            list.add(request);
                        }
                    }

                    incomingRequests.setValue(list);
                });
    }

    /**
     * Sends a match request to another user
     */
    public void sendRequest(UserProfile toUser) {
        repository.sendMatchRequest(toUser.getUserId())
                .addOnSuccessListener(v ->
                        toastMessage.setValue("בקשה נשלחה ל-" + toUser.getName())
                )
                .addOnFailureListener(e ->
                        toastMessage.setValue("שגיאה בשליחה")
                );
    }

    /**
     * Approves an incoming match request
     * After approval, refresh the list
     */
    public void approveRequest(MatchRequest request) {
        repository.updateMatchRequestStatus(request.getRequestId(), "accepted")
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("אישרת את הבקשה!");
                    loadIncomingRequests(); // Refresh UI
                });
    }

    /**
     * Rejects an incoming match request
     * After rejection, refresh the list
     */
    public void rejectRequest(MatchRequest request) {
        repository.updateMatchRequestStatus(request.getRequestId(), "rejected")
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הבקשה נדחתה");
                    loadIncomingRequests(); // Refresh UI
                });
    }
}
