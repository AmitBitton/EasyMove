package com.example.easymove.adapters;

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

public class MoversAdapter extends RecyclerView.Adapter<MoversAdapter.MoverViewHolder> {

    private List<UserProfile> movers = new ArrayList<>();
    private final OnMoverClickListener listener;

    public interface OnMoverClickListener {
        void onChatClick(UserProfile mover);
    }

    public MoversAdapter(OnMoverClickListener listener) {
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

        // הצגת רשימת אזורים
        if (mover.getServiceAreas() != null && !mover.getServiceAreas().isEmpty()) {
            holder.tvAreas.setText("אזורים: " + String.join(", ", mover.getServiceAreas()));
        } else {
            holder.tvAreas.setText("אזורים: לא צוין");
        }

        // הצגת דירוג (אם יש)
        holder.tvRating.setText(mover.getRating() > 0 ? String.format("%.1f (%d)", mover.getRating(), mover.getRatingCount()) : "חדש!");

        // תמונה
        if (mover.getProfileImageUrl() != null && !mover.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(mover.getProfileImageUrl()).circleCrop().into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnChat.setOnClickListener(v -> listener.onChatClick(mover));
    }

    @Override
    public int getItemCount() {
        return movers.size();
    }

    static class MoverViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvAreas, tvRating, tvAbout;
        Button btnChat;

        public MoverViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.imgMoverProfile);
            tvName = itemView.findViewById(R.id.tvMoverName);
            tvAreas = itemView.findViewById(R.id.tvServiceAreas);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAbout = itemView.findViewById(R.id.tvAbout);
            btnChat = itemView.findViewById(R.id.btnChatWithMover);
        }
    }
}