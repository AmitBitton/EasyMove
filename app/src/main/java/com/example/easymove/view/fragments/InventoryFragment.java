package com.example.easymove.view.fragments;

import android.app.AlertDialog;
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
import com.example.easymove.adapters.InventoryAdapter;
import com.example.easymove.model.InventoryItem;
import com.example.easymove.viewmodel.InventoryViewModel;
import com.example.easymove.viewmodel.ViewModelFactoryProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class InventoryFragment extends Fragment {

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, ViewModelFactoryProvider.factory).get(InventoryViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerInventory);
        textEmpty = view.findViewById(R.id.textEmptyInventory);
        FloatingActionButton fab = view.findViewById(R.id.fabAddItem);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new InventoryAdapter(new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(InventoryItem item) {
                new AlertDialog.Builder(getContext())
                        .setTitle("מחיקה")
                        .setMessage("למחוק את " + item.getName() + "?")
                        .setPositiveButton("כן", (d, w) -> viewModel.deleteItem(item))
                        .setNegativeButton("לא", null)
                        .show();
            }

            @Override
            public void onItemClick(InventoryItem item) {
                // אופציה לעתיד: עריכה
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getInventoryList().observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
            textEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        fab.setOnClickListener(v -> {
            AddItemDialogFragment dialog = new AddItemDialogFragment();
            dialog.show(getChildFragmentManager(), "AddItemDialog");
        });
    }

    // ✅ חדש: מתחילים להאזין כשנכנסים למסך (זה המקום הנכון ל-listener)
    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) {
            viewModel.startInventoryListener();
        }
    }

    // ✅ חדש: עוצרים את ההאזנה כשעוזבים את המסך
    @Override
    public void onStop() {
        super.onStop();
        if (viewModel != null) {
            viewModel.stopInventoryListener();
        }
    }
}
