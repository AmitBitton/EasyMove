package com.example.easymove.model.repository;

import android.net.Uri;
import com.example.easymove.model.InventoryItem;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private final String COLLECTION_NAME = "inventory_items";

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    // הוספת פריט (כולל העלאת תמונה אם יש)
    public Task<DocumentReference> addInventoryItem(InventoryItem item, Uri imageUri) {
        if (imageUri != null) {
            String filename = UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child("inventory_images/" + filename);
            UploadTask uploadTask = ref.putFile(imageUri);

            // שרשור: קודם תמונה -> אז שמירה ב-DB
            return uploadTask.continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            }).continueWithTask(task -> {
                if (task.isSuccessful()) {
                    item.setImageUrl(task.getResult().toString());
                }
                return db.collection(COLLECTION_NAME).add(item);
            });
        } else {
            return db.collection(COLLECTION_NAME).add(item);
        }
    }

    // שליפת הפריטים של המשתמש
    public Task<List<InventoryItem>> getMyInventory() {
        String uid = getCurrentUserId();
        if (uid == null) return null;

        return db.collection(COLLECTION_NAME)
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    List<InventoryItem> items = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            InventoryItem item = doc.toObject(InventoryItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                items.add(item);
                            }
                        }
                    }
                    return items;
                });
    }

    // מחיקת פריט
    public Task<Void> deleteInventoryItem(String itemId) {
        return db.collection(COLLECTION_NAME).document(itemId).delete();
    }
}