package com.example.easymove.model; // מודלים של האפליקציה

import com.google.firebase.Timestamp; // Timestamp של Firebase לשדות זמן
import java.util.List; // רשימת משתמשים

public class Chat implements ChatListItem {
    // מודל שמייצג מסמך "Chat" ב-Firestore
    // וגם מממש ChatListItem כדי שהאדפטר ידע איך להציג אותו ברשימה

    private String id; // מזהה הצ'אט (בדרך כלל חיבור של שני ה-ID)
    private List<String> userIds; // רשימה של ה-UID של המשתתפים (לחיפוש קל)

    // מידע על ההודעה האחרונה לתצוגה ברשימה הראשית
    private String lastMessage; // טקסט ההודעה האחרונה
    private Timestamp lastUpdated; // זמן עדכון אחרון
    private String lastSenderId; // מי שלח את ההודעה האחרונה

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
    private Long moverConfirmedAt;      // זמן אישור המוביל
    private Long customerConfirmedAt;   // זמן אישור הלקוח

    // שדה עזר (לא נשמר במסד נתונים) לדעת מי "האני" הנוכחי באפליקציה
    private transient String currentUserId;
    // transient = לא נשמר/לא נקרא מול פיירבייס; זה רק ללוגיקה של תצוגה

    public Chat() {} // חובה לפיירבייס (בנאי ריק כדי ש-Firestore יוכל ליצור אובייקט)

    // --- לוגיקה חכמה למימוש הממשק ---

    // פונקציה שנקראת באדפטר כדי להגדיר מי המשתמש הנוכחי
    public void setCurrentUserId(String currentUserId) {
        // קלט: currentUserId = ה-UID של המשתמש המחובר
        // פלט: אין, רק שומר ערך כדי שהמודל ידע "מי אני"
        this.currentUserId = currentUserId;
    }

    @Override
    public String getId() {
        // מחזירה את מזהה הצ'אט
        return id;
    }

    @Override
    public long getTimestampLong() {
        // מחזירה זמן עדכון אחרון כמספר long (מילישניות) כדי למיין/להציג
        return lastUpdated != null ? lastUpdated.toDate().getTime() : 0;
    }

    @Override
    public String getChatTitle() {
        // מחזירה את השם שיוצג ברשימת הצ'אטים:
        // תמיד "השם של הצד השני" ביחס ל-currentUserId
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            // אם אני = user1, אז הצד השני הוא user2
            return user2Name;
        } else {
            // אחרת אני = user2 (או currentUserId עדיין לא הוגדר), אז מציגים user1
            return user1Name;
        }
    }

    @Override
    public String getChatImageUrl() {
        // מחזירה את תמונת הפרופיל שיוצג ברשימת הצ'אטים:
        // תמיד "התמונה של הצד השני"
        if (currentUserId != null && currentUserId.equals(user1Id)) {
            return user2Image;
        } else {
            return user1Image;
        }
    }

    @Override
    public String getLastMessageText() {
        // מחזירה טקסט הודעה אחרונה (או ריק אם null)
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
