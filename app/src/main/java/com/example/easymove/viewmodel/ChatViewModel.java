package com.example.easymove.viewmodel; // ViewModels

import androidx.lifecycle.LiveData; // נתונים נצפים
import androidx.lifecycle.MutableLiveData; // LiveData שניתן לשנות
import androidx.lifecycle.ViewModel; // בסיס ל-ViewModel

import com.example.easymove.model.Chat; // מודל צ'אט
import com.example.easymove.model.UserProfile; // מודל משתמש
import com.example.easymove.model.repository.ChatRepository; // ריפו לצ'אטים
import com.example.easymove.model.repository.UserRepository; // ריפו למשתמשים
import com.google.firebase.firestore.DocumentSnapshot; // מסמך מהמסד

import java.util.ArrayList; // רשימה
import java.util.List; // ממשק רשימה

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository = new ChatRepository();
    // ריפו שמדבר עם Firestore על "chats"

    private final UserRepository userRepository = new UserRepository();
    // ריפו שמחזיר את הפרופיל שלי, כדי לדעת מי אני ומה ה-UID שלי

    // משתנים לניווט ושגיאות
    private final MutableLiveData<String> navigateToChatId = new MutableLiveData<>();
    // משמש לניווט למסך צ'אט: כשהוא מקבל ערך -> ה-UI יכול לפתוח ChatActivity

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // שומר הודעת שגיאה להצגה ב-Toast/דיאלוג

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    // מציין האם יש טעינה כרגע (להציג progress)

    // משתנה לרשימת הצ'אטים (עבור מסך ChatsFragment)
    private final MutableLiveData<List<Chat>> userChats = new MutableLiveData<>();
    // רשימת הצ'אטים שלי להצגה במסך

    // --- Getters ---
    public LiveData<String> getNavigateToChatId() { return navigateToChatId; }
    // מחזיר LiveData לניווט

    public LiveData<String> getErrorMessage() { return errorMessage; }
    // מחזיר LiveData לשגיאות

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    // מחזיר LiveData לטעינה

    public LiveData<List<Chat>> getUserChatsLiveData() { return userChats; }
    // מחזיר LiveData לרשימת צ'אטים

    /**
     * פונקציה שנקראת כשלוחצים על "צור קשר" בכרטיס מוביל
     * יוצרת צ'אט חדש או מחזירה ID של צ'אט קיים
     */
    public void startChatWithMover(UserProfile mover) {
        // קלט: mover = פרופיל המובילה/מוביל שאליו פונים
        // פלט: אין ישיר, אבל מעדכן navigateToChatId או errorMessage

        isLoading.setValue(true); // מתחילים טעינה

        // שלב 1: קבלת הפרופיל שלי (הלקוח)
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            // הצלחה: קיבלנו פרופיל שלי
            if (myProfile == null) {
                // אם מסיבה כלשהי אין פרופיל
                isLoading.setValue(false);
                errorMessage.setValue("שגיאה בזיהוי המשתמש");
                return;
            }

            // שלב 2: יצירת הצ'אט או קבלת הקיים
            chatRepository.getOrCreateChat(myProfile, mover)
                    .addOnSuccessListener(chatId -> {
                        // הצלחה: יש לנו chatId
                        isLoading.setValue(false);
                        navigateToChatId.setValue(chatId); // ה-UI ישתמש בזה כדי לעבור למסך
                    })
                    .addOnFailureListener(e -> {
                        // כישלון: שמים הודעת שגיאה
                        isLoading.setValue(false);
                        errorMessage.setValue("שגיאה ביצירת צ'אט: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            // כישלון בהבאת הפרופיל שלי
            isLoading.setValue(false);
            errorMessage.setValue("נכשל בטעינת הפרופיל שלי");
        });
    }

    /**
     * פונקציה שנקראת במסך "הצ'אטים שלי" (ChatsFragment)
     * טוענת את כל השיחות הפתוחות של המשתמש
     */
    public void loadUserChats() {
        // קלט: אין (מביא לפי המשתמשת המחוברת)
        // פלט: אין ישיר, אבל מעדכן userChats / errorMessage / isLoading

        isLoading.setValue(true); // מתחילים טעינה

        // קודם משיגים את ה-ID שלי כדי לדעת את מי לחפש
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            if (myProfile == null) {
                // אם אין פרופיל - מפסיקים
                isLoading.setValue(false);
                return;
            }

            chatRepository.getUserChats(myProfile.getUserId())
                    .addOnSuccessListener(querySnapshot -> {
                        // הצלחה: קיבלנו רשימת מסמכים
                        List<Chat> chats = new ArrayList<>(); // רשימה שנבנה מהמסמכים

                        if (querySnapshot != null) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                // מעבר על כל המסמכים
                                Chat chat = doc.toObject(Chat.class); // המרה ל-Chat
                                if (chat != null) {
                                    // חשוב! מגדירים מי "אני" כדי שהמודל ידע איזה שם להציג (של השני)
                                    chat.setCurrentUserId(myProfile.getUserId());
                                    chats.add(chat); // הוספה לרשימה
                                }
                            }
                        }

                        userChats.setValue(chats); // מעדכנים LiveData -> המסך יתעדכן
                        isLoading.setValue(false); // סיום טעינה
                    })
                    .addOnFailureListener(e -> {
                        // כישלון בהבאת צ'אטים
                        isLoading.setValue(false);
                        errorMessage.setValue("שגיאה בטעינת צ'אטים");
                    });

        }).addOnFailureListener(e -> {
            // כישלון בהבאת פרופיל
            isLoading.setValue(false);
            errorMessage.setValue("נכשל בטעינת פרופיל משתמש");
        });
    }

    // איפוס הניווט אחרי שהשתמשנו בו (כדי שלא יקפוץ שוב כשחוזרים למסך)
    public void onChatNavigated() {
        // קלט: אין
        // פלט: אין
        navigateToChatId.setValue(null); // מאפס כדי למנוע ניווט חוזר
    }
}
