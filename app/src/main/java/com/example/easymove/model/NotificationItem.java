package com.example.easymove.model;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean isRead;

    // Empty constructor for Firestore
    public NotificationItem() {}

    public NotificationItem(String title, String message, Timestamp timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}