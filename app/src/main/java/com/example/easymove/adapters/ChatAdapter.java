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
import java.util.Objects;

/**
 * Adapter responsible for displaying chat messages in a RecyclerView.
 * It handles the distinction between sent messages (by the current user)
 * and received messages (by others) by toggling the visibility of UI layouts.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    // Performance Optimization: Define formatter once to avoid creating it for every item bind.
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<Message> messages = new ArrayList<>();
    private final String currentUserId;

    /**
     * Constructor for ChatAdapter.
     *
     * @param currentUserId The UID of the currently logged-in user.
     */
    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Updates the list of messages and refreshes the UI.
     *
     * @param messages The new list of messages to display.
     */
    public void setMessages(List<Message> messages) {
        // Prevent NullPointerException if null is passed
        this.messages = Objects.requireNonNullElseGet(messages, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // Format timestamp to readable string
        String time = "";
        if (message.getTimestamp() != null) {
            time = TIME_FORMAT.format(message.getTimestamp().toDate());
        }

        // Determine if the message was sent by the current user or received
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            // --- My Message ---
            holder.layoutMy.setVisibility(View.VISIBLE);
            holder.layoutOther.setVisibility(View.GONE);

            holder.tvMessageMy.setText(message.getText());
            holder.tvTimeMy.setText(time);
        } else {
            // --- Other's Message ---
            holder.layoutMy.setVisibility(View.GONE);
            holder.layoutOther.setVisibility(View.VISIBLE);

            holder.tvMessageOther.setText(message.getText());
            holder.tvTimeOther.setText(time);
            holder.tvNameOther.setText(message.getSenderName());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * ViewHolder class to cache view references for performance.
     */
    static class MessageViewHolder extends RecyclerView.ViewHolder {

        final LinearLayout layoutMy;
        final LinearLayout layoutOther;
        final TextView tvMessageMy;
        final TextView tvTimeMy;
        final TextView tvMessageOther;
        final TextView tvTimeOther;
        final TextView tvNameOther;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // My Message Views
            layoutMy = itemView.findViewById(R.id.layoutMessageMy);
            tvMessageMy = itemView.findViewById(R.id.tvMessageMy);
            tvTimeMy = itemView.findViewById(R.id.tvTimeMy);

            // Other Message Views
            layoutOther = itemView.findViewById(R.id.layoutMessageOther);
            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
            tvNameOther = itemView.findViewById(R.id.tvNameOther);
        }
    }
}