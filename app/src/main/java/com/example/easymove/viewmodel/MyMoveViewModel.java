package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;

public class MyMoveViewModel extends ViewModel {

    private final MoveRepository repository = new MoveRepository();

    private final MutableLiveData<MoveRequest> currentMove = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMsg = new MutableLiveData<>();

    public LiveData<MoveRequest> getCurrentMove() {
        return currentMove;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMsg() {
        return errorMsg;
    }

    public void loadCurrentMove() {
        isLoading.setValue(true);

        String uid = repository.getCurrentUserId();
        if (uid == null) {
            errorMsg.setValue("אין משתמש מחובר");
            isLoading.setValue(false);
            currentMove.setValue(null);
            return;
        }

        repository.ensureActiveMoveForCustomer(uid)
                .addOnSuccessListener(move -> {
                    currentMove.setValue(move);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMsg.setValue("שגיאה בטעינת הובלה: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void cancelCurrentMove() {
        MoveRequest move = currentMove.getValue();
        if (move == null) return;

        isLoading.setValue(true);
        repository.cancelMove(move.getId())
                .addOnSuccessListener(v -> loadCurrentMove())
                .addOnFailureListener(e -> {
                    errorMsg.setValue("ביטול נכשל");
                    isLoading.setValue(false);
                });
    }

    // --- הפונקציה החדשה לסגירת הובלה ---
    public void markMoveAsCompleted() {
        MoveRequest move = currentMove.getValue();
        if (move != null) {
            isLoading.setValue(true);
            repository.completeMove(move.getId())
                    .addOnSuccessListener(v -> loadCurrentMove()) // טוען מחדש (וייצור חדשה)
                    .addOnFailureListener(e -> {
                        errorMsg.setValue("שגיאה בעדכון הסטטוס");
                        isLoading.setValue(false);
                    });
        }
    }
}
