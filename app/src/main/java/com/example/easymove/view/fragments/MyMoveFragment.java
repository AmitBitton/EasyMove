package com.example.easymove.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.easymove.view.activities.ChatActivity;
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
    private MaterialButton btnChatWithMover;

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

        initViews(view);
        setupButtons();
        observeViewModel();

        // טעינת הנתונים
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
        btnChatWithMover = view.findViewById(R.id.btnChatWithMover);
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

        // 1. עדכון כתובות
        String source = (move.getSourceAddress() != null && !move.getSourceAddress().isEmpty())
                ? move.getSourceAddress() : "כתובת מוצא חסרה";
        String dest = (move.getDestAddress() != null && !move.getDestAddress().isEmpty())
                ? move.getDestAddress() : "כתובת יעד חסרה";

        textFrom.setText(source);
        textTo.setText(dest);

        // 2. עדכון תאריך (החלק שהיה חסר לך)
        if (move.getMoveDate() > 0) {
            try {
                Date date = new Date(move.getMoveDate());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                textDate.setText(sdf.format(date));
                textDate.setTextColor(Color.BLACK); // וודא שזה שחור
            } catch (Exception e) {
                textDate.setText("שגיאה בתאריך");
                Log.e("MyMoveFragment", "Date format error", e);
            }
        } else {
            textDate.setText("טרם נקבע תאריך");
            textDate.setTextColor(Color.RED);
        }

        // 3. עדכון כפתור הצ'אט
        if ("CONFIRMED".equals(move.getStatus()) && move.getChatId() != null && !move.getChatId().isEmpty()) {
            btnChatWithMover.setVisibility(View.VISIBLE);
        } else {
            btnChatWithMover.setVisibility(View.GONE);
        }

        // בדיקה אם ההובלה עברה
        checkIfMoveIsFinished(move);
    }

    private void setupButtons() {
        // כפתור צ'אט
        btnChatWithMover.setOnClickListener(v -> {
            MoveRequest move = viewModel.getCurrentMove().getValue();
            if (move != null && move.getChatId() != null) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", move.getChatId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "שגיאה בפרטי הצ'אט", Toast.LENGTH_SHORT).show();
            }
        });

        // ביטול הובלה
        btnCancelMove.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("ביטול הובלה")
                    .setMessage("האם את בטוחה שברצונך לבטל את ההובלה?\n")
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

        // ✅ התיקון: החזרנו את המעבר למסך שותפים (במקום ה-Toast)
        btnAddPartner.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    // וודא שזה הנתיב הנכון לפרגמנט שלך, או תוסיף import למעלה
                    .replace(R.id.fragmentContainer, new com.example.easymove.view.fragments.PartnerMatchFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void checkIfMoveIsFinished(MoveRequest move) {
        if ("CONFIRMED".equals(move.getStatus()) && move.getMoveDate() > 0) {
            long now = System.currentTimeMillis();
            // אם עבר יום מאז התאריך (סתם כדי לא להציק באותו רגע)
            if (move.getMoveDate() < now - 86400000L) {
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
                .setNegativeButton("לא, ההובלה נדחתה", null)
                .show();
    }
}