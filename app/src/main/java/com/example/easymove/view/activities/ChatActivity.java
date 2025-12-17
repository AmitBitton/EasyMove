package com.example.easymove.view.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private ChatAdapter adapter;
    private List<Message> messageList;

    private EditText editInput;
    private RecyclerView recyclerView;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. קבלת נתונים
        chatId = getIntent().getStringExtra("CHAT_ID");
        currentUserId = FirebaseAuth.getInstance().getUid();

        if (chatId == null || currentUserId == null) {
            Toast.makeText(this, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // טעינת השם שלי (לשליחת הודעות)
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> {
            currentUserName = name;
        });

        initViews();
        setupRecyclerView();
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
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
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