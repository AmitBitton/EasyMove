package com.example.easymove.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;

import java.util.List;

public class MoversAdapter extends RecyclerView.Adapter<MoversAdapter.MoverViewHolder> {

    private final Context context;
    private List<UserProfile> moverList;
    private final OnMoverActionClickListener listener;

    // --- Interface for handling clicks (Restored all 4 actions) ---
    public interface OnMoverActionClickListener {
        void onChatClick(UserProfile mover);
        void onDetailsClick(UserProfile mover);
        void onReviewsClick(UserProfile mover);
        void onReportClick(UserProfile mover);
    }

    // Constructor
    public MoversAdapter(Context context, List<UserProfile> moverList, OnMoverActionClickListener listener) {
        this.context = context;
        this.moverList = moverList;
        this.listener = listener;
    }

    // Helper to update list dynamically
    public void updateList(List<UserProfile> newList) {
        this.moverList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mover_card, parent, false);
        return new MoverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoverViewHolder holder, int position) {
        UserProfile mover = moverList.get(position);

        // 1. Name
        holder.tvName.setText(mover.getName() != null ? mover.getName() : "מוביל");

        // 2. About
        String about = mover.getAbout();
        holder.tvAbout.setText(about != null && !about.isEmpty() ? about : "אין פירוט נוסף");

        // 3. Image (Using Glide with Placeholder)
        if (mover.getProfileImageUrl() != null && !mover.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(mover.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_placeholder) // Use your existing placeholder
                    .into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.ic_person_placeholder);
        }

        // 4. Distance Logic
        double distance = mover.getDistanceFromUser();
        if (distance > 0) {
            holder.tvDistance.setText(String.format("מרחק: %.1f ק\"מ", distance / 1000));
        } else {
            holder.tvDistance.setText("מרחק: לא ידוע");
        }

        // 5. Rating Logic (Restored from Old Code)
        if (mover.getRating() > 0) {
            holder.tvRating.setText(String.format("%.1f", mover.getRating()));
        } else {
            holder.tvRating.setText("-");
        }

        // 6. Reviews Text (Restored)
        holder.btnReviews.setText(String.format("(%d ביקורות) לחץ לצפייה", mover.getRatingCount()));

        // 7. Report Button Style (Restored Underline)
        holder.btnReport.setPaintFlags(holder.btnReport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // --- BUTTON CLICKS ---
        holder.btnChat.setOnClickListener(v -> listener.onChatClick(mover));
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(mover));
        holder.btnReviews.setOnClickListener(v -> listener.onReviewsClick(mover));
        holder.btnReport.setOnClickListener(v -> listener.onReportClick(mover));
    }

    @Override
    public int getItemCount() {
        return moverList.size();
    }

    // --- ViewHolder Class ---
    public static class MoverViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView tvName, tvDistance, tvRating, btnReviews, tvAbout;
        Button btnDetails, btnChat, btnReport;

        public MoverViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgMoverProfile);
            tvName = itemView.findViewById(R.id.tvMoverName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnReviews = itemView.findViewById(R.id.btnReviews); // This is a clickable TextView
            tvAbout = itemView.findViewById(R.id.tvAboutPreview);

            btnDetails = itemView.findViewById(R.id.btnMoverDetails);
            btnChat = itemView.findViewById(R.id.btnChatWithMover);
            btnReport = itemView.findViewById(R.id.btnReportMover);
        }
    }
}