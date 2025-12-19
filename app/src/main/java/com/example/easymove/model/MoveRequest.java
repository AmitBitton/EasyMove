package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class MoveRequest implements Serializable {
    private String id;              // מזהה ההובלה
    private String customerId;      // מזהה הלקוח שיצר
    private String partnerId;       // מזהה שותף (אם יש)
    private String moverId;         // מזהה המוביל (אם נבחר)
    private String chatId;
    private boolean confirmed;
    private long createdAt;

    // כתובות
    private String sourceAddress;
    private String destAddress;
    private double sourceLat, sourceLng;
    private double destLat, destLng;

    private long moveDate;          // תאריך מתוכנן
    private String status; // "OPEN", "CONFIRMED", "CANCELED"
    private String notes;           // הערות כלליות

    public MoveRequest() {} // חובה ל-Firebase

    // בנאי בסיסי ליצירת הובלה
    public MoveRequest(String customerId, String sourceAddress, String destAddress, long moveDate) {
        this.customerId = customerId;
        this.sourceAddress = sourceAddress;
        this.destAddress = destAddress;
        this.moveDate = moveDate;
        this.status = "OPEN";
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getMoverId() { return moverId; }
    public void setMoverId(String moverId) { this.moverId = moverId; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getSourceAddress() { return sourceAddress; }
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }

    public String getDestAddress() { return destAddress; }
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }
    public double getSourceLat() { return sourceLat; }
    public void setSourceLat(double sourceLat) { this.sourceLat = sourceLat; }

    public double getSourceLng() { return sourceLng; }
    public void setSourceLng(double sourceLng) { this.sourceLng = sourceLng; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }
    public long getMoveDate() { return moveDate; }
    public void setMoveDate(long moveDate) { this.moveDate = moveDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // ... אפשר להוסיף עוד שדות לפי הצורך
}