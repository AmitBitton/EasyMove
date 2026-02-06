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

/**
 * ViewModel responsible for managing Chat data and logic.
 * Handles:
 * 1. Single Chat Room (Messages, Metadata, Confirmation logic).
 * 2. Chat List (For the ChatsFragment).
 * 3. Creating new chats (from Search).
 */
public class ChatViewModel extends ViewModel {

    // Repositories
    private final ChatRepository chatRepository = new ChatRepository();
    private final MoveRepository moveRepository = new MoveRepository();
    private final UserRepository userRepository = new UserRepository();

    // --- LiveData: Single Chat (ChatActivity) ---
    private final MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Chat> chatMetadataLiveData = new MutableLiveData<>();

    // --- LiveData: Chat List (ChatsFragment) ---
    private final MutableLiveData<List<Chat>> userChatsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    // --- LiveData: General / Navigation ---
    private final MutableLiveData<String> navigateToChatId = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Listeners (Must be removed in onCleared)
    private ListenerRegistration messagesListener;
    private ListenerRegistration chatMetadataListener;
    private String currentChatIdListener = null; // Track which chat we are listening to

    // --- Getters ---
    public LiveData<List<Message>> getMessages() { return messagesLiveData; }
    public LiveData<Chat> getChatMetadata() { return chatMetadataLiveData; }
    public LiveData<List<Chat>> getUserChatsLiveData() { return userChatsLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getNavigateToChatId() { return navigateToChatId; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public String getCurrentUserId() {
        return moveRepository.getCurrentUserId();
    }

    // =================================================================
    //  Logic: Chat List (ChatsFragment)
    // =================================================================

    /**
     * Fetches the list of chats for the current user.
     */
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
                            // Set current user ID so the Adapter knows which image/name to display (the "Other" person)
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
    //  Logic: Create Chat (SearchMoverFragment)
    // =================================================================

    /**
     * Initiates a chat with a specific Mover. If one exists, opens it; otherwise, creates it.
     */
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
                    .addOnSuccessListener(navigateToChatId::setValue)
                    .addOnFailureListener(e -> errorMessage.setValue("שגיאה ביצירת צ'אט: " + e.getMessage()));
        }).addOnFailureListener(e -> errorMessage.setValue("שגיאה בטעינת נתונים: " + e.getMessage()));
    }

    public void onChatNavigated() {
        navigateToChatId.setValue(null);
    }

    // =================================================================
    //  Logic: Single Chat Room (ChatActivity)
    // =================================================================

    /**
     * Starts listening for real-time messages and metadata for a specific chat.
     */
    public void startListening(String chatId) {
        if (chatId == null) return;

        // If already listening to this specific chat, do nothing
        if (chatId.equals(currentChatIdListener)) return;

        // If listening to a different chat, clear old listeners first
        if (currentChatIdListener != null) {
            removeListeners();
        }

        currentChatIdListener = chatId;

        // 1. Listen to Messages
        messagesListener = chatRepository.listenToMessages(chatId, (value, error) -> {
            if (error != null) return;
            if (value != null) {
                List<Message> list = value.toObjects(Message.class);
                messagesLiveData.setValue(list);
            }
        });

        // 2. Listen to Chat Metadata (e.g., Confirmation Status)
        chatMetadataListener = chatRepository.listenToChatMetadata(chatId, (value, error) -> {
            if (error != null || value == null || !value.exists()) return;
            Chat chat = value.toObject(Chat.class);
            if (chat != null) {
                chatMetadataLiveData.setValue(chat);
            }
        });
    }

    public void sendMessage(String chatId, String text, String senderId, String senderName) {
        if (text == null || text.trim().isEmpty()) return;

        Message message = new Message(senderId, senderName, text.trim(), new Timestamp(new Date()));
        chatRepository.sendMessage(chatId, message);
    }

    public void markAsSeen(String chatId) {
        String myUid = getCurrentUserId();
        if (myUid != null) {
            chatRepository.updateLastSeen(chatId, myUid);
        }
    }

    // =================================================================
    //  Logic: Move Confirmation (Closing a Deal)
    // =================================================================

    public void confirmByMover(String chatId) {
        chatRepository.setMoverConfirmed(chatId)
                .addOnSuccessListener(v -> toastMessage.setValue("אישרת ✅ ממתין ללקוח"))
                .addOnFailureListener(e -> toastMessage.setValue("שגיאה: " + e.getMessage()));
    }

    public void confirmByCustomer(String chatId, String moverId, String customerId) {
        // Step 1: Ensure user doesn't already have an active move
        moveRepository.hasActiveConfirmedMove(customerId)
                .addOnSuccessListener(hasActive -> {
                    if (hasActive) {
                        toastMessage.setValue("כבר קיימת הובלה פעילה - לא ניתן לאשר חדשה");
                    } else {
                        // Step 2: Confirm the move
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
        removeListeners();
    }

    private void removeListeners() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
        if (chatMetadataListener != null) {
            chatMetadataListener.remove();
            chatMetadataListener = null;
        }
        currentChatIdListener = null;
    }
}