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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ChatsListAdapter
 * ----------------
 * RecyclerView.Adapter responsible for displaying a list of chats
 * (similar to a "conversations list" in messaging apps).
 *
 * Each item represents a single Chat and shows:
 * - Chat title (user name / group name)
 * - Last message preview
 * - Timestamp of the last message
 * - Chat image (profile or group image)
 *
 * The adapter also handles click events via the OnChatClickListener
 * interface to notify the hosting Fragment/Activity.
 */
public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.ChatViewHolder> {

    /**
     * List of Chat objects displayed in the RecyclerView.
     * Initialized to an empty list to avoid null handling.
     */
    private List<Chat> chats = new ArrayList<>();

    /**
     * Listener used to notify when a chat item is clicked.
     * Implemented by the hosting Fragment or Activity.
     */
    private final OnChatClickListener listener;

    /**
     * Callback interface for handling chat click events.
     *
     * This keeps the adapter decoupled from UI navigation logic.
     */
    public interface OnChatClickListener {

        /**
         * Called when a chat item is clicked.
         *
         * @param chat the Chat object that was clicked
         */
        void onChatClick(Chat chat);
    }

    /**
     * Constructor for ChatsListAdapter.
     *
     * @param listener listener that handles chat item click events
     */
    public ChatsListAdapter(OnChatClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of chats and refreshes the RecyclerView.
     *
     * Typically called after fetching chats from Firestore or another backend.
     *
     * @param chats the new list of Chat objects
     */
    public void setChats(List<Chat> chats) {
        this.chats = chats;
        notifyDataSetChanged(); // Forces RecyclerView to rebind all items
    }

    /**
     * Inflates the chat list item layout and creates a ViewHolder.
     *
     * @param parent the parent ViewGroup
     * @param viewType view type (not used here since all items share one layout)
     * @return a new ChatViewHolder instance
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);

        return new ChatViewHolder(view);
    }

    /**
     * Binds chat data to the ViewHolder at the given position.
     *
     * This method:
     * - Sets the chat title using logic from Chat.java
     * - Displays the last message text
     * - Formats and displays the last message timestamp
     * - Loads the chat image using Glide
     * - Handles click events for the chat item
     *
     * @param holder the ViewHolder to bind data to
     * @param position the position of the chat in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {

        // Get the Chat object for this position
        Chat chat = chats.get(position);

        // Use Chat.java logic to decide which title to display (user or group name)
        holder.tvName.setText(chat.getChatTitle());

        // Display last message preview
        holder.tvLastMessage.setText(chat.getLastMessageText());

        // Format and display timestamp of the last message
        if (chat.getTimestampLong() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(chat.getTimestampLong()));
        } else {
            // No timestamp available
            holder.tvTime.setText("");
        }

        // Load chat image (profile picture or group image)
        String imageUrl = chat.getChatImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {

            // Load image from URL and crop it into a circle
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .circleCrop()
                    .into(holder.ivImage);

        } else {
            // Fallback image when no image URL is provided
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Notify listener when the chat item is clicked
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    /**
     * Returns the total number of chat items.
     *
     * @return size of the chats list
     */
    @Override
    public int getItemCount() {
        return chats.size();
    }

    /**
     * ChatViewHolder
     * --------------
     * Holds references to views inside a single chat list item.
     *
     * Improves performance by avoiding repeated findViewById calls.
     */
    static class ChatViewHolder extends RecyclerView.ViewHolder {

        // Chat image (profile / group)
        ImageView ivImage;

        // Chat title, last message preview, and timestamp
        TextView tvName, tvLastMessage, tvTime;

        /**
         * Constructor for ChatViewHolder.
         *
         * Finds and caches all required views from the item layout.
         *
         * @param itemView the inflated chat list item view
         */
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);

            ivImage = itemView.findViewById(R.id.ivChatImage);
            tvName = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvChatTime);
        }
    }
}
