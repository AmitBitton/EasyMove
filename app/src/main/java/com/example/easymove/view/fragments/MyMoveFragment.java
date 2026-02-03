package com.example.easymove.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
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
    private TextView textTitle; // הוספתי קישור לכותרת
    private TextView textFrom, textTo, textDate;
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

        // הוספתי את ה-ID הזה ב-XML למטה, חשוב!
        textTitle = view.findViewById(R.id.textTitle);

        textFrom = view.findViewById(R.id.textFrom);
        textTo = view.findViewById(R.id.textTo);
        textDate = view.findViewById(R.id.textDate);

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

        // עדכון שדות טקסט
        textFrom.setText(move.getSourceAddress() != null ? move.getSourceAddress() : "טרם הוגדר");
        textTo.setText(move.getDestAddress() != null ? move.getDestAddress() : "טרם הוגדר");

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
            textDate.setTextColor(Color.GRAY);
        }

        // ✅ בדיקה: האם זו הובלה אמיתית (יש ID) או טיוטה מהפרופיל (אין ID)
        boolean isRealMove = (move.getId() != null);

        if (!isRealMove) {
            // --- מצב טיוטה (אין הובלה פעילה) ---
            if (textTitle != null) textTitle.setText("המעבר המתוכנן שלי");

            // מציגים רק את כפתור הרשומות
            btnViewItems.setVisibility(View.VISIBLE);

            // מסתירים את כל השאר
            btnCancelMove.setVisibility(View.GONE);
            btnAddPartner.setVisibility(View.GONE);
            btnChatWithMover.setVisibility(View.GONE);
            tvPartnerInfo.setVisibility(View.GONE);
            tvIntermediateAddress.setVisibility(View.GONE);

        } else {
            // --- מצב הובלה פעילה (אמיתית) ---
            if (textTitle != null) textTitle.setText("פרטי הובלה");

            // מציגים כפתורים רלוונטיים
            btnViewItems.setVisibility(View.VISIBLE);
            btnCancelMove.setVisibility(View.VISIBLE); // הובלה פעילה אפשר לבטל

            // לוגיקת שותפים וצ'אט המקורית שלך
            handlePartnerAndChatUI(move);
            checkIfMoveIsFinished(move);
        }
    }

    // הפונקציה המקורית שלך לניהול תצוגת שותפים (נקראת רק כשיש הובלה אמיתית)
    private void handlePartnerAndChatUI(MoveRequest move) {
        String myId = new com.example.easymove.model.repository.MoveRepository().getCurrentUserId();

        if (move.getPartnerId() != null && !move.getPartnerId().isEmpty()) {
            // יש שותף!
            btnAddPartner.setVisibility(View.GONE);
            tvPartnerInfo.setVisibility(View.VISIBLE);

            if (move.getIntermediateAddress() != null && !move.getIntermediateAddress().isEmpty()) {
                tvIntermediateAddress.setVisibility(View.VISIBLE);
                tvIntermediateAddress.setText("➕ איסוף נוסף מ: " + move.getIntermediateAddress());
            } else {
                tvIntermediateAddress.setVisibility(View.GONE);
            }

            String otherId = myId.equals(move.getCustomerId()) ? move.getPartnerId() : move.getCustomerId();
            String label = myId.equals(move.getCustomerId()) ? "שותף:" : "הובלה ראשית של:";

            new com.example.easymove.model.repository.UserRepository().getUserNameById(otherId)
                    .addOnSuccessListener(name -> tvPartnerInfo.setText("✅ " + label + " " + name));
        } else {
            // אין שותף
            tvPartnerInfo.setVisibility(View.GONE);
            tvIntermediateAddress.setVisibility(View.GONE);

            // כפתור הוספה רק לבעלים
            if (myId.equals(move.getCustomerId())) {
                btnAddPartner.setVisibility(View.VISIBLE);
            } else {
                btnAddPartner.setVisibility(View.GONE);
            }
        }

        if ("CONFIRMED".equals(move.getStatus()) && move.getChatId() != null && !move.getChatId().isEmpty()) {
            btnChatWithMover.setVisibility(View.VISIBLE);
        } else {
            btnChatWithMover.setVisibility(View.GONE);
        }
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

        btnCancelMove.setOnClickListener(v -> onCancelClicked());

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

    private void onCancelClicked() {
        MoveRequest move = viewModel.getCurrentMove().getValue();
        if (move == null || move.getId() == null) return; // הגנה

        String myId = new com.example.easymove.model.repository.MoveRepository().getCurrentUserId();
        if (myId == null) return;

        boolean iAmPartner = myId.equals(move.getPartnerId());
        boolean iAmCustomer = myId.equals(move.getCustomerId());

        String title = "ביטול";
        String message;

        if (iAmPartner) {
            message = "ביטול שותפות יחזיר את ההובלה למצב רגיל ללא שותף. להמשיך?";
        } else if (iAmCustomer) {
            long now = System.currentTimeMillis();
            long weekMs = 7L * 24L * 60L * 60L * 1000L;
            long moveDate = move.getMoveDate();

            boolean needsMoverApproval = (moveDate > 0) && ((moveDate - now) < weekMs);

            if (needsMoverApproval) {
                message = "הביטול מתבצע פחות משבוע מראש ולכן נדרש אישור מהמוביל. לשלוח בקשת ביטול?";
            } else {
                message = "האם את בטוחה שברצונך לבטל את ההובלה?";
            }
        } else {
            message = "אין לך הרשאה לבטל את ההובלה הזו.";
        }

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("כן", (d, w) -> viewModel.cancelMoveWithPolicy())
                .setNegativeButton("לא", null)
                .show();
    }
}