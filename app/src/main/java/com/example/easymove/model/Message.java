package com.example.easymove.model;

import com.google.firebase.Timestamp;

/**
 * Model class representing a single chat message within a conversation.
 * Stored in Firestore under: chats/{chatId}/messages/{messageId}
 */
public class Message {

    private String senderId;   // UID of the user who sent the message
    private String senderName; // Display name of the sender
    private String text;       // The content of the message
    private Timestamp timestamp; // Server timestamp of when the message was sent

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public Message() {
    }

    /**
     * Constructor for creating a new message.
     *
     * @param senderId   The UID of the sender.
     * @param senderName The display name of the sender.
     * @param text       The message content.
     * @param timestamp  The time the message was created.
     */
    public Message(String senderId, String senderName, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}