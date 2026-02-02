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

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private final List<Review> reviews = new ArrayList<>();

    public void setReviews(List<Review> list) {
        reviews.clear();
        if (list != null) reviews.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review r = reviews.get(position);

        holder.tvReviewerName.setText(r.getReviewerName() != null ? r.getReviewerName() : "משתמש");
        holder.tvStars.setText(starsText(r.getStars()));
        holder.tvReviewText.setText(r.getText() != null ? r.getText() : "");
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    private String starsText(int stars) {
        if (stars < 1) stars = 1;
        if (stars > 5) stars = 5;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) sb.append("⭐");
        return sb.toString();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReviewerName, tvStars, tvReviewText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvStars = itemView.findViewById(R.id.tvStars);
            tvReviewText = itemView.findViewById(R.id.tvReviewText);
        }
    }
}
