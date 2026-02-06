package com.example.easymove.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.MyDeliveriesAdapter;
import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.view.activities.ChatActivity;
import com.example.easymove.viewmodel.MyDeliveriesViewModel;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Fragment for Movers to manage their active jobs (Deliveries).
 * Features:
 * 1. Displays a list of assigned moves.
 * 2. Handles navigation to Chat.
 * 3. Handles "Details" click to open the {@link MoveDetailsBottomSheetFragment}.
 * 4. Manages approval/rejection of Partner Requests via the BottomSheet.
 */
public class MyDeliveriesFragment extends Fragment {

    private MyDeliveriesViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MyDeliveriesAdapter adapter;

    public MyDeliveriesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_deliveries, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MyDeliveriesViewModel.class);

        // 2. Initialize UI Components
        recyclerView = view.findViewById(R.id.recyclerMyDeliveries);
        tvEmpty = view.findViewById(R.id.tvEmptyDeliveries);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Setup Adapter with Callbacks
        setupAdapter();

        // 4. Observe Data
        observeViewModel();

        // 5. Initial Data Load
        viewModel.loadMyDeliveries();
    }

    /**
     * Sets up the RecyclerView Adapter and defines the actions for Chat and Details clicks.
     */
    private void setupAdapter() {
        adapter = new MyDeliveriesAdapter(new MyDeliveriesAdapter.OnDeliveryActionClickListener() {
            @Override
            public void onChatClick(MoveRequest move) {
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", move.getChatId());
                startActivity(intent);
            }

            @Override
            public void onDetailsClick(MoveRequest move, MatchRequest pendingRequest) {
                showMoveDetailsBottomSheet(move, pendingRequest);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    /**
     * Opens the Bottom Sheet to show move details and handle Partner/Cancellation approvals.
     */
    private void showMoveDetailsBottomSheet(MoveRequest move, MatchRequest pendingRequest) {
        MoveDetailsBottomSheetFragment bottomSheet = MoveDetailsBottomSheetFragment.newInstance(move, pendingRequest);

        bottomSheet.setListener(new MoveDetailsBottomSheetFragment.OnActionListener() {
            @Override
            public void onApprove(MatchRequest req) {
                viewModel.approvePartner(req);
                Toast.makeText(requireContext(), "השותף אושר בהצלחה!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(MatchRequest req) {
                viewModel.rejectPartner(req);
                Toast.makeText(requireContext(), "הבקשה נדחתה", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onApproveCancel(MoveRequest move) {
                String currentMoverId = FirebaseAuth.getInstance().getUid();
                if (currentMoverId != null) {
                    viewModel.approveCancel(move, currentMoverId);
                    Toast.makeText(requireContext(), "הביטול אושר ✅", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "שגיאת זיהוי משתמש", Toast.LENGTH_SHORT).show();
                }
            }
        });

        bottomSheet.show(getParentFragmentManager(), "MoveDetails");
    }

    private void observeViewModel() {
        // Observe list of active deliveries
        viewModel.getDeliveries().observe(getViewLifecycleOwner(), moves -> {
            if (moves != null && !moves.isEmpty()) {
                adapter.setDeliveryList(moves);
                tvEmpty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        // Observe pending partner requests (to update red notification dots on cards)
        viewModel.getActiveRequestsMap().observe(getViewLifecycleOwner(),
                map -> adapter.setRequestsMap(map));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to the screen
        if (viewModel != null) {
            viewModel.loadMyDeliveries();
        }
    }
}