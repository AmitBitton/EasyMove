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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment responsible for displaying the user's notification history.
 * <p>
 * Features:
 * 1. Real-time updates from Firestore.
 * 2. Swipe-to-delete functionality.
 * 3. "Clear All" batch deletion.
 * 4. Empty state handling.
 */
public class NotificationsFragment extends Fragment {

    // UI Components
    private RecyclerView recyclerView;
    private NotificationsAdapter adapter;
    private TextView emptyStateText;

    // Data & Firebase
    private List<NotificationItem> notificationList;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration firestoreListener;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            return; // Should not happen if Auth flow is correct
        }

        // 2. Initialize UI
        recyclerView = view.findViewById(R.id.recyclerNotifications);
        emptyStateText = view.findViewById(R.id.textEmptyState);
        View tvClearAll = view.findViewById(R.id.tvClearAll);

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationsAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // 4. Setup Features
        setupSwipeToDelete();
        loadNotifications();

        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> clearAllNotifications());
        }
    }

    /**
     * Attaches a Real-time SnapshotListener to the user's notifications collection.
     * Updates the list automatically whenever a document is added/removed.
     */
    private void loadNotifications() {
        if (currentUserId == null) return;

        firestoreListener = db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value == null) return;

                    notificationList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        if (item != null) {
                            item.setId(doc.getId()); // Inject Document ID
                            notificationList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    /**
     * Toggles visibility of the "No Notifications" text.
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
     * Configures ItemTouchHelper to handle Swipe-to-Delete gestures.
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // Drag & Drop not supported
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationItem item = notificationList.get(position);

                // Optimistic Remove from UI
                notificationList.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyState();

                // Delete from Firestore
                db.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .document(item.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            if (getContext() != null) {
                                Toast.makeText(requireContext(), "ההתראה נמחקה", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    /**
     * Deletes all notifications using a WriteBatch.
     */
    private void clearAllNotifications() {
        if (notificationList.isEmpty()) return;

        WriteBatch batch = db.batch();

        for (NotificationItem item : notificationList) {
            batch.delete(db.collection("users")
                    .document(currentUserId)
                    .collection("notifications")
                    .document(item.getId()));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "כל ההתראות נמחקו", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Cleanup listener when view is destroyed to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) {
            firestoreListener.remove();
            firestoreListener = null;
        }
    }
}