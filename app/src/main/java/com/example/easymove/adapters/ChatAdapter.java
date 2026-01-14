package com.example.easymove.adapters; // חבילת (package) האדפטרים באפליקציה

import android.view.LayoutInflater; // מאפשר "לנפח" (inflate) קבצי XML של פריטים לרכיבי View
import android.view.View; // מחלקת בסיס לכל רכיב UI
import android.view.ViewGroup; // קונטיינר של Views (כמו RecyclerView עצמו)
import android.widget.LinearLayout; // Layout אופקי/אנכי
import android.widget.TextView; // טקסט במסך
import androidx.annotation.NonNull; // אנוטציה שמבטיחה שלא נשלח null
import androidx.recyclerview.widget.RecyclerView; // RecyclerView + Adapter + ViewHolder
import com.example.easymove.R; // גישה למשאבים (layouts, ids וכו')
import com.example.easymove.model.Message; // מודל הודעה (senderId, senderName, text, timestamp)
import java.text.SimpleDateFormat; // פורמט זמן להצגה
import java.util.ArrayList; // רשימה דינמית
import java.util.List; // ממשק לרשימה
import java.util.Locale; // שפה/אזור לפורמט תאריך/שעה

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    // אדפטר שמחבר בין רשימת הודעות (data) לבין תצוגה ב-RecyclerView (UI)

    private List<Message> messages = new ArrayList<>();
    // רשימת ההודעות שמוצגות במסך הצ'אט (מתעדכנת מה-Firestore בזמן אמת)

    private final String currentUserId;
    // מזהה המשתמש הנוכחי (מי שמחובר), כדי לדעת להציג "הודעה שלי" מול "של הצד השני"

    public ChatAdapter(String currentUserId) {
        // בנאי: מקבל את ה-UID של המשתמש הנוכחי ושומר אותו באדפטר
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        // פונקציה שמעדכנת את רשימת ההודעות שהאדפטר מציג
        // קלט: messages = רשימת הודעות חדשה
        // פלט: אין (void), אבל מעדכן UI בעזרת notifyDataSetChanged
        this.messages = messages; // החלפה של הדאטה באדפטר
        notifyDataSetChanged(); // אומר ל-RecyclerView "הדאטה השתנה, תצייר מחדש"
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // נקראת כשה-RecyclerView צריך ליצור View חדש לפריט ברשימה
        // קלט: parent = ה-RecyclerView שמכיל את הפריטים, viewType = סוג פריט (לא בשימוש כאן)
        // פלט: ViewHolder שמחזיק את ה-View של פריט הודעה
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        // inflate של ה-XML item_message לקובץ View אמיתי
        return new MessageViewHolder(view); // יצירת ViewHolder שמחזיק הפניות ל-Views שבתוך item_message
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // נקראת עבור כל שורה כדי "לקשור" (bind) נתונים ל-Views
        // קלט: holder = המחזיק של ה-Views, position = מיקום הפריט ברשימה
        // פלט: אין (void), אבל מעדכן את הטקסט/נראות לפי ההודעה
        Message message = messages.get(position); // שליפת ההודעה המתאימה למיקום

        // פורמט לשעה (למשל 14:30)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        // יצירת פורמט של שעה:דקה בהתאם ללוקאל של המכשיר
        String time = message.getTimestamp() != null ? sdf.format(message.getTimestamp().toDate()) : "";
        // אם יש timestamp -> ממירים ל-Date ואז לפורמט HH:mm
        // אם אין timestamp -> מציגים מחרוזת ריקה כדי לא לקרוס

        // בדיקה מי שלח את ההודעה
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            // --- הודעה שלי ---
            holder.layoutMy.setVisibility(View.VISIBLE); // מציגים את הבועה של "שלי"
            holder.layoutOther.setVisibility(View.GONE); // מסתירים את הבועה של "הצד השני"

            holder.tvMessageMy.setText(message.getText()); // מציגים את טקסט ההודעה שלי
            holder.tvTimeMy.setText(time); // מציגים את השעה ליד ההודעה שלי
        } else {
            // --- הודעה של הצד השני ---
            holder.layoutMy.setVisibility(View.GONE); // מסתירים את הבועה של "שלי"
            holder.layoutOther.setVisibility(View.VISIBLE); // מציגים את הבועה של "הצד השני"

            holder.tvMessageOther.setText(message.getText()); // מציגים את טקסט ההודעה של הצד השני
            holder.tvTimeOther.setText(time); // מציגים את השעה ליד ההודעה שלו/שלה
            holder.tvNameOther.setText(message.getSenderName());
            // מציגים שם שולח (כדי שיהיה ברור מי כתב, בעיקר אם בעתיד יהיה יותר ממשתתף)
        }
    }

    @Override
    public int getItemCount() {
        // מחזירה כמה פריטים יש לאדפטר (כמה הודעות יוצגו)
        // קלט: אין
        // פלט: מספר ההודעות ברשימה
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        // ViewHolder: מחזיק הפניות ל-Views בתוך item_message כדי לא לחפש אותם כל פעם מחדש (יותר יעיל)

        LinearLayout layoutMy, layoutOther;
        // שני layouts שונים: אחד להודעה שלי ואחד להודעה של הצד השני

        TextView tvMessageMy, tvTimeMy;
        // הטקסט והשעה של ההודעה שלי

        TextView tvMessageOther, tvTimeOther, tvNameOther;
        // הטקסט, השעה, ושם השולח של ההודעה של הצד השני

        public MessageViewHolder(@NonNull View itemView) {
            // בנאי שמקבל את ה-View של הפריט ומאתר בתוכו את כל רכיבי ה-UI לפי id
            super(itemView);

            layoutMy = itemView.findViewById(R.id.layoutMessageMy);
            // מצביע ל-layout של ההודעה שלי
            layoutOther = itemView.findViewById(R.id.layoutMessageOther);
            // מצביע ל-layout של ההודעה של הצד השני

            tvMessageMy = itemView.findViewById(R.id.tvMessageMy);
            // הטקסט של ההודעה שלי
            tvTimeMy = itemView.findViewById(R.id.tvTimeMy);
            // השעה של ההודעה שלי

            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            // הטקסט של ההודעה של הצד השני
            tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
            // השעה של ההודעה של הצד השני
            tvNameOther = itemView.findViewById(R.id.tvNameOther);
            // שם השולח של הצד השני
        }
    }
}
