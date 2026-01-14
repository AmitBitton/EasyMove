package com.example.easymove.view.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
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

public class InventoryFragment extends Fragment {

    private InventoryViewModel viewModel;
    private InventoryAdapter adapter;
    private TextView textEmpty;
    private TextView tvActiveFilters; // הטקסט שמציג אם יש סינון פעיל

    // משתנים לשמירת מצב הסינון הזמני בדיאלוג
    private int tempSelectedRoomIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inventory, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, ViewModelFactoryProvider.factory).get(InventoryViewModel.class);

        // חיבור רכיבים
        RecyclerView recyclerView = view.findViewById(R.id.recyclerInventory);
        textEmpty = view.findViewById(R.id.textEmptyInventory);
        tvActiveFilters = view.findViewById(R.id.tvActiveFilters);
        FloatingActionButton fab = view.findViewById(R.id.fabAddItem);
        Button btnSort = view.findViewById(R.id.btnSort);
        Button btnFilter = view.findViewById(R.id.btnFilter);

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
            public void onItemClick(InventoryItem item) { }
        });
        recyclerView.setAdapter(adapter);

        // --- Observers ---
        viewModel.getInventoryList().observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items);
            textEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // --- Listeners לכפתורי מיון וסינון ---

        btnSort.setOnClickListener(v -> showSortMenu(v));

        btnFilter.setOnClickListener(v -> showFilterDialog());

        fab.setOnClickListener(v -> {
            AddItemDialogFragment dialog = new AddItemDialogFragment();
            dialog.show(getChildFragmentManager(), "AddItemDialog");
        });
    }

    private void showSortMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        // יצירת התפריט ידנית בקוד (או דרך XML resource)
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

    private void showFilterDialog() {
        // 1. הכנת הנתונים
        List<String> rooms = viewModel.getUniqueRooms();

        // 2. ניפוח ה-XML שיצרנו
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_filter_inventory, null);

        // 3. איתור הרכיבים מתוך ה-View
        android.widget.Spinner spinnerRooms = dialogView.findViewById(R.id.spinnerFilterRoom);
        android.widget.CheckBox cbFragile = dialogView.findViewById(R.id.cbFilterFragile);

        // 4. הגדרת האדפטר לספינר
        android.widget.ArrayAdapter<String> adapterRooms = new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        spinnerRooms.setAdapter(adapterRooms);

        // ✅ טיפ: אם כבר יש סינון פעיל, נציג אותו כבחור מראש
        // (זה דורש לשמור את המצב ב-ViewModel ולחשוף אותו, אבל כרגע נשאיר פשוט)

        // 5. בניית הדיאלוג - בלי setSingleChoiceItems!
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("סינון מלאי");
        builder.setView(dialogView); // רק ה-View שלנו

        builder.setPositiveButton("סנן", (dialog, which) -> {
            String selectedRoom = (String) spinnerRooms.getSelectedItem();
            boolean isFragile = cbFragile.isChecked();

            // שליחת הנתונים ל-ViewModel
            viewModel.setFilters(selectedRoom, isFragile);

            // עדכון טקסט חיווי למשתמש
            updateFilterText(selectedRoom, isFragile);
        });

        builder.setNegativeButton("בטל סינון", (dialog, which) -> {
            viewModel.setFilters(null, false);
            tvActiveFilters.setVisibility(View.GONE);
        });

        builder.setNeutralButton("ביטול", null); // סוגר את החלון בלי לשנות כלום

        builder.show();
    }

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

    @Override
    public void onStart() {
        super.onStart();
        if (viewModel != null) viewModel.startInventoryListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (viewModel != null) viewModel.stopInventoryListener();
    }
}