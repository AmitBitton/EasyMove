package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ChatAdapter
 * -----------
 * RecyclerView.Adapter responsible for displaying chat messages in a conversation.
 *
 * This adapter:
 * - Holds a list of Message objects (the chat history)
 * - Differentiates between messages sent by the current user and other users
 * - Inflates a single message layout that contains two sub-layouts:
 *   one for "my messages" and one for "other messages"
 *
 * Depending on the sender, it toggles visibility between these layouts.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    /**
     * List containing all chat messages to be displayed.
     * Initialized as an empty list to avoid null checks.
     */
    private List<Message> messages = new ArrayList<>();

    /**
     * ID of the currently logged-in user.
     * Used to determine whether a message was sent by "me" or by someone else.
     */
    private final String currentUserId;

    /**
     * Constructor for ChatAdapter.
     *
     * @param currentUserId the unique identifier of the current user
     */
    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Updates the adapter's message list and refreshes the RecyclerView.
     *
     * Typically called after fetching messages from Firestore or another data source.
     *
     * @param messages the new list of Message objects
     */
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged(); // Notifies RecyclerView to redraw all items
    }

    /**
     * Creates a new ViewHolder when the RecyclerView needs one.
     *
     * This inflates the message item layout (item_message.xml)
     * and wraps it in a MessageViewHolder.
     *
     * @param parent the parent ViewGroup
     * @param viewType the view type (not used here since we use one layout)
     * @return a new MessageViewHolder instance
     */
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);

        return new MessageViewHolder(view);
    }

    /**
     * Binds data to a ViewHolder at a specific position.
     *
     * This method:
     * - Retrieves the message at the given position
     * - Formats the message timestamp
     * - Checks whether the message was sent by the current user
     * - Displays the appropriate layout (my message vs other message)
     *
     * @param holder the ViewHolder to bind data to
     * @param position the position of the message in the list
     */
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        // Get the message corresponding to this position
        Message message = messages.get(position);

        // Create a formatter for displaying time (e.g., 14:30)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Convert Firestore timestamp to formatted time string
        String time = message.getTimestamp() != null
                ? sdf.format(message.getTimestamp().toDate())
                : "";

        // Check if the message was sent by the current user
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {

            // ---------- My Message ----------
            // Show my message layout and hide the other user's layout
            holder.layoutMy.setVisibility(View.VISIBLE);
            holder.layoutOther.setVisibility(View.GONE);

            // Set message text and timestamp
            holder.tvMessageMy.setText(message.getText());
            holder.tvTimeMy.setText(time);

        } else {

            // ---------- Other User's Message ----------
            // Hide my message layout and show the other user's layout
            holder.layoutMy.setVisibility(View.GONE);
            holder.layoutOther.setVisibility(View.VISIBLE);

            // Set message text, timestamp, and sender's name
            holder.tvMessageOther.setText(message.getText());
            holder.tvTimeOther.setText(time);
            holder.tvNameOther.setText(message.getSenderName());
        }
    }

    /**
     * Returns the total number of messages.
     *
     * Used by RecyclerView to know how many items to display.
     *
     * @return number of messages in the list
     */
    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * MessageViewHolder
     * -----------------
     * Holds references to all views inside a single chat message item.
     *
     * Using a ViewHolder improves performance by avoiding repeated calls
     * to findViewById during scrolling.
     */
    static class MessageViewHolder extends RecyclerView.ViewHolder {

        // Layout containers for my message and other user's message
        LinearLayout layoutMy, layoutOther;

        // Views for my message
        TextView tvMessageMy, tvTimeMy;

        // Views for other user's message
        TextView tvMessageOther, tvTimeOther, tvNameOther;

        /**
         * Constructor for MessageViewHolder.
         *
         * Finds and stores references to all required views from the item layout.
         *
         * @param itemView the inflated message item view
         */
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            // Root layouts
            layoutMy = itemView.findViewById(R.id.layoutMessageMy);
            layoutOther = itemView.findViewById(R.id.layoutMessageOther);

            // My message views
            tvMessageMy = itemView.findViewById(R.id.tvMessageMy);
            tvTimeMy = itemView.findViewById(R.id.tvTimeMy);

            // Other user's message views
            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
            tvNameOther = itemView.findViewById(R.id.tvNameOther);
        }
    }
}
