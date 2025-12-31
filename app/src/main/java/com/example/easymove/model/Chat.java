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
    // תיאום הובלה
    private String customerId;          // מי פתח את הצ'אט (הלקוח)
    private String moverId;             // המוביל
    private boolean moverConfirmed;     // המוביל לחץ "תיאמתי"
    private boolean customerConfirmed;  // הלקוח אישר
    private Long moverConfirmedAt;
    private Long customerConfirmedAt;

    // שדה עזר (לא נשמר במסד נתונים) לדעת מי "האני" הנוכחי באפליקציה
    private transient String currentUserId;
    private String chatTitle;
    private String chatImageUrl;
    private String lastMessageText;
    private long timestampLong;

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
    public String getChatTitle() { return chatTitle; }
    public void setChatTitle(String chatTitle) { this.chatTitle = chatTitle; }

    public String getChatImageUrl() { return chatImageUrl; }
    public void setChatImageUrl(String chatImageUrl) { this.chatImageUrl = chatImageUrl; }

    public String getLastMessageText() { return lastMessageText; }
    public void setLastMessageText(String lastMessageText) { this.lastMessageText = lastMessageText; }

    public long getTimestampLong() { return timestampLong; }
    public void setTimestampLong(long timestampLong) { this.timestampLong = timestampLong; }
}