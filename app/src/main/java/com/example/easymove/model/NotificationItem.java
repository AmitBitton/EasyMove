package com.example.easymove.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

/**
 * Model class representing a single user notification.
 * Stored in Firestore (usually in a sub-collection 'notifications' under the user).
 */
public class NotificationItem implements Serializable {

    private String id;          // Firestore Document ID (Excluded from DB payload)
    private String title;       // Notification Title
    private String message;     // Notification Body
    private Timestamp timestamp;// Time of creation
    private boolean isRead;     // Read status (true/false)

    /**
     * Default constructor required for Firestore serialization.
     */
    public NotificationItem() {
    }

    /**
     * Constructor for creating a new notification locally.
     *
     * @param title     The title of the notification.
     * @param message   The body text of the notification.
     * @param timestamp The time the notification was generated.
     */
    public NotificationItem(String title, String message, Timestamp timestamp) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false; // Default to unread
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}