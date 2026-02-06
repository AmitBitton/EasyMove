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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter responsible for displaying a list of potential partners for a move.
 * Allows the user to view basic details (name, move date) and send an invite.
 */
public class PotentialPartnerAdapter extends RecyclerView.Adapter<PotentialPartnerAdapter.ViewHolder> {

    // Optimization: Define formatter once to avoid object creation during scrolling
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private List<UserProfile> users = new ArrayList<>();
    private final OnInviteClickListener listener;

    /**
     * Interface for handling click events on the "Invite" button.
     */
    public interface OnInviteClickListener {
        void onInviteClick(UserProfile user);
    }

    /**
     * Constructor for PotentialPartnerAdapter.
     *
     * @param listener The listener to handle invite clicks.
     */
    public PotentialPartnerAdapter(OnInviteClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of potential partners.
     *
     * @param users The new list of user profiles.
     */
    public void setUsers(List<UserProfile> users) {
        this.users = Objects.requireNonNullElseGet(users, ArrayList::new);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_potential_partner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.name.setText(user.getName());

        // --- Bind Move Date ---
        String infoText;
        if (user.getDefaultMoveDate() != null && user.getDefaultMoveDate() > 0) {
            String dateStr = DATE_FORMAT.format(new Date(user.getDefaultMoveDate()));
            infoText = "תאריך מעבר: " + dateStr;
        } else {
            infoText = "טרם נקבע תאריך";
        }
        holder.details.setText(infoText);

        // --- Load Profile Image ---
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // --- Handle Invite Click ---
        holder.btnInvite.setOnClickListener(v -> listener.onInviteClick(user));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder class to cache view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView details;
        final ImageView image;
        final Button btnInvite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvPartnerName);
            details = itemView.findViewById(R.id.tvPartnerDetails);
            image = itemView.findViewById(R.id.imgPartnerProfile);
            btnInvite = itemView.findViewById(R.id.btnInvitePartner);
        }
    }
}