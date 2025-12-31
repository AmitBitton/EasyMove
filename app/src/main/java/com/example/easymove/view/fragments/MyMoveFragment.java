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

    public MyMoveFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_move, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MyMoveViewModel.class);

        initViews(view);
        setupButtons();
        observeViewModel();

        // טעינת הנתונים בכניסה למסך
        viewModel.loadCurrentMove();
    }

    private void initViews(View view) {
        textNoMove = view.findViewById(R.id.textNoMove);
        cardMoveDetails = view.findViewById(R.id.cardMoveDetails);

        textFrom = view.findViewById(R.id.textFrom);
        textTo = view.findViewById(R.id.textTo);
        textDate = view.findViewById(R.id.textDate);

        btnViewItems = view.findViewById(R.id.btnViewItems);
        btnAddPartner = view.findViewById(R.id.btnAddPartner);
        btnCancelMove = view.findViewById(R.id.btnCancelMove);
    }

    private void observeViewModel() {
        viewModel.getCurrentMove().observe(getViewLifecycleOwner(), this::updateUI);

        viewModel.getErrorMsg().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUI(MoveRequest move) {
        if (move == null) {
            textNoMove.setVisibility(View.VISIBLE);
            cardMoveDetails.setVisibility(View.GONE);
            return;
        }

        textNoMove.setVisibility(View.GONE);
        cardMoveDetails.setVisibility(View.VISIBLE);

        String source = (move.getSourceAddress() != null && !move.getSourceAddress().isEmpty())
                ? move.getSourceAddress() : "טרם נבחרה כתובת";
        String dest = (move.getDestAddress() != null && !move.getDestAddress().isEmpty())
                ? move.getDestAddress() : "טרם נבחרה כתובת";

        textFrom.setText(source);
        textTo.setText(dest);

        if (move.getMoveDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            textDate.setText(sdf.format(new Date(move.getMoveDate())));
        } else {
            textDate.setText("טרם נקבע תאריך");
        }

        checkIfMoveIsFinished(move);
    }

//    private void setupButtons() {
//        btnCancelMove.setOnClickListener(v -> {
//            new AlertDialog.Builder(getContext())
//                    .setTitle("ביטול הובלה")
//                    .setMessage("האם את בטוחה שברצונך לבטל את ההובלה?\nהיא תועבר להיסטוריה ותיפתח הובלה חדשה.")
//                    .setPositiveButton("כן, בטל", (d, w) -> viewModel.cancelCurrentMove())
//                    .setNegativeButton("לא", null)
//                    .show();
//        });
//
//        btnViewItems.setOnClickListener(v -> {
//            getParentFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.fragmentContainer, new InventoryFragment())
//                    .addToBackStack(null)
//                    .commit();
//        });
//
//        btnAddPartner.setOnClickListener(v ->
//                Toast.makeText(getContext(), "פיצ'ר שותף להובלה יפתח בקרוב!", Toast.LENGTH_SHORT).show()
//        );
//    }

    private void setupButtons() {

        // ביטול הובלה
        btnCancelMove.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("ביטול הובלה")
                    .setMessage("האם את בטוחה שברצונך לבטל את ההובלה?\nהיא תועבר להיסטוריה ותיפתח הובלה חדשה.")
                    .setPositiveButton("כן, בטל", (d, w) -> viewModel.cancelCurrentMove())
                    .setNegativeButton("לא", null)
                    .show();
        });

        // צפייה ברשומות
        btnViewItems.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new InventoryFragment())
                    .addToBackStack(null)
                    .commit();
        });


        btnAddPartner.setOnClickListener(v -> {
            // מעבר למסך חיפוש שותפים החדש
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new PartnerMatchFragment())
                    .addToBackStack(null) // כדי שכפתור חזור יעבוד
                    .commit();
        });
    }

    private void checkIfMoveIsFinished(MoveRequest move) {
        if ("CONFIRMED".equals(move.getStatus())) {
            long now = System.currentTimeMillis();
            if (move.getMoveDate() > 0 && move.getMoveDate() < now) {
                showCompletionDialog();
            }
        }
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("האם ההובלה הסתיימה?")
                .setMessage("ראינו שתאריך ההובלה עבר. האם המעבר בוצע בהצלחה?")
                .setCancelable(false)
                .setPositiveButton("כן, הכל עבר בשלום ✅", (dialog, which) -> {
                    viewModel.markMoveAsCompleted();
                    Toast.makeText(getContext(), "מזל טוב! ההובלה עברה לארכיון.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("לא, ההובלה נדחתה", (dialog, which) -> {
                    Toast.makeText(getContext(), "עדכני תאריך חדש דרך האזור האישי.", Toast.LENGTH_LONG).show();
                })
                .show();
    }
}