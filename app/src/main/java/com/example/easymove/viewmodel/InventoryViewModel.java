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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewModel responsible for managing the User's Inventory.
 * Features:
 * 1. Real-time Firestore synchronization.
 * 2. Local Filtering (By Room, Fragility).
 * 3. Local Sorting (Date, Quantity, A-Z, Fragile First).
 * 4. CRUD Operations (Add, Delete).
 */
public class InventoryViewModel extends ViewModel {

    private final InventoryRepository repository;

    // --- Data Sources ---
    // The "Source of Truth" - raw list directly from Firestore (Unfiltered)
    private List<InventoryItem> masterList = new ArrayList<>();

    // The "View State" - processed list sent to the UI (Filtered & Sorted)
    private final MutableLiveData<List<InventoryItem>> displayList = new MutableLiveData<>(new ArrayList<>());

    // --- UI Events ---
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addSuccess = new MutableLiveData<>();

    private ListenerRegistration inventoryRegistration;

    // --- Filter & Sort State ---
    public enum SortOption {
        DATE_NEWEST,    // Default
        QUANTITY_DESC,
        ROOM_AZ,
        FRAGILE_FIRST
    }

    private SortOption currentSort = SortOption.DATE_NEWEST;
    private String filterRoom = null; // null or "הכל" = All rooms
    private boolean filterFragileOnly = false;

    // Constructor with Dependency Injection
    public InventoryViewModel(InventoryRepository repository) {
        this.repository = repository;
    }

    // --- Getters ---
    public LiveData<List<InventoryItem>> getInventoryList() { return displayList; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<Boolean> getAddSuccess() { return addSuccess; }

    // =================================================================
    //  Core Logic: Processing (Filter -> Sort -> Emit)
    // =================================================================

    /**
     * The main processing method.
     * It takes the `masterList`, applies the current filters, sorts the result,
     * and updates the `displayList` LiveData.
     */
    private void processData() {
        if (masterList == null) return;

        List<InventoryItem> result = new ArrayList<>();

        // 1. Filtering
        for (InventoryItem item : masterList) {
            // Filter by Room
            if (filterRoom != null && !filterRoom.equals("הכל") && !filterRoom.equals(item.getRoomType())) {
                continue;
            }
            // Filter by Fragility
            if (filterFragileOnly && !item.isFragile()) {
                continue;
            }
            result.add(item);
        }

        // 2. Sorting
        switch (currentSort) {
            case QUANTITY_DESC:
                result.sort((o1, o2) -> Integer.compare(o2.getQuantity(), o1.getQuantity()));
                break;
            case ROOM_AZ:
                result.sort((o1, o2) -> {
                    String r1 = o1.getRoomType() != null ? o1.getRoomType() : "";
                    String r2 = o2.getRoomType() != null ? o2.getRoomType() : "";
                    return r1.compareTo(r2);
                });
                break;
            case FRAGILE_FIRST:
                // Boolean.compare(true, false) -> 1. We want True first, so we compare (o2, o1).
                result.sort((o1, o2) -> Boolean.compare(o2.isFragile(), o1.isFragile()));
                break;
            case DATE_NEWEST:
            default:
                result.sort((o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                break;
        }

        // 3. Update UI
        displayList.postValue(result);
    }

    // =================================================================
    //  UI Actions: Sorting & Filtering
    // =================================================================

    public void setSortOption(SortOption option) {
        this.currentSort = option;
        processData(); // Re-process with new sort
    }

    public void setFilters(String room, boolean fragileOnly) {
        this.filterRoom = room;
        this.filterFragileOnly = fragileOnly;
        processData(); // Re-process with new filters
    }

    /**
     * Generates a list of unique room names currently existing in the inventory.
     * Used to populate the Filter Spinner.
     */
    public List<String> getUniqueRooms() {
        Set<String> rooms = new HashSet<>();
        for (InventoryItem item : masterList) {
            if (item.getRoomType() != null && !item.getRoomType().isEmpty()) {
                rooms.add(item.getRoomType());
            }
        }
        List<String> sortedRooms = new ArrayList<>(rooms);
        Collections.sort(sortedRooms);
        sortedRooms.add(0, "הכל"); // Add "All" option at the top
        return sortedRooms;
    }

    // =================================================================
    //  Data Fetching (Real-time)
    // =================================================================

    public void startInventoryListener() {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("אין משתמש מחובר");
            return;
        }

        // Prevent duplicate listeners
        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
        }

        inventoryRegistration = repository.listenToMyInventory(new InventoryRepository.InventoryListener() {
            @Override
            public void onChanged(List<InventoryItem> items) {
                // Update the Source of Truth
                masterList = new ArrayList<>(items);
                // Trigger processing (Filter/Sort)
                processData();
            }

            @Override
            public void onError(Exception e) {
                toastMessage.postValue("שגיאה בטעינת נתונים: " + e.getMessage());
            }
        });
    }

    public void stopInventoryListener() {
        if (inventoryRegistration != null) {
            inventoryRegistration.remove();
            inventoryRegistration = null;
        }
    }

    // =================================================================
    //  CRUD Operations
    // =================================================================

    public void addItem(String name, String description, String roomType, boolean isFragile, int quantity, Uri imageUri) {
        String uid = repository.getCurrentUserId();
        if (uid == null) {
            toastMessage.setValue("שגיאת משתמש");
            return;
        }

        InventoryItem item = new InventoryItem(uid, name, description, roomType, isFragile, quantity, null);

        repository.addInventoryItem(item, imageUri)
                .addOnSuccessListener(ref -> {
                    toastMessage.setValue("הפריט נוסף בהצלחה");
                    addSuccess.setValue(true);
                    // No need to call loadMyInventory(), the real-time listener will catch the change automatically
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    public void deleteItem(InventoryItem item) {
        repository.deleteInventoryItem(item.getId())
                .addOnSuccessListener(v -> toastMessage.setValue("הפריט נמחק"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה במחיקה"));
    }

    public void resetAddSuccess() {
        addSuccess.setValue(false);
    }

    /**
     * Automatically cleans up listeners when the ViewModel is destroyed.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        stopInventoryListener();
    }
}