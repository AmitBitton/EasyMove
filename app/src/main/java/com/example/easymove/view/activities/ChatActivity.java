package com.example.easymove.view.activities;

import android.graphics.Color;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.ChatAdapter;
import com.example.easymove.model.Chat;
import com.example.easymove.model.Message;
import com.example.easymove.model.repository.ChatRepository;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.model.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>ChatActivity</b>
 * <p>
 * This Activity handles the real-time messaging interface between a Customer and a Mover.
 * Beyond simple text exchange, this class serves as the control center for finalizing a Move.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 * <li><b>Real-time Messaging:</b> Uses Firebase Firestore listeners to sync messages instantly.</li>
 * <li><b>Move Confirmation Workflow:</b> Handles the logic where the Mover initiates a confirmation,
 * and the Customer accepts it to finalize the booking.</li>
 * <li><b>Dynamic UI:</b> Updates the interface state based on whether the current user is the Mover or Customer.</li>
 * </ul>
 *
 * <h3>Required Intent Extras:</h3>
 * <ul>
 * <li>{@code CHAT_ID}: The unique ID of the Firestore chat document.</li>
 * <li>{@code SOURCE_ADDRESS}: The move's pickup address (for confirmation logic).</li>
 * <li>{@code DEST_ADDRESS}: The move's destination address (for confirmation logic).</li>
 * </ul>
 */
public class ChatActivity extends AppCompatActivity {

    // Region: Data Fields
    private String chatId;
    private String currentUserId;
    private String currentUserName;
    private String sourceAddress;
    private String destAddress;

    // Region: Firebase & Repositories
    private FirebaseFirestore db;
    private ChatRepository chatRepository;
    private MoveRepository moveRepository;

    // Region: UI Components & Adapters
    private ChatAdapter adapter;
    private List<Message> messageList;
    private Chat currentChat; // Holds the current state of the chat metadata

    private EditText editInput;
    private RecyclerView recyclerView;
    private TextView tvTitle;

    // UI for Move Confirmation
    private LinearLayout layoutConfirmMove;
    private TextView tvConfirmStatus;
    private Button btnConfirmMove;

    /**
     * Initializes the Activity, extracts Intent data, and sets up Firestore listeners.
     *
     * @param savedInstanceState Saved state from a previous execution.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.view.View view = getLayoutInflater().inflate(R.layout.activity_chat, null);
        setContentView(view);

        // 1. Retrieve Intent Data
        chatId = getIntent().getStringExtra("CHAT_ID");
        sourceAddress = getIntent().getStringExtra("SOURCE_ADDRESS");
        destAddress = getIntent().getStringExtra("DEST_ADDRESS");

        // 2. Fallback logic for addresses if not provided in Intent
        if (sourceAddress == null) sourceAddress = "Address not specified";
        if (destAddress == null) destAddress = "Address not specified";

        // 3. Authenticate User
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Validate essential data
        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "Error loading chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 4. Initialize Firebase & Repositories
        db = FirebaseFirestore.getInstance();
        chatRepository = new ChatRepository();
        moveRepository = new MoveRepository();

        // 5. Async Fetch: Get current user's name (for message metadata)
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> {
            currentUserName = name;
        });

        // 6. Setup UI and Listeners
        initViews();
        setupRecyclerView();
        listenForChatHeader(); // Listens for changes in move status (Confirmed/Pending)
        listenForMessages();   // Listens for new text messages
    }

    /**
     * initializes UI views, sets up the Toolbar, and attaches click listeners.
     */
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable Back button
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText("Chat"); // Default text, updated later by listenForChatHeader

        editInput = findViewById(R.id.editMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);
        recyclerView = findViewById(R.id.recyclerChat);

        // Send button listener
        btnSend.setOnClickListener(v -> sendMessage());

        // Move Confirmation UI elements
        layoutConfirmMove = findViewById(R.id.layoutConfirmMove);
        tvConfirmStatus = findViewById(R.id.tvConfirmStatus);
        btnConfirmMove = findViewById(R.id.btnConfirmMove);

