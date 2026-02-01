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


public class MoveHistoryAdapter extends RecyclerView.Adapter<MoveHistoryAdapter.ViewHolder> {



    private List<MoveRequest> moves = new ArrayList<>();
    private OnAddReviewClickListener listener;

    public interface OnAddReviewClickListener {
        void onAddReviewClicked(MoveRequest move);
    }






    public void setMoves(List<MoveRequest> moves) {
        this.moves = moves;
        notifyDataSetChanged();
    }
    public void setOnAddReviewClickListener(OnAddReviewClickListener listener) {
        this.listener = listener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_move_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MoveRequest move = moves.get(position);

        holder.tvSource.setText(move.getSourceAddress());
        holder.tvDest.setText(move.getDestAddress());

        String moverName = move.getMoverName();

        if (moverName != null && !moverName.trim().isEmpty()) {
            holder.tvMoverName.setText("מוביל: " + moverName);
        } else {
            holder.tvMoverName.setText("מוביל: טוען...");
        }




        // טיפול בתאריך (שיפור קטן בטקסט ברירת המחדל)
        if (move.getMoveDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(move.getMoveDate())));
        } else {
            holder.tvDate.setText("תאריך לא צוין");
        }

        // --- התיקון החכם מהקוד השני ---
        // נרמול הסטטוס (אותיות גדולות ומניעת קריסה אם ריק)
        String status = (move.getStatus() != null) ? move.getStatus().toUpperCase() : "";

        switch (status) {
            case "COMPLETED":
                holder.tvStatus.setText("הושלם ✅");
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // ירוק
                holder.btnAddReview.setVisibility(View.VISIBLE);
                holder.btnAddReview.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddReviewClicked(move);
                    }
                });

                break;

            case "CANCELED":
                holder.tvStatus.setText("בוטל ❌");
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // אדום
                holder.btnAddReview.setVisibility(View.GONE);
                holder.btnAddReview.setOnClickListener(null);

                break;

            case "CONFIRMED":
                // הוספתי את זה ליתר ביטחון, שיהיה יפה אם יקרה
                holder.tvStatus.setText("אושר");
                holder.tvStatus.setTextColor(Color.parseColor("#2196F3")); // כחול
                holder.btnAddReview.setVisibility(View.GONE);
                holder.btnAddReview.setOnClickListener(null);

                break;

            default:
                holder.tvStatus.setText(status); // מציג את הסטטוס המקורי אם לא זיהינו
                holder.tvStatus.setTextColor(Color.GRAY);
                holder.btnAddReview.setVisibility(View.GONE);
                holder.btnAddReview.setOnClickListener(null);

                break;
        }
    }

    @Override
    public int getItemCount() {
        return moves.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvStatus, tvSource, tvDest, tvMoverName;
        Button btnAddReview;

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