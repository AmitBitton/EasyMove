package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.MoveRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * MoveHistoryAdapter
 *
 * RecyclerView adapter responsible for displaying a list of completed,
 * confirmed, or canceled move requests in the move history screen.
 *
 * Each list item shows:
 * - Source address
 * - Destination address
 * - Move date
 * - Move status (with color indication)
 *
 * The adapter binds {@link MoveRequest} model data to the UI elements
 * defined in {@code item_move_history.xml}.
 */
public class MoveHistoryAdapter
        extends RecyclerView.Adapter<MoveHistoryAdapter.HistoryViewHolder> {

    /** List of move requests to be displayed */
    private List<MoveRequest> movesList;

    /**
     * Constructor
     *
     * @param movesList List of move history items
     */
    public MoveHistoryAdapter(List<MoveRequest> movesList) {
        this.movesList = movesList;
    }

    /**
     * Creates and inflates the ViewHolder for a single history item.
     *
     * @param parent   Parent ViewGroup
     * @param viewType View type (not used here)
     * @return A new HistoryViewHolder instance
     */
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_move_history, parent, false);

        return new HistoryViewHolder(view);
    }

    /**
     * Binds move data to the ViewHolder UI components.
     *
     * @param holder   ViewHolder instance
     * @param position Position of the item in the list
     */
    @Override
    public void onBindViewHolder(
            @NonNull HistoryViewHolder holder,
            int position
    ) {
        MoveRequest move = movesList.get(position);

        /* ---------- 1. Addresses ---------- */
        holder.tvSource.setText(move.getSourceAddress());
        holder.tvDest.setText(move.getDestAddress());

        /* ---------- 2. Move Date ---------- */
        // Display formatted date if available, otherwise show a fallback text
        if (move.getMoveDate() > 0) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(move.getMoveDate()));
        } else {
            holder.tvDate.setText("תאריך לא צוין");
        }

        /* ---------- 3. Status Handling ---------- */
        // Normalize status string to avoid case-sensitivity issues
        String status = move.getStatus() != null
                ? move.getStatus().toUpperCase()
                : "";

        switch (status) {
            case "COMPLETED":
                holder.tvStatus.setText("הושלם");
                holder.tvStatus.setTextColor(0xFF4CAF50); // Green
                break;

            case "CANCELED":
                holder.tvStatus.setText("בוטל");
                holder.tvStatus.setTextColor(0xFFF44336); // Red
                break;

            case "CONFIRMED":
                holder.tvStatus.setText("אושר");
                holder.tvStatus.setTextColor(0xFF2196F3); // Blue
                break;

            default:
                holder.tvStatus.setText("פתוח");
                holder.tvStatus.setTextColor(0xFF757575); // Grey
                break;
        }

        /* ---------- 4. Price ---------- */
        // Price is not yet supported in the MoveRequest model,
        // so the view is hidden to prevent showing placeholder text.
        holder.tvPrice.setVisibility(View.GONE);
    }

    /**
     * Returns the total number of items in the list.
     *
     * @return Number of move history entries
     */
    @Override
    public int getItemCount() {
        return movesList.size();
    }

    /**
     * HistoryViewHolder
     *
     * Holds references to the UI elements for a single move history item.
     * Improves performance by avoiding repeated findViewById calls.
     */
    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        TextView tvStatus;
        TextView tvSource;
        TextView tvDest;
        TextView tvPrice;

        /**
         * ViewHolder constructor
         *
         * @param itemView Root view of the history item layout
         */
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDate = itemView.findViewById(R.id.tvMoveDate);
            tvStatus = itemView.findViewById(R.id.tvMoveStatus);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvDest = itemView.findViewById(R.id.tvDest);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
