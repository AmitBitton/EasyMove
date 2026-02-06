package com.example.easymove.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

/**
 * Model class representing a request to partner up for a move (Shared Delivery).
 * This document tracks the negotiation flow between two customers:
 * 1. Sender (Owner of the move) -> invites Recipient.
 * 2. Status: 'pending' -> 'waiting_for_mover' (if partner approves) -> 'approved' (if mover accepts).
 */
public class MatchRequest implements Serializable {

    private String requestId;     // Firestore Document ID (Excluded from DB payload)

    // Sender Details (The Main Customer)
    private String fromUserId;
    private String fromUserName;

    // Recipient Details (The Potential Partner)
    private String toUserId;
    private String toUserName;

    // Move Context
    private String moveId;        // ID of the existing move to join
    private String status;        // "pending", "waiting_for_mover", "approved", "rejected"
    private String partnerAddress; // The partner's address (filled upon approval)

    // Move Details (So the recipient knows what they are joining)
    private String originalSourceAddress;
    private String originalDestAddress;

    private long timestamp;

    /**
     * Empty constructor required for Firestore serialization.
     */
    public MatchRequest() {
        // Default constructor
    }

    /**
     * Full constructor for creating a new match request.
     *
     * @param fromUserId   ID of the requester.
     * @param fromUserName Name of the requester.
     * @param toUserId     ID of the invitee.
     * @param toUserName   Name of the invitee.
     * @param moveId       ID of the move context.
     * @param source       Original source address.
     * @param dest         Original destination address.
     */
    public MatchRequest(String fromUserId, String fromUserName, String toUserId, String toUserName,
                        String moveId, String source, String dest) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.toUserName = toUserName;
        this.moveId = moveId;
        this.originalSourceAddress = source;
        this.originalDestAddress = dest;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // ------------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------------

    @Exclude
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getMoveId() {
        return moveId;
    }

    public void setMoveId(String moveId) {
        this.moveId = moveId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPartnerAddress() {
        return partnerAddress;
    }

    public void setPartnerAddress(String partnerAddress) {
        this.partnerAddress = partnerAddress;
    }

    public String getOriginalSourceAddress() {
        return originalSourceAddress;
    }

    public void setOriginalSourceAddress(String originalSourceAddress) {
        this.originalSourceAddress = originalSourceAddress;
    }

    public String getOriginalDestAddress() {
        return originalDestAddress;
    }

    public void setOriginalDestAddress(String originalDestAddress) {
        this.originalDestAddress = originalDestAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}