package com.example.easymove.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.R;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.viewmodel.MyMoveViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyMoveFragment extends Fragment {

    private MyMoveViewModel viewModel;

    // UI elements
    private TextView textNoMove;
    private CardView cardMoveDetails;
    private TextView textFrom, textTo, textDate;
    private Button btnViewItems, btnAddPartner;
    private MaterialButton btnCancelMove;
    private FloatingActionButton fabEditMove;

    public MyMoveFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_move, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MyMoveViewModel.class);

        // חיבור לרכיבי המסך
        textNoMove = view.findViewById(R.id.textNoMove);
        cardMoveDetails = view.findViewById(R.id.cardMoveDetails);
        textFrom = view.findViewById(R.id.textFrom);
        textTo = view.findViewById(R.id.textTo);
        textDate = view.findViewById(R.id.textDate);

        btnViewItems = view.findViewById(R.id.btnViewItems);
        btnAddPartner = view.findViewById(R.id.btnAddPartner);
        btnCancelMove = view.findViewById(R.id.btnCancelMove);
        fabEditMove = view.findViewById(R.id.fabEditMove);

        // האזנה לשינויים במידע
        viewModel.getCurrentMove().observe(getViewLifecycleOwner(), this::updateUI);
        viewModel.getErrorMsg().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // הגדרת כפתורים
        setupButtons();

        // טעינה ראשונית
        viewModel.loadCurrentMove();
    }

    private void updateUI(MoveRequest move) {
        if (move == null) {
            // מצב שאין הובלה
            textNoMove.setVisibility(View.VISIBLE);
            cardMoveDetails.setVisibility(View.GONE);
            btnCancelMove.setVisibility(View.GONE);
            fabEditMove.setVisibility(View.GONE);
        } else {
            // מצב שיש הובלה פעילה
            textNoMove.setVisibility(View.GONE);
            cardMoveDetails.setVisibility(View.VISIBLE);
            btnCancelMove.setVisibility(View.VISIBLE);
            fabEditMove.setVisibility(View.VISIBLE);

            textFrom.setText("מ: " + move.getSourceAddress());
            textTo.setText("ל: " + move.getDestAddress());

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textDate.setText("תאריך: " + sdf.format(new Date(move.getMoveDate())));
        }
    }

    private void setupButtons() {
        btnCancelMove.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("ביטול הובלה")
                    .setMessage("האם אתה בטוח שברצונך לבטל את ההובלה הנוכחית?")
                    .setPositiveButton("כן, בטל", (d, w) -> viewModel.cancelCurrentMove())
                    .setNegativeButton("לא", null)
                    .show();
        });

        btnViewItems.setOnClickListener(v -> {
            // פתיחת מסך המלאי
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new InventoryFragment()) // החלפת המסך
                    .addToBackStack(null) // כדי שכפתור 'חזור' יחזיר למסך הקודם
                    .commit();
        });

        btnAddPartner.setOnClickListener(v -> {
            // TODO: פתיחת מסך חיפוש שותף
            Toast.makeText(getContext(), "כאן יפתח חיפוש שותף", Toast.LENGTH_SHORT).show();
        });

        fabEditMove.setOnClickListener(v -> {
            // TODO: פתיחת מסך עריכה
            Toast.makeText(getContext(), "עריכת הובלה", Toast.LENGTH_SHORT).show();
        });
    }
}