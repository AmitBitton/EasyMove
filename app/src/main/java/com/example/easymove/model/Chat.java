package com.example.easymove.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat implements ChatListItem {

    private String id;
    private List<String> userIds;

    private String lastMessage;
    private Timestamp lastUpdated;
    private String lastSenderId;

    // ✅ שדה חדש: מתי כל משתמש ראה את הצ'אט לאחרונה
    private Map<String, Timestamp> lastSeen = new HashMap<>();

    private String user1Id;
    private String user1Name;
    private String user1Image;

    private String user2Id;
    private String user2Name;
    private String user2Image;

    private String customerId;
    private String moverId;
    private boolean moverConfirmed;
    private boolean customerConfirmed;
    private Long moverConfirmedAt;
    private Long customerConfirmedAt;

    private transient String currentUserId;

    public Chat() {}

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    // ✅✅✅ הפונקציה שהייתה חסרה וגרמה לשגיאה ✅✅✅
    public boolean hasUnreadMessages() {
        if (currentUserId == null || lastUpdated == null) return false;

        // אם אני שלחתי את ההודעה האחרונה, אין לי מה לקרוא
        if (lastSenderId != null && lastSenderId.equals(currentUserId)) {
            return false;
        }

        // בדיקה מתי ראיתי לאחרונה
        Timestamp myLastSeen = lastSeen != null ? lastSeen.get(currentUserId) : null;

        if (myLastSeen == null) {
            // אם מעולם לא ראיתי ויש הודעות -> יש הודעות חדשות
            return true;
        }

        // אם זמן ההודעה האחרונה מאוחר יותר מזמן הצפייה שלי -> יש חדש
        return lastUpdated.compareTo(myLastSeen) > 0;
    }

    // --- שאר הגטרים והסטרים ---

    @Override
    public String getId() { return id; }

    @Override
    public long getTimestampLong() {
        return lastUpdated != null ? lastUpdated.toDate().getTime() : 0;
    }

    @Override
    public String getChatTitle() {
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Name;
        } else {
            return user1Name;
        }
    }

    @Override
    public String getChatImageUrl() {
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Image;
        } else {
            return user1Image;
        }
    }

    @Override
    public String getLastMessageText() {
        return lastMessage != null ? lastMessage : "";
    }

    public void setId(String id) { this.id = id; }
    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

    // ✅ Getter & Setter לשדה החדש
    public Map<String, Timestamp> getLastSeen() { return lastSeen; }
    public void setLastSeen(Map<String, Timestamp> lastSeen) { this.lastSeen = lastSeen; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }
    public String getUser1Name() { return user1Name; }
    public void setUser1Name(String user1Name) { this.user1Name = user1Name; }
    public String getUser1Image() { return user1Image; }
    public void setUser1Image(String user1Image) { this.user1Image = user1Image; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }
    public String getUser2Name() { return user2Name; }
    public void setUser2Name(String user2Name) { this.user2Name = user2Name; }
    public String getUser2Image() { return user2Image; }
    public void setUser2Image(String user2Image) { this.user2Image = user2Image; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getMoverId() { return moverId; }
    public void setMoverId(String moverId) { this.moverId = moverId; }
    public boolean isMoverConfirmed() { return moverConfirmed; }
    public void setMoverConfirmed(boolean moverConfirmed) { this.moverConfirmed = moverConfirmed; }
    public boolean isCustomerConfirmed() { return customerConfirmed; }
    public void setCustomerConfirmed(boolean customerConfirmed) { this.customerConfirmed = customerConfirmed; }
    public Long getMoverConfirmedAt() { return moverConfirmedAt; }
    public void setMoverConfirmedAt(Long moverConfirmedAt) { this.moverConfirmedAt = moverConfirmedAt; }
    public Long getCustomerConfirmedAt() { return customerConfirmedAt; }
    public void setCustomerConfirmedAt(Long customerConfirmedAt) { this.customerConfirmedAt = customerConfirmedAt; }
}