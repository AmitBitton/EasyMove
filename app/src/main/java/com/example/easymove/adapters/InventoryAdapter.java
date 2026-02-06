package com.example.easymove.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.easymove.R;
import com.example.easymove.model.InventoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for displaying inventory items in a RecyclerView.
 * Handles the display of item details (image, name, room, quantity) and user interactions
 * like deleting an item or viewing full details in a dialog.
 */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    // Format for displaying the creation date
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private List<InventoryItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    /**
     * Interface definition for callbacks to be invoked when an item action is performed.
     */
    public interface OnItemClickListener {
        void onDeleteClick(InventoryItem item);
        void onItemClick(InventoryItem item);
    }

    /**
     * Constructor for InventoryAdapter.
     *
     * @param listener The listener that will handle click events.
     */
    public InventoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of inventory items and refreshes the view.
     *
     * @param items The new list of items to display.
     */
    public void setItems(List<InventoryItem> items) {
        this.items = Objects.requireNonNullElseGet(items, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventory_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        // Bind text fields
        holder.tvName.setText(item.getName());
        holder.tvRoom.setText(String.format("חדר: %s", item.getRoomType()));
        holder.tvQuantity.setText(String.format("x%s", item.getQuantity()));

        // Handle "Created At" Date
        if (item.getCreatedAt() > 0) {
            String dateStr = dateFormat.format(new Date(item.getCreatedAt()));
            holder.tvDate.setText(String.format("נוסף ב: %s", dateStr));
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // Handle "Fragile" Label
        if (item.isFragile()) {
            holder.tvFragile.setVisibility(View.VISIBLE);
        } else {
            holder.tvFragile.setVisibility(View.GONE);
        }

        // Handle Image Loading
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click Listeners
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));

        // Opens the details dialog internally within the adapter context
        holder.btnDetails.setOnClickListener(v -> showDetailsDialog(context, item));

        // Optional: Click on the whole card
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Builds and shows an AlertDialog with the full details of the item.
     */
    private void showDetailsDialog(Context context, InventoryItem item) {
        String description = (item.getDescription() != null && !item.getDescription().isEmpty())
                ? item.getDescription()
                : "אין תיאור נוסף";

        String fragileText = item.isFragile() ? "\n⚠️ זהו פריט שביר!" : "";

        // Construct the detailed message
        String message = "חדר: " + item.getRoomType() +
                "\nכמות: " + item.getQuantity() +
                "\n\nתיאור: " + description +
                fragileText;

        new AlertDialog.Builder(context)
                .setTitle(item.getName())
                .setMessage(message)
                .setPositiveButton("סגור", null)
                .show();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvRoom, tvQuantity, tvFragile, tvDate;
        final ImageView ivImage;
        final ImageButton btnDelete;
        final Button btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvRoom = itemView.findViewById(R.id.tvItemRoom);
            tvQuantity = itemView.findViewById(R.id.tvItemQuantity);
            tvFragile = itemView.findViewById(R.id.tvItemFragile);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
            btnDetails = itemView.findViewById(R.id.btnItemDetails);
        }
    }
}