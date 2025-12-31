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

/**
 * ChatViewModel
 * -------------
 * ViewModel responsible for managing chat-related data between the UI and the repository layer.
 *
 * Responsibilities:
 * - Creating or retrieving existing chats with a mover
 * - Loading all chats for the current user
 * - Exposing LiveData for UI to observe:
 *   - Navigation to chat screen
 *   - Loading state
 *   - Error messages
 *   - List of user chats
 */
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserRepository userRepository = new UserRepository(); // To get current user profile

    // --- LiveData variables for UI communication ---
    private final MutableLiveData<String> navigateToChatId = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Chat>> userChats = new MutableLiveData<>();

    // --- Getters for UI observation ---
    public LiveData<String> getNavigateToChatId() { return navigateToChatId; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<Chat>> getUserChatsLiveData() { return userChats; }

    /**
     * Starts a chat with a selected mover.
     * If a chat already exists between the current user and the mover, returns existing chat ID.
     * Otherwise, creates a new chat and returns its ID.
     *
     * @param mover The mover profile to start a chat with
     */
    public void startChatWithMover(UserProfile mover) {
        isLoading.setValue(true);

        // Step 1: Get current user profile (customer)
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            if (myProfile == null) {
                isLoading.setValue(false);
                errorMessage.setValue("שגיאה בזיהוי המשתמש");
                return;
            }

            // Step 2: Create new chat or retrieve existing one
            chatRepository.getOrCreateChat(myProfile, mover)
                    .addOnSuccessListener(chatId -> {
                        isLoading.setValue(false);
                        navigateToChatId.setValue(chatId); // Notify UI to navigate to chat
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
     * Loads all chats for the current user.
     * Updates LiveData `userChats` with the list of chats.
     */
    public void loadUserChats() {
        isLoading.setValue(true);

        // Step 1: Get current user profile
        userRepository.getMyProfile().addOnSuccessListener(myProfile -> {
            if (myProfile == null) {
                isLoading.setValue(false);
                return;
            }

            // Step 2: Retrieve chats for this user from ChatRepository
            chatRepository.getUserChats(myProfile.getUserId())
                    .addOnSuccessListener(querySnapshot -> {
                        List<Chat> chats = new ArrayList<>();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Chat chat = doc.toObject(Chat.class);
                                if (chat != null) {
                                    // Mark the current user in the chat object so UI can display the other party's name
                                    chat.setCurrentUserId(myProfile.getUserId());
                                    chats.add(chat);
                                }
                            }
                        }
                        userChats.setValue(chats); // Update LiveData for UI
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

    /**
     * Resets the navigation LiveData after navigation has occurred.
     * Prevents duplicate navigation events when returning to the fragment.
     */
    public void onChatNavigated() {
        navigateToChatId.setValue(null);
    }
}
