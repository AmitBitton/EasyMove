package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easymove.R;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;

public class MyDeliveriesAdapter extends RecyclerView.Adapter<MyDeliveriesAdapter.DeliveryViewHolder> {

    private List<MoveRequest> deliveryList = new ArrayList<>();
    private final OnDeliveryActionClickListener listener;
    private final UserRepository userRepository = new UserRepository();

    // ממשק ללחיצות על הכפתורים
    public interface OnDeliveryActionClickListener {
        void onChatClick(MoveRequest move);
        void onDetailsClick(MoveRequest move);
    }

    public MyDeliveriesAdapter(OnDeliveryActionClickListener listener) {
        this.listener = listener;
    }

    public void setDeliveryList(List<MoveRequest> list) {
        this.deliveryList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeliveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_card, parent, false);
        return new DeliveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeliveryViewHolder holder, int position) {
        MoveRequest move = deliveryList.get(position);

        holder.tvSource.setText(move.getSourceAddress());
        holder.tvDest.setText(move.getDestAddress());

        // --- לוגיקה נקייה לטקסט הסטטוס/תאריך ---
        if ("CONFIRMED".equals(move.getStatus())) {
            // אם מאושר: מציגים תאריך בצבע רגיל/מודגש
            if (move.getMoveDate() > 0) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                holder.tvStatus.setText("📅 לתאריך: " + sdf.format(new java.util.Date(move.getMoveDate())));
                holder.tvStatus.setTextColor(android.graphics.Color.BLACK);
                holder.tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                holder.tvStatus.setText("ממתין לתאריך");
                holder.tvStatus.setTextColor(android.graphics.Color.RED);
            }
        } else {
            // אם לא מאושר: מציגים את הסטטוס (למשל OPEN)
            holder.tvStatus.setText(move.getStatus());
            holder.tvStatus.setTextColor(android.graphics.Color.GRAY);
        }

        // --- הצגת שם הלקוח (במקום הכפתור הירוק הישן) ---
        // וודא שב-XML (item_delivery_card) המיקום של tvCustomerName מתאים לך
        holder.tvCustomerName.setText("טוען לקוח...");
        if (move.getCustomerId() != null) {
            userRepository.getUserNameById(move.getCustomerId())
                    .addOnSuccessListener(name -> {
                        if (name != null) holder.tvCustomerName.setText("לקוח: " + name);
                    });
        }

        holder.btnChat.setOnClickListener(v -> listener.onChatClick(move));

        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(move));
    }

    @Override
    public int getItemCount() {
        return deliveryList.size();
    }

    static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvSource, tvDest, tvStatus;
        Button btnChat, btnDetails;

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvSource = itemView.findViewById(R.id.tvSourceAddress);
            tvDest = itemView.findViewById(R.id.tvDestAddress);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}