package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class MyDeliveriesViewModel extends ViewModel {

    private final MoveRepository moveRepository;
    private final MutableLiveData<List<MoveRequest>> deliveries = new MutableLiveData<>();

    public MyDeliveriesViewModel() {
        moveRepository = new MoveRepository();
    }

    public LiveData<List<MoveRequest>> getDeliveries() {
        return deliveries;
    }

    public void loadMyDeliveries() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            deliveries.setValue(java.util.Collections.emptyList());
            return;
        }

        moveRepository.getMoverConfirmedMoves(currentUserId)
                .addOnSuccessListener(list -> {
                    if (list == null) list = java.util.Collections.emptyList();
                    deliveries.setValue(list);
                })
                .addOnFailureListener(e -> {
                    // כדי שתראי מה הבעיה ב-Logcat
                    android.util.Log.e("MyDeliveriesVM", "loadMyDeliveries failed", e);

                    // כדי שהמסך לא יישאר "תקוע" ריק
                    deliveries.setValue(java.util.Collections.emptyList());
                });
    }

}