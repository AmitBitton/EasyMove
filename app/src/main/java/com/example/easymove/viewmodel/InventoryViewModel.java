package com.example.easymove.viewmodel;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.InventoryItem;
import com.example.easymove.model.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.List;

public class InventoryViewModel extends ViewModel {

    private final InventoryRepository repository;

    // רשימת הפריטים לתצוגה (המסך מאזין לזה)
    private final MutableLiveData<List<InventoryItem>> inventoryList = new MutableLiveData<>(new ArrayList<>());

    // הודעות למשתמש (שגיאות/הצלחות)
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    // דגל להצלחת הוספה (כדי לסגור את החלון הקופץ)
    private final MutableLiveData<Boolean> addSuccess = new MutableLiveData<>();

    // בנאי שמקבל את הריפוזיטורי (הזרקת תלויות)
    public InventoryViewModel(InventoryRepository repository) {
        this.repository = repository;
    }

    // Getters ל-LiveData
    public LiveData<List<InventoryItem>> getInventoryList() { return inventoryList; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getAddSuccess() { return addSuccess; }

    // טעינת הפריטים
    public void loadMyInventory() {
        if (repository.getCurrentUserId() == null) return;

        repository.getMyInventory().addOnSuccessListener(items -> {
            inventoryList.setValue(items);
        }).addOnFailureListener(e -> {
            toastMessage.setValue("שגיאה בטעינת המלאי: " + e.getMessage());
        });
    }

    // הוספת פריט חדש
    public void addItem(String name, String description, String roomType, boolean isFragile, int quantity, Uri imageUri) {
        if (name.isEmpty() || quantity <= 0) {
            toastMessage.setValue("נא למלא שם וכמות תקינה");
            return;
        }

        InventoryItem item = new InventoryItem(
                repository.getCurrentUserId(),
                name, description, roomType, isFragile, quantity, null
        );

        repository.addInventoryItem(item, imageUri)
                .addOnSuccessListener(ref -> {
                    toastMessage.setValue("הפריט נוסף בהצלחה!");
                    addSuccess.setValue(true);
                    loadMyInventory(); // רענון הרשימה מיד אחרי ההוספה
                })
                .addOnFailureListener(e -> {
                    toastMessage.setValue("שגיאה בהוספה: " + e.getMessage());
                    addSuccess.setValue(false);
                });
    }

    // מחיקת פריט
    public void deleteItem(InventoryItem item) {
        repository.deleteInventoryItem(item.getId())
                .addOnSuccessListener(v -> {
                    toastMessage.setValue("הפריט נמחק");
                    loadMyInventory(); // רענון הרשימה
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה"));
    }

    public void resetAddSuccess() {
        addSuccess.setValue(false);
    }
}