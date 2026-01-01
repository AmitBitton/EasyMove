package com.example.easymove.model; // מודלים

public interface ChatListItem {
    // ממשק שמגדיר "מה צריך" כדי להציג פריט ברשימת צ'אטים

    long getTimestampLong(); // מחזיר זמן (Long) לצורך מיון/הצגה
    String getChatTitle();   // מחזיר את השם שיוצג (שם המוביל/ה או הלקוחה - בפועל שם הצד השני)
    String getChatImageUrl(); // מחזיר URL לתמונת פרופיל להצגה
    String getLastMessageText(); // מחזיר טקסט ההודעה האחרונה
    String getId(); // מחזיר מזהה הצ'אט (doc id)
}
