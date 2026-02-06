package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.MatchRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adapter for displaying incoming match requests in a RecyclerView.
 * Handles the display of the requester's name and provides action buttons
 * for approving or rejecting the partnership request.
 */
public class IncomingRequestAdapter extends RecyclerView.Adapter<IncomingRequestAdapter.ViewHolder> {

    private List<MatchRequest> requests = new ArrayList<>();
    private final OnActionListener listener;

    /**
     * Interface definition for callbacks to be invoked when a request action is performed.
     */
    public interface OnActionListener {
        void onApprove(MatchRequest request);
        void onReject(MatchRequest request);
    }

    /**
     * Constructor for IncomingRequestAdapter.
     *
     * @param listener The listener that will handle approve/reject actions.
     */
    public IncomingRequestAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the data set and refreshes the adapter.
     *
     * @param requests The new list of match requests.
     */
    public void setRequests(List<MatchRequest> requests) {
        this.requests = Objects.requireNonNullElseGet(requests, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchRequest req = requests.get(position);

        // Display the requester's name. The static text (e.g., "New Request") is handled in the XML layout.
        String userName = req.getFromUserName();
        holder.name.setText(userName != null ? userName : "Unknown User");

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) listener.onApprove(req);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(req);
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    /**
     * ViewHolder class to cache view references for performance.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final ImageButton btnApprove;
        final ImageButton btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.requestNameTextView);
            btnApprove = itemView.findViewById(R.id.approveButton);
            btnReject = itemView.findViewById(R.id.rejectButton);
        }
    }
}