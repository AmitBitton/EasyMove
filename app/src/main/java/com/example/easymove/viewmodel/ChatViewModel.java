package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.Chat;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.ChatRepository;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserRepository userRepository = new UserRepository(); // נצטרך את זה כדי לדעת מי "אני"

    // משתנים לניווט ושגיאות
    private final MutableLiveData<String> navigateToChatId = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // משתנה לרשימת הצ'אטים (עבור מסך ChatsFragment)
    private final MutableLiveData<List<Chat>> userChats = new MutableLiveData<>();

    // --- Getters ---
    public LiveData<String> getNavigateToChatId() { return navigateToChatId; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<Chat>> getUserChatsLiveData() { return userChats; }

    /**
     * פונקציה שנקראת כשלוחצים על "צור קשר" בכרטיס מוביל
     * יוצרת צ'אט חדש או מחזירה ID של צ'אט קיים
     */
    public void startChatWithMover(UserProfile mover) {
        isLoading.setValue(true);

        // שלב 1: קבלת הפרופיל שלי (הלקוח)
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            if (myProfile == null) {
                isLoading.setValue(false);
                errorMessage.setValue("שגיאה בזיהוי המשתמש");
                return;
            }

            // שלב 2: יצירת הצ'אט או קבלת הקיים
            chatRepository.getOrCreateChat(myProfile, mover)
                    .addOnSuccessListener(chatId -> {
                        isLoading.setValue(false);
                        navigateToChatId.setValue(chatId); // זה יגרום למעבר מסך
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        errorMessage.setValue("שגיאה ביצירת צ'אט: " + e.getMessage());
                    });

        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            errorMessage.setValue("נכשל בטעינת הפרופיל שלי");
        });
    }

    /**
     * פונקציה שנקראת במסך "הצ'אטים שלי" (ChatsFragment)
     * טוענת את כל השיחות הפתוחות של המשתמש
     */
    public void loadUserChats() {
        isLoading.setValue(true);

        // קודם משיגים את ה-ID שלי כדי לדעת את מי לחפש
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            if (myProfile == null) {
                isLoading.setValue(false);
                return;
            }

            chatRepository.getUserChats(myProfile.getUserId())
                    .addOnSuccessListener(querySnapshot -> {
                        List<Chat> chats = new ArrayList<>();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Chat chat = doc.toObject(Chat.class);
                                if (chat != null) {
                                    // חשוב! מגדירים מי "אני" כדי שהמודל ידע איזה שם להציג (של השני)
                                    chat.setCurrentUserId(myProfile.getUserId());
                                    chats.add(chat);
                                }
                            }
                        }
                        userChats.setValue(chats);
                        isLoading.setValue(false);
                    })
                    .addOnFailureListener(e -> {
                        isLoading.setValue(false);
                        errorMessage.setValue("שגיאה בטעינת צ'אטים");
                    });
        }).addOnFailureListener(e -> {
            isLoading.setValue(false);
            errorMessage.setValue("נכשל בטעינת פרופיל משתמש");
        });
    }

    // איפוס הניווט אחרי שהשתמשנו בו (כדי שלא יקפוץ שוב כשחוזרים למסך)
    public void onChatNavigated() {
        navigateToChatId.setValue(null);
    }
}