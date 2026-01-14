package com.example.easymove.model; // מודלים

import com.google.firebase.Timestamp; // זמן של Firebase

public class Message {
    // מודל הודעה שנשמר במסד ב: chats/{chatId}/messages

    private String senderId; // מי שלח (UID)
    private String senderName; // שם השולח (להצגה במסך)
    private String text; // תוכן ההודעה
    private Timestamp timestamp; // מתי נשלחה ההודעה

    public Message() { } // חובה לפיירבייס (בנאי ריק)

    public Message(String senderId, String senderName, String text, Timestamp timestamp) {
        // בנאי מלא ליצירת הודעה חדשה לפני שמירה למסד
        this.senderId = senderId; // שמירת UID שולח
        this.senderName = senderName; // שמירת שם שולח
        this.text = text; // שמירת תוכן
        this.timestamp = timestamp; // שמירת זמן שליחה
    }

    // Getters & Setters
    public String getSenderId() { return senderId; } // מחזיר UID שולח
    public void setSenderId(String senderId) { this.senderId = senderId; } // מעדכן UID שולח

    public String getSenderName() { return senderName; } // מחזיר שם שולח
    public void setSenderName(String senderName) { this.senderName = senderName; } // מעדכן שם שולח

    public String getText() { return text; } // מחזיר טקסט
    public void setText(String text) { this.text = text; } // מעדכן טקסט

    public Timestamp getTimestamp() { return timestamp; } // מחזיר timestamp
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; } // מעדכן timestamp
}
