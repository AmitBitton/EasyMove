package com.example.easymove.model.repository;

import com.example.easymove.model.Chat;
import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ChatRepository
 * --------------
 * Repository class for managing chat-related Firestore operations.
 *
 * Responsibilities:
 * - Create or fetch a chat between two users
 * - Retrieve all chats for a given user
 * - Set confirmation flags for mover and customer
 *
 * Encapsulates Firestore logic so the UI layer can remain clean.
 */
public class ChatRepository {

    // Firestore database instance
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Collection name for chats
    private final String CHATS_COLLECTION = "chats";

    /**
     * Fetches a chat between two users if it exists, or creates a new one if it doesn't.
     *
     * Ensures no duplicate chats exist between the same two users by generating
     * a deterministic chat ID based on both user IDs.
     *
     * @param me the current user profile (customer)
     * @param other the other user profile (mover)
     * @return a Task that resolves to the chat ID
     */
    public Task<String> getOrCreateChat(UserProfile me, UserProfile other) {

        // 1. Generate a unique chat ID based on both user IDs, sorted alphabetically
        String chatId = generateChatId(me.getUserId(), other.getUserId());

        // 2. Check if chat already exists in Firestore
        return db.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .continueWithTask(task -> {

                    if (!task.isSuccessful()) {
                        // Propagate Firestore errors
                        throw Objects.requireNonNull(task.getException());
                    }

                    DocumentSnapshot doc = task.getResult();

                    if (doc.exists()) {
                        // Chat already exists → return existing chat ID
                        return Tasks.forResult(chatId);
                    } else {
                        // Chat does not exist → create a new one
                        return createNewChat(chatId, me, other);
                    }
                });
    }

    /**
     * Creates a new chat document in Firestore with default values.
     *
     * @param chatId unique ID for the chat
     * @param me customer user profile
     * @param other mover user profile
     * @return Task that resolves to the chat ID once saved
     */
    private Task<String> createNewChat(String chatId, UserProfile me, UserProfile other) {

        Chat chat = new Chat();
        chat.setId(chatId);

        // List of both user IDs for query purposes
        chat.setUserIds(Arrays.asList(me.getUserId(), other.getUserId()));

        // Set customer info (user1)
        chat.setUser1Id(me.getUserId());
        chat.setUser1Name(me.getName());
        chat.setUser1Image(me.getProfileImageUrl());

        // Set mover info (user2)
        chat.setUser2Id(other.getUserId());
        chat.setUser2Name(other.getName());
        chat.setUser2Image(other.getProfileImageUrl());

        // Additional fields for domain logic
        chat.setCustomerId(me.getUserId());
        chat.setMoverId(other.getUserId());
        chat.setMoverConfirmed(false);
        chat.setCustomerConfirmed(false);

        // Timestamp and initial message
        chat.setLastUpdated(new Timestamp(new Date()));
        chat.setLastMessage("צ'אט חדש נוצר");

        // Save chat to Firestore and return chat ID upon completion
        return db.collection(CHATS_COLLECTION)
                .document(chatId)
                .set(chat)
                .continueWith(task -> chatId);
    }

    /**
     * Helper method to generate a deterministic chat ID between two users.
     *
     * This prevents duplicate chat documents for the same pair of users.
     *
     * @param userId1 first user ID
     * @param userId2 second user ID
     * @return a combined chat ID string
     */
    private String generateChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    /**
     * Retrieves all chats that include the specified user.
     *
     * Results are ordered by the most recently updated chat first.
     *
     * @param myUserId the user ID to search for
     * @return Task that resolves to a QuerySnapshot of matching chats
     */
    public Task<QuerySnapshot> getUserChats(String myUserId) {
        return db.collection(CHATS_COLLECTION)
                .whereArrayContains("userIds", myUserId)
                .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Sets the mover's confirmation flag to true and stores the confirmation timestamp.
     *
     * Typically called when the mover clicks "I coordinated with the customer".
     *
     * @param chatId ID of the chat to update
     * @return Task representing the Firestore update operation
     */
    public Task<Void> setMoverConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("moverConfirmed", true);
        updates.put("moverConfirmedAt", System.currentTimeMillis());

        return db.collection(CHATS_COLLECTION)
                .document(chatId)
                .update(updates);
    }

    /**
     * Sets the customer's confirmation flag to true and stores the confirmation timestamp.
     *
     * Typically called after the mover has confirmed coordination.
     *
     * @param chatId ID of the chat to update
     * @return Task representing the Firestore update operation
     */
    public Task<Void> setCustomerConfirmed(String chatId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("customerConfirmed", true);
        updates.put("customerConfirmedAt", System.currentTimeMillis());

        return db.collection(CHATS_COLLECTION)
                .document(chatId)
                .update(updates);
    }
}