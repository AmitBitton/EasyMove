package com.example.easymove.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.MoveRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter responsible for displaying the user's move history.
 * Handles the display of move details (date, status, addresses, mover name)
 * and manages the visibility of the "Add Review" button based on the move status.
 */
public class MoveHistoryAdapter extends RecyclerView.Adapter<MoveHistoryAdapter.ViewHolder> {

    // Optimization: Define formatter and colors once to avoid recreation during binding.
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final int COLOR_COMPLETED = Color.parseColor("#4CAF50"); // Green
    private static final int COLOR_CANCELED = Color.parseColor("#F44336"); // Red
    private static final int COLOR_CONFIRMED = Color.parseColor("#2196F3"); // Blue
    private static final int COLOR_DEFAULT = Color.GRAY;

    private List<MoveRequest> moves = new ArrayList<>();
    private OnAddReviewClickListener listener;

    /**
     * Interface for handling clicks on the "Add Review" button.
     */
    public interface OnAddReviewClickListener {
        void onAddReviewClicked(MoveRequest move);
    }

    /**
     * Updates the list of moves and refreshes the UI.
     *
     * @param moves The new list of move requests.
     */
    public void setMoves(List<MoveRequest> moves) {
        this.moves = Objects.requireNonNullElseGet(moves, ArrayList::new);
        notifyDataSetChanged();
    }

    public void setOnAddReviewClickListener(OnAddReviewClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_move_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoveRequest move = moves.get(position);

        // Bind Addresses
        holder.tvSource.setText(move.getSourceAddress());
        holder.tvDest.setText(move.getDestAddress());

        // Bind Mover Name
        String moverName = move.getMoverName();
        if (moverName != null && !moverName.trim().isEmpty()) {
            holder.tvMoverName.setText("מוביל: " + moverName);
        } else {
            holder.tvMoverName.setText("מוביל: טוען...");
        }

        // Bind Date
        if (move.getMoveDate() > 0) {
            holder.tvDate.setText(DATE_FORMAT.format(new Date(move.getMoveDate())));
        } else {
            holder.tvDate.setText("תאריך לא צוין");
        }

        // Handle Status Logic
        String status = (move.getStatus() != null) ? move.getStatus().toUpperCase() : "";
        boolean isReviewable = false;

        switch (status) {
            case "COMPLETED":
                holder.tvStatus.setText("הושלם ✅");
                holder.tvStatus.setTextColor(COLOR_COMPLETED);
                isReviewable = true; // Only completed moves can be reviewed
                break;

            case "CANCELED":
                holder.tvStatus.setText("בוטל ❌");
                holder.tvStatus.setTextColor(COLOR_CANCELED);
                break;

            case "CONFIRMED":
                holder.tvStatus.setText("אושר");
                holder.tvStatus.setTextColor(COLOR_CONFIRMED);
                break;

            default:
                holder.tvStatus.setText(status);
                holder.tvStatus.setTextColor(COLOR_DEFAULT);
                break;
        }

        // Toggle "Add Review" button visibility
        if (isReviewable) {
            holder.btnAddReview.setVisibility(View.VISIBLE);
            holder.btnAddReview.setOnClickListener(v -> {
                if (listener != null) listener.onAddReviewClicked(move);
            });
        } else {
            holder.btnAddReview.setVisibility(View.GONE);
            holder.btnAddReview.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return moves.size();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDate, tvStatus, tvSource, tvDest, tvMoverName;
        final Button btnAddReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvMoveDate);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvDest = itemView.findViewById(R.id.tvDest);
            tvMoverName = itemView.findViewById(R.id.tvMoverName);
            btnAddReview = itemView.findViewById(R.id.btnAddReview);
        }
    }
}