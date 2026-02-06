package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter responsible for displaying a list of user notifications in a RecyclerView.
 * Displays the title, message, formatted timestamp, and a read/unread indicator.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder> {

    // Optimization: Define formatter once
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

    private List<NotificationItem> items;

    /**
     * Constructor.
     * Initializes with an empty list or the provided list.
     */
    public NotificationsAdapter(List<NotificationItem> items) {
        this.items = Objects.requireNonNullElseGet(items, ArrayList::new);
    }

    /**
     * Updates the list of notifications and refreshes the UI.
     *
     * @param newItems The new list of notifications.
     */
    public void setItems(List<NotificationItem> newItems) {
        this.items = Objects.requireNonNullElseGet(newItems, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        NotificationItem item = items.get(position);

        // Bind Text Data
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getMessage());

        // Bind Timestamp
        if (item.getTimestamp() != null) {
            holder.tvTime.setText(DATE_FORMAT.format(item.getTimestamp().toDate()));
        } else {
            holder.tvTime.setText("");
        }

        // Handle Read/Unread Indicator
        if (item.isRead()) {
            holder.viewUnreadDot.setVisibility(View.INVISIBLE);
        } else {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
        }

        // Set Static Icon
        holder.imgIcon.setImageResource(android.R.drawable.ic_popup_reminder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class NotifViewHolder extends RecyclerView.ViewHolder {

        final TextView tvTitle;
        final TextView tvBody;
        final TextView tvTime;
        final ImageView imgIcon;
        final View viewUnreadDot;

        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }
}