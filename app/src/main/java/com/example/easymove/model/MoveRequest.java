package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

/**
 * The central model representing a moving request.
 * Contains all details regarding the move: locations, dates, partner details (if any),
 * status, and cancellation workflows.
 */
public class MoveRequest implements Serializable {

    private String id;              // Firestore Document ID (Excluded from DB payload)

    // --- Participants ---
    private String customerId;      // The owner of the move
    private String partnerId;       // The partner joining the move (Optional)
    private String moverId;         // The service provider (assigned after confirmation)
    private String moverName;       // Cached name of the mover
    private String chatId;          // ID of the chat associated with this move

    // --- Locations ---
    private String sourceAddress;
    private double sourceLat, sourceLng;

    private String destAddress;
    private double destLat, destLng;

    // --- Partner Pickup (Intermediate Stop) ---
    private String intermediateAddress;
    private Double intermediateLat; // Double object to allow nulls (if no partner)
    private Double intermediateLng;

    // --- Status & Time ---
    private String status;          // OPEN, CONFIRMED, COMPLETED, CANCELED
    private boolean confirmed;      // True if mover accepted
    private long moveDate;          // Scheduled date (timestamp)
    private long createdAt;         // Creation date (timestamp)
    private String notes;

    // --- Cancellation Flow (Mover approval logic) ---
    private Boolean cancelRequestPending;
    private Long cancelRequestedAt;
    private String cancelRequestedBy;
    private Long cancelApprovedAt;
    private String cancelApprovedBy;

    /**
     * Default constructor required for Firestore serialization.
     */
    public MoveRequest() {
    }

    /**
     * Helper constructor for creating a new Draft/Open request.
     */
    public MoveRequest(String customerId, String sourceAddress, String destAddress, long moveDate) {
        this.customerId = customerId;
        this.sourceAddress = sourceAddress;
        this.destAddress = destAddress;
        this.moveDate = moveDate;
        this.status = "OPEN";
        this.createdAt = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public String getMoverId() { return moverId; }
    public void setMoverId(String moverId) { this.moverId = moverId; }

    public String getMoverName() { return moverName; }
    public void setMoverName(String moverName) { this.moverName = moverName; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    // --- Address Getters/Setters with PropertyName ---

    @PropertyName("sourceAddress")
    public String getSourceAddress() { return sourceAddress; }
    @PropertyName("sourceAddress")
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }

    @PropertyName("destAddress")
    public String getDestAddress() { return destAddress; }
    @PropertyName("destAddress")
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }

    // --- Intermediate (Partner) Address ---

    @PropertyName("intermediateAddress")
    public String getIntermediateAddress() { return intermediateAddress; }
    @PropertyName("intermediateAddress")
    public void setIntermediateAddress(String intermediateAddress) { this.intermediateAddress = intermediateAddress; }

    @PropertyName("intermediateLat")
    public Double getIntermediateLat() { return intermediateLat; }
    @PropertyName("intermediateLat")
    public void setIntermediateLat(Double intermediateLat) { this.intermediateLat = intermediateLat; }

    @PropertyName("intermediateLng")
    public Double getIntermediateLng() { return intermediateLng; }
    @PropertyName("intermediateLng")
    public void setIntermediateLng(Double intermediateLng) { this.intermediateLng = intermediateLng; }

    // --- Coordinates (Primitives) ---

    public double getSourceLat() { return sourceLat; }
    public void setSourceLat(double sourceLat) { this.sourceLat = sourceLat; }

    public double getSourceLng() { return sourceLng; }
    public void setSourceLng(double sourceLng) { this.sourceLng = sourceLng; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }

    // --- Status & Meta ---

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    @PropertyName("moveDate")
    public long getMoveDate() { return moveDate; }
    @PropertyName("moveDate")
    public void setMoveDate(long moveDate) { this.moveDate = moveDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // --- Cancellation Flow ---

    @PropertyName("cancelRequestPending")
    public Boolean getCancelRequestPending() { return cancelRequestPending; }
    @PropertyName("cancelRequestPending")
    public void setCancelRequestPending(Boolean cancelRequestPending) { this.cancelRequestPending = cancelRequestPending; }

    @PropertyName("cancelRequestedAt")
    public Long getCancelRequestedAt() { return cancelRequestedAt; }
    @PropertyName("cancelRequestedAt")
    public void setCancelRequestedAt(Long cancelRequestedAt) { this.cancelRequestedAt = cancelRequestedAt; }

    @PropertyName("cancelRequestedBy")
    public String getCancelRequestedBy() { return cancelRequestedBy; }
    @PropertyName("cancelRequestedBy")
    public void setCancelRequestedBy(String cancelRequestedBy) { this.cancelRequestedBy = cancelRequestedBy; }

    @PropertyName("cancelApprovedAt")
    public Long getCancelApprovedAt() { return cancelApprovedAt; }
    @PropertyName("cancelApprovedAt")
    public void setCancelApprovedAt(Long cancelApprovedAt) { this.cancelApprovedAt = cancelApprovedAt; }

    @PropertyName("cancelApprovedBy")
    public String getCancelApprovedBy() { return cancelApprovedBy; }
    @PropertyName("cancelApprovedBy")
    public void setCancelApprovedBy(String cancelApprovedBy) { this.cancelApprovedBy = cancelApprovedBy; }
}