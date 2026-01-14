package com.example.easymove.viewmodel;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.easymove.model.InventoryItem;
import com.example.easymove.model.repository.InventoryRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InventoryViewModel extends ViewModel {

    private final InventoryRepository repository;

    // הרשימה המקורית המלאה מה-DB (ללא סינונים)
    private List<InventoryItem> masterList = new ArrayList<>();

    // הרשימה המוצגת (אחרי סינון ומיון) שנשלחת ל-UI
    private final MutableLiveData<List<InventoryItem>> displayList = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccess = new MutableLiveData<>();

    private ListenerRegistration inventoryRegistration;

    // --- משתני מצב לסינון ומיון ---
    public enum SortOption {
        DATE_NEWEST,    // תאריך (ברירת מחדל)
        QUANTITY_DESC,  // כמות (מהגדול לקטן)
        ROOM_AZ,        // חדר (א-ת)
        FRAGILE_FIRST   // שביר קודם
    }

    private SortOption currentSort = SortOption.DATE_NEWEST;
    private String filterRoom = null; // null = כל החדרים
    private boolean filterFragileOnly = false; // false = הכל

    public InventoryViewModel(InventoryRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<InventoryItem>> getInventoryList() { return displayList; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getAddSuccess() { return addSuccess; }

    // --- לוגיקת עיבוד נתונים ---

    /**
     * הפונקציה הראשית: לוקחת את ה-Master List, מסננת, ממיינת ומעדכנת את ה-LiveData
     */
    private void processData() {
        if (masterList == null) return;

        List<InventoryItem> result = new ArrayList<>();

        // 1. סינון (Filtering)
        for (InventoryItem item : masterList) {
            // סינון לפי חדר (אם נבחר חדר ספציפי)
            if (filterRoom != null && !filterRoom.equals("הכל") && !filterRoom.equals(item.getRoomType())) {
                continue;
            }
            // סינון לפי שביר (אם המשתמש ביקש רק שביר)
            if (filterFragileOnly && !item.isFragile()) {
                continue;
            }
            result.add(item);
        }

        // 2. מיון (Sorting)
        switch (currentSort) {
            case QUANTITY_DESC:
                Collections.sort(result, (o1, o2) -> Integer.compare(o2.getQuantity(), o1.getQuantity()));
                break;
            case ROOM_AZ:
                Collections.sort(result, (o1, o2) -> o1.getRoomType().compareTo(o2.getRoomType()));
                break;
            case FRAGILE_FIRST:
                // Boolean.compare מחזיר true בסוף, אז עושים הפוך כדי ש-true יהיה ראשון
                Collections.sort(result, (o1, o2) -> Boolean.compare(o2.isFragile(), o1.isFragile()));
                break;
            case DATE_NEWEST:
            default:
                Collections.sort(result, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                break;
        }

        // 3. עדכון ה-UI
        displayList.postValue(result);
    }

    // --- פונקציות UI לשינוי מצבי סינון/מיון ---

    public void setSortOption(SortOption option) {
        this.currentSort = option;
        processData();
    }

    public void setFilters(String room, boolean fragileOnly) {
        this.filterRoom = room;
        this.filterFragileOnly = fragileOnly;
        processData();
    }

    // פונקציית עזר לדיאלוג הסינון - מחזירה רשימה של כל החדרים הקיימים כרגע במלאי
    public List<String> getUniqueRooms() {
        Set<String> rooms = new HashSet<>();
        for (InventoryItem item : masterList) {
            if (item.getRoomType() != null && !item.getRoomType().isEmpty()) {
                rooms.add(item.getRoomType());
            }
        }
        List<String> sortedRooms = new ArrayList<>(rooms);
        Collections.sort(sortedRooms);
        sortedRooms.add(0, "הכל"); // הוספת אפשרות לביטול סינון
        return sortedRooms;
    }

    // --- Firestore Listeners ---

    public void startInventoryListener() {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("אין משתמש מחובר");
            return;
        }

        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
        }

        inventoryRegistration = repository.listenToMyInventory(new InventoryRepository.InventoryListener() {
            @Override
            public void onChanged(List<InventoryItem> items) {
                // עדכון ה-Master List בלבד
                masterList = new ArrayList<>(items);
                // הפעלת לוגיקת העיבוד
                processData();
            }

            @Override
            public void onError(Exception e) {
                toastMessage.postValue("Listener נכשל: " + e.getMessage());
            }
        });
    }

    public void loadMyInventory() {
        // ... (השארתי ללא שינוי מהותי, רק צריך לעדכן את masterList)
        String uid = repository.getCurrentUserId();
        if (uid == null) return;
        repository.getMyInventory().addOnSuccessListener(items -> {
            masterList = new ArrayList<>(items);
            processData();
        });
    }

    // --- הוספה ומחיקה (ללא שינוי, רק מוודאים ש-loadMyInventory נקרא בסוף) ---
    public void addItem(String name, String description, String roomType, boolean isFragile, int quantity, Uri imageUri) {
        // ... (אותו קוד כמו מקודם) ...
        // בתוך ההצלחה:
        // loadMyInventory(); -> זה יפעיל מחדש את processData
        // אני מעתיק את הקוד המלא כדי שיהיה לך קל:
        String uid = repository.getCurrentUserId();
        if (uid == null) return;

        InventoryItem item = new InventoryItem(uid, name, description, roomType, isFragile, quantity, null);
        repository.addInventoryItem(item, imageUri).addOnSuccessListener(ref -> {
            toastMessage.setValue("נוסף בהצלחה");
            addSuccess.setValue(true);
        }).addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    public void deleteItem(InventoryItem item) {
        repository.deleteInventoryItem(item.getId())
                .addOnSuccessListener(v -> toastMessage.setValue("נמחק"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה"));
    }

    public void stopInventoryListener() {
        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
            inventoryRegistration = null;
        }
    }

    public void resetAddSuccess() {
        addSuccess.setValue(false);
    }
}