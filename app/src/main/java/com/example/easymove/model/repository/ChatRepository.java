package com.example.easymove.model.repository;

import com.example.easymove.model.Chat;
import com.example.easymove.model.Message;
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
import java.util.Objects;

/**
 * Repository class responsible for handling all Chat-related operations in Firestore.
 * This includes sending messages, listening for real-time updates, creating new chat rooms,
 * and managing chat metadata (confirmations, last seen, etc.).
 */
public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_CHATS = "chats";
    private static final String SUB_COLLECTION_MESSAGES = "messages";

    // ------------------------------------------------------------------------
    // Real-Time Listeners (For ViewModel)
    // ------------------------------------------------------------------------

    /**
     * Listens for new messages in a specific chat room in real-time.
     *
     * @param chatId   The ID of the chat room.
     * @param listener The EventListener to handle the incoming QuerySnapshot.
     * @return A ListenerRegistration object to remove the listener when needed.
     */
    public ListenerRegistration listenToMessages(String chatId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(SUB_COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    /**
     * Listens for changes in the main chat document (Metadata).
     * Useful for updating the "Last Message", "Typing Status", or "Move Confirmation" in real-time.
     *
     * @param chatId   The ID of the chat room.
     * @param listener The EventListener to handle the DocumentSnapshot.
     * @return A ListenerRegistration object.
     */
    public ListenerRegistration listenToChatMetadata(String chatId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_CHATS)
                .document(chatId)
                .addSnapshotListener(listener);
    }

    // ------------------------------------------------------------------------
    // Chat Actions
    // ------------------------------------------------------------------------

    /**
     * Sends a new message and updates the main chat document (lastMessage field)
     * atomically using a WriteBatch.
     *
     * @param chatId  The ID of the chat room.
     * @param message The Message object to send.
     */
    public void sendMessage(String chatId, Message message) {
        WriteBatch batch = db.batch();

        // 1. Create a new document in the 'messages' sub-collection
        DocumentReference msgRef = db.collection(COLLECTION_CHATS)
                .document(chatId)
                .collection(SUB_COLLECTION_MESSAGES)
                .document();
        batch.set(msgRef, message);

        // 2. Update the main chat document with metadata (for the chat list view)
        DocumentReference chatRef = db.collection(COLLECTION_CHATS).document(chatId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message.getText());
        updates.put("lastUpdated", message.getTimestamp());
        updates.put("lastSenderId", message.getSenderId());

        batch.update(chatRef, updates);

        batch.commit();
    }

    /**
     * Updates the 'lastSeen' timestamp for a specific user in the chat.
     * Uses Dot Notation ("lastSeen.userId") to update a specific key in the Firestore Map.
     *
     * @param chatId The chat ID.
     * @param userId The user ID whose last seen status is being updated.
     */
    public void updateLastSeen(String chatId, String userId) {
        String fieldPath = "lastSeen." + userId;
        db.collection(COLLECTION_CHATS)
                .document(chatId)
                .update(fieldPath, new Timestamp(new Date()));
    }

    // ------------------------------------------------------------------------
    // Chat Management & Creation
    // ------------------------------------------------------------------------

    /**
     * Retrieves an existing chat ID or creates a new chat room if one doesn't exist.
     * The Chat ID is deterministic based on the two user IDs (user1_user2).
     *
     * @param me    The current user's profile.
     * @param other The other participant's profile.
     * @return A Task containing the Chat ID.
     */
    public Task<String> getOrCreateChat(UserProfile me, UserProfile other) {
        String chatId = generateChatId(me.getUserId(), other.getUserId());

        return db.collection(COLLECTION_CHATS).document(chatId).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        return Tasks.forResult(chatId);
                    } else {
                        return createNewChat(chatId, me, other);
                    }
                });
    }

    /**
     * Fetches all chats where the specific user is a participant.
     *
     * @param myUserId The current user's ID.
     * @return A Task containing the QuerySnapshot of chats.
     */
    public Task<QuerySnapshot> getUserChats(String myUserId) {
        return db.collection(COLLECTION_CHATS)
                .whereArrayContains("userIds", myUserId)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get();
    }

    // ------------------------------------------------------------------------
    // Move Confirmation Logic
    // ------------------------------------------------------------------------

    public Task<Void> setMoverConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("moverConfirmed", true);
        updates.put("moverConfirmedAt", System.currentTimeMillis());
        return db.collection(COLLECTION_CHATS).document(chatId).update(updates);
    }

    public Task<Void> setCustomerConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("customerConfirmed", true);
        updates.put("customerConfirmedAt", System.currentTimeMillis());
        return db.collection(COLLECTION_CHATS).document(chatId).update(updates);
    }

    // ------------------------------------------------------------------------
    // Private Helpers
    // ------------------------------------------------------------------------

    /**
     * Creates a new Chat document in Firestore with initial data.
     */
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

        // Assumption: 'me' is the Customer initiating the chat.
        // If this logic changes, consider passing roles explicitly.
        chat.setCustomerId(me.getUserId());
        chat.setMoverId(other.getUserId());

        chat.setMoverConfirmed(false);
        chat.setCustomerConfirmed(false);

        chat.setLastUpdated(new Timestamp(new Date()));
        chat.setLastMessage("צ'אט חדש נוצר");

        return db.collection(COLLECTION_CHATS)
                .document(chatId)
                .set(chat)
                .continueWith(task -> chatId);
    }

    /**
     * Generates a unique, deterministic Chat ID based on two User IDs.
     * Logic: Sorts IDs alphabetically so ID(A,B) is the same as ID(B,A).
     */
    private String generateChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
}