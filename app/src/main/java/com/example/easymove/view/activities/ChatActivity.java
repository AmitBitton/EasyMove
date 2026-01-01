package com.example.easymove.view.activities; // Activities של האפליקציה

import android.graphics.Color; // (לא בשימוש בקוד הזה בפועל)
import android.os.Bundle; // Bundle לנתונים ב-onCreate
import android.text.TextUtils; // בדיקות טקסט (ריק/לא ריק)
import android.view.View; // View בסיסי
import android.widget.Button; // כפתור
import android.widget.EditText; // שדה קלט
import android.widget.ImageButton; // כפתור תמונה (שליחה)
import android.widget.LinearLayout; // Layout
import android.widget.TextView; // טקסט
import android.widget.Toast; // הודעות קצרות למשתמשת

import androidx.appcompat.app.AppCompatActivity; // Activity תואם AppCompat
import androidx.appcompat.widget.Toolbar; // Toolbar
import androidx.recyclerview.widget.LinearLayoutManager; // LayoutManager לרשימה
import androidx.recyclerview.widget.RecyclerView; // רשימת הודעות

import com.example.easymove.model.Chat; // מודל צ'אט
import com.example.easymove.model.repository.ChatRepository; // פעולות על chats במסד
import com.example.easymove.model.repository.MoveRepository; // פעולות על הובלות (confirmMoveByCustomer)
import com.example.easymove.R; // משאבים
import com.example.easymove.adapters.ChatAdapter; // אדפטר הודעות
import com.example.easymove.model.Message; // מודל הודעה
import com.example.easymove.model.repository.UserRepository; // להביא שם משתמש
import com.google.firebase.Timestamp; // זמן
import com.google.firebase.auth.FirebaseAuth; // אימות כדי לקבל UID
import com.google.firebase.firestore.DocumentChange; // שינוי במסמכים בזמן אמת
import com.google.firebase.firestore.FirebaseFirestore; // Firestore
import com.google.firebase.firestore.Query; // Query ל-orderBy

import java.util.ArrayList; // רשימה
import java.util.Date; // תאריך/שעה
import java.util.HashMap; // מפה לעדכון שדות
import java.util.List; // ממשק רשימה
import java.util.Map; // ממשק מפה

public class ChatActivity extends AppCompatActivity {

    private String chatId; // מזהה הצ'אט (מסמך chats/{chatId})
    private String currentUserId; // ה-UID של המשתמשת המחוברת
    private String currentUserName; // שם המשתמשת (לשדה senderName בהודעות)

    private FirebaseFirestore db; // גישה למסד
    private ChatRepository chatRepository; // פעולות קשורות לצ'אט (אישור מוביל/לקוחה)
    private MoveRepository moveRepository; // פעולות קשורות להובלה (תיאום)
    private ChatAdapter adapter; // אדפטר להצגת הודעות
    private List<Message> messageList; // רשימה מקומית של הודעות

    private EditText editInput; // שדה הקלט להודעה
    private RecyclerView recyclerView; // הרשימה של ההודעות
    private TextView tvTitle; // כותרת הצ'אט (שם הצד השני)
    private LinearLayout layoutConfirmMove; // כרטיס תיאום הובלה
    private TextView tvConfirmStatus; // טקסט מצב בכרטיס התיאום
    private Button btnConfirmMove; // כפתור "תיאמתי/אישרתי"

    private Chat currentChat; // אובייקט צ'אט שנשלף בזמן אמת כדי לדעת מצב אישורים ושמות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // נקודת כניסה למסך הצ'אט
        super.onCreate(savedInstanceState);

        android.view.View view = getLayoutInflater().inflate(R.layout.activity_chat, null);
        // מנפחים את layout של activity_chat
        setContentView(view); // מציגים את המסך

        // 1. קבלת נתונים
        chatId = getIntent().getStringExtra("CHAT_ID");
        // מקבלים את מזהה הצ'אט שנשלח מהמסך הקודם (ChatsFragment)

        currentUserId = FirebaseAuth.getInstance().getUid();
        // ה-UID של המשתמשת המחוברת

        if (chatId == null || currentUserId == null) {
            // אם חסר chatId או אין משתמשת מחוברת
            Toast.makeText(this, "שגיאה בטעינת הצ'אט", Toast.LENGTH_SHORT).show();
            finish(); // סוגרים מסך כדי לא להמשיך במצב תקול
            return;
        }

        db = FirebaseFirestore.getInstance(); // מופע Firestore
        chatRepository = new ChatRepository(); // ריפו צ'אטים
        moveRepository = new MoveRepository(); // ריפו הובלות

        // טעינת השם שלי (לשליחת הודעות)
        new UserRepository().getUserNameById(currentUserId).addOnSuccessListener(name -> {
            // אסינכרוני: כשמגיע השם, שומרים אותו
            currentUserName = name;
        });

