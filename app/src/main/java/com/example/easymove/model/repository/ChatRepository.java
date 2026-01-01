package com.example.easymove.model.repository; // שכבת Repository (תקשורת עם Firestore)

import com.example.easymove.model.Chat; // מודל צ'אט למסד נתונים
import com.example.easymove.model.UserProfile; // מודל פרופיל משתמש
import com.google.android.gms.tasks.Task; // Task של Firebase (אסינכרוני)
import com.google.android.gms.tasks.Tasks; // יצירת Task מיידי (forResult)
import com.google.firebase.Timestamp; // Timestamp של Firebase
import com.google.firebase.firestore.DocumentSnapshot; // תוצאה של מסמך יחיד
import com.google.firebase.firestore.FirebaseFirestore; // Firestore
import com.google.firebase.firestore.QuerySnapshot; // תוצאה של Query
import com.google.firebase.firestore.SetOptions; // (לא בשימוש בקוד הזה בפועל)
import java.util.Arrays; // יצירת רשימה משני איברים
import java.util.Date; // זמן נוכחי
import java.util.HashMap; // מפה לעדכונים
import java.util.Map; // ממשק מפה

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // מופע של Firestore כדי לבצע קריאות/כתיבות למסד

    private final String CHATS_COLLECTION = "chats";
    // שם הקולקשן במסד שבו נשמרים הצ'אטים

    /**
     * פונקציה חכמה: בודקת אם קיים צ'אט. אם לא - יוצרת אותו.
     * מחזירה את ה-Chat ID בסיום.
     */
    public Task<String> getOrCreateChat(UserProfile me, UserProfile other) {
        // קלט: me = אני (לרוב הלקוחה), other = הצד השני (לרוב המובילה/מוביל)
        // פלט: Task<String> שמסתיים עם chatId

        // 1. יצירת מזהה ייחודי לצ'אט (מבוסס על שני ה-ID ממוינים)
        // זה מבטיח שלא יהיו כפילויות בין אותם שני אנשים
        String chatId = generateChatId(me.getUserId(), other.getUserId());

        // 2. בדיקה האם הצ'אט קיים
        return db.collection(CHATS_COLLECTION).document(chatId).get()
                // .get() מחזיר Task<DocumentSnapshot> (קריאה אסינכרונית)
                .continueWithTask(task -> {
                    // continueWithTask מאפשר "לשרשר" פעולה נוספת אחרי שהקריאה הסתיימה

                    if (!task.isSuccessful()) {
                        // אם הייתה שגיאה בקריאה
                        throw task.getException(); // זורקת חריגה כדי שה-Task יחזור כ-failure
                    }

                    DocumentSnapshot doc = task.getResult(); // המסמך שחזר מהמסד
                    if (doc.exists()) {
                        // הצ'אט קיים - מחזירים את ה-ID שלו
                        return Tasks.forResult(chatId); // Task שכבר מצליח עם הערך chatId
                    } else {
                        // הצ'אט לא קיים - יוצרים אותו
                        return createNewChat(chatId, me, other); // מחזיר Task<String>
                    }
                });
    }

    private Task<String> createNewChat(String chatId, UserProfile me, UserProfile other) {
        // קלט: chatId מחושב, me, other
        // פלט: Task<String> שמחזיר chatId אחרי שמירה ב-Firestore

        Chat chat = new Chat(); // יצירת אובייקט Chat חדש
        chat.setId(chatId); // שמירת ה-ID בתוך האובייקט
        chat.setUserIds(Arrays.asList(me.getUserId(), other.getUserId()));
        // שדה userIds משמש לחיפוש קל: whereArrayContains("userIds", myUid)

        // שמירת פרטי משתמש 1 (אני) me = customer
        chat.setUser1Id(me.getUserId());
        chat.setUser1Name(me.getName());
        chat.setUser1Image(me.getProfileImageUrl());

        // other = mover שמירת פרטי משתמש 2 (הצד השני)
        chat.setUser2Id(other.getUserId());
        chat.setUser2Name(other.getName());
        chat.setUser2Image(other.getProfileImageUrl());

        chat.setCustomerId(me.getUserId()); // מזהה הלקוחה (מי שפתחה את הצ'אט)
        chat.setMoverId(other.getUserId()); // מזהה המוביל/ה
        chat.setMoverConfirmed(false); // עדיין לא אושר תיאום על ידי המוביל/ה
        chat.setCustomerConfirmed(false); // עדיין לא אושר על ידי הלקוחה

        chat.setLastUpdated(new Timestamp(new Date())); // זמן עדכון אחרון (עכשיו)
        chat.setLastMessage("צ'אט חדש נוצר"); // הודעה אחרונה ברירת מחדל

        // שמירה ב-Firestore
        return db.collection(CHATS_COLLECTION).document(chatId)
                .set(chat) // שומר את כל האובייקט במסמך עם id קבוע
                .continueWith(task -> chatId);
        // אחרי שמירה, מחזירים chatId (גם אם לא משתמשים ב-result של set)
    }

    // פונקציית עזר ליצירת ID אחיד
    private String generateChatId(String userId1, String userId2) {
        // המטרה: עבור אותם שני משתמשים - תמיד ייצא אותו chatId, בלי תלות מי "אני" ומי "הוא/היא"
        if (userId1.compareTo(userId2) < 0) {
            // אם userId1 "קטן" לקסיקוגרפית
            return userId1 + "_" + userId2; // מחברים בצורה קבועה
        } else {
            // אחרת
            return userId2 + "_" + userId1; // מחברים הפוך כדי לשמור עקביות
        }
    }

    public Task<QuerySnapshot> getUserChats(String myUserId) {
        // קלט: myUserId = ה-UID שלי
        // פלט: Task<QuerySnapshot> עם כל הצ'אטים שמכילים אותי
        return db.collection(CHATS_COLLECTION)
                .whereArrayContains("userIds", myUserId)
                // רק צ'אטים שבהם userIds מכיל את ה-UID שלי
                .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING)
                // מיון לפי עדכון אחרון כדי שהצ'אט הכי חדש יופיע למעלה
                .get(); // קריאה חד פעמית מהמסד
    }

    /**
     * המוביל לוחץ ראשון "תיאמתי עם הלקוח"
     */
    public Task<Void> setMoverConfirmed(String chatId) {
        // קלט: chatId = המסמך של הצ'אט
        // פלט: Task<Void> שמסתיים בהצלחה אחרי update
        Map<String, Object> updates = new HashMap<>();
        updates.put("moverConfirmed", true); // סימון שהמוביל אישר
        updates.put("moverConfirmedAt", System.currentTimeMillis()); // זמן אישור במילישניות
        return db.collection(CHATS_COLLECTION).document(chatId).update(updates); // update לשדות בלבד
    }

    /**
     * הלקוח מאשר אחרי שהמוביל אישר
     */
    public Task<Void> setCustomerConfirmed(String chatId) {
        // קלט: chatId = המסמך של הצ'אט
        // פלט: Task<Void> שמסתיים בהצלחה אחרי update
        Map<String, Object> updates = new HashMap<>();
        updates.put("customerConfirmed", true); // סימון שהלקוחה אישרה
        updates.put("customerConfirmedAt", System.currentTimeMillis()); // זמן אישור
        return db.collection(CHATS_COLLECTION).document(chatId).update(updates); // עדכון במסד
    }
}
