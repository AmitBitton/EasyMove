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

/**
 * Fragment responsible for displaying the list of active chats for the current user.
 * It handles:
 * 1. Fetching the user's chat history.
 * 2. Displaying chats in a RecyclerView.
 * 3. Handling navigation to a specific chat room {@link ChatActivity}.
 * 4. Refreshing the list when returning from a conversation.
 */
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

        // 1. Initialize ViewModel
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // 2. Initialize UI Components
        RecyclerView recyclerView = view.findViewById(R.id.recyclerChatsList);
        tvEmpty = view.findViewById(R.id.tvEmptyChats);
        progressBar = view.findViewById(R.id.progressChats);

        // 3. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Adapter with Click Listener
        adapter = new ChatsListAdapter(chat -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("CHAT_ID", chat.getId()); // Pass Chat ID to load messages
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 4. Observe Data Changes
        observeViewModel();

        // 5. Initial Data Load
        chatViewModel.loadUserChats();
    }

    private void observeViewModel() {
        // Observe Chat List
        chatViewModel.getUserChatsLiveData().observe(getViewLifecycleOwner(), chats -> {
            if (chats == null || chats.isEmpty()) {
                // Show "Empty State"
                adapter.setChats(new ArrayList<>());
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                // Show List
                tvEmpty.setVisibility(View.GONE);
                adapter.setChats(chats);
            }
        });

        // Observe Loading State
        chatViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list when returning from the Chat Activity.
        // This ensures the "Last Message" and timestamps are up-to-date.
        chatViewModel.loadUserChats();
    }
}