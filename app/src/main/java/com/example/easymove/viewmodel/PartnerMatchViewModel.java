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

public class PartnerMatchViewModel extends ViewModel {

    private final UserRepository repository = new UserRepository();

    private final MutableLiveData<List<UserProfile>> potentialPartners = new MutableLiveData<>();
    private final MutableLiveData<List<MatchRequest>> incomingRequests = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // רשימה מלאה לסינון מקומי (Client side search)
    private List<UserProfile> allUsersCache = new ArrayList<>();

    public LiveData<List<UserProfile>> getPotentialPartners() { return potentialPartners; }
    public LiveData<List<MatchRequest>> getIncomingRequests() { return incomingRequests; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void loadData() {
        loadPotentialPartners();
        loadIncomingRequests();
    }

    private void loadPotentialPartners() {
        repository.getAllPotentialPartners()
                .addOnSuccessListener(snapshot -> {
                    List<UserProfile> list = new ArrayList<>();
                    String myUid = repository.getCurrentUserId();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // לסנן את עצמי
                        if (doc.getId().equals(myUid)) continue;

                        UserProfile p = doc.toObject(UserProfile.class);
                        if (p != null) {
                            p.setUserId(doc.getId());
                            list.add(p);
                        }
                    }
                    allUsersCache = list;
                    potentialPartners.setValue(list);
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת משתמשים"));
    }

    public void searchPartners(String query) {
        if (query == null || query.isEmpty()) {
            potentialPartners.setValue(allUsersCache);
            return;
        }

        List<UserProfile> filtered = new ArrayList<>();
        for (UserProfile p : allUsersCache) {
            if (p.getName() != null && p.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(p);
            }
        }
        potentialPartners.setValue(filtered);
    }

    private void loadIncomingRequests() {
        repository.getIncomingRequests()
                .addOnSuccessListener(snapshot -> {
                    List<MatchRequest> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        MatchRequest req = doc.toObject(MatchRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            list.add(req);
                        }
                    }
                    incomingRequests.setValue(list);
                });
    }

    public void sendRequest(UserProfile toUser) {
        repository.sendMatchRequest(toUser.getUserId())
                .addOnSuccessListener(v -> toastMessage.setValue("בקשה נשלחה ל-" + toUser.getName()))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בשליחה"));
    }

    public void approveRequest(MatchRequest request) {
        repository.updateMatchRequestStatus(request.getRequestId(), "accepted")
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("אישרת את הבקשה!");
                    loadIncomingRequests(); // רענון הרשימה
                });
    }

    public void rejectRequest(MatchRequest request) {
        repository.updateMatchRequestStatus(request.getRequestId(), "rejected")
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הבקשה נדחתה");
                    loadIncomingRequests(); // רענון הרשימה
                });
    }
}