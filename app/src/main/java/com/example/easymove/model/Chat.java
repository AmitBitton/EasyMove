package com.example.easymove.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model class representing a Chat conversation between two users (e.g., Customer and Mover).
 * Implements {@link ChatListItem} to allow easy binding to UI adapters.
 */
public class Chat implements ChatListItem {

    private String id;
    private List<String> userIds; // List of UIDs participating in the chat

    private String lastMessage;
    private Timestamp lastUpdated;
    private String lastSenderId;

    // Maps User ID -> Timestamp of when they last opened the chat.
    // Used to calculate the "Unread" red dot.
    private Map<String, Timestamp> lastSeen = new HashMap<>();

    // Denormalized user data to avoid extra API fetches for the chat list
    private String user1Id;
    private String user1Name;
    private String user1Image;

    private String user2Id;
    private String user2Name;
    private String user2Image;

    // Specific fields for the "EasyMove" business logic
    private String customerId;
    private String moverId;

    // Status flags for the move confirmation flow within the chat
    private boolean moverConfirmed;
    private boolean customerConfirmed;
    private Long moverConfirmedAt;
    private Long customerConfirmedAt;

    // Helper field for UI logic (Not saved to DB)
    private transient String currentUserId;

    public Chat() {
        // Empty constructor required for Firestore serialization
    }

    /**
     * Sets the ID of the currently logged-in user.
     * This is crucial for determining which name/image to display (the "other" person)
     * and for calculating unread messages.
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Determines if the chat has unread messages for the current user.
     * Logic:
     * 1. If I sent the last message -> Read.
     * 2. If I have never seen the chat -> Unread.
     * 3. If the last message is newer than my last seen time -> Unread.
     *
     * @return true if there are unread messages.
     */
    public boolean hasUnreadMessages() {
        if (currentUserId == null || lastUpdated == null) return false;

        // If the current user sent the last message, they don't have unread messages
        if (lastSenderId != null && lastSenderId.equals(currentUserId)) {
            return false;
        }

        // Get the timestamp of when the current user last opened the chat
        Timestamp myLastSeen = lastSeen != null ? lastSeen.get(currentUserId) : null;

        if (myLastSeen == null) {
            // User has never opened the chat, but a message exists -> Unread
            return true;
        }

        // Check if the message is newer than the last visit
        return lastUpdated.compareTo(myLastSeen) > 0;
    }

    // ------------------------------------------------------------------------
    // ChatListItem Interface Implementation
    // ------------------------------------------------------------------------

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getTimestampLong() {
        return lastUpdated != null ? lastUpdated.toDate().getTime() : 0;
    }

    @Override
    public String getChatTitle() {
        // Return the name of the OTHER user
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Name;
        } else {
            return user1Name;
        }
    }

    @Override
    public String getChatImageUrl() {
        // Return the image of the OTHER user
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Image;
        } else {
            return user1Image;
        }
    }

    @Override
    public String getLastMessageText() {
        return lastMessage != null ? lastMessage : "";
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public void setId(String id) { this.id = id; }

    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    public Map<String, Timestamp> getLastSeen() { return lastSeen; }
    public void setLastSeen(Map<String, Timestamp> lastSeen) { this.lastSeen = lastSeen; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser1Name() { return user1Name; }
    public void setUser1Name(String user1Name) { this.user1Name = user1Name; }

    public String getUser1Image() { return user1Image; }
    public void setUser1Image(String user1Image) { this.user1Image = user1Image; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public String getUser2Name() { return user2Name; }
    public void setUser2Name(String user2Name) { this.user2Name = user2Name; }

    public String getUser2Image() { return user2Image; }
    public void setUser2Image(String user2Image) { this.user2Image = user2Image; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getMoverId() { return moverId; }
    public void setMoverId(String moverId) { this.moverId = moverId; }

    public boolean isMoverConfirmed() { return moverConfirmed; }
    public void setMoverConfirmed(boolean moverConfirmed) { this.moverConfirmed = moverConfirmed; }

    public boolean isCustomerConfirmed() { return customerConfirmed; }
    public void setCustomerConfirmed(boolean customerConfirmed) { this.customerConfirmed = customerConfirmed; }

    public Long getMoverConfirmedAt() { return moverConfirmedAt; }
    public void setMoverConfirmedAt(Long moverConfirmedAt) { this.moverConfirmedAt = moverConfirmedAt; }

    public Long getCustomerConfirmedAt() { return customerConfirmedAt; }
    public void setCustomerConfirmedAt(Long customerConfirmedAt) { this.customerConfirmedAt = customerConfirmedAt; }
}