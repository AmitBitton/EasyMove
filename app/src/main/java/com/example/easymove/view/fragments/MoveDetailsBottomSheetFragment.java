package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.example.easymove.R;
import com.example.easymove.model.MatchRequest;
import com.example.easymove.model.MoveRequest;
import com.example.easymove.model.repository.UserRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BottomSheetFragment for displaying extended details of a Move.
 * * Features:
 * 1. General Move Details (Source, Dest, Date, Customer Name).
 * 2. Existing Partner Details (if a partner is already assigned).
 * 3. Pending Partner Requests (Actions: Approve/Reject).
 * 4. Cancellation Requests (Actions: Mover approves customer cancel).
 */
public class MoveDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_MOVE = "move_data";
    private static final String ARG_PENDING_REQ = "pending_req_data";

    // Data objects
    private MoveRequest move;
    private MatchRequest pendingRequest;

    // Action Listener
    private OnActionListener listener;

    /**
     * Interface to handle actions triggered from this sheet.
     */
    public interface OnActionListener {
        void onApprove(MatchRequest req);
        void onReject(MatchRequest req);
        void onApproveCancel(MoveRequest move);
    }

    /**
     * Factory method to create a new instance with necessary data.
     * Uses Bundle arguments to survive configuration changes (rotation).
     */
    public static MoveDetailsBottomSheetFragment newInstance(MoveRequest move, MatchRequest pendingRequest) {
        MoveDetailsBottomSheetFragment fragment = new MoveDetailsBottomSheetFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MOVE, move);
        args.putSerializable(ARG_PENDING_REQ, pendingRequest);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(OnActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_move_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Unpack Arguments
        if (getArguments() != null) {
            move = (MoveRequest) getArguments().getSerializable(ARG_MOVE);
            pendingRequest = (MatchRequest) getArguments().getSerializable(ARG_PENDING_REQ);
        }

        // 2. Initialize UI Components
        TextView tvCustomer = view.findViewById(R.id.bsCustomerName);
        TextView tvSource = view.findViewById(R.id.bsSource);
        TextView tvDest = view.findViewById(R.id.bsDest);
        TextView tvDate = view.findViewById(R.id.bsDate);

        // --- General Move Details ---
        if (move != null) {
            tvSource.setText(move.getSourceAddress());
            tvDest.setText(move.getDestAddress());

            if (move.getMoveDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(move.getMoveDate())));
            }

            // Fetch Main Customer Name
            if (move.getCustomerId() != null) {
                new UserRepository().getUserNameById(move.getCustomerId())
                        .addOnSuccessListener(tvCustomer::setText);
            }
        }

        // --- Existing Partner Section ---
        LinearLayout layoutExisting = view.findViewById(R.id.layoutExistingPartner);
        TextView tvExName = view.findViewById(R.id.bsExistingPartnerName);
        TextView tvExAddr = view.findViewById(R.id.bsExistingPartnerAddress);

        if (move != null && move.getPartnerId() != null && !move.getPartnerId().isEmpty()) {
            layoutExisting.setVisibility(View.VISIBLE);
            tvExAddr.setText("איסוף מ: " + move.getIntermediateAddress());

            new UserRepository().getUserNameById(move.getPartnerId())
                    .addOnSuccessListener(tvExName::setText);
        } else {
            layoutExisting.setVisibility(View.GONE);
        }

        // --- Pending Request Section (The "Yellow Box") ---
        CardView cardPending = view.findViewById(R.id.bsCardPendingRequest);
        TextView tvPendingInfo = view.findViewById(R.id.bsPendingInfo);
        Button btnApprove = view.findViewById(R.id.bsBtnApprove);
        Button btnReject = view.findViewById(R.id.bsBtnReject);

        if (pendingRequest != null) {
            cardPending.setVisibility(View.VISIBLE);

            // Display the Partner's Name (toUserName)
            String partnerName = pendingRequest.getToUserName();
            if (partnerName == null || partnerName.isEmpty()) {
                partnerName = "שותף (שם לא זמין)";
            }

            String info = "בקשת הצטרפות להובלה!\n" +
                    "שם השותף: " + partnerName + "\n" +
                    "כתובת איסוף: " + pendingRequest.getPartnerAddress();

            tvPendingInfo.setText(info);

            // Approve Action
            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(pendingRequest);
                dismiss();
            });

            // Reject Action
            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(pendingRequest);
                dismiss();
            });
        } else {
            cardPending.setVisibility(View.GONE);
        }

        // --- "Approve Cancel" Section (Only if Customer requested cancel) ---
        Button btnApproveCancel = view.findViewById(R.id.bsBtnApproveCancel);

        boolean cancelPending = move != null
                && move.getCancelRequestPending() != null
                && move.getCancelRequestPending();

        btnApproveCancel.setVisibility(cancelPending ? View.VISIBLE : View.GONE);

        btnApproveCancel.setOnClickListener(v -> {
            if (listener != null && move != null) listener.onApproveCancel(move);
            dismiss();
        });

        // Close Button
        view.findViewById(R.id.bsBtnClose).setOnClickListener(v -> dismiss());
    }
}