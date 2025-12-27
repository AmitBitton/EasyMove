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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private final String currentUserId;

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // פורמט לשעה (למשל 14:30)
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = message.getTimestamp() != null ? sdf.format(message.getTimestamp().toDate()) : "";

        // בדיקה מי שלח את ההודעה
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            // --- הודעה שלי ---
            holder.layoutMy.setVisibility(View.VISIBLE);
            holder.layoutOther.setVisibility(View.GONE);

            holder.tvMessageMy.setText(message.getText());
            holder.tvTimeMy.setText(time);
        } else {
            // --- הודעה של הצד השני ---
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

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutMy, layoutOther;
        TextView tvMessageMy, tvTimeMy;
        TextView tvMessageOther, tvTimeOther, tvNameOther;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutMy = itemView.findViewById(R.id.layoutMessageMy);
            layoutOther = itemView.findViewById(R.id.layoutMessageOther);

            tvMessageMy = itemView.findViewById(R.id.tvMessageMy);
            tvTimeMy = itemView.findViewById(R.id.tvTimeMy);

            tvMessageOther = itemView.findViewById(R.id.tvMessageOther);
            tvTimeOther = itemView.findViewById(R.id.tvTimeOther);
            tvNameOther = itemView.findViewById(R.id.tvNameOther);
        }
    }
}