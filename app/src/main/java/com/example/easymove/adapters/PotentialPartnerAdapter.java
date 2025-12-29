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
import java.text.SimpleDateFormat; // אימפורט לעיצוב תאריך
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PotentialPartnerAdapter extends RecyclerView.Adapter<PotentialPartnerAdapter.ViewHolder> {

    private List<UserProfile> users = new ArrayList<>();
    private final OnInviteClickListener listener;

    public interface OnInviteClickListener {
        void onInviteClick(UserProfile user);
    }

    public PotentialPartnerAdapter(OnInviteClickListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<UserProfile> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_potential_partner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.name.setText(user.getName());

        // --- השינוי כאן: מציגים תאריך במקום כתובת ---
        String infoText;
        if (user.getNextMoveDate() != null && user.getNextMoveDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(user.getNextMoveDate()));
            infoText = "תאריך מעבר: " + dateStr;
        } else {
            infoText = "טרם נקבע תאריך";
        }

        holder.details.setText(infoText);
        // ----------------------------------------------

        // טעינת תמונה
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            try {
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(holder.image);
            } catch (Exception e) { }
        } else {
            holder.image.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        holder.btnInvite.setOnClickListener(v -> listener.onInviteClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, details;
        ImageView image;
        Button btnInvite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvPartnerName);
            details = itemView.findViewById(R.id.tvPartnerDetails);
            image = itemView.findViewById(R.id.imgPartnerProfile);
            btnInvite = itemView.findViewById(R.id.btnInvitePartner);
        }
    }
}