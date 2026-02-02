package com.example.easymove.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class MyMoveViewModel extends ViewModel {

    private final MoveRepository repository = new MoveRepository();
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

        // שיניתי מעט את השאילתה כדי שתהיה קלה יותר (ללא whereIn בהתחלה לבדיקה)
        // אם זה עובד עכשיו, סימן שהבעיה הייתה באינדקס של ה-whereIn
        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("partnerId", uid)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) {
                        Log.e("DEBUG_MOVE", "🔥 שגיאה קריטית בחיפוש שותף: " + error.getMessage());
                        errorMsg.setValue("שגיאת פיירבייס (בדוק לוגים): " + error.getMessage());
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // סינון ידני לסטטוס (כדי למנוע צורך באינדקס מורכב כרגע)
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String status = doc.getString("status");
                            if ("OPEN".equals(status) || "CONFIRMED".equals(status)) {
                                Log.d("DEBUG_MOVE", "🎉 בול! נמצאה הובלה כשותף! ID: " + doc.getId());
                                updateMoveData(doc);
                                return;
                            }
                        }
                        Log.d("DEBUG_MOVE", "נמצאו מסמכים כשותף, אבל הסטטוס לא מתאים.");
                        currentMove.setValue(null);
                    } else {
                        Log.d("DEBUG_MOVE", "לא נמצא שום מסמך שבו אני שותף.");
                        currentMove.setValue(null);
                    }
                });
    }

    private void updateMoveData(DocumentSnapshot doc) {
        MoveRequest move = doc.toObject(MoveRequest.class);
        if (move != null) {
            move.setId(doc.getId());
            currentMove.setValue(move);
        }
        isLoading.setValue(false);
    }

    // ... שאר הפונקציות (listenForMatchRequests, approveMatch וכו') ללא שינוי ...
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
        if (move != null) repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
    }

    public void markMoveAsCompleted() {
        MoveRequest move = currentMove.getValue();
        if (move != null) repository.completeMove(move.getId());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (moveListener != null) moveListener.remove();
        if (requestListener != null) requestListener.remove();
    }

    public void cancelMoveWithPolicy() {
        MoveRequest move = currentMove.getValue();
        if (move == null) return;

        String myId = repository.getCurrentUserId();
        if (myId == null) return;

        boolean iAmPartner = myId.equals(move.getPartnerId());
        boolean iAmCustomer = myId.equals(move.getCustomerId());

        // Partner cancellation: cancel partnership only (no mover approval)
        if (iAmPartner) {
            repository.cancelPartnerParticipation(move.getId());
            return;
        }

        if (!iAmCustomer) return; // Only the main customer can request/cancel the whole move

        long now = System.currentTimeMillis();
        long weekMs = 7L * 24L * 60L * 60L * 1000L;

        // If moveDate is unknown, treat as "can cancel immediately" (you can change this rule if you want)
        long moveDate = move.getMoveDate();

        if (moveDate == 0 || (moveDate - now) >= weekMs) {
            // Cancel immediately using existing logic
            repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId());
        } else {
            // Less than a week -> request mover approval
            repository.requestCancelMoveByCustomer(move.getId(), move.getCustomerId());
        }
    }

}