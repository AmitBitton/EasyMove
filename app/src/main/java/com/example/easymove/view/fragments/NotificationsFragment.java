package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.NotificationsAdapter;
import com.example.easymove.model.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsFragment
 * ---------------------
 * Displays the user's notifications list.
 *
 * Features:
 * - Real-time updates using Firestore SnapshotListener
 * - Swipe left/right to delete a single notification
 * - "Clear all" button to delete all notifications
 * - Empty-state UI when no notifications exist
 *
 * Notifications are stored under:
 * users/{userId}/notifications
 */
public class NotificationsFragment extends Fragment {

    /** RecyclerView that displays notifications */
    private RecyclerView recyclerView;

    /** Adapter that binds notification data */
    private NotificationsAdapter adapter;

    /** In-memory list of notifications */
    private List<NotificationItem> notificationList;

    /** Text shown when there are no notifications */
    private TextView emptyStateText;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** UID of the currently logged-in user */
    private String currentUserId;

    /**
     * Inflates the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_notifications,
                container,
                false
        );
    }

    /**
     * Called after the fragment view is created.
     * Initializes UI components, Firestore, and listeners.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // ---- Firebase Initialization ----
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        // ---- View Binding ----
        recyclerView = view.findViewById(R.id.recyclerNotifications);
        emptyStateText = view.findViewById(R.id.textEmptyState);

        // ---- RecyclerView Setup ----
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext())
        );

        notificationList = new ArrayList<>();
        adapter = new NotificationsAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // ---- Enable Swipe-to-Delete ----
        setupSwipeToDelete();

        // ---- Load Notifications ----
        loadNotifications();

        // ---- Clear All Notifications Button ----
        View tvClearAll = view.findViewById(R.id.tvClearAll);
        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> clearAllNotifications());
        }
    }

    /**
     * Loads notifications in real-time using a Firestore SnapshotListener.
     * Automatically updates the UI when notifications are added or removed.
     */
    private void loadNotifications() {
        db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) return;

                    notificationList.clear();

                    if (value != null) {
                        for (var doc : value.getDocuments()) {
                            NotificationItem item =
                                    doc.toObject(NotificationItem.class);

                            if (item != null) {
                                item.setId(doc.getId());
                                notificationList.add(item);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Updates the empty-state UI depending on whether notifications exist.
     */
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Enables swipe left/right gestures to delete individual notifications.
     */
    private void setupSwipeToDelete() {

        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                ) {
                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target
                    ) {
                        return false;
                    }

                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            int direction
                    ) {
                        int position =
                                viewHolder.getAdapterPosition();
                        NotificationItem item =
                                notificationList.get(position);

                        // ---- Delete from Firestore ----
                        db.collection("users")
                                .document(currentUserId)
                                .collection("notifications")
                                .document(item.getId())
                                .delete()
                                .addOnSuccessListener(aVoid ->
                                        Toast.makeText(
                                                getContext(),
                                                "ההתראה נמחקה",
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );

                        // ---- Remove from UI immediately ----
                        notificationList.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateEmptyState();
                    }
                };

        new ItemTouchHelper(swipeCallback)
                .attachToRecyclerView(recyclerView);
    }

    /**
     * Deletes all notifications using a Firestore batch operation.
     */
    private void clearAllNotifications() {

        if (notificationList.isEmpty()) return;

        WriteBatch batch = db.batch();

        for (NotificationItem item : notificationList) {
            batch.delete(
                    db.collection("users")
                            .document(currentUserId)
                            .collection("notifications")
                            .document(item.getId())
            );
        }

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(
                                getContext(),
                                "כל ההתראות נמחקו",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                getContext(),
                                "שגיאה במחיקה",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}
