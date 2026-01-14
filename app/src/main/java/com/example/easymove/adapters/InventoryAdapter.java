package com.example.easymove.adapters;

import android.app.AlertDialog; // לדיאלוג הפירוט
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

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

    private List<InventoryItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    // פורמט תאריך פשוט
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnItemClickListener {
        void onDeleteClick(InventoryItem item);
        void onItemClick(InventoryItem item);
    }

    public InventoryAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<InventoryItem> items) {
        this.items = items;
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

        holder.tvName.setText(item.getName());
        holder.tvRoom.setText("חדר: " + item.getRoomType());
        holder.tvQuantity.setText("x" + item.getQuantity());

        // ✅ תאריך הוספה
        if (item.getCreatedAt() > 0) {
            String dateStr = dateFormat.format(new Date(item.getCreatedAt()));
            holder.tvDate.setText("נוסף ב: " + dateStr);
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // ✅ שביר
        if (item.isFragile()) {
            holder.tvFragile.setVisibility(View.VISIBLE);
        } else {
            holder.tvFragile.setVisibility(View.GONE);
        }

        // תמונה
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getImageUrl())
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // מאזין למחיקה (הכפתור האלגנטי)
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));

        // ✅ כפתור פירוט - פותח דיאלוג עם המידע המלא
        holder.btnDetails.setOnClickListener(v -> showDetailsDialog(context, item));

        // לחיצה על הכרטיס עצמו (אם עדיין רלוונטי)
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    private void showDetailsDialog(Context context, InventoryItem item) {
        String description = (item.getDescription() != null && !item.getDescription().isEmpty())
                ? item.getDescription()
                : "אין תיאור נוסף";

        String fragileText = item.isFragile() ? "\n⚠️ זהו פריט שביר!" : "";

        new AlertDialog.Builder(context)
                .setTitle(item.getName())
                .setMessage("חדר: " + item.getRoomType() +
                        "\nכמות: " + item.getQuantity() +
                        "\n\nתיאור: " + description +
                        fragileText)
                .setPositiveButton("סגור", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRoom, tvQuantity, tvFragile, tvDate;
        ImageView ivImage;
        ImageButton btnDelete;
        Button btnDetails; // ✅ הכפתור החדש

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvRoom = itemView.findViewById(R.id.tvItemRoom);
            tvQuantity = itemView.findViewById(R.id.tvItemQuantity);
            tvFragile = itemView.findViewById(R.id.tvItemFragile);
            tvDate = itemView.findViewById(R.id.tvItemDate); // ✅ חדש

            ivImage = itemView.findViewById(R.id.ivItemImage);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
            btnDetails = itemView.findViewById(R.id.btnItemDetails); // ✅ חדש
        }
    }
}