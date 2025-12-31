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

/**
 * PotentialPartnerAdapter
 * -----------------------
 * RecyclerView Adapter responsible for displaying a list of potential move partners.
 *
 * Each item represents a {@link UserProfile} and displays:
 * - User name
 * - Next move date (if available)
 * - Profile image
 * - Invite button
 *
 * The adapter communicates user actions (Invite clicks)
 * back to the hosting Fragment/Activity via {@link OnInviteClickListener}.
 */
public class PotentialPartnerAdapter
        extends RecyclerView.Adapter<PotentialPartnerAdapter.ViewHolder> {

    /**
     * List of users displayed in the RecyclerView.
     * Initialized as an empty list to avoid null checks.
     */
    private List<UserProfile> users = new ArrayList<>();

    /**
     * Listener used to notify when the invite button is clicked.
     */
    private final OnInviteClickListener listener;

    /**
     * Callback interface for invite button actions.
     * Implemented by the Fragment or Activity that owns this adapter.
     */
    public interface OnInviteClickListener {
        /**
         * Called when the invite button is clicked for a specific user.
         *
         * @param user The selected {@link UserProfile}
         */
        void onInviteClick(UserProfile user);
    }

    /**
     * Constructor
     *
     * @param listener Listener for invite button click events
     */
    public PotentialPartnerAdapter(OnInviteClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the adapter data and refreshes the RecyclerView.
     *
     * @param users New list of potential partners
     */
    public void setUsers(List<UserProfile> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    /**
     * Inflates the layout for a single potential partner item.
     *
     * @param parent   Parent ViewGroup (RecyclerView)
     * @param viewType View type (single layout used)
     * @return A new {@link ViewHolder} instance
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_potential_partner, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds user data to the ViewHolder views.
     *
     * @param holder   ViewHolder instance
     * @param position Position of the item in the list
     */
    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        UserProfile user = users.get(position);

        // ---- User Name ----
        holder.name.setText(user.getName());

        // ---- Move Date Display ----
        // Displays next move date if available, otherwise a fallback text
        String infoText;
        if (user.getNextMoveDate() != null && user.getNextMoveDate() > 0) {
            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr =
                    sdf.format(new Date(user.getNextMoveDate()));
            infoText = "תאריך מעבר: " + dateStr;
        } else {
            infoText = "טרם נקבע תאריך";
        }
        holder.details.setText(infoText);

        // ---- Profile Image Loading ----
        // Uses Glide for efficient image loading and caching
        if (user.getProfileImageUrl() != null &&
                !user.getProfileImageUrl().isEmpty()) {
            try {
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(holder.image);
            } catch (Exception e) {
                // Fail silently to avoid crashing the UI
            }
        } else {
            holder.image.setImageResource(
                    android.R.drawable.sym_def_app_icon
            );
        }

        // ---- Invite Button Action ----
        holder.btnInvite.setOnClickListener(
                v -> listener.onInviteClick(user)
        );
    }

    /**
     * @return Number of users in the adapter
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder
     * ----------
     * Holds references to views inside a single potential partner item.
     * Improves RecyclerView performance by avoiding repeated view lookups.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        TextView details;
        ImageView image;
        Button btnInvite;

        /**
         * ViewHolder constructor.
         *
         * @param itemView Root view of the item layout
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.tvPartnerName);
            details = itemView.findViewById(R.id.tvPartnerDetails);
            image = itemView.findViewById(R.id.imgPartnerProfile);
            btnInvite = itemView.findViewById(R.id.btnInvitePartner);
        }
    }
}