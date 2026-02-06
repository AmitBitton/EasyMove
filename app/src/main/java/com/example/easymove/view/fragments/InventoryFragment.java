package com.example.easymove.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.Spinner;
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

import java.util.List;

/**
 * Fragment responsible for managing the user's Inventory.
 * Features:
 * 1. Displaying the list of items.
 * 2. Adding new items (via FAB).
 * 3. Deleting items (via Adapter callback).
 * 4. Sorting the list (Date, Quantity, Room, Fragility).
 * 5. Filtering the list (By Room, Fragility).
 */
public class InventoryFragment extends Fragment {

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;

    // UI Components
    private TextView textEmpty;
    private TextView tvActiveFilters; // Indicates if filters are currently applied

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel using a Factory to ensure dependencies are passed correctly
        viewModel = new ViewModelProvider(this, ViewModelFactoryProvider.getFactory())
                .get(InventoryViewModel.class);

        // 1. Initialize UI Elements
        RecyclerView recyclerView = view.findViewById(R.id.recyclerInventory);
        textEmpty = view.findViewById(R.id.textEmptyInventory);
        tvActiveFilters = view.findViewById(R.id.tvActiveFilters);
        FloatingActionButton fab = view.findViewById(R.id.fabAddItem);
        Button btnSort = view.findViewById(R.id.btnSort);
        Button btnFilter = view.findViewById(R.id.btnFilter);

        // 2. Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new InventoryAdapter(new InventoryAdapter.OnItemClickListener() {
            @Override
            public void onDeleteClick(InventoryItem item) {
                // Confirm deletion with user
                new AlertDialog.Builder(getContext())
                        .setTitle("מחיקה")
                        .setMessage("למחוק את " + item.getName() + "?")
                        .setPositiveButton("כן", (d, w) -> viewModel.deleteItem(item))
                        .setNegativeButton("לא", null)
                        .show();
            }

            @Override
            public void onItemClick(InventoryItem item) {
                // Optional: Open Edit Dialog here in the future
            }
        });
        recyclerView.setAdapter(adapter);

        // 3. Observe ViewModel Data
        viewModel.getInventoryList().observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
            // Toggle "Empty State" text visibility
            textEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Observe Toast messages (Success/Error feedback)
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // 4. Setup Button Listeners
        btnSort.setOnClickListener(this::showSortMenu);
        btnFilter.setOnClickListener(v -> showFilterDialog());
        fab.setOnClickListener(v -> {
            AddItemDialogFragment dialog = new AddItemDialogFragment();
            dialog.show(getChildFragmentManager(), "AddItemDialog");
        });
    }

    /**
     * Displays a PopupMenu for sorting options.
     */
    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);

        // Add menu items programmatically
        popup.getMenu().add(0, 1, 0, "תאריך (מהחדש לישן)");
        popup.getMenu().add(0, 2, 0, "כמות (מהגדול לקטן)");
        popup.getMenu().add(0, 3, 0, "חדר (א-ת)");
        popup.getMenu().add(0, 4, 0, "שביר קודם");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    viewModel.setSortOption(InventoryViewModel.SortOption.DATE_NEWEST);
                    return true;
                case 2:
                    viewModel.setSortOption(InventoryViewModel.SortOption.QUANTITY_DESC);
                    return true;
                case 3:
                    viewModel.setSortOption(InventoryViewModel.SortOption.ROOM_AZ);
                    return true;
                case 4:
                    viewModel.setSortOption(InventoryViewModel.SortOption.FRAGILE_FIRST);
                    return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Displays a custom Dialog for filtering the inventory list.
     * Allows filtering by Room Name and Fragility status.
     */
    private void showFilterDialog() {
        // 1. Prepare Data
        List<String> rooms = viewModel.getUniqueRooms();

        // 2. Inflate Custom Layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_inventory, null);

        // 3. Find Views inside the dialog
        Spinner spinnerRooms = dialogView.findViewById(R.id.spinnerFilterRoom);
        CheckBox cbFragile = dialogView.findViewById(R.id.cbFilterFragile);

        // 4. Setup Spinner Adapter
        ArrayAdapter<String> adapterRooms = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, rooms);
        adapterRooms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRooms.setAdapter(adapterRooms);

        // 5. Build Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("סינון מלאי");
        builder.setView(dialogView);

        // Action: Apply Filter
        builder.setPositiveButton("סנן", (dialog, which) -> {
            String selectedRoom = (String) spinnerRooms.getSelectedItem();
            boolean isFragile = cbFragile.isChecked();

            // Send to ViewModel
            viewModel.setFilters(selectedRoom, isFragile);

            // Update UI indication
            updateFilterText(selectedRoom, isFragile);
        });

        // Action: Reset Filter
        builder.setNegativeButton("בטל סינון", (dialog, which) -> {
            viewModel.setFilters(null, false);
            tvActiveFilters.setVisibility(View.GONE);
        });

        // Action: Cancel (Do nothing)
        builder.setNeutralButton("ביטול", null);

        builder.show();
    }

    /**
     * Updates the text view showing which filters are currently active.
     */
    private void updateFilterText(String room, boolean fragile) {
        StringBuilder sb = new StringBuilder("מסנן: ");
        boolean active = false;

        if (room != null && !room.equals("הכל")) {
            sb.append("חדר: ").append(room).append(" ");
            active = true;
        }
        if (fragile) {
            if (active) sb.append("| ");
            sb.append("רק שביר");
            active = true;
        }

        if (active) {
            tvActiveFilters.setText(sb.toString());
            tvActiveFilters.setVisibility(View.VISIBLE);
        } else {
            tvActiveFilters.setVisibility(View.GONE);
        }
    }

    // ------------------------------------------------------------------------
    // Lifecycle Management
    // ------------------------------------------------------------------------

    @Override
    public void onStart() {
        super.onStart();
        // Start listening to Real-time updates from Firestore
        if (viewModel != null) viewModel.startInventoryListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach listener to save resources
        if (viewModel != null) viewModel.stopInventoryListener();
    }
}