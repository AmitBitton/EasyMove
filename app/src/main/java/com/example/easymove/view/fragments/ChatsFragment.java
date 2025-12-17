package com.example.easymove.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.ChatsListAdapter;
import com.example.easymove.view.activities.ChatActivity;
import com.example.easymove.viewmodel.ChatViewModel;

import java.util.ArrayList;

public class ChatsFragment extends Fragment {

    private ChatViewModel chatViewModel;
    private ChatsListAdapter adapter;
    private TextView tvEmpty;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerChatsList);
        tvEmpty = view.findViewById(R.id.tvEmptyChats);
        progressBar = view.findViewById(R.id.progressChats);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // לחיצה על צ'אט ברשימה פותחת את מסך ההודעות
        adapter = new ChatsListAdapter(chat -> {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("CHAT_ID", chat.getId());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // האזנה לשינויים ברשימה
        chatViewModel.getUserChatsLiveData().observe(getViewLifecycleOwner(), chats -> {
            if (chats == null || chats.isEmpty()) {
                adapter.setChats(new ArrayList<>());
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                adapter.setChats(chats);
            }
        });

        chatViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        // טעינת הנתונים
        chatViewModel.loadUserChats();
    }

    @Override
    public void onResume() {
        super.onResume();
        // רענון הרשימה כשחוזרים ממסך השיחה (כדי לעדכן את "ההודעה האחרונה")
        chatViewModel.loadUserChats();
    }
}