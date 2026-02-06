package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.IncomingRequestAdapter;
import com.example.easymove.adapters.PotentialPartnerAdapter;
import com.example.easymove.model.MatchRequest;
import com.example.easymove.viewmodel.PartnerMatchViewModel;

/**
 * Fragment responsible for the "Partner Matching" feature.
 * Allows a user to:
 * 1. Search for potential partners (Customers in the same area).
 * 2. Send partnership requests.
 * 3. View and Approve/Reject incoming partnership requests.
 */
public class PartnerMatchFragment extends Fragment {

    private PartnerMatchViewModel viewModel;

    // UI Components
    private RecyclerView rvPotential;
    private RecyclerView rvIncoming;
    private SearchView searchView;
    private LinearLayout layoutIncoming; // Container for the incoming requests section

    // Adapters
    private PotentialPartnerAdapter partnerAdapter;
    private IncomingRequestAdapter requestAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_partner_match, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PartnerMatchViewModel.class);

        // 2. Initialize Views
        rvPotential = view.findViewById(R.id.rvPotentialPartners);
        rvIncoming = view.findViewById(R.id.rvIncomingRequests);
        searchView = view.findViewById(R.id.searchViewPartners);
        layoutIncoming = view.findViewById(R.id.layoutIncoming);

        // 3. Setup Logic
        setupAdapters();
        observeViewModel();
        setupSearchListener();

        // 4. Initial Data Load
        viewModel.loadData();
    }

    private void setupAdapters() {
        // --- Potential Partners List (Search Results) ---
        partnerAdapter = new PotentialPartnerAdapter(user -> {
            // Click listener: Send a request to this user
            viewModel.sendRequest(user);
        });
        rvPotential.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPotential.setAdapter(partnerAdapter);

        // --- Incoming Requests List ---
        requestAdapter = new IncomingRequestAdapter(new IncomingRequestAdapter.OnActionListener() {
            @Override
            public void onApprove(MatchRequest request) {
                viewModel.approveRequest(request);
            }

            @Override
            public void onReject(MatchRequest request) {
                viewModel.rejectRequest(request);
            }
        });
        rvIncoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rvIncoming.setAdapter(requestAdapter);
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchPartners(query);
                searchView.clearFocus(); // Hide keyboard on submit
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.searchPartners(newText);
                return false;
            }
        });
    }

    private void observeViewModel() {
        // Observe Search Results
        viewModel.getPotentialPartners().observe(getViewLifecycleOwner(), users -> {
            if (partnerAdapter != null) {
                partnerAdapter.setUsers(users);
            }
        });

        // Observe Incoming Requests & Toggle Visibility
        viewModel.getIncomingRequests().observe(getViewLifecycleOwner(), requests -> {
            if (layoutIncoming == null) return;

            if (requests != null && !requests.isEmpty()) {
                layoutIncoming.setVisibility(View.VISIBLE);
                requestAdapter.setRequests(requests);
            } else {
                layoutIncoming.setVisibility(View.GONE);
            }
        });

        // Observe Feedback Messages (Toasts)
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && getContext() != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}