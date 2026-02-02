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
    private Map<String, MatchRequest> requestsMap = new HashMap<>();

    private final OnDeliveryActionClickListener listener;
    private final UserRepository userRepository = new UserRepository();

    public interface OnDeliveryActionClickListener {
        void onChatClick(MoveRequest move);
        void onDetailsClick(MoveRequest move, MatchRequest pendingRequest);
    }

    public MyDeliveriesAdapter(OnDeliveryActionClickListener listener) {
        this.listener = listener;
    }

    public void setDeliveryList(List<MoveRequest> list) {
        this.deliveryList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setRequestsMap(Map<String, MatchRequest> map) {
        this.requestsMap = map != null ? map : new HashMap<>();
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

        holder.tvSource.setText(move.getSourceAddress() != null ? move.getSourceAddress() : "");
        holder.tvDest.setText(move.getDestAddress() != null ? move.getDestAddress() : "");

        // Show intermediate pickup address (partner stop) if exists
        if (move.getIntermediateAddress() != null && !move.getIntermediateAddress().isEmpty()) {
            holder.tvIntermediateAddress.setVisibility(View.VISIBLE);
            holder.tvIntermediateAddress.setText("➕ איסוף נוסף מ: " + move.getIntermediateAddress());
        } else {
            holder.tvIntermediateAddress.setVisibility(View.GONE);
        }

        // Date/status label
        if (move.getMoveDate() > 0) {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            holder.tvStatus.setText("📅 לתאריך: " + sdf.format(new java.util.Date(move.getMoveDate())));
            holder.tvStatus.setTextColor(android.graphics.Color.BLACK);
            holder.tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.tvStatus.setText(move.getStatus() != null ? move.getStatus() : "");
            holder.tvStatus.setTextColor(android.graphics.Color.GRAY);
        }

        // Customer name
        holder.tvCustomerName.setText("טוען לקוח...");
        if (move.getCustomerId() != null) {
            userRepository.getUserNameById(move.getCustomerId())
                    .addOnSuccessListener(name -> {
                        if (name != null) holder.tvCustomerName.setText("לקוח: " + name);
                    })
                    .addOnFailureListener(e -> holder.tvCustomerName.setText("לקוח: לא זמין"));
        } else {
            holder.tvCustomerName.setText("לקוח: לא זמין");
        }

        // Partner request badge (existing)
        MatchRequest req = requestsMap.get(move.getId());
        holder.viewNotificationBadge.setVisibility(req != null ? View.VISIBLE : View.GONE);

        // Cancel request badge (new) - requires viewCancelBadge in XML
        boolean cancelPending = move.getCancelRequestPending() != null && move.getCancelRequestPending();
        if (holder.viewCancelBadge != null) {
            holder.viewCancelBadge.setVisibility(cancelPending ? View.VISIBLE : View.GONE);
        }

        holder.btnChat.setOnClickListener(v -> listener.onChatClick(move));

        // Pass the pending partner request (could be null)
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(move, req));
    }

    @Override
    public int getItemCount() {
        return deliveryList.size();
    }

    static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvSource, tvDest, tvStatus, tvIntermediateAddress;
        Button btnChat, btnDetails;
        View viewNotificationBadge; // Partner request badge
        View viewCancelBadge;       // Cancel request badge

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvSource = itemView.findViewById(R.id.tvSourceAddress);
            tvDest = itemView.findViewById(R.id.tvDestAddress);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            tvIntermediateAddress = itemView.findViewById(R.id.tvIntermediateAddress);

            // Keep your existing IDs (as in your current XML)
            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnDetails = itemView.findViewById(R.id.btnDetails);

            // Existing badge for partner requests
            viewNotificationBadge = itemView.findViewById(R.id.viewNotificationBadge);

            // New badge for cancel requests (add this View to XML)
            // If the view doesn't exist yet, this will just stay null safely.
            viewCancelBadge = itemView.findViewById(R.id.viewCancelBadge);
        }
    }
}
