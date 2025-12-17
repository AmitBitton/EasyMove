package com.example.easymove.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat implements ChatListItem {
    private String id; // מזהה הצ'אט (בדרך כלל חיבור של שני ה-ID)
    private List<String> userIds; // רשימה של ה-UID של המשתתפים (לחיפוש קל)

    // מידע על ההודעה האחרונה לתצוגה ברשימה הראשית
    private String lastMessage;
    private Timestamp lastUpdated;
    private String lastSenderId;

    // פרטי משתמש א' (למשל הלקוח)
    private String user1Id;
    private String user1Name;
    private String user1Image;

    // פרטי משתמש ב' (למשל המוביל)
    private String user2Id;
    private String user2Name;
    private String user2Image;

    // שדה עזר (לא נשמר במסד נתונים) לדעת מי "האני" הנוכחי באפליקציה
    private String currentUserId;

    public Chat() {} // חובה לפיירבייס

    // --- לוגיקה חכמה למימוש הממשק ---

    // פונקציה שנקראת באדפטר כדי להגדיר מי המשתמש הנוכחי
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getTimestampLong() {
        return lastUpdated != null ? lastUpdated.toDate().getTime() : 0;
    }

    @Override
    public String getChatTitle() {
        // אם אני משתמש 1, הצג לי את השם של משתמש 2, ולהפך
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Name;
        } else {
            return user1Name;
        }
    }

    @Override
    public String getChatImageUrl() {
        // אם אני משתמש 1, הצג לי את התמונה של משתמש 2, ולהפך
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

    // --- Getters & Setters רגילים ---
    public void setId(String id) { this.id = id; }
    public List<String> getUserIds() { return userIds; }
    public void setUserIds(List<String> userIds) { this.userIds = userIds; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
    public String getLastSenderId() { return lastSenderId; }
    public void setLastSenderId(String lastSenderId) { this.lastSenderId = lastSenderId; }

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
}