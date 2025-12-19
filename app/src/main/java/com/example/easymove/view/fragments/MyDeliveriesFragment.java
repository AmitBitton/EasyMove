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
import com.example.easymove.model.MoveRequest;
import com.example.easymove.view.activities.ChatActivity;
import com.example.easymove.viewmodel.MyDeliveriesViewModel;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_deliveries, container, false);

        // 1. אתחול הרכיבים מה-XML
        recyclerView = view.findViewById(R.id.recyclerMyDeliveries);
        tvEmpty = view.findViewById(R.id.tvEmptyDeliveries);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. יצירת האדפטר
        adapter = new MyDeliveriesAdapter(new MyDeliveriesAdapter.OnDeliveryActionClickListener() {
            @Override
            public void onChatClick(MoveRequest move) {
                // מעבר לצ'אט
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", move.getChatId());
                startActivity(intent);
            }

            @Override
            public void onDetailsClick(MoveRequest move) {
                // בהמשך כאן נפתח דיאלוג עם פרטים
                Toast.makeText(getContext(), "פרטי ההובלה", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 3. חיבור ל-ViewModel
        viewModel = new ViewModelProvider(this).get(MyDeliveriesViewModel.class);

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

        viewModel.loadMyDeliveries();
    }

    @Override
    public void onResume() {
        super.onResume();
        // רענון הרשימה בכל פעם שחוזרים למסך
        if (viewModel != null) {
            viewModel.loadMyDeliveries();
        }
    }
}