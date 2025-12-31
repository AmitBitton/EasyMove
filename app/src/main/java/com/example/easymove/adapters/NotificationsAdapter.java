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
import java.util.List;
import java.util.Locale;

/**
 * NotificationsAdapter
 * ---------------------
 * RecyclerView Adapter responsible for displaying a list of notification items
 * inside a RecyclerView.
 *
 * Each item represents a {@link NotificationItem} and shows:
 * - Notification title
 * - Notification body/message
 * - Formatted timestamp
 * - Unread indicator (dot)
 * - Static notification icon
 *
 * This adapter is intentionally kept simple:
 * - No click handling
 * - No action buttons
 * - No dynamic icon logic
 *
 * It assumes the list is already prepared by the Fragment or ViewModel.
 */
public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsAdapter.NotifViewHolder> {

    /**
     * List of notification items to be displayed.
     * This list is provided by the Fragment and is not modified by the adapter.
     */
    private final List<NotificationItem> items;

    /**
     * Constructor
     *
     * @param items List of notifications to display.
     *              Matches the Fragment implementation that owns the data.
     */
    public NotificationsAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    /**
     * Creates and inflates the notification item layout.
     *
     * @param parent   Parent ViewGroup (RecyclerView)
     * @param viewType View type (not used, since all items are the same)
     * @return A new {@link NotifViewHolder} instance
     */
    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    /**
     * Binds notification data to the ViewHolder views.
     *
     * @param holder   ViewHolder containing item views
     * @param position Position of the item in the list
     */
    @Override
    public void onBindViewHolder(
            @NonNull NotifViewHolder holder,
            int position
    ) {
        NotificationItem item = items.get(position);

        // ---- Text Content ----
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getMessage());

        // ---- Timestamp Formatting ----
        // Converts Firestore Timestamp -> Date -> formatted string
        if (item.getTimestamp() != null) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.tvTime.setText(
                    sdf.format(item.getTimestamp().toDate())
            );
        } else {
            holder.tvTime.setText("");
        }

        // ---- Read / Unread Indicator ----
        // Unread notifications show a dot
        holder.viewUnreadDot.setVisibility(
                item.isRead() ? View.INVISIBLE : View.VISIBLE
        );

        // ---- Static Icon ----
        // A single reminder-style icon for all notifications
        holder.imgIcon.setImageResource(
                android.R.drawable.ic_popup_reminder
        );
    }

    /**
     * @return Total number of notification items
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * NotifViewHolder
     * ----------------
     * Holds references to all views inside a single notification item layout.
     *
     * This improves RecyclerView performance by avoiding repeated
     * findViewById calls during scrolling.
     */
    static class NotifViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle;
        TextView tvBody;
        TextView tvTime;
        ImageView imgIcon;
        View viewUnreadDot;

        /**
         * ViewHolder constructor.
         *
         * @param itemView Root view of the notification item layout
         */
        public NotifViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvBody = itemView.findViewById(R.id.tvNotifBody);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);

            // Action buttons were intentionally removed
            // to keep notifications read-only
        }
    }
}
