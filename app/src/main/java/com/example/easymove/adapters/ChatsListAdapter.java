package com.example.easymove.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
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
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for displaying the list of active chats (conversations) in the "My Chats" screen.
 * Handles displaying user avatars, last messages, timestamps, and unread indicators.
 */
public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.ChatViewHolder> {

    // Optimization: Create formatter once.
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
    // Optimization: Define colors once.
    private static final int COLOR_UNREAD = Color.BLACK;
    private static final int COLOR_READ = Color.parseColor("#757575");

    private List<Chat> chats = new ArrayList<>();
    private final OnChatClickListener listener;
    private final String currentUserId;

    /**
     * Interface for handling chat item clicks.
     */
    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    /**
     * Constructor.
     *
     * @param listener The listener to handle clicks on chat items.
     */
    public ChatsListAdapter(OnChatClickListener listener) {
        this.listener = listener;
        // Fetch current user ID once during initialization
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    /**
     * Updates the list of chats.
     *
     * @param chats The new list of chats.
     */
    public void setChats(List<Chat> chats) {
        this.chats = Objects.requireNonNullElseGet(chats, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);

        // Inject the current user ID into the model so it can determine logic like "unread messages"
        chat.setCurrentUserId(currentUserId);

        holder.tvName.setText(chat.getChatTitle());
        holder.tvLastMessage.setText(chat.getLastMessageText());

        // Handle Timestamp
        if (chat.getTimestampLong() > 0) {
            holder.tvTime.setText(DATE_FORMAT.format(chat.getTimestampLong()));
        } else {
            holder.tvTime.setText("");
        }

        // Handle Image Loading
        String imageUrl = chat.getChatImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .circleCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Handle Unread Indicator Logic
        if (chat.hasUnreadMessages()) {
            holder.unreadBadge.setVisibility(View.VISIBLE);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvLastMessage.setTextColor(COLOR_UNREAD);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMessage.setTextColor(COLOR_READ);
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    /**
     * ViewHolder for Chat items.
     */
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvLastMessage, tvTime;
        View unreadBadge;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivChatImage);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
            unreadBadge = itemView.findViewById(R.id.viewUnreadBadge);
        }
    }
}