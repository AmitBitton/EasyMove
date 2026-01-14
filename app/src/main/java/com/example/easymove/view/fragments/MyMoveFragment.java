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
    // ✅ שדות חדשים
    private TextView tvPartnerInfo, tvIntermediateAddress;

    private Button btnViewItems, btnAddPartner;
    private MaterialButton btnCancelMove;
    private MaterialButton btnChatWithMover;

    private CardView cardIncomingRequest;
    private TextView tvRequestDetails;
    private Button btnApproveReq, btnRejectReq;

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

        viewModel.loadCurrentMove();

        String uid = new com.example.easymove.model.repository.MoveRepository().getCurrentUserId();
        if (uid != null) {
            viewModel.listenForMatchRequests(uid);
        }
    }

    private void initViews(View view) {
        textNoMove = view.findViewById(R.id.textNoMove);
        cardMoveDetails = view.findViewById(R.id.cardMoveDetails);
        textFrom = view.findViewById(R.id.textFrom);
        textTo = view.findViewById(R.id.textTo);
        textDate = view.findViewById(R.id.textDate);

        // ✅ חיבור לרכיבים החדשים
        tvPartnerInfo = view.findViewById(R.id.tvPartnerInfo);
        tvIntermediateAddress = view.findViewById(R.id.tvIntermediateAddress);

        btnViewItems = view.findViewById(R.id.btnViewItems);
        btnAddPartner = view.findViewById(R.id.btnAddPartner);
        btnCancelMove = view.findViewById(R.id.btnCancelMove);
        btnChatWithMover = view.findViewById(R.id.btnChatWithMover);

        cardIncomingRequest = view.findViewById(R.id.cardIncomingRequest);
        tvRequestDetails = view.findViewById(R.id.tvRequestDetails);
        btnApproveReq = view.findViewById(R.id.btnApproveReq);
        btnRejectReq = view.findViewById(R.id.btnRejectReq);
    }

    private void observeViewModel() {
        viewModel.getCurrentMove().observe(getViewLifecycleOwner(), this::updateUI);

        viewModel.getErrorMsg().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getIncomingRequest().observe(getViewLifecycleOwner(), req -> {
            if (req != null) {
                cardIncomingRequest.setVisibility(View.VISIBLE);
                String info = req.getFromUserName() + " רוצה לחלוק איתך הובלה:\n" +
                        "מוצא: " + req.getOriginalSourceAddress() + "\n" +
                        "יעד: " + req.getOriginalDestAddress();
                tvRequestDetails.setText(info);

                btnApproveReq.setOnClickListener(v -> viewModel.approveMatch(req));
                btnRejectReq.setOnClickListener(v -> viewModel.rejectMatch(req));
            } else {
                cardIncomingRequest.setVisibility(View.GONE);
            }
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

        textFrom.setText(move.getSourceAddress() != null ? move.getSourceAddress() : "כתובת מוצא חסרה");
        textTo.setText(move.getDestAddress() != null ? move.getDestAddress() : "כתובת יעד חסרה");

        // ✅ טיפול בכתובת עצירה (שותף)
        if (move.getIntermediateAddress() != null && !move.getIntermediateAddress().isEmpty()) {
            tvIntermediateAddress.setVisibility(View.VISIBLE);
            tvIntermediateAddress.setText("➕ איסוף נוסף מ: " + move.getIntermediateAddress());
        } else {
            tvIntermediateAddress.setVisibility(View.GONE);
        }

        if (move.getMoveDate() > 0) {
            try {
                Date date = new Date(move.getMoveDate());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                textDate.setText(sdf.format(date));
                textDate.setTextColor(Color.BLACK);
            } catch (Exception e) {
                textDate.setText("שגיאה בתאריך");
            }
        } else {
            textDate.setText("טרם נקבע תאריך");
            textDate.setTextColor(Color.RED);
        }

        // ✅ לוגיקה חכמה לזיהוי מי אני (הלקוח הראשי או השותף)
        String myId = new com.example.easymove.model.repository.MoveRepository().getCurrentUserId();

        if (move.getPartnerId() != null && !move.getPartnerId().isEmpty()) {
            // יש שותף!
            btnAddPartner.setVisibility(View.GONE);
            tvPartnerInfo.setVisibility(View.VISIBLE);

            // אם אני היוצר של ההובלה -> מציגים את השם של השותף
            // אם אני השותף -> מציגים את השם של היוצר
            String otherId;
            String label;

            if (myId.equals(move.getCustomerId())) {
                otherId = move.getPartnerId();
                label = "שותף:";
            } else {
                otherId = move.getCustomerId();
                label = "הובלה ראשית של:";
            }

            // שליפת שם האדם השני
            new com.example.easymove.model.repository.UserRepository().getUserNameById(otherId)
                    .addOnSuccessListener(name -> {
                        tvPartnerInfo.setText("✅ " + label + " " + name);
                    });
        } else {
            // אין שותף עדיין

            // כפתור הוספת שותף מוצג רק אם אני בעל ההובלה (ולא אם אני סתם צופה)
            if (myId.equals(move.getCustomerId())) {
                btnAddPartner.setVisibility(View.VISIBLE);
            } else {
                btnAddPartner.setVisibility(View.GONE);
            }
            tvPartnerInfo.setVisibility(View.GONE);
        }

        if ("CONFIRMED".equals(move.getStatus()) && move.getChatId() != null && !move.getChatId().isEmpty()) {
            btnChatWithMover.setVisibility(View.VISIBLE);
        } else {
            btnChatWithMover.setVisibility(View.GONE);
        }

        checkIfMoveIsFinished(move);
    }

    private void setupButtons() {
        btnChatWithMover.setOnClickListener(v -> {
            MoveRequest move = viewModel.getCurrentMove().getValue();
            if (move != null && move.getChatId() != null) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", move.getChatId());
                startActivity(intent);
            }
        });

        btnCancelMove.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("ביטול הובלה")
                    .setMessage("האם את בטוחה שברצונך לבטל את ההובלה?")
                    .setPositiveButton("כן, בטל", (d, w) -> viewModel.cancelCurrentMove())
                    .setNegativeButton("לא", null)
                    .show();
        });

        btnViewItems.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new InventoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        btnAddPartner.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new com.example.easymove.view.fragments.PartnerMatchFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void checkIfMoveIsFinished(MoveRequest move) {
        if ("CONFIRMED".equals(move.getStatus()) && move.getMoveDate() > 0) {
            long now = System.currentTimeMillis();
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