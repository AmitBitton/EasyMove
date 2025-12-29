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

public class IncomingRequestAdapter extends RecyclerView.Adapter<IncomingRequestAdapter.ViewHolder> {

    private List<MatchRequest> requests = new ArrayList<>();
    private final OnActionListener listener;

    public interface OnActionListener {
        void onApprove(MatchRequest request);
        void onReject(MatchRequest request);
    }

    public IncomingRequestAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void setRequests(List<MatchRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MatchRequest req = requests.get(position);

        // מציגים רק את השם, הכותרת "בקשה חדשה" כבר ב-XML
        holder.name.setText(req.getFromUserName());

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(req));
        holder.btnReject.setOnClickListener(v -> listener.onReject(req));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageButton btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.requestNameTextView);
            btnApprove = itemView.findViewById(R.id.approveButton);
            btnReject = itemView.findViewById(R.id.rejectButton);
        }
    }
}