package com.example.easymove.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.easymove.model.Chat;
import com.example.easymove.model.Message;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.ChatRepository;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository = new ChatRepository();
    private final MoveRepository moveRepository = new MoveRepository();
    private final UserRepository userRepository = new UserRepository();

    // --- LiveData לשיחה בודדת (ChatActivity) ---
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Chat> chatMetadataLiveData = new MutableLiveData<>();

    // --- LiveData לרשימת הצ'אטים (ChatsFragment) - התיקון שלך כאן ---
    private final MutableLiveData<List<Chat>> userChatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // --- LiveData כלליים ---
    private final MutableLiveData<String> navigateToChatId = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // מחזיקי האזנה
    private ListenerRegistration messagesListener;
    private ListenerRegistration chatMetadataListener;

    // --- Getters ---
    public LiveData<List<Message>> getMessages() { return messagesLiveData; }
    public LiveData<Chat> getChatMetadata() { return chatMetadataLiveData; }
    public LiveData<List<Chat>> getUserChatsLiveData() { return userChatsLiveData; } // ✅ הוספנו
    public LiveData<Boolean> getIsLoading() { return isLoading; } // ✅ הוספנו
    public LiveData<String> getNavigateToChatId() { return navigateToChatId; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public String getCurrentUserId() {
        return moveRepository.getCurrentUserId();
    }

    // =================================================================
    //  לוגיקה לרשימת הצ'אטים (ChatsFragment) ✅
    // =================================================================
    public void loadUserChats() {
        String myId = getCurrentUserId();
        if (myId == null) return;

        isLoading.setValue(true);
        chatRepository.getUserChats(myId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Chat> chats = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Chat chat = doc.toObject(Chat.class);
                            // הגדרת ה-ID הנוכחי כדי שהאדפטר ידע להציג את השם/תמונה הנכונים
                            chat.setCurrentUserId(myId);
                            chats.add(chat);
                        }
                    }
                    userChatsLiveData.setValue(chats);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("שגיאה בטעינת צ'אטים: " + e.getMessage());
                });
    }

    // =================================================================
    //  לוגיקה ליצירת צ'אט (SearchMoverFragment)
    // =================================================================

    public void startChatWithMover(UserProfile mover) {
        String myId = getCurrentUserId();
        if (myId == null) {
            errorMessage.setValue("משתמש לא מחובר");
            return;
        }

        userRepository.getUserById(myId).addOnSuccessListener(me -> {
            if (me == null) {
                errorMessage.setValue("שגיאה בטעינת פרופיל משתמש");
                return;
            }
            chatRepository.getOrCreateChat(me, mover)
                    .addOnSuccessListener(chatId -> navigateToChatId.setValue(chatId))
                    .addOnFailureListener(e -> errorMessage.setValue("שגיאה ביצירת צ'אט: " + e.getMessage()));
        }).addOnFailureListener(e -> errorMessage.setValue("שגיאה בטעינת נתונים: " + e.getMessage()));
    }

    public void onChatNavigated() {
        navigateToChatId.setValue(null);
    }

    // =================================================================
    //  לוגיקה למסך הצ'אט עצמו (ChatActivity)
    // =================================================================

    public void startListening(String chatId) {
        if (chatId == null) return;

        if (messagesListener == null) {
            messagesListener = chatRepository.listenToMessages(chatId, (value, error) -> {
                if (error != null) return;
                if (value != null) {
                    List<Message> list = value.toObjects(Message.class);
                    messagesLiveData.setValue(list);
                }
            });
        }

        if (chatMetadataListener == null) {
            chatMetadataListener = chatRepository.listenToChatMetadata(chatId, (value, error) -> {
                if (error != null || value == null || !value.exists()) return;
                Chat chat = value.toObject(Chat.class);
                if (chat != null) {
                    chatMetadataLiveData.setValue(chat);
                }
            });
        }
    }

    public void sendMessage(String chatId, String text, String senderId, String senderName) {
        if (text == null || text.trim().isEmpty()) return;
        Message message = new Message(senderId, senderName, text, new Timestamp(new Date()));
        chatRepository.sendMessage(chatId, message);
    }

    public void confirmByMover(String chatId) {
        chatRepository.setMoverConfirmed(chatId)
                .addOnSuccessListener(v -> toastMessage.setValue("אישרת ✅ ממתין ללקוח"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    public void confirmByCustomer(String chatId, String moverId, String customerId) {
        moveRepository.hasActiveConfirmedMove(customerId)
                .addOnSuccessListener(hasActive -> {
                    if (hasActive) {
                        toastMessage.setValue("כבר קיימת הובלה פעילה - לא ניתן לאשר חדשה");
                    } else {
                        moveRepository.confirmMoveByCustomer(chatId, moverId, customerId)
                                .addOnSuccessListener(v -> toastMessage.setValue("ההובלה תואמה בהצלחה!"))
                                .addOnFailureListener(e -> toastMessage.setValue("שגיאה באישור: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה בבדיקת הובלות: " + e.getMessage()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (messagesListener != null) messagesListener.remove();
        if (chatMetadataListener != null) chatMetadataListener.remove();
    }
}