package com.example.easymove.model;

import com.google.firebase.Timestamp;

public class Message {
    private String senderId;
    private String senderName;
    private String text;
    private Timestamp timestamp;

    public Message() { } // חובה לפיירבייס

    public Message(String senderId, String senderName, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters & Setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}