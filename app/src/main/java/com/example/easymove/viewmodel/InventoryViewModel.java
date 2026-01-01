package com.example.easymove.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.InventoryItem;
import com.example.easymove.model.repository.InventoryRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class InventoryViewModel extends ViewModel {

    private final InventoryRepository repository;

    private final MutableLiveData<List<InventoryItem>> inventoryList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccess = new MutableLiveData<>();

    // ✅ שומר את ה-listener כדי שנוכל לעצור אותו
    private ListenerRegistration inventoryRegistration;

    public InventoryViewModel(InventoryRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<InventoryItem>> getInventoryList() { return inventoryList; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getAddSuccess() { return addSuccess; }

    // ✅ טעינה חד פעמית (וגם משמש "גיבוי" אחרי הוספה/מחיקה)
    public void loadMyInventory() {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("אין משתמש מחובר");
            inventoryList.setValue(new ArrayList<>());
            return;
        }

        repository.getMyInventory()
                .addOnSuccessListener(inventoryList::setValue)
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בטעינת המלאי: " + e.getMessage()));
    }

    // ✅ Listener בזמן אמת
    public void startInventoryListener() {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("אין משתמש מחובר");
            return;
        }

        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
            inventoryRegistration = null;
        }

        inventoryRegistration = repository.listenToMyInventory(new InventoryRepository.InventoryListener() {
            @Override
            public void onChanged(List<InventoryItem> items) {
                inventoryList.postValue(items);
            }

            @Override
            public void onError(Exception e) {
                toastMessage.postValue("Listener נכשל: " + e.getMessage());
            }
        });

        // ✅ טיפ קטן: רענון ראשוני כדי שלא נחכה לאירוע הראשון של ה-listener
        loadMyInventory();
    }

    public void stopInventoryListener() {
        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
            inventoryRegistration = null;
        }
    }

    public void addItem(String name, String description, String roomType, boolean isFragile, int quantity, Uri imageUri) {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("אין משתמש מחובר");
            addSuccess.setValue(false);
            return;
        }

        if (name == null || name.trim().isEmpty() || quantity <= 0) {
            toastMessage.setValue("נא למלא שם וכמות תקינה");
            addSuccess.setValue(false);
            return;
        }

        InventoryItem item = new InventoryItem(
                uid,
                name.trim(),
                description,
                roomType,
                isFragile,
                quantity,
                null
        );

        repository.addInventoryItem(item, imageUri)
                .addOnSuccessListener(ref -> {
                    toastMessage.setValue("הפריט נוסף בהצלחה!");
                    addSuccess.setValue(true);

                    // ✅ גיבוי חשוב: גם אם listener לא תפס, אנחנו מרעננים ידנית
                    loadMyInventory();
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בהוספה: " + e.getMessage());
                    addSuccess.setValue(false);
                });
    }

    public void deleteItem(InventoryItem item) {
        if (item == null || item.getId() == null || item.getId().isEmpty()) {
            toastMessage.setValue("שגיאה: אין מזהה לפריט למחיקה");
            return;
        }

        repository.deleteInventoryItem(item.getId())
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הפריט נמחק");

                    // ✅ גיבוי חשוב: גם אם listener לא תפס, אנחנו מרעננים ידנית
                    loadMyInventory();
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה: " + e.getMessage()));
    }

    public void resetAddSuccess() {
        addSuccess.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopInventoryListener();
    }
}
