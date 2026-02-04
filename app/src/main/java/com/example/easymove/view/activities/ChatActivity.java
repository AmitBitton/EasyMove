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
import com.example.easymove.model.Message;
import com.example.easymove.model.repository.UserRepository;
import com.example.easymove.viewmodel.ChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String chatId;
    private String currentUserId;
    private String currentUserName;

    private ChatViewModel chatViewModel; // ✅ ViewModel במקום Repositories ישירים

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

        chatId = getIntent().getStringExtra("CHAT_ID");
        if (chatId == null) {
            Toast.makeText(this, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // אתחול ה-ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        currentUserId = chatViewModel.getCurrentUserId();

        if (currentUserId == null) {
            finish();
            return;
        }

        // טעינת שם המשתמש (לצורך שליחת הודעות)
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> {
            currentUserName = name;
        });

        initViews();
        setupRecyclerView();
        observeViewModel(); // ✅ האזנה לשינויים מה-ViewModel

        // התחלת האזנה לצ'אט הספציפי הזה
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
        tvTitle.setText("צ'אט");

        editInput = findViewById(R.id.editMessageInput);
        ImageButton btnSend = findViewById(R.id.btnSendMessage);
        recyclerView = findViewById(R.id.recyclerChat);

        layoutConfirmMove = findViewById(R.id.layoutConfirmMove);
        tvConfirmStatus = findViewById(R.id.tvConfirmStatus);
        btnConfirmMove = findViewById(R.id.btnConfirmMove);

        btnSend.setOnClickListener(v -> sendMessage());
        btnConfirmMove.setOnClickListener(v -> onConfirmClicked());
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void observeViewModel() {
        // 1. האזנה להודעות חדשות
        chatViewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    recyclerView.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // 2. האזנה לשינויים בסטטוס הצ'אט (כותרת, אישורים)
        chatViewModel.getChatMetadata().observe(this, chat -> {
            if (chat != null) {
                currentChat = chat;
                // עדכון הכותרת עם השם של הצד השני
                chat.setCurrentUserId(currentUserId);
                String title = chat.getChatTitle();
                if (title != null && !title.trim().isEmpty()) {
                    tvTitle.setText(title);
                }

                // עדכון כרטיס התיאום
                updateConfirmCardUi(chat);
            }
        });

        // 3. האזנה להודעות טוסט (שגיאות או הצלחות)
        chatViewModel.getToastMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
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
                layoutConfirmMove.setVisibility(View.GONE);
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

        if (isMover) {
            if (currentChat.isMoverConfirmed()) return;
            btnConfirmMove.setEnabled(false);

            // ✅ קריאה ל-ViewModel
            chatViewModel.confirmByMover(chatId);

        } else if (isCustomer) {
            if (!currentChat.isMoverConfirmed()) {
                Toast.makeText(this, "המוביל חייב לאשר קודם", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChat.isCustomerConfirmed()) return;

            btnConfirmMove.setEnabled(false);

            // ✅ קריאה ל-ViewModel שמבצע את כל הבדיקות והאישורים
            chatViewModel.confirmByCustomer(chatId, currentChat.getMoverId(), currentUserId);
        }
    }

    private void sendMessage() {
        String text = editInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        editInput.setText("");

        // ✅ קריאה ל-ViewModel לשליחת הודעה
        chatViewModel.sendMessage(chatId, text, currentUserId, currentUserName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ✅ ברגע שנכנסים (או חוזרים) למסך, מסמנים שראינו
        if (chatId != null) {
            chatViewModel.markAsSeen(chatId);
        }
    }
}