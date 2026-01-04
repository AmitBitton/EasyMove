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

    public LiveData<MoveRequest> getCurrentMove() { return currentMove; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMsg() { return errorMsg; }

    public void loadCurrentMove() {
        isLoading.setValue(true);
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            isLoading.setValue(false);
            return;
        }

        repository.ensureActiveMoveForCustomer(uid)
                .addOnSuccessListener(move -> {
                    currentMove.setValue(move);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMsg.setValue(e.getMessage());
                    isLoading.setValue(false);
                });
    }

    /**
     * ✅ ביטול הובלה משופר:
     * מבטל את ההובלה + מאפס את האישורים בצ'אט
     */
    public void cancelCurrentMove() {
        MoveRequest move = currentMove.getValue();
        if (move == null) return;

        isLoading.setValue(true);

        // ✅ מעבירים גם את move.getCustomerId()
        repository.cancelMoveAndResetChat(move.getId(), move.getChatId(), move.getCustomerId())
                .addOnSuccessListener(v -> {
                    loadCurrentMove(); // טוענים מחדש (יהיה ריק)
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
                    .addOnSuccessListener(v -> loadCurrentMove())
                    .addOnFailureListener(e -> {
                        errorMsg.setValue("שגיאה בעדכון");
                        isLoading.setValue(false);
                    });
        }
    }
}