package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.easymove.R;
import com.example.easymove.model.Chat;
import com.google.firebase.auth.FirebaseAuth; // צריך כדי לדעת מי אני

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.ChatViewHolder> {

    private List<Chat> chats = new ArrayList<>();
    private final OnChatClickListener listener;
    private final String currentUserId; // ✅ שומרים את ה-ID שלי

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatsListAdapter(OnChatClickListener listener) {
        this.listener = listener;
        // שולפים את המשתמש הנוכחי פעם אחת
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);

        // חייבים להגדיר לצ'אט מי אני, כדי שהפונקציות שלו יעבדו נכון
        chat.setCurrentUserId(currentUserId);

        holder.tvName.setText(chat.getChatTitle());
        holder.tvLastMessage.setText(chat.getLastMessageText());

        if (chat.getTimestampLong() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(chat.getTimestampLong()));
        } else {
            holder.tvTime.setText("");
        }

        String imageUrl = chat.getChatImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(imageUrl).circleCrop().into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // ✅ הצגה/הסתרה של הנקודה האדומה לפי הלוגיקה במודל
        if (chat.hasUnreadMessages()) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD); // הדגשה
            holder.tvLastMessage.setTextColor(android.graphics.Color.BLACK);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(android.graphics.Color.parseColor("#757575"));
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvLastMessage, tvTime;
        View unreadBadge; // ✅ הרכיב החדש

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivChatImage);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            unreadBadge = itemView.findViewById(R.id.viewUnreadBadge); // חיבור ל-XML
        }
    }
}