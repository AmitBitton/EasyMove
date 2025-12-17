package com.example.easymove.adapters;

import android.graphics.Paint;
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
import com.example.easymove.model.UserProfile;
import java.util.ArrayList;
import java.util.List;

public class MoversAdapter extends RecyclerView.Adapter<MoversAdapter.MoverViewHolder> {

    private List<UserProfile> movers = new ArrayList<>();
    private final OnMoverActionClickListener listener;

    // הרחבנו את ה-Interface כדי לתמוך בכל הכפתורים
    public interface OnMoverActionClickListener {
        void onChatClick(UserProfile mover);
        void onDetailsClick(UserProfile mover);
        void onReviewsClick(UserProfile mover);
        void onReportClick(UserProfile mover);
    }

    public MoversAdapter(OnMoverActionClickListener listener) {
        this.listener = listener;
    }

    public void setMovers(List<UserProfile> movers) {
        this.movers = movers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mover_card, parent, false);
        return new MoverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoverViewHolder holder, int position) {
        UserProfile mover = movers.get(position);

        holder.tvName.setText(mover.getName());
        holder.tvAbout.setText(mover.getAbout() != null ? mover.getAbout() : "אין פירוט נוסף");
        holder.btnReport.setPaintFlags(holder.btnReport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // הצגת מרחק (החלק החשוב!)
        double distance = mover.getDistanceFromUser();
        if (distance > 0) {
            holder.tvDistance.setText(String.format("מרחק: %.1f ק\"מ", distance / 1000));
        } else {
            holder.tvDistance.setText("מרחק לא ידוע");
        }

        // הצגת דירוג
        holder.tvRating.setText(mover.getRating() > 0 ? String.format("%.1f", mover.getRating()) : "-");
        holder.btnReviews.setText(String.format("(%d ביקורות) לחץ לצפייה", mover.getRatingCount()));

        // תמונה
        if (mover.getProfileImageUrl() != null && !mover.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(mover.getProfileImageUrl()).circleCrop().into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // --- חיבור כפתורים ---
        holder.btnChat.setOnClickListener(v -> listener.onChatClick(mover));
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(mover));
        holder.btnReviews.setOnClickListener(v -> listener.onReviewsClick(mover));
        holder.btnReport.setOnClickListener(v -> listener.onReportClick(mover));
    }

    @Override
    public int getItemCount() {
        return movers.size();
    }

    static class MoverViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvDistance, tvRating, tvAbout, btnReviews;
        Button btnChat, btnDetails;
        Button btnReport;

        public MoverViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.imgMoverProfile);
            tvName = itemView.findViewById(R.id.tvMoverName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnReviews = itemView.findViewById(R.id.btnReviews); // TextView לחיץ
            tvAbout = itemView.findViewById(R.id.tvAboutPreview);

            btnChat = itemView.findViewById(R.id.btnChatWithMover);
            btnDetails = itemView.findViewById(R.id.btnMoverDetails);
            btnReport = itemView.findViewById(R.id.btnReportMover);
        }
    }
}