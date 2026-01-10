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

// שימוש בפרגמנט החדש של ה-Bottom Sheet
import com.example.easymove.view.fragments.MoveDetailsBottomSheetFragment;

public class MyDeliveriesFragment extends Fragment {

    private MyDeliveriesViewModel viewModel;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MyDeliveriesAdapter adapter;

    public MyDeliveriesFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_deliveries, container, false);

        recyclerView = view.findViewById(R.id.recyclerMyDeliveries);
        tvEmpty = view.findViewById(R.id.tvEmptyDeliveries);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MyDeliveriesAdapter(new MyDeliveriesAdapter.OnDeliveryActionClickListener() {
            @Override
            public void onChatClick(MoveRequest move) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", move.getChatId());
                startActivity(intent);
            }

            @Override
            public void onDetailsClick(MoveRequest move, MatchRequest pendingRequest) {
                // פתיחת ה-Bottom Sheet עם פרטי ההובלה והבקשה (אם יש)
                MoveDetailsBottomSheetFragment bottomSheet = MoveDetailsBottomSheetFragment.newInstance(move, pendingRequest);

                // האזנה לאירועים מתוך ה-Bottom Sheet (אישור/דחייה)
                bottomSheet.setListener(new MoveDetailsBottomSheetFragment.OnActionListener() {
                    @Override
                    public void onApprove(MatchRequest req) {
                        viewModel.approvePartner(req);
                        Toast.makeText(getContext(), "השותף אושר בהצלחה!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReject(MatchRequest req) {
                        viewModel.rejectPartner(req);
                        Toast.makeText(getContext(), "הבקשה נדחתה", Toast.LENGTH_SHORT).show();
                    }
                });

                bottomSheet.show(getParentFragmentManager(), "MoveDetails");
            }
        });

        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MyDeliveriesViewModel.class);

        // האזנה לרשימת ההובלות
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

        // האזנה לבקשות שותפים (כדי לעדכן את הנקודות האדומות)
        viewModel.getActiveRequestsMap().observe(getViewLifecycleOwner(), map -> {
            adapter.setRequestsMap(map);
        });

        viewModel.loadMyDeliveries();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadMyDeliveries();
    }
}