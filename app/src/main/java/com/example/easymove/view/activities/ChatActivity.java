package com.example.easymove.view.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.ChatAdapter;
import com.example.easymove.model.Chat;
import com.example.easymove.model.repository.UserRepository;
import com.example.easymove.viewmodel.ChatViewModel;

/**
 * Activity responsible for the individual Chat screen.
 * Handles sending/receiving messages and the specific logic for confirming a move
 * between a Customer and a Mover within the chat context.
 */
public class ChatActivity extends AppCompatActivity {

    private String chatId;
    private String currentUserId;
    private String currentUserName;

    private ChatViewModel chatViewModel; // ViewModel handles all business logic

    // UI Components
    private ChatAdapter adapter;
    private EditText editInput;
    private RecyclerView recyclerView;
    private TextView tvTitle;
    private LinearLayout layoutConfirmMove;
    private TextView tvConfirmStatus;
    private Button btnConfirmMove;

    private Chat currentChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. Validate Intent Data
        chatId = getIntent().getStringExtra("CHAT_ID");
        if (chatId == null) {
            Toast.makeText(this, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Initialize ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        currentUserId = chatViewModel.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Load User Name (needed for sending messages with proper sender name)
        // Note: Ideally, this should also be moved to the ViewModel or UserSession.
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> currentUserName = name);

        initViews();
        setupRecyclerView();
        observeViewModel();

        // 4. Start listening for real-time updates for this specific chat
        chatViewModel.startListening(chatId);
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText("צ'אט"); // Default title, will be updated by ViewModel

        editInput = findViewById(R.id.editMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);
        recyclerView = findViewById(R.id.recyclerChat);

        // Confirmation UI (Bottom card)
        layoutConfirmMove = findViewById(R.id.layoutConfirmMove);
        tvConfirmStatus = findViewById(R.id.tvConfirmStatus);
        btnConfirmMove = findViewById(R.id.btnConfirmMove);

        btnSend.setOnClickListener(v -> sendMessage());
        btnConfirmMove.setOnClickListener(v -> onConfirmClicked());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(currentUserId);

        // StackFromEnd isn't strictly necessary with scrollToPosition, but helpful for chat UIs
        LinearLayoutManager manager = new LinearLayoutManager(this);
        // manager.setStackFromEnd(true); // Optional: Keeps list at bottom on open
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        // 1. Observe Messages List
        chatViewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // 2. Observe Chat Metadata (Title, Confirmation Status)
        chatViewModel.getChatMetadata().observe(this, chat -> {
            if (chat != null) {
                currentChat = chat;

                // Update Toolbar Title with the OTHER person's name
                chat.setCurrentUserId(currentUserId);
                String title = chat.getChatTitle();
                if (title != null && !title.trim().isEmpty()) {
                    tvTitle.setText(title);
                }

                // Update the "Confirm Move" card UI based on roles and status
                updateConfirmCardUi(chat);
            }
        });

        // 3. Observe Toast Messages (Errors / Successes)
        chatViewModel.getToastMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the UI for the "Confirm Move" card at the top of the chat.
     * Logic depends on whether the user is a Mover or Customer, and the current confirmation state.
     */
    private void updateConfirmCardUi(Chat chat) {
        boolean isMover = currentUserId.equals(chat.getMoverId());
        boolean isCustomer = currentUserId.equals(chat.getCustomerId());

        // If user is neither (e.g., admin or error), hide layout
        if (!isMover && !isCustomer) {
            layoutConfirmMove.setVisibility(View.GONE);
            return;
        }

        boolean moverConfirmed = chat.isMoverConfirmed();
        boolean customerConfirmed = chat.isCustomerConfirmed();

        // --- Logic for MOVER ---
        if (isMover) {
            layoutConfirmMove.setVisibility(View.VISIBLE);

            if (!moverConfirmed) {
                // Step 1: Mover hasn't confirmed yet
                tvConfirmStatus.setText("לחץ כדי לאשר שתיאמתם הובלה");
                btnConfirmMove.setText("תיאמתי עם הלקוח");
                btnConfirmMove.setEnabled(true);
            } else if (!customerConfirmed) {
                // Step 2: Mover confirmed, waiting for Customer
                tvConfirmStatus.setText("אישרת ✅ ממתינים לאישור הלקוח...");
                btnConfirmMove.setText("ממתין ללקוח");
                btnConfirmMove.setEnabled(false);
            } else {
                // Step 3: Both confirmed
                tvConfirmStatus.setText("הובלה תואמה ונסגרה ✅");
                btnConfirmMove.setText("סגור");
                btnConfirmMove.setEnabled(false);
            }
        }
        // --- Logic for CUSTOMER ---
        else {
            if (!moverConfirmed) {
                // Step 1: Hide card until Mover initiates confirmation
                layoutConfirmMove.setVisibility(View.GONE);
            } else {
                layoutConfirmMove.setVisibility(View.VISIBLE);

                if (!customerConfirmed) {
                    // Step 2: Mover confirmed, Customer needs to accept
                    tvConfirmStatus.setText("המוביל אישר! אשר/י גם את/ה:");
                    btnConfirmMove.setText("אני מאשר/ת את ההובלה");
                    btnConfirmMove.setEnabled(true);
                } else {
                    // Step 3: Both confirmed
                    tvConfirmStatus.setText("ההובלה תואמה בהצלחה! 🎉");
                    btnConfirmMove.setText("תואם");
                    btnConfirmMove.setEnabled(false);
                }
            }
        }
    }

    private void onConfirmClicked() {
        if (currentChat == null) return;

        boolean isMover = currentUserId.equals(currentChat.getMoverId());
        boolean isCustomer = currentUserId.equals(currentChat.getCustomerId());

        if (isMover) {
            if (currentChat.isMoverConfirmed()) return;
            btnConfirmMove.setEnabled(false); // Prevent double clicks

            // Mover initiates confirmation
            chatViewModel.confirmByMover(chatId);

        } else if (isCustomer) {
            if (!currentChat.isMoverConfirmed()) {
                Toast.makeText(this, "המוביל חייב לאשר קודם", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChat.isCustomerConfirmed()) return;

            btnConfirmMove.setEnabled(false);

            // Customer finalizes confirmation
            chatViewModel.confirmByCustomer(chatId, currentChat.getMoverId(), currentUserId);
        }
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        editInput.setText(""); // Clear input

        // Send message via ViewModel
        chatViewModel.sendMessage(chatId, text, currentUserId, currentUserName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update "Last Seen" timestamp whenever the user enters or returns to the chat
        if (chatId != null) {
            chatViewModel.markAsSeen(chatId);
        }
    }
}