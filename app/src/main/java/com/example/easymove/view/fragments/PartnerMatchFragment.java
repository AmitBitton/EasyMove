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

public class PartnerMatchFragment extends Fragment {

    private PartnerMatchViewModel viewModel;
    private RecyclerView rvPotential, rvIncoming;
    private SearchView searchView;
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

        viewModel = new ViewModelProvider(this).get(PartnerMatchViewModel.class);

        // אתחול Views
        rvPotential = view.findViewById(R.id.rvPotentialPartners);
        rvIncoming = view.findViewById(R.id.rvIncomingRequests);
        searchView = view.findViewById(R.id.searchViewPartners);

        setupAdapters();
        observeViewModel();

        // מאזין לחיפוש
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        });

        // טעינה ראשונית
        viewModel.loadData();
    }

    private void setupAdapters() {
        // רשימת חיפוש
        partnerAdapter = new PotentialPartnerAdapter(user -> {
            viewModel.sendRequest(user);
        });
        rvPotential.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPotential.setAdapter(partnerAdapter);

        // רשימת בקשות נכנסות
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

    private void observeViewModel() {
        viewModel.getPotentialPartners().observe(getViewLifecycleOwner(), users -> {
            partnerAdapter.setUsers(users);
        });

        // הצגה/הסתרה של בקשות נכנסות
        viewModel.getIncomingRequests().observe(getViewLifecycleOwner(), requests -> {
            View layoutIncoming = getView().findViewById(R.id.layoutIncoming);

            if (layoutIncoming != null) {
                if (requests != null && !requests.isEmpty()) {
                    layoutIncoming.setVisibility(View.VISIBLE);
                    requestAdapter.setRequests(requests);
                } else {
                    layoutIncoming.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }
}