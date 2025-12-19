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

import com.example.easymove.model.Chat;
import com.example.easymove.model.repository.ChatRepository;
import com.example.easymove.model.repository.MoveRepository;
import com.example.easymove.R;
import com.example.easymove.adapters.ChatAdapter;
import com.example.easymove.model.Message;
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

public class ChatActivity extends AppCompatActivity {

    private String chatId;
    private String currentUserId;
    private String currentUserName;

    private FirebaseFirestore db;
    private ChatRepository chatRepository;
    private MoveRepository moveRepository;
    private ChatAdapter adapter;
    private List<Message> messageList;

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
        android.view.View view = getLayoutInflater().inflate(R.layout.activity_chat, null);
        setContentView(view);
        // 1. קבלת נתונים
        chatId = getIntent().getStringExtra("CHAT_ID");
        currentUserId = FirebaseAuth.getInstance().getUid();

        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        chatRepository = new ChatRepository();
        moveRepository = new MoveRepository();
        // טעינת השם שלי (לשליחת הודעות)
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> {
            currentUserName = name;
        });

        initViews();
        setupRecyclerView();
        listenForChatHeader();
        listenForMessages(); // האזנה בזמן אמת
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // כפתור חזור
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTitle = findViewById(R.id.tvChatTitle);
        tvTitle.setText("צ'אט"); // אפשר להעביר גם את שם הצד השני ב-Intent ולשים כאן

        editInput = findViewById(R.id.editMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);
        recyclerView = findViewById(R.id.recyclerChat);

        btnSend.setOnClickListener(v -> sendMessage());
        layoutConfirmMove = findViewById(R.id.layoutConfirmMove);
        tvConfirmStatus = findViewById(R.id.tvConfirmStatus);
        btnConfirmMove = findViewById(R.id.btnConfirmMove);

        btnConfirmMove.setOnClickListener(v -> onConfirmClicked());
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    private void listenForChatHeader() {
        db.collection("chats").document(chatId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot == null || !snapshot.exists()) return;

                    currentChat = snapshot.toObject(Chat.class);
                    if (currentChat == null) return;

                    currentChat.setCurrentUserId(currentUserId);
                    String title = currentChat.getChatTitle();
                    if (title != null && !title.trim().isEmpty()) {
                        tvTitle.setText(title);
                    }

                    updateConfirmCardUi(currentChat);
                });
    }
    private void updateConfirmCardUi(Chat chat) {
        boolean isMover = currentUserId.equals(chat.getMoverId());
        boolean isCustomer = currentUserId.equals(chat.getCustomerId());

        if (!isMover && !isCustomer) {
            layoutConfirmMove.setVisibility(View.GONE);
            return;
        }

        boolean moverConfirmed = chat.isMoverConfirmed();
        boolean customerConfirmed = chat.isCustomerConfirmed();

        // לוגיקה מעודכנת להצגת הכפתור
        if (isMover) {
            layoutConfirmMove.setVisibility(View.VISIBLE);
            if (!moverConfirmed) {
                tvConfirmStatus.setText("לחץ כדי לאשר שתיאמתם הובלה");
                btnConfirmMove.setText("תיאמתי עם הלקוח");
                btnConfirmMove.setEnabled(true);
            } else if (!customerConfirmed) {
                tvConfirmStatus.setText("אישרת ✅ ממתינים לאישור הלקוח...");
                btnConfirmMove.setText("ממתין ללקוח");
                btnConfirmMove.setEnabled(false);
            } else {
                tvConfirmStatus.setText("הובלה תואמה ונסגרה ✅");
                btnConfirmMove.setText("סגור");
                btnConfirmMove.setEnabled(false);
            }
        } else if (isCustomer) {
            if (!moverConfirmed) {
                layoutConfirmMove.setVisibility(View.GONE); // לקוח לא רואה עד שהמוביל מאשר
            } else {
                layoutConfirmMove.setVisibility(View.VISIBLE);
                if (!customerConfirmed) {
                    tvConfirmStatus.setText("המוביל אישר! אשר/י גם את/ה:");
                    btnConfirmMove.setText("אני מאשר/ת את ההובלה");
                    btnConfirmMove.setEnabled(true);
                } else {
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

        // --- פעולת מוביל (ללא שינוי) ---
        if (isMover) {
            if (currentChat.isMoverConfirmed()) return;

            btnConfirmMove.setEnabled(false);
            chatRepository.setMoverConfirmed(chatId)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "אישרת ✅ ממתין ללקוח", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        btnConfirmMove.setEnabled(true);
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        // --- פעולת לקוח (התיקון כאן) ---
        if (isCustomer) {
            if (!currentChat.isMoverConfirmed()) {
                Toast.makeText(this, "המוביל חייב לאשר קודם", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChat.isCustomerConfirmed()) return;

            btnConfirmMove.setEnabled(false);

            // ✅ קריאה לפונקציה החדשה בלי כתובות!
            // המערכת תמצא לבד את ההובלה הפתוחה שלך ותחבר אליה את המוביל
            moveRepository.confirmMoveByCustomer(
                    chatId,
                    currentChat.getMoverId(),
                    currentUserId
            ).addOnSuccessListener(unused -> {
                Toast.makeText(this, "ההובלה תואמה בהצלחה! בדוק את 'ההובלה שלי'", Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                btnConfirmMove.setEnabled(true);
                Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void listenForMessages() {
        // האזנה לתת-קולקשן "messages" בתוך הצ'אט הספציפי
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        for (DocumentChange change : value.getDocumentChanges()) {
                            if (change.getType() == DocumentChange.Type.ADDED) {
                                Message message = change.getDocument().toObject(Message.class);
                                messageList.add(message);
                            }
                        }

                        adapter.setMessages(messageList);
                        // גלילה למטה להודעה האחרונה
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        editInput.setText(""); // ניקוי השדה

        Timestamp now = new Timestamp(new Date());
        Message message = new Message(currentUserId, currentUserName, text, now);

        // 1. הוספת ההודעה לקולקשן ההודעות
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message);

        // 2. עדכון המסמך הראשי של הצ'אט (כדי שיופיע ברשימה הראשית עם ההודעה האחרונה)
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastUpdated", now);
        updates.put("lastSenderId", currentUserId);

        db.collection("chats").document(chatId).update(updates);
    }
}