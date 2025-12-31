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
 * <b>ChatsFragment</b>
 * <p>
 * This Fragment is responsible for displaying a historical list of all chat conversations
 * associated with the currently logged-in user. It follows the MVVM architectural pattern.
 * </p>
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 * <li>Initializing the {@link RecyclerView} and its {@link ChatsListAdapter}.</li>
 * <li>Observing {@link androidx.lifecycle.LiveData} from {@link ChatViewModel} to react to data changes.</li>
 * <li>Handling UI states: Loading (ProgressBar), Empty (TextView), and Content (RecyclerView).</li>
 * <li>Navigating to the detailed {@link ChatActivity} when a specific chat item is clicked.</li>
 * </ul>
 *
 * @author [Your Name/Organization]
 * @version 1.0
 * @see ChatViewModel
 * @see ChatsListAdapter
 */
public class ChatsFragment extends Fragment {

    // Region: Fields
    /**
     * ViewModel that handles the business logic and data fetching for chats.
     */
    private ChatViewModel chatViewModel;

    /**
     * Adapter responsible for binding chat data to the RecyclerView rows.
     */
    private ChatsListAdapter adapter;

    /**
     * TextView displayed when the chat list is empty.
     */
    private TextView tvEmpty;

    /**
     * ProgressBar displayed while data is being fetched from the repository.
     */
    private ProgressBar progressBar;
    // End Region: Fields

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     * any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the XML layout for this fragment
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * <p>
     * This is the primary location for initialization logic, such as finding views,
     * setting up adapters, and observing ViewModel LiveData.
     * </p>
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        // We use ViewModelProvider to ensure the ViewModel survives configuration changes.
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // 2. Initialize UI Components
        RecyclerView recyclerView = view.findViewById(R.id.recyclerChatsList);
        tvEmpty = view.findViewById(R.id.tvEmptyChats);
        progressBar = view.findViewById(R.id.progressChats);

        // Set LayoutManager for vertical list presentation
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3. Setup RecyclerView Adapter
        // We pass a lambda function to the adapter to handle item clicks.
        adapter = new ChatsListAdapter(chat -> {
            // Create an intent to navigate to the specific ChatActivity
            Intent intent = new Intent(getContext(), ChatActivity.class);

            // Pass the unique Chat ID so the Activity knows which conversation to load
            intent.putExtra("CHAT_ID", chat.getId());

            // Execute navigation
            startActivity(intent);
        });

        // Attach adapter to the RecyclerView
        recyclerView.setAdapter(adapter);

        // 4. Observe Data Changes (MVVM Pattern)
        // Observe the list of chats. This triggers whenever the database/network updates the list.
        chatViewModel.getUserChatsLiveData().observe(getViewLifecycleOwner(), chats -> {
            if (chats == null || chats.isEmpty()) {
                // Scenario: No chats available
                adapter.setChats(new ArrayList<>()); // Clear any existing data in adapter
                tvEmpty.setVisibility(View.VISIBLE); // Show "No chats" message
            } else {
                // Scenario: Chats exist
                tvEmpty.setVisibility(View.GONE);    // Hide "No chats" message
                adapter.setChats(chats);             // Submit new list to adapter
            }
        });

        // 5. Observe Loading State
        // Toggle the progress bar based on the ViewModel's loading state
        chatViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        // 6. Trigger Initial Data Load
        // Instruct the ViewModel to begin fetching data from the repository
        chatViewModel.loadUserChats();
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * <p>
     * This method is overridden to refresh the chat list. This is crucial because
     * when a user returns from {@link ChatActivity}, the "last message" or "timestamp"
     * of a chat might have changed, and the list needs to reflect that update immediately.
     * </p>
     */
    @Override
    public void onResume() {
        super.onResume();
        // Trigger a reload of the chat list to ensure data freshness
        // (e.g., updating the last message preview after sending a message in ChatActivity)
        chatViewModel.loadUserChats();
    }
}