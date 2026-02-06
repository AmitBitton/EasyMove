package com.example.easymove.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter responsible for displaying the list of active deliveries (moves) for a Mover.
 * It handles the display of move details, customer names (fetched asynchronously),
 * and notification badges for pending actions (cancellations or partner requests).
 */
public class MyDeliveriesAdapter extends RecyclerView.Adapter<MyDeliveriesAdapter.DeliveryViewHolder> {

    // Optimization: Define formatter once to avoid object creation during scrolling
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private List<MoveRequest> deliveryList = new ArrayList<>();
    private Map<String, MatchRequest> requestsMap = new HashMap<>();

    private final OnDeliveryActionClickListener listener;
    private final UserRepository userRepository;

    /**
     * Interface for handling click actions on specific delivery items.
     */
    public interface OnDeliveryActionClickListener {
        void onChatClick(MoveRequest move);
        void onDetailsClick(MoveRequest move, MatchRequest pendingRequest);
    }

    /**
     * Constructor for MyDeliveriesAdapter.
     *
     * @param listener The listener for button click events.
     */
    public MyDeliveriesAdapter(OnDeliveryActionClickListener listener) {
        this.listener = listener;
        this.userRepository = new UserRepository();
    }

    /**
     * Updates the list of deliveries.
     *
     * @param list The new list of move requests.
     */
    public void setDeliveryList(List<MoveRequest> list) {
        this.deliveryList = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Updates the map of pending partner requests.
     *
     * @param map A map where Key = MoveID and Value = MatchRequest.
     */
    public void setRequestsMap(Map<String, MatchRequest> map) {
        this.requestsMap = (map != null) ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeliveryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_delivery_card, parent, false);
        return new DeliveryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeliveryViewHolder holder, int position) {
        MoveRequest move = deliveryList.get(position);

        // --- Bind Addresses ---
        holder.tvSource.setText(move.getSourceAddress() != null ? move.getSourceAddress() : "");
        holder.tvDest.setText(move.getDestAddress() != null ? move.getDestAddress() : "");

        // --- Bind Intermediate Address ---
        if (move.getIntermediateAddress() != null && !move.getIntermediateAddress().isEmpty()) {
            holder.tvIntermediateAddress.setVisibility(View.VISIBLE);
            holder.tvIntermediateAddress.setText("➕ איסוף נוסף מ: " + move.getIntermediateAddress());
        } else {
            holder.tvIntermediateAddress.setVisibility(View.GONE);
        }

        // --- Bind Date & Status ---
        if (move.getMoveDate() > 0) {
            holder.tvStatus.setText("📅 לתאריך: " + DATE_FORMAT.format(new Date(move.getMoveDate())));
            holder.tvStatus.setTextColor(Color.BLACK);
            holder.tvStatus.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvStatus.setText(move.getStatus() != null ? move.getStatus() : "");
            holder.tvStatus.setTextColor(Color.GRAY);
        }

        // --- Fetch Customer Name ---
        holder.tvCustomerName.setText("טוען לקוח..."); // Placeholder
        if (move.getCustomerId() != null) {
            userRepository.getUserNameById(move.getCustomerId())
                    .addOnSuccessListener(name -> {
                        if (name != null) {
                            holder.tvCustomerName.setText("לקוח: " + name);
                        }
                    })
                    .addOnFailureListener(e -> holder.tvCustomerName.setText("לקוח: לא זמין"));
        } else {
            holder.tvCustomerName.setText("לקוח: לא זמין");
        }

        // --- Unified Notification Badge Logic ---
        MatchRequest req = requestsMap.get(move.getId());
        boolean hasPartnerRequest = (req != null);
        boolean hasCancelRequest = (move.getCancelRequestPending() != null && move.getCancelRequestPending());

        // Show badge if there is EITHER a partner request OR a cancel request
        if (hasPartnerRequest || hasCancelRequest) {
            holder.viewNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            holder.viewNotificationBadge.setVisibility(View.GONE);
        }

        // --- Click Listeners ---
        holder.btnChat.setOnClickListener(v -> listener.onChatClick(move));
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(move, req));
    }

    @Override
    public int getItemCount() {
        return deliveryList.size();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class DeliveryViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCustomerName, tvSource, tvDest, tvStatus, tvIntermediateAddress;
        final Button btnChat, btnDetails;
        final View viewNotificationBadge;

        public DeliveryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvSource = itemView.findViewById(R.id.tvSourceAddress);
            tvDest = itemView.findViewById(R.id.tvDestAddress);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            tvIntermediateAddress = itemView.findViewById(R.id.tvIntermediateAddress);

            btnChat = itemView.findViewById(R.id.btnOpenChat);
            btnDetails = itemView.findViewById(R.id.btnDetails);
            viewNotificationBadge = itemView.findViewById(R.id.viewNotificationBadge);
        }
    }
}