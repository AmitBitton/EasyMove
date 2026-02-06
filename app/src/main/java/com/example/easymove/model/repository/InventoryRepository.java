package com.example.easymove.model.repository;

import android.net.Uri;

import com.example.easymove.model.InventoryItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Repository class responsible for managing Inventory Items in Firestore and Storage.
 * Handles adding items (with or without images), fetching lists, listening to real-time updates,
 * and deleting items.
 */
public class InventoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private static final String COLLECTION_NAME = "inventory_items";
    private static final String STORAGE_PATH = "inventory_images/";

    /**
     * @return The current user's UID or null if not logged in.
     */
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // ------------------------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------------------------

    /**
     * Creates a base query for the current user's inventory.
     * Note: We avoid 'orderBy' here to prevent crashes due to missing Firestore indexes.
     * Sorting can be done client-side if needed.
     *
     * @return A Query object filtered by ownerId, or null if no user is logged in.
     */
    private Query myInventoryQuery() {
        String uid = getCurrentUserId();
        if (uid == null) return null;
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("ownerId", uid);
    }

    // ------------------------------------------------------------------------
    // CRUD Operations
    // ------------------------------------------------------------------------

    /**
     * Adds a new inventory item.
     * If an image URI is provided, it uploads the image to Firebase Storage first,
     * retrieves the download URL, sets it on the item, and then saves to Firestore.
     *
     * @param item     The InventoryItem to save.
     * @param imageUri Optional URI of the image to upload. Can be null.
     * @return A Task representing the final Firestore document creation.
     */
    public Task<DocumentReference> addInventoryItem(InventoryItem item, Uri imageUri) {
        if (imageUri != null) {
            // 1. Generate unique filename
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child(STORAGE_PATH + filename);
            UploadTask uploadTask = ref.putFile(imageUri);

            // 2. Chain tasks: Upload -> Get URL -> Save to DB
            return uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return ref.getDownloadUrl();
            }).continueWithTask(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    item.setImageUrl(task.getResult().toString());
                }
                return db.collection(COLLECTION_NAME).add(item);
            });
        } else {
            // No image, save directly
            return db.collection(COLLECTION_NAME).add(item);
        }
    }

    /**
     * Deletes an item from the inventory by its ID.
     * Note: This does not delete the associated image from Storage (cleanup logic could be added here).
     *
     * @param itemId The document ID to delete.
     * @return A Task representing the delete operation.
     */
    public Task<Void> deleteInventoryItem(String itemId) {
        return db.collection(COLLECTION_NAME).document(itemId).delete();
    }

    // ------------------------------------------------------------------------
    // Data Fetching & Listening
    // ------------------------------------------------------------------------

    /**
     * Fetches the user's inventory once (Single fetch).
     *
     * @return A Task containing the list of InventoryItems.
     */
    public Task<List<InventoryItem>> getMyInventory() {
        Query q = myInventoryQuery();
        if (q == null) return null;

        return q.get().continueWith(task -> {
            List<InventoryItem> items = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult()) {
                    InventoryItem item = doc.toObject(InventoryItem.class);
                    if (item != null) {
                        // Manually set ID because it's not part of the document body
                        item.setId(doc.getId());
                        items.add(item);
                    }
                }
            }
            return items;
        });
    }

    /**
     * Listens for real-time changes to the user's inventory.
     *
     * @param listener A callback interface to handle updates or errors.
     * @return A ListenerRegistration object (used to remove the listener later).
     */
    public ListenerRegistration listenToMyInventory(InventoryListener listener) {
        Query q = myInventoryQuery();
        if (q == null) return null;

        return q.addSnapshotListener((snap, e) -> {
            if (e != null) {
                if (listener != null) listener.onError(e);
                return;
            }

            List<InventoryItem> items = new ArrayList<>();
            if (snap != null) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    InventoryItem item = doc.toObject(InventoryItem.class);
                    if (item != null) {
                        item.setId(doc.getId());
                        items.add(item);
                    }
                }
            }
            if (listener != null) listener.onChanged(items);
        });
    }

    /**
     * Interface for real-time inventory updates.
     */
    public interface InventoryListener {
        void onChanged(List<InventoryItem> items);
        void onError(Exception e);
    }
}