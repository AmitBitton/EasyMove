package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
 * PartnerMatchFragment
 * --------------------
 * Handles the partner-matching feature of the application.
 *
 * Responsibilities:
 * - Display a searchable list of potential partners
 * - Display incoming match requests
 * - Allow sending, approving, and rejecting match requests
 * - Observe and react to ViewModel state changes (MVVM architecture)
 *
 * Data is provided by {@link PartnerMatchViewModel}.
 */
public class PartnerMatchFragment extends Fragment {

    /** ViewModel that manages partner matching logic */
    private PartnerMatchViewModel viewModel;

    /** RecyclerView for potential partners list */
    private RecyclerView rvPotential;

    /** RecyclerView for incoming match requests */
    private RecyclerView rvIncoming;

    /** SearchView for filtering potential partners */
    private SearchView searchView;

    /** Adapter for potential partners list */
    private PotentialPartnerAdapter partnerAdapter;

    /** Adapter for incoming requests list */
    private IncomingRequestAdapter requestAdapter;

    /**
     * Inflates the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_partner_match,
                container,
                false
        );
    }

    /**
     * Called after the fragment view has been created.
     * Initializes ViewModel, views, adapters, observers, and listeners.
     */
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        // ---- ViewModel Initialization ----
        viewModel = new ViewModelProvider(this)
                .get(PartnerMatchViewModel.class);

        // ---- View Binding ----
        rvPotential = view.findViewById(R.id.rvPotentialPartners);
        rvIncoming = view.findViewById(R.id.rvIncomingRequests);
        searchView = view.findViewById(R.id.searchViewPartners);

        // ---- Setup RecyclerViews and Adapters ----
        setupAdapters();

        // ---- Observe LiveData from ViewModel ----
        observeViewModel();

        // ---- Search Listener ----
        // Triggers filtering in real time as the user types
        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        viewModel.searchPartners(query);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        viewModel.searchPartners(newText);
                        return false;
                    }
                }
        );

        // ---- Initial Data Load ----
        viewModel.loadData();
    }

    /**
     * Initializes RecyclerView adapters and their interaction callbacks.
     */
    private void setupAdapters() {

        // ---- Potential Partners Adapter ----
        // Sends a match request when invite button is clicked
        partnerAdapter = new PotentialPartnerAdapter(user -> {
            viewModel.sendRequest(user);
        });

        rvPotential.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        rvPotential.setAdapter(partnerAdapter);

        // ---- Incoming Requests Adapter ----
        // Handles approve and reject actions
        requestAdapter =
                new IncomingRequestAdapter(
                        new IncomingRequestAdapter.OnActionListener() {

                            @Override
                            public void onApprove(MatchRequest request) {
                                viewModel.approveRequest(request);
                            }

                            @Override
                            public void onReject(MatchRequest request) {
                                viewModel.rejectRequest(request);
                            }
                        }
                );

        rvIncoming.setLayoutManager(
                new LinearLayoutManager(getContext())
        );
        rvIncoming.setAdapter(requestAdapter);
    }

    /**
     * Observes LiveData objects from the ViewModel and updates the UI accordingly.
     */
    private void observeViewModel() {

        // ---- Potential Partners Observer ----
        viewModel.getPotentialPartners()
                .observe(
                        getViewLifecycleOwner(),
                        users -> partnerAdapter.setUsers(users)
                );

        // ---- Incoming Requests Observer ----
        // Shows or hides the entire "Incoming Requests" section
        viewModel.getIncomingRequests()
                .observe(
                        getViewLifecycleOwner(),
                        requests -> {

                            // Find the container layout that wraps the incoming requests block
                            View layoutIncoming =
                                    getView().findViewById(R.id.layoutIncoming);

                            if (layoutIncoming != null) {
                                if (requests != null && !requests.isEmpty()) {
                                    layoutIncoming.setVisibility(View.VISIBLE);
                                    requestAdapter.setRequests(requests);
                                } else {
                                    layoutIncoming.setVisibility(View.GONE);
                                }
                            }
                        }
                );

        // ---- Toast Messages Observer ----
        // Used for feedback messages (success / error)
        viewModel.getToastMessage()
                .observe(
                        getViewLifecycleOwner(),
                        msg -> {
                            if (msg != null) {
                                Toast.makeText(
                                        getContext(),
                                        msg,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
    }
}
