package com.example.easymove.model.repository;

import com.example.easymove.model.Chat;
import com.example.easymove.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.Date;

public class ChatRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String CHATS_COLLECTION = "chats";

    /**
     * פונקציה חכמה: בודקת אם קיים צ'אט. אם לא - יוצרת אותו.
     * מחזירה את ה-Chat ID בסיום.
     */
    public Task<String> getOrCreateChat(UserProfile me, UserProfile other) {
        // 1. יצירת מזהה ייחודי לצ'אט (מבוסס על שני ה-ID ממוינים)
        // זה מבטיח שלא יהיו כפילויות בין אותם שני אנשים
        String chatId = generateChatId(me.getUserId(), other.getUserId());

        // 2. בדיקה האם הצ'אט קיים
        return db.collection(CHATS_COLLECTION).document(chatId).get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        // הצ'אט קיים - מחזירים את ה-ID שלו
                        return Tasks.forResult(chatId);
                    } else {
                        // הצ'אט לא קיים - יוצרים אותו
                        return createNewChat(chatId, me, other);
                    }
                });
    }

    private Task<String> createNewChat(String chatId, UserProfile me, UserProfile other) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUserIds(Arrays.asList(me.getUserId(), other.getUserId()));

        // שמירת פרטי משתמש 1 (אני)
        chat.setUser1Id(me.getUserId());
        chat.setUser1Name(me.getName());
        chat.setUser1Image(me.getProfileImageUrl());

        // שמירת פרטי משתמש 2 (הצד השני)
        chat.setUser2Id(other.getUserId());
        chat.setUser2Name(other.getName());
        chat.setUser2Image(other.getProfileImageUrl());

        chat.setLastUpdated(new Timestamp(new Date()));
        chat.setLastMessage("צ'אט חדש נוצר");

        // שמירה ב-Firestore
        return db.collection(CHATS_COLLECTION).document(chatId)
                .set(chat)
                .continueWith(task -> chatId); // החזרת ה-ID בסיום
    }

    // פונקציית עזר ליצירת ID אחיד
    private String generateChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public Task<QuerySnapshot> getUserChats(String myUserId) {
        return db.collection(CHATS_COLLECTION)
                .whereArrayContains("userIds", myUserId) // רק צ'אטים שאני נמצא בהם
                .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING) // מיון לפי זמן
                .get();
    }
}