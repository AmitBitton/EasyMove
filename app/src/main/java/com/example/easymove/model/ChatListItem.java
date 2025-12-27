package com.example.easymove.model;

public interface ChatListItem {
    long getTimestampLong(); // הזמן ב-Long למיון
    String getChatTitle();   // השם שיוצג (שם המוביל או הלקוח)
    String getChatImageUrl(); // ה-URL של התמונה שתוצג
    String getLastMessageText(); // תוכן ההודעה האחרונה
    String getId(); // מזהה הצ'אט
}