        // Confirmation logic listener
        btnConfirmMove.setOnClickListener(v -> onConfirmClicked());
    }

    /**
     * Configures the RecyclerView with the ChatAdapter and LinearLayoutManager.
     */
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(currentUserId); // Pass ID to distinguish outgoing/incoming messages
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Listens to the specific Chat Document in Firestore.
     * <p>
     * This is crucial for:
     * 1. Updating the Chat Title (e.g., the other user's name).
     * 2. Monitoring the 'confirmed' boolean flags to update the "Confirm Move" button state.
     * </p>
     */
    private void listenForChatHeader() {
        db.collection("chats").document(chatId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot == null || !snapshot.exists()) return;

                    currentChat = snapshot.toObject(Chat.class);
                    if (currentChat == null) return;

                    // Set context for the chat object to helper logic
                    currentChat.setCurrentUserId(currentUserId);

                    // Update Toolbar Title
                    String title = currentChat.getChatTitle();
                    if (title != null && !title.trim().isEmpty()) {
                        tvTitle.setText(title);
                    }

                    // Update the "Confirm Move" UI block based on status
                    updateConfirmCardUi(currentChat);
                });
    }

    /**
     * Updates the "Confirm Move" UI panel based on the user role and current confirmation state.
     * * <p><b>State Logic:</b></p>
     * <ul>
     * <li><b>Mover:</b> Sees "Confirm" button initially. Once clicked, sees "Waiting for Customer".</li>
     * <li><b>Customer:</b> Panel is hidden until Mover confirms. Then sees "Approve Move".</li>
     * </ul>
     *
     * @param chat The current chat object containing confirmation flags.
     */
    private void updateConfirmCardUi(Chat chat) {
        boolean isMover = currentUserId.equals(chat.getMoverId());
        boolean isCustomer = currentUserId.equals(chat.getCustomerId());

        // Safety check: If user is neither mover nor customer, hide controls
        if (!isMover && !isCustomer) {
            layoutConfirmMove.setVisibility(View.GONE);
            return;
        }

        boolean moverConfirmed = chat.isMoverConfirmed();
        boolean customerConfirmed = chat.isCustomerConfirmed();

        if (isMover) {
            // --- MOVER VIEW ---
            layoutConfirmMove.setVisibility(View.VISIBLE);
            if (!moverConfirmed) {
                // Step 1: Mover hasn't confirmed yet
                tvConfirmStatus.setText("Click to confirm move details");
                btnConfirmMove.setText("Confirm with Customer");
                btnConfirmMove.setEnabled(true);
            } else if (!customerConfirmed) {
                // Step 2: Mover confirmed, waiting for customer
                tvConfirmStatus.setText("Confirmed ✅ Waiting for customer approval...");
                btnConfirmMove.setText("Waiting...");
                btnConfirmMove.setEnabled(false);
            } else {
                // Step 3: Both confirmed
                tvConfirmStatus.setText("Move Confirmed & Closed ✅");
                btnConfirmMove.setText("Closed");
                btnConfirmMove.setEnabled(false);
            }
        } else {
            // --- CUSTOMER VIEW ---
            if (!moverConfirmed) {
                // Customer cannot confirm until Mover initiates
                layoutConfirmMove.setVisibility(View.GONE);
            } else {
                layoutConfirmMove.setVisibility(View.VISIBLE);
                if (!customerConfirmed) {
                    // Mover confirmed, now Customer must approve
                    tvConfirmStatus.setText("Mover confirmed! Please approve:");
                    btnConfirmMove.setText("I Approve this Move");
                    btnConfirmMove.setEnabled(true);
                } else {
                    // Both confirmed
                    tvConfirmStatus.setText("Move Successfully Scheduled! 🎉");
                    btnConfirmMove.setText("Done");
                    btnConfirmMove.setEnabled(false);
                }
            }
        }
    }

    /**
     * Handles the click event for the confirmation button.
     * <p>
     * <b>Mover Logic:</b> Updates Firestore to set {@code moverConfirmed = true}.
     * <b>Customer Logic:</b> Validates Mover status, then calls {@link MoveRepository}
     * to finalize the move using addresses and generating a formal Move record.
     * </p>
     */
    private void onConfirmClicked() {
        if (currentChat == null) return;

        boolean isMover = currentUserId.equals(currentChat.getMoverId());
        boolean isCustomer = currentUserId.equals(currentChat.getCustomerId());

        // Retrieve fresh address data
        String source = getIntent().getStringExtra("SOURCE_ADDRESS");
        String dest = getIntent().getStringExtra("DEST_ADDRESS");
        long date = System.currentTimeMillis(); // Default to 'now'

        // --- SCENARIO 1: MOVER ---
        if (isMover) {
            if (currentChat.isMoverConfirmed()) return; // Already done

            btnConfirmMove.setEnabled(false); // Prevent double clicks

            // Just update the flag in the Chat document
            chatRepository.setMoverConfirmed(chatId)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Confirmed ✅ Waiting for customer", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        btnConfirmMove.setEnabled(true);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // --- SCENARIO 2: CUSTOMER ---
        if (isCustomer) {
            // Guard clause: Mover must confirm first
            if (!currentChat.isMoverConfirmed()) {
                Toast.makeText(this, "Mover must confirm first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChat.isCustomerConfirmed()) return; // Already done

            btnConfirmMove.setEnabled(false);

            // Execute complex business logic: Link Move, Chat, and User data
            moveRepository.confirmMoveByCustomer(
                    chatId,
                    currentChat.getMoverId(),
                    currentUserId,
                    source,
                    dest,
                    date
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Move confirmed successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Close chat and return to previous screen
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Confirmation Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }



    /**
     * Sets up a real-time listener for the "messages" sub-collection in Firestore.
     * <p>
     * - Order: Ascending by timestamp (oldest top, newest bottom).
     * - Action: Appends new messages to the adapter and scrolls the RecyclerView to the bottom.
     * </p>
     */
    private void listenForMessages() {
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        for (DocumentChange change : value.getDocumentChanges()) {
                            // Only handle new messages (avoid reprocessing modified/removed)
                            if (change.getType() == DocumentChange.Type.ADDED) {
                                Message message = change.getDocument().toObject(Message.class);
                                messageList.add(message);
                            }
                        }

                        adapter.setMessages(messageList);

                        // Auto-scroll to the newest message
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    /**
     * Sends a text message to the chat.
     * <p>
     * Performs a two-step atomic-like update:
     * 1. Adds the {@link Message} object to the 'messages' sub-collection.
     * 2. Updates the parent 'Chat' document with {@code lastMessage}, {@code lastUpdated},
     * and {@code lastSenderId} to support the "Inbox" list view.
     * </p>
     */
    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        editInput.setText(""); // Clear input field

        Timestamp now = new Timestamp(new Date());
        Message message = new Message(currentUserId, currentUserName, text, now);

        // 1. Add message to sub-collection
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message);

        // 2. Update Chat metadata (for inbox preview)
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastUpdated", now);
        updates.put("lastSenderId", currentUserId);

        db.collection("chats").document(chatId).update(updates);
    }
}