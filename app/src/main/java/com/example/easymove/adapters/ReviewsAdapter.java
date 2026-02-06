package com.example.easymove.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.model.Review;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter responsible for displaying user reviews.
 * Handles the display of the reviewer's name, the review text, and converts
 * numeric ratings into a visual star representation (⭐⭐⭐⭐⭐).
 */
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private final List<Review> reviews = new ArrayList<>();

    /**
     * Updates the list of reviews and refreshes the UI.
     *
     * @param list The new list of reviews.
     */
    public void setReviews(List<Review> list) {
        reviews.clear();
        if (list != null) {
            reviews.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review r = reviews.get(position);

        // Bind Name (Default to "User" if null)
        holder.tvReviewerName.setText(r.getReviewerName() != null ? r.getReviewerName() : "משתמש");

        // Bind Review Text
        holder.tvReviewText.setText(r.getText() != null ? r.getText() : "");

        // Bind Stars (Convert int to Emoji string)
        holder.tvStars.setText(getStarRatingString(r.getStars()));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    /**
     * Helper method to convert a numeric integer (1-5) into a string of star emojis.
     *
     * @param stars The number of stars.
     * @return A string containing the corresponding number of "⭐".
     */
    private String getStarRatingString(int stars) {
        // Clamp value between 1 and 5
        if (stars < 1) stars = 1;
        if (stars > 5) stars = 5;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("⭐");
        }
        return sb.toString();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvReviewerName;
        final TextView tvStars;
        final TextView tvReviewText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvStars = itemView.findViewById(R.id.tvStars);
            tvReviewText = itemView.findViewById(R.id.tvReviewText);
        }
    }
}