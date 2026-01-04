package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.MoveRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class MyDeliveriesViewModel extends ViewModel {

    // ✅ יצירת מופע של ה-Repository
    private final MoveRepository moveRepository = new MoveRepository();

    private final MutableLiveData<List<MoveRequest>> deliveries = new MutableLiveData<>();

    // מחזיק את הרישום להאזנה כדי שנוכל לנתק אותו בסוף
    private ListenerRegistration listenerRegistration;

    public LiveData<List<MoveRequest>> getDeliveries() {
        return deliveries;
    }

    public void loadMyDeliveries() {
        // מבקשים מהריפו את ה-ID של המשתמש הנוכחי
        String currentUserId = moveRepository.getCurrentUserId();

        if (currentUserId == null) {
            // אם אין משתמש מחובר, מנקים את הרשימה ויוצאים
            deliveries.setValue(null);
            return;
        }

        // אם כבר יש האזנה פעילה - מנתקים אותה לפני שיוצרים חדשה
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        // ✅ שימוש בפונקציה החדשה שיצרנו ב-MoveRepository
        listenerRegistration = moveRepository.listenToMoverConfirmedMoves(currentUserId, (value, error) -> {
            if (error != null) {
                // במקרה של שגיאה (למשל בעיית אינטרנט או הרשאות)
                // אפשר להוסיף כאן לוג או LiveData של שגיאות
                return;
            }

            if (value != null) {
                // המרת המסמכים לרשימת אובייקטים של MoveRequest
                List<MoveRequest> list = value.toObjects(MoveRequest.class);
                deliveries.setValue(list);
            }
        });
    }

    // הפונקציה הזו נקראת אוטומטית כשהמסך (Fragment/Activity) נסגר סופית
    @Override
    protected void onCleared() {
        super.onCleared();
        // חשוב מאוד: מנתקים את ההאזנה כדי לחסוך סוללה ואינטרנט
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}