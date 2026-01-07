package com.example.easymove.model;

import com.google.firebase.firestore.PropertyName; // ✅ חובה
import java.io.Serializable;

public class MoveRequest implements Serializable {
    private String id;
    private String customerId;
    private String partnerId;
    private String moverId;
    private String chatId;
    private boolean confirmed;
    private long createdAt;

    // ✅ האנוטציות האלו מכריחות את פיירבייס לקרוא את השדות הנכונים
    @PropertyName("sourceAddress")
    private String sourceAddress;

    @PropertyName("destAddress")
    private String destAddress;

    private double sourceLat, sourceLng;
    private double destLat, destLng;

    @PropertyName("moveDate")
    private long moveDate;

    private String status;
    private String notes;

    public MoveRequest() {}

    public MoveRequest(String customerId, String sourceAddress, String destAddress, long moveDate) {
        this.customerId = customerId;
        this.sourceAddress = sourceAddress;
        this.destAddress = destAddress;
        this.moveDate = moveDate;
        this.status = "OPEN";
    }

    // --- Getters & Setters ---

    @PropertyName("id")
    public String getId() { return id; }
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("sourceAddress")
    public String getSourceAddress() { return sourceAddress; }
    @PropertyName("sourceAddress")
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }

    @PropertyName("destAddress")
    public String getDestAddress() { return destAddress; }
    @PropertyName("destAddress")
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }

    @PropertyName("moveDate")
    public long getMoveDate() { return moveDate; }
    @PropertyName("moveDate")
    public void setMoveDate(long moveDate) { this.moveDate = moveDate; }

    // שאר הגטרים והסטרים הרגילים
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
    public double getSourceLat() { return sourceLat; }
    public void setSourceLat(double sourceLat) { this.sourceLat = sourceLat; }
    public double getSourceLng() { return sourceLng; }
    public void setSourceLng(double sourceLng) { this.sourceLng = sourceLng; }
    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }
    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}