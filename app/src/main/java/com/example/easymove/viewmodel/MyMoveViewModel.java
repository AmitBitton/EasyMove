package com.example.easymove.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository; // הוספנו
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class MyMoveViewModel extends ViewModel {

    private final MoveRepository repository = new MoveRepository();
    private final UserRepository userRepository = new UserRepository(); // הוספנו לטעינת פרופיל

    private final MutableLiveData<MoveRequest> currentMove = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();
    private final MutableLiveData<MatchRequest> incomingRequest = new MutableLiveData<>();

    private ListenerRegistration moveListener;
    private ListenerRegistration requestListener;

    public LiveData<MoveRequest> getCurrentMove() { return currentMove; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }
    public LiveData<MatchRequest> getIncomingRequest() { return incomingRequest; }

    public void loadCurrentMove() {
        String uid = repository.getCurrentUserId();
        if (uid == null) return;

        if (moveListener != null) moveListener.remove();
        listenForMatchRequests(uid);

        isLoading.setValue(true);
        Log.d("DEBUG_MOVE", "בודק אם אני בעל ההובלה (Customer): " + uid);

        // 1. בדיקה אם אני הבעלים
        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("customerId", uid)
                .whereIn("status", Arrays.asList("OPEN", "CONFIRMED"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("DEBUG_MOVE", "שגיאה בחיפוש כבעלים: " + error.getMessage());
                        // במקרה שגיאה ננסה לבדוק אם שותף, ואם לא - טיוטה
                        checkIfImPartner(uid);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        Log.d("DEBUG_MOVE", "נמצאה הובלה כבעלים!");
                        updateMoveData(value.getDocuments().get(0));
                    } else {
                        Log.d("DEBUG_MOVE", "לא נמצאה הובלה כבעלים, בודק אם אני שותף...");
                        checkIfImPartner(uid);
                    }
                });
    }

    private void checkIfImPartner(String uid) {
        if (moveListener != null) moveListener.remove();

        Log.d("DEBUG_MOVE", "מחפש הובלה שבה partnerId הוא: " + uid);

        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("partnerId", uid)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false); // סיימנו לטעון בכל מקרה

                    if (error != null) {
                        Log.e("DEBUG_MOVE", "🔥 שגיאה בחיפוש שותף: " + error.getMessage());
                        // במקרה שגיאה - נטען טיוטה מהפרופיל
                        loadDraftFromProfile();
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // לולאה כדי למצוא את ההובלה הפעילה (למקרה שיש היסטוריה)
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");
                            if ("OPEN".equals(status) || "CONFIRMED".equals(status)) {
                                Log.d("DEBUG_MOVE", "🎉 בול! נמצאה הובלה כשותף! ID: " + doc.getId());
                                updateMoveData(doc);
                                return; // יצאנו כי מצאנו הובלה אמיתית
                            }
                        }
                    }

                    // אם הגענו לפה - אין הובלה פעילה (לא כבעלים ולא כשותף)
                    Log.d("DEBUG_MOVE", "אין הובלה פעילה. טוען טיוטה מהפרופיל...");
                    loadDraftFromProfile();
                });
    }

    // ✅ פונקציה חדשה: טוענת את הכתובות מהפרופיל ומציגה אותן כ"טיוטה"
    private void loadDraftFromProfile() {
        userRepository.getMyProfile().addOnSuccessListener(profile -> {
            if (profile != null) {
                MoveRequest draftMove = new MoveRequest();
                draftMove.setId(null); // חשוב מאוד! זה הסימן שזו טיוטה ולא הובלה אמיתית
                draftMove.setSourceAddress(profile.getDefaultFromAddress());
                draftMove.setDestAddress(profile.getDefaultToAddress());
                if (profile.getDefaultMoveDate() != null) {
                    draftMove.setMoveDate(profile.getDefaultMoveDate());
                }
                // מציגים את הטיוטה
                currentMove.setValue(draftMove);
            } else {
                currentMove.setValue(null); // באמת אין כלום
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

    public void listenForMatchRequests(String uid) {
        if (requestListener != null) requestListener.remove();
        requestListener = FirebaseFirestore.getInstance().collection("match_requests")
                .whereEqualTo("toUserId", uid).whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        MatchRequest req = value.getDocuments().get(0).toObject(MatchRequest.class);
                        if (req != null) { req.setRequestId(value.getDocuments().get(0).getId()); incomingRequest.setValue(req); }
                    } else { incomingRequest.setValue(null); }
                });
    }

    public void approveMatch(MatchRequest req) { repository.approveMatchByPartner(req.getRequestId(), repository.getCurrentUserId()); }
    public void rejectMatch(MatchRequest req) { repository.rejectAndDeleteRequest(req.getRequestId()); }

    public void cancelCurrentMove() {
        MoveRequest move = currentMove.getValue();
        // רק אם יש ID (הובלה אמיתית) מבטלים
        if (move != null && move.getId() != null) {
            repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
        }
    }

    public void markMoveAsCompleted() {
        MoveRequest move = currentMove.getValue();
        if (move != null && move.getId() != null) repository.completeMove(move.getId());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (moveListener != null) moveListener.remove();
        if (requestListener != null) requestListener.remove();
    }

    public void cancelMoveWithPolicy() {
        MoveRequest move = currentMove.getValue();
        if (move == null || move.getId() == null) return; // הגנה

        String myId = repository.getCurrentUserId();
        if (myId == null) return;

        boolean iAmPartner = myId.equals(move.getPartnerId());
        boolean iAmCustomer = myId.equals(move.getCustomerId());

        if (iAmPartner) {
            repository.cancelPartnerParticipation(move.getId());
            return;
        }

        if (!iAmCustomer) return;

        long now = System.currentTimeMillis();
        long weekMs = 7L * 24L * 60L * 60L * 1000L;
        long moveDate = move.getMoveDate();

        if (moveDate == 0 || (moveDate - now) >= weekMs) {
            repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
        } else {
            repository.requestCancelMoveByCustomer(move.getId(), move.getCustomerId());
        }
    }
}