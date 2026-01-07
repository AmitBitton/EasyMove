package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.DocumentSnapshot; // חשוב
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class MyMoveViewModel extends ViewModel {

    private final MoveRepository repository = new MoveRepository();
    private final MutableLiveData<MoveRequest> currentMove = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();

    // שומר את ההאזנה כדי לנתק ביציאה
    private ListenerRegistration moveListener;

    public LiveData<MoveRequest> getCurrentMove() { return currentMove; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }

    public void loadCurrentMove() {
        String uid = repository.getCurrentUserId();
        if (uid == null) return;

        // מנקים האזנה קודמת אם קיימת
        if (moveListener != null) moveListener.remove();

        isLoading.setValue(true);

        // שלב 1: מנסים להביא הובלה מאושרת (CONFIRMED) בלבד
        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "CONFIRMED")
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        errorMsg.setValue("שגיאה: " + error.getMessage());
                        isLoading.setValue(false);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        // בול! מצאנו הובלה מאושרת. מציגים אותה.
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        MoveRequest move = doc.toObject(MoveRequest.class);
                        if (move != null) {
                            move.setId(doc.getId());
                            currentMove.setValue(move);
                        }
                        isLoading.setValue(false);
                    } else {
                        // אין הובלה מאושרת. בוא נחפש טיוטה פתוחה (OPEN)
                        // קוראים לפונקציה הנוספת (שקודם הייתה חסרה לך)
                        loadOpenDraft(uid);
                    }
                });
    }

    // ✅ זו הפונקציה שהייתה חסרה לך!
    private void loadOpenDraft(String uid) {
        // מסירים את המאזין הקודם כדי למנוע כפילויות
        if (moveListener != null) moveListener.remove();

        moveListener = FirebaseFirestore.getInstance().collection("moves")
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "OPEN")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    isLoading.setValue(false);

                    if (error != null) return; // מתעלמים משגיאות בשלב הזה

                    if (value != null && !value.isEmpty()) {
                        // מצאנו טיוטה פתוחה
                        DocumentSnapshot doc = value.getDocuments().get(0);
                        MoveRequest move = doc.toObject(MoveRequest.class);
                        if (move != null) {
                            move.setId(doc.getId());
                            currentMove.setValue(move);
                        }
                    } else {
                        // אין שום הובלה (לא מאושרת ולא פתוחה)
                        currentMove.setValue(null);
                    }
                });
    }

    public void cancelCurrentMove() {
        MoveRequest move = currentMove.getValue();
        if (move == null) return;

        isLoading.setValue(true);

        repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId())
                .addOnSuccessListener(v -> {
                    // ה-Listener כבר יתעדכן לבד ל-null או לטיוטה אחרת
                })
                .addOnFailureListener(e -> {
                    errorMsg.setValue("ביטול נכשל");
                    isLoading.setValue(false);
                });
    }

    public void markMoveAsCompleted() {
        MoveRequest move = currentMove.getValue();
        if (move != null) {
            isLoading.setValue(true);
            repository.completeMove(move.getId())
                    .addOnSuccessListener(v -> {
                        // ה-Listener יתעדכן לבד
                    })
                    .addOnFailureListener(e -> {
                        errorMsg.setValue("שגיאה בעדכון");
                        isLoading.setValue(false);
                    });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (moveListener != null) {
            moveListener.remove();
        }
    }
}