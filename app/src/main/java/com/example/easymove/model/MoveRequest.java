package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

public class MoveRequest implements Serializable {
    private String id;              // מזהה ההובלה
    private String customerId;      // מזהה הלקוח שיצר
    private String partnerId;       // מזהה שותף (אם יש)
    private String moverId;         // מזהה המוביל (אם נבחר)

    // כתובות
    private String sourceAddress;
    private String destAddress;
    private double sourceLat, sourceLng;
    private double destLat, destLng;

    private long moveDate;          // תאריך מתוכנן
    private String status;          // "OPEN", "CLOSED", "CANCELED"
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

    public String getSourceAddress() { return sourceAddress; }
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }

    public String getDestAddress() { return destAddress; }
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }

    public long getMoveDate() { return moveDate; }
    public void setMoveDate(long moveDate) { this.moveDate = moveDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    // ... אפשר להוסיף עוד שדות לפי הצורך
}