package com.example.easymove.model;

public class MatchRequest {
    private String requestId;      // מזהה הבקשה (ID של המסמך)
    private String fromUserId;     // מי שלח
    private String fromUserName;   // שם השולח (כדי להציג יפה ברשימה)
    private String toUserId;       // למי נשלח
    private String status;         // "pending", "accepted", "rejected"
    private long timestamp;

    public MatchRequest() {} // חובה לפיירבייס

    public MatchRequest(String fromUserId, String fromUserName, String toUserId) {
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.toUserId = toUserId;
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}