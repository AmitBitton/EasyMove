package com.example.easymove.adapters; // חבילת אדפטרים

import android.view.LayoutInflater; // inflate של XML לפריט
import android.view.View; // רכיב UI בסיסי
import android.view.ViewGroup; // הורה לפריטים
import android.widget.ImageView; // להצגת תמונת פרופיל
import android.widget.TextView; // להצגת טקסטים
import androidx.annotation.NonNull; // לא null
import androidx.recyclerview.widget.RecyclerView; // אדפטר לרשימות
import com.bumptech.glide.Glide; // ספרייה לטעינת תמונות מ-URL בצורה יעילה
import com.example.easymove.R; // משאבים
import com.example.easymove.model.Chat; // מודל צ'אט
import java.text.SimpleDateFormat; // פורמט זמן
import java.util.ArrayList; // רשימה
import java.util.List; // ממשק רשימה
import java.util.Locale; // לוקאל

public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.ChatViewHolder> {
    // אדפטר שמציג "רשימת צ'אטים" (מסך ChatsFragment)

    private List<Chat> chats = new ArrayList<>();
    // רשימת הצ'אטים המוצגים במסך

    private final OnChatClickListener listener;
    // מאזין ללחיצה על צ'אט כדי לפתוח ChatActivity

    public interface OnChatClickListener {
        // ממשק (interface) שמאפשר ל-Fragment להגיב ללחיצה על פריט
        void onChatClick(Chat chat); // מקבלת את הצ'אט שנלחץ
    }

    public ChatsListAdapter(OnChatClickListener listener) {
        // בנאי: מקבל listener ושומר אותו
        this.listener = listener;
    }

    public void setChats(List<Chat> chats) {
        // עדכון רשימת הצ'אטים באדפטר
        // קלט: chats = רשימת צ'אטים חדשה
        // פלט: אין, אבל מעדכן UI
        this.chats = chats;
        notifyDataSetChanged(); // רענון הרשימה במסך
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // יצירת View חדש עבור שורה ברשימת הצ'אטים
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // קישור נתונים לתצוגה עבור צ'אט ספציפי
        Chat chat = chats.get(position);

        // כאן משתמשים בלוגיקה החכמה של Chat.java שמחליטה איזה שם להציג
        holder.tvName.setText(chat.getChatTitle());
        // שם שמוצג הוא "הצד השני" לפי currentUserId בתוך Chat

        holder.tvLastMessage.setText(chat.getLastMessageText());
        // מציגים הודעה אחרונה (או ריק אם אין)

        // פורמט תאריך
        if (chat.getTimestampLong() > 0) {
            // אם יש זמן עדכון אחרון
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(chat.getTimestampLong()));
            // timestampLong הוא מילישניות -> SimpleDateFormat יפרש נכון
        } else {
            holder.tvTime.setText(""); // אין תאריך להצגה
        }

        // תמונה
        String imageUrl = chat.getChatImageUrl();
        // גם כאן, הלוגיקה ב-Chat תחליט "התמונה של מי להציג" לפי currentUserId
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(imageUrl).circleCrop().into(holder.ivImage);
            // Glide מוריד תמונה מ-URL, חותך לעיגול, ושם בתוך ImageView
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
            // אם אין תמונה, מציגים אייקון ברירת מחדל
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
        // בלחיצה על כל השורה מפעילים את הקולבק כדי לפתוח את מסך השיחה
    }

    @Override
    public int getItemCount() {
        // מספר הצ'אטים ברשימה
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        // מחזיק את רכיבי ה-UI של שורת צ'אט אחת
        ImageView ivImage; // תמונת פרופיל
        TextView tvName, tvLastMessage, tvTime; // שם הצד השני, הודעה אחרונה, זמן

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivChatImage); // תמונה
            tvName = itemView.findViewById(R.id.tvChatName); // שם
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage); // הודעה אחרונה
            tvTime = itemView.findViewById(R.id.tvChatTime); // זמן
        }
    }
}
