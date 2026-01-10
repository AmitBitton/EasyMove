package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easymove.R;
import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyDeliveriesAdapter extends RecyclerView.Adapter<MyDeliveriesAdapter.DeliveryViewHolder> {

    private List<MoveRequest> deliveryList = new ArrayList<>();
    private Map<String, MatchRequest> requestsMap = new HashMap<>(); // מפה לבקשות ממתינות

    private final OnDeliveryActionClickListener listener;
    private final UserRepository userRepository = new UserRepository();

    public interface OnDeliveryActionClickListener {
        void onChatClick(MoveRequest move);
        // מעבירים גם את הבקשה (יכולה להיות null) כדי שהפרגמנט ידע לפתוח את הבוטום-שיט עם המידע הנכון
        void onDetailsClick(MoveRequest move, MatchRequest pendingRequest);
    }

    public MyDeliveriesAdapter(OnDeliveryActionClickListener listener) {
        this.listener = listener;
    }

    public void setDeliveryList(List<MoveRequest> list) {
        this.deliveryList = list;
        notifyDataSetChanged();
    }

    public void setRequestsMap(Map<String, MatchRequest> map) {
        this.requestsMap = map;
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

        // --- לוגיקה לטקסט הסטטוס/תאריך וכתובת ביניים ---
        if (move.getIntermediateAddress() != null && !move.getIntermediateAddress().isEmpty()) {
            // אם יש שותף מאושר
            holder.tvIntermediateAddress.setVisibility(View.VISIBLE);
            holder.tvIntermediateAddress.setText("➕ איסוף נוסף מ: " + move.getIntermediateAddress());
        } else {
            holder.tvIntermediateAddress.setVisibility(View.GONE);
        }

        // תאריך
        if (move.getMoveDate() > 0) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            holder.tvStatus.setText("📅 לתאריך: " + sdf.format(new java.util.Date(move.getMoveDate())));
            holder.tvStatus.setTextColor(android.graphics.Color.BLACK);
            holder.tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvStatus.setText(move.getStatus());
            holder.tvStatus.setTextColor(android.graphics.Color.GRAY);
        }

        // שם הלקוח
        holder.tvCustomerName.setText("טוען לקוח...");
        if (move.getCustomerId() != null) {
            userRepository.getUserNameById(move.getCustomerId())
                    .addOnSuccessListener(name -> {
                        if (name != null) holder.tvCustomerName.setText("לקוח: " + name);
                    });
        }

        // --- בדיקה אם יש בקשת שותפות להובלה הזו ---
        MatchRequest req = requestsMap.get(move.getId());

        if (req != null) {
            // יש בקשה! מציגים את הנקודה האדומה על כפתור הפרטים
            holder.viewNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            // אין בקשה
            holder.viewNotificationBadge.setVisibility(View.GONE);
        }

        holder.btnChat.setOnClickListener(v -> listener.onChatClick(move));

        // לחיצה על פרטים - מעבירה גם את הבקשה (אם יש)
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(move, req));
    }

    @Override
    public int getItemCount() {
        return deliveryList.size();
    }

    static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvSource, tvDest, tvStatus, tvIntermediateAddress;
        Button btnChat, btnDetails;
        View viewNotificationBadge; // הנקודה האדומה

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvSource = itemView.findViewById(R.id.tvSourceAddress);
            tvDest = itemView.findViewById(R.id.tvDestAddress);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            // וודא שיש לך את השדה הזה ב-XML כפי שסיכמנו
            tvIntermediateAddress = itemView.findViewById(R.id.tvIntermediateAddress);

            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnDetails = itemView.findViewById(R.id.btnDetails);

            // וודא שיש לך את השדה הזה ב-XML בתוך FrameLayout מעל הכפתור
            viewNotificationBadge = itemView.findViewById(R.id.viewNotificationBadge);
        }
    }
}