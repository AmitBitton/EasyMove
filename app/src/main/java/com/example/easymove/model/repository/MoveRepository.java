package com.example.easymove.model.repository;

import com.example.easymove.model.MoveRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class MoveRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final String COLLECTION = "moves";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * מביא את ההובלה ה"פעילה" של הלקוח.
     * הנחה: ללקוח יש רק הובלה אחת פעילה בסטטוס OPEN בו זמנית.
     */
    public Task<MoveRequest> getCurrentActiveMove() {
        String uid = getCurrentUserId();
        if (uid == null) return Tasks.forException(new Exception("No user logged in"));

        return db.collection(COLLECTION)
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "OPEN") // רק הובלות פתוחות
                .limit(1) // לוקחים את הראשונה
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        MoveRequest move = snapshot.getDocuments().get(0).toObject(MoveRequest.class);
                        if (move != null) move.setId(snapshot.getDocuments().get(0).getId());
                        return move;
                    }
                    return null; // אין הובלה פעילה
                });
    }

    // ביטול הובלה
    public Task<Void> cancelMove(String moveId) {
        return db.collection(COLLECTION).document(moveId).update("status", "CANCELED");
    }
}