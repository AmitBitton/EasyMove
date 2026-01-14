package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository; // שינוי חשוב
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration; // הוספנו
import com.google.firebase.firestore.FirebaseFirestore; // הוספנו

import java.util.ArrayList;
import java.util.List;

public class PartnerMatchViewModel extends ViewModel {

    private final UserRepository userRepository = new UserRepository();
    private final MoveRepository moveRepository = new MoveRepository(); // הוספנו

    private final MutableLiveData<List<UserProfile>> potentialPartners = new MutableLiveData<>();
    private final MutableLiveData<List<MatchRequest>> incomingRequests = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    private List<UserProfile> allUsersCache = new ArrayList<>();

    // שומר את ההאזנה לבקשות
    private ListenerRegistration requestsListener;

    public LiveData<List<UserProfile>> getPotentialPartners() { return potentialPartners; }
    public LiveData<List<MatchRequest>> getIncomingRequests() { return incomingRequests; }
    public LiveData<String> getToastMessage() { return toastMessage; }

    public void loadData() {
        loadPotentialPartners();
        startListeningToRequests(); // החלפנו את loadIncomingRequests בהאזנה רציפה
    }

    private void loadPotentialPartners() {
        userRepository.getAllPotentialPartners()
                .addOnSuccessListener(snapshot -> {
                    List<UserProfile> list = new ArrayList<>();
                    String myUid = userRepository.getCurrentUserId();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
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

    // ✅ האזנה בזמן אמת לבקשות נכנסות (במקום load חד פעמי)
    public void startListeningToRequests() {
        String myUid = userRepository.getCurrentUserId();
        if (myUid == null) return;

        if (requestsListener != null) requestsListener.remove();

        // מאזינים לבקשות שסטטוס שלהן הוא "pending" והן מיועדות אליי
        requestsListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("toUserId", myUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // אפשר לטפל בשגיאה
                        return;
                    }

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

    public void sendRequest(UserProfile toUser) {
        // שימוש בפונקציה המעודכנת ב-UserRepository ששולחת גם את moveId
        userRepository.sendMatchRequest(toUser.getUserId())
                .addOnSuccessListener(v -> toastMessage.setValue("בקשה נשלחה ל-" + toUser.getName()))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    // ✅ אישור: משתמש בפונקציה החדשה ב-MoveRepository
    public void approveRequest(MatchRequest request) {
        String myUid = userRepository.getCurrentUserId();
        if (myUid == null) return;

        moveRepository.approveMatchByPartner(request.getRequestId(), myUid)
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הבקשה אושרה! ממתין לאישור המוביל.");
                    // הרשימה תתעדכן לבד בגלל ה-Listener
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה באישור"));
    }

    // ✅ דחייה: מחיקת הבקשה
    public void rejectRequest(MatchRequest request) {
        moveRepository.rejectAndDeleteRequest(request.getRequestId())
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הבקשה נדחתה ונמחקה");
                    // הרשימה תתעדכן לבד
                })
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