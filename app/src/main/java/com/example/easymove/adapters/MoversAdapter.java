package com.example.easymove.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter responsible for displaying a list of Movers (Service Providers).
 * It handles the display of profile information, distance calculation, ratings,
 * and provides interaction callbacks for chat, details, reviews, and reporting.
 */
public class MoversAdapter extends RecyclerView.Adapter<MoversAdapter.MoverViewHolder> {

    private List<UserProfile> movers = new ArrayList<>();
    private final OnMoverActionClickListener listener;

    /**
     * Interface definition for callbacks to be invoked when a user interacts with a Mover card.
     */
    public interface OnMoverActionClickListener {
        void onChatClick(UserProfile mover);
        void onDetailsClick(UserProfile mover);
        void onReviewsClick(UserProfile mover);
        void onReportClick(UserProfile mover);
    }

    /**
     * Constructor for MoversAdapter.
     *
     * @param listener The listener that will handle button clicks.
     */
    public MoversAdapter(OnMoverActionClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of movers and refreshes the UI.
     *
     * @param movers The new list of user profiles (movers).
     */
    public void setMovers(List<UserProfile> movers) {
        this.movers = Objects.requireNonNullElseGet(movers, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MoverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mover_card, parent, false);
        return new MoverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoverViewHolder holder, int position) {
        UserProfile mover = movers.get(position);

        // --- Bind Basic Info ---
        holder.tvName.setText(mover.getName());
        holder.tvAbout.setText(mover.getAbout() != null ? mover.getAbout() : "אין פירוט נוסף");

        // Style the "Report" button (Underline)
        holder.btnReport.setPaintFlags(holder.btnReport.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // --- Bind Distance ---
        double distance = mover.getDistanceFromUser();
        if (distance > 0) {
            // Convert meters to km and format
            holder.tvDistance.setText(String.format(Locale.getDefault(), "מרחק: %.1f ק\"מ", distance / 1000));
        } else {
            holder.tvDistance.setText("מרחק לא ידוע");
        }

        // --- Bind Ratings ---
        if (mover.getRating() > 0) {
            holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", mover.getRating()));
        } else {
            holder.tvRating.setText("-");
        }
        holder.btnReviews.setText(String.format(Locale.getDefault(), "(%d ביקורות) לחץ לצפייה", mover.getRatingCount()));

        // --- Bind Image ---
        if (mover.getProfileImageUrl() != null && !mover.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(mover.getProfileImageUrl())
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // --- Setup Click Listeners ---
        holder.btnChat.setOnClickListener(v -> listener.onChatClick(mover));
        holder.btnDetails.setOnClickListener(v -> listener.onDetailsClick(mover));
        holder.btnReviews.setOnClickListener(v -> listener.onReviewsClick(mover));
        holder.btnReport.setOnClickListener(v -> listener.onReportClick(mover));
    }

    @Override
    public int getItemCount() {
        return movers.size();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class MoverViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivProfile;
        final TextView tvName, tvDistance, tvRating, tvAbout, btnReviews;
        final Button btnChat, btnDetails, btnReport;

        public MoverViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.imgMoverProfile);
            tvName = itemView.findViewById(R.id.tvMoverName);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnReviews = itemView.findViewById(R.id.btnReviews);
            tvAbout = itemView.findViewById(R.id.tvAboutPreview);

            btnChat = itemView.findViewById(R.id.btnChatWithMover);
            btnDetails = itemView.findViewById(R.id.btnMoverDetails);
            btnReport = itemView.findViewById(R.id.btnReportMover);
        }
    }
}