        initViews(); // חיבור רכיבי UI והגדרת מאזינים
        setupRecyclerView(); // הגדרת רשימת ההודעות והאדפטר
        listenForChatHeader(); // מאזין למסמך הצ'אט כדי לעדכן כותרת ומצב תיאום
        listenForMessages(); // האזנה בזמן אמת להודעות בתת-קולקשן messages
    }

    private void initViews() {
        // מאתחלת את כל רכיבי ה-UI ומגדירה מאזינים לכפתורים

        Toolbar toolbar = findViewById(R.id.toolbarChat); // איתור toolbar
        setSupportActionBar(toolbar); // קביעה כ-toolbar של activity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // הצגת חץ חזרה
        toolbar.setNavigationOnClickListener(v -> finish()); // בלחיצה על חזרה -> סוגרים activity

        tvTitle = findViewById(R.id.tvChatTitle); // כותרת המסך
        tvTitle.setText("צ'אט"); // ערך ברירת מחדל עד שטוענים שם צד שני

        editInput = findViewById(R.id.editMessageInput); // שדה הקלט
        ImageButton btnSend = findViewById(R.id.btnSendMessage); // כפתור שליחה
        recyclerView = findViewById(R.id.recyclerChat); // RecyclerView הודעות

        btnSend.setOnClickListener(v -> sendMessage()); // בלחיצה שולחים הודעה

        layoutConfirmMove = findViewById(R.id.layoutConfirmMove); // אזור תיאום
        tvConfirmStatus = findViewById(R.id.tvConfirmStatus); // טקסט מצב
        btnConfirmMove = findViewById(R.id.btnConfirmMove); // כפתור אישור

        btnConfirmMove.setOnClickListener(v -> onConfirmClicked());
        // בלחיצה - מפעילה לוגיקה שונה ללקוחה/מוביל
    }

    private void setupRecyclerView() {
        // מאתחלת את רשימת ההודעות והאדפטר

        messageList = new ArrayList<>(); // יצירת רשימה ריקה
        adapter = new ChatAdapter(currentUserId); // אדפטר עם UID כדי להבדיל הודעה שלי/אחר
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // סידור אנכי רגיל
        recyclerView.setAdapter(adapter); // חיבור האדפטר ל-RecyclerView
    }

    private void listenForChatHeader() {
        // מאזין למסמך הראשי של הצ'אט (chats/{chatId}) כדי לעדכן:
        // 1) את הכותרת (שם הצד השני)
        // 2) את כרטיס התיאום (moverConfirmed/customerConfirmed)

        db.collection("chats").document(chatId)
                .addSnapshotListener((snapshot, error) -> {
                    // snapshotListener: מופעל בכל שינוי במסמך בזמן אמת
                    if (error != null) return; // אם יש שגיאה - יוצאים
                    if (snapshot == null || !snapshot.exists()) return; // אם אין מסמך - יוצאים

                    currentChat = snapshot.toObject(Chat.class); // המרה לאובייקט Chat
                    if (currentChat == null) return; // הגנה מ-null

                    currentChat.setCurrentUserId(currentUserId);
                    // חשוב: כדי ש-Chat.getChatTitle() ידע להחזיר את שם הצד השני

                    String title = currentChat.getChatTitle(); // שם צד שני
                    if (title != null && !title.trim().isEmpty()) {
                        tvTitle.setText(title); // עדכון הכותרת
                    }

                    updateConfirmCardUi(currentChat); // עדכון UI של כרטיס התיאום לפי מצב במסד
                });
    }

    private void updateConfirmCardUi(Chat chat) {
        // מעדכנת את כרטיס התיאום לפי:
        // האם המשתמשת היא מובילה או לקוחה
        // והאם כבר בוצעו אישורים

        boolean isMover = currentUserId.equals(chat.getMoverId()); // האם אני מובילה
        boolean isCustomer = currentUserId.equals(chat.getCustomerId()); // האם אני לקוחה

        if (!isMover && !isCustomer) {
            // אם אני לא אחת מהמשתתפות הרלוונטיות (מקרה חריג)
            layoutConfirmMove.setVisibility(View.GONE); // מסתירים כרטיס
            return;
        }

        boolean moverConfirmed = chat.isMoverConfirmed(); // האם המובילה אישרה
        boolean customerConfirmed = chat.isCustomerConfirmed(); // האם הלקוחה אישרה

        // לוגיקה מעודכנת להצגת הכפתור
        if (isMover) {
            // --- מצב מובילה ---
            layoutConfirmMove.setVisibility(View.VISIBLE); // מובילה תמיד רואה כרטיס

            if (!moverConfirmed) {
                // עוד לא אישרה
                tvConfirmStatus.setText("לחץ כדי לאשר שתיאמתם הובלה");
                btnConfirmMove.setText("תיאמתי עם הלקוח");
                btnConfirmMove.setEnabled(true);
            } else if (!customerConfirmed) {
                // המובילה אישרה, מחכים ללקוחה
                tvConfirmStatus.setText("אישרת ✅ ממתינים לאישור הלקוח...");
                btnConfirmMove.setText("ממתין ללקוח");
                btnConfirmMove.setEnabled(false);
            } else {
                // שני הצדדים אישרו
                tvConfirmStatus.setText("הובלה תואמה ונסגרה ✅");
                btnConfirmMove.setText("סגור");
                btnConfirmMove.setEnabled(false);
            }

        } else if (isCustomer) {
            // --- מצב לקוחה ---
            if (!moverConfirmed) {
                // לקוחה לא רואה עד שהמובילה מאשרת
                layoutConfirmMove.setVisibility(View.GONE);
            } else {
                // אחרי שהמובילה אישרה - הלקוחה רואה ויכולה לאשר
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
        // מופעלת בלחיצה על כפתור התיאום
        // מבצעת פעולה אחרת לפי תפקיד המשתמשת (מובילה/לקוחה)

        if (currentChat == null) return; // אם עדיין לא נטען הצ'אט - לא עושים כלום

        boolean isMover = currentUserId.equals(currentChat.getMoverId()); // בדיקת תפקיד
        boolean isCustomer = currentUserId.equals(currentChat.getCustomerId()); // בדיקת תפקיד

        // --- פעולת מובילה ---
        if (isMover) {
            if (currentChat.isMoverConfirmed()) return; // אם כבר אישרה - אין מה לעשות

            btnConfirmMove.setEnabled(false); // סוגרים כפתור כדי למנוע לחיצות כפולות

            chatRepository.setMoverConfirmed(chatId)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "אישרת ✅ ממתין ללקוח", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        btnConfirmMove.setEnabled(true); // מחזירים כפתור אם נכשל
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return; // חשוב: לא ממשיכים לחלק של הלקוחה
        }

        // --- פעולת לקוחה ---
        if (isCustomer) {
            if (!currentChat.isMoverConfirmed()) {
                // לא ניתן לאשר לפני שהמובילה אישרה
                Toast.makeText(this, "המוביל חייב לאשר קודם", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentChat.isCustomerConfirmed()) return; // אם כבר אישרה - אין מה לעשות

            btnConfirmMove.setEnabled(false); // מניעת לחיצות כפולות

            // קריאה לפונקציה שמבצעת תיאום הובלה בפועל (ברמת ה-Move)
            moveRepository.confirmMoveByCustomer(
                    chatId, // מזהה הצ'אט ששייך לתיאום
                    currentChat.getMoverId(), // המובילה/מוביל
                    currentUserId // הלקוחה (אני)
            ).addOnSuccessListener(unused -> {
                Toast.makeText(this, "ההובלה תואמה בהצלחה! בדוק את 'ההובלה שלי'", Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                btnConfirmMove.setEnabled(true); // מחזירים כפתור אם נכשל
                Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void listenForMessages() {
        // מאזין לתת-קולקשן messages בתוך הצ'אט:
        // chats/{chatId}/messages
        // ומביא הודעות לפי סדר זמן עולה

        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    // listener בזמן אמת: מופעל בכל שינוי בהודעות
                    if (error != null) return; // אם יש שגיאה - יוצאים

                    if (value != null) {
                        // עוברים על השינויים (DocumentChange) כדי לדעת מה נוסף/שונה/נמחק
                        for (DocumentChange change : value.getDocumentChanges()) {
                            if (change.getType() == DocumentChange.Type.ADDED) {
                                // אנחנו מטפלות רק בהודעות חדשות שנוספו
                                Message message = change.getDocument().toObject(Message.class);
                                messageList.add(message); // מוסיפים לרשימה המקומית
                            }
                        }

                        adapter.setMessages(messageList); // מעדכנים אדפטר ורענון UI

                        // גלילה למטה להודעה האחרונה
                        if (!messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void sendMessage() {
        // שולחת הודעה חדשה למסד ומעדכנת את מסמך הצ'אט הראשי

        String text = editInput.getText().toString().trim(); // קריאת הטקסט מהשדה
        if (TextUtils.isEmpty(text)) return; // אם ריק - לא שולחים

        editInput.setText(""); // ניקוי השדה אחרי שליחה

        Timestamp now = new Timestamp(new Date()); // זמן שליחה עכשיו
        Message message = new Message(currentUserId, currentUserName, text, now);
        // יצירת אובייקט Message לשמירה במסד

        // 1. הוספת ההודעה לקולקשן ההודעות
        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message);
        // add יוצר מסמך חדש אוטומטית ומוסיף את ההודעה

        // 2. עדכון המסמך הראשי של הצ'אט (כדי שיופיע ברשימה הראשית עם ההודעה האחרונה)
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text); // ההודעה האחרונה להצגה במסך הצ'אטים
        updates.put("lastUpdated", now); // זמן עדכון אחרון
        updates.put("lastSenderId", currentUserId); // מי שלח את ההודעה האחרונה

        db.collection("chats").document(chatId).update(updates);
        // update למסמך הראשי כדי שרשימת הצ'אטים תתעדכן
    }
}
