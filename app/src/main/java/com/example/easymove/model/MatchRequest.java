package com.example.easymove.model;

public class MatchRequest {
    private String requestId;
    private String fromUserId;
    private String fromUserName;
    private String toUserId;
    private String moveId;
    private String status;         // "pending", "waiting_for_mover", "approved", "rejected"
    private String partnerAddress; // הכתובת של המקבל (מתמלא באישור)

    // ✅ שדות חדשים: פרטי ההובלה של השולח (כדי שהמקבל ידע למה הוא מסכים)
    private String originalSourceAddress;
    private String originalDestAddress;

    private long timestamp;

    public MatchRequest() {}

    public MatchRequest(String fromUserId, String fromUserName, String toUserId, String moveId,
                        String source, String dest) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
        this.moveId = moveId;
        this.originalSourceAddress = source;
        this.originalDestAddress = dest;
        this.status = "pending";
        this.timestamp = System.currentTimeMillis();
    }

    // Getters & Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getMoveId() { return moveId; }
    public void setMoveId(String moveId) { this.moveId = moveId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPartnerAddress() { return partnerAddress; }
    public void setPartnerAddress(String partnerAddress) { this.partnerAddress = partnerAddress; }

    public String getOriginalSourceAddress() { return originalSourceAddress; }
    public void setOriginalSourceAddress(String originalSourceAddress) { this.originalSourceAddress = originalSourceAddress; }

    public String getOriginalDestAddress() { return originalDestAddress; }
    public void setOriginalDestAddress(String originalDestAddress) { this.originalDestAddress = originalDestAddress; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}