package com.example.easymove.model.repository;

import com.example.easymove.model.Chat;
import com.example.easymove.model.Message; // וודא שיש לך את המודל הזה
import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String CHATS_COLLECTION = "chats";

    // --- פונקציות חדשות לשימוש ה-ViewModel ---

    /**
     * מאזין להודעות בצ'אט ספציפי (בזמן אמת)
     */
    public ListenerRegistration listenToMessages(String chatId, EventListener<QuerySnapshot> listener) {
        return db.collection(CHATS_COLLECTION).document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    /**
     * מאזין לשינויים במסמך הראשי של הצ'אט (כותרת, סטטוס אישור הובלה וכו')
     */
    public ListenerRegistration listenToChatMetadata(String chatId, EventListener<DocumentSnapshot> listener) {
        return db.collection(CHATS_COLLECTION).document(chatId)
                .addSnapshotListener(listener);
    }

    /**
     * שולח הודעה חדשה ומעדכן את המסמך הראשי של הצ'אט (הודעה אחרונה) בטרנזקציה אחת (Batch)
     */
    public Task<Void> sendMessage(String chatId, Message message) {
        WriteBatch batch = db.batch();

        // 1. יצירת מסמך חדש להודעה בקולקשן messages
        DocumentReference msgRef = db.collection(CHATS_COLLECTION).document(chatId)
                .collection("messages").document();
        batch.set(msgRef, message);

        // 2. עדכון המסמך הראשי של הצ'אט (Last Message)
        DocumentReference chatRef = db.collection(CHATS_COLLECTION).document(chatId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getText());
        updates.put("lastUpdated", message.getTimestamp());
        updates.put("lastSenderId", message.getSenderId());

        batch.update(chatRef, updates);

        return batch.commit();
    }
    // ----------------------------------------------------

    public Task<String> getOrCreateChat(UserProfile me, UserProfile other) {
        String chatId = generateChatId(me.getUserId(), other.getUserId());
        return db.collection(CHATS_COLLECTION).document(chatId).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        return Tasks.forResult(chatId);
                    } else {
                        return createNewChat(chatId, me, other);
                    }
                });
    }

    private Task<String> createNewChat(String chatId, UserProfile me, UserProfile other) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUserIds(Arrays.asList(me.getUserId(), other.getUserId()));

        chat.setUser1Id(me.getUserId());
        chat.setUser1Name(me.getName());
        chat.setUser1Image(me.getProfileImageUrl());

        chat.setUser2Id(other.getUserId());
        chat.setUser2Name(other.getName());
        chat.setUser2Image(other.getProfileImageUrl());

        chat.setCustomerId(me.getUserId());
        chat.setMoverId(other.getUserId());
        chat.setMoverConfirmed(false);
        chat.setCustomerConfirmed(false);

        chat.setLastUpdated(new Timestamp(new Date()));
        chat.setLastMessage("צ'אט חדש נוצר");

        return db.collection(CHATS_COLLECTION).document(chatId)
                .set(chat)
                .continueWith(task -> chatId);
    }

    private String generateChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public Task<QuerySnapshot> getUserChats(String myUserId) {
        return db.collection(CHATS_COLLECTION)
                .whereArrayContains("userIds", myUserId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> setMoverConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("moverConfirmed", true);
        updates.put("moverConfirmedAt", System.currentTimeMillis());
        return db.collection(CHATS_COLLECTION).document(chatId).update(updates);
    }

    public Task<Void> setCustomerConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("customerConfirmed", true);
        updates.put("customerConfirmedAt", System.currentTimeMillis());
        return db.collection(CHATS_COLLECTION).document(chatId).update(updates);
    }
}