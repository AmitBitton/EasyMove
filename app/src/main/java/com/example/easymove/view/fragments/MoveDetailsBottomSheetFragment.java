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

public class MoveDetailsBottomSheetFragment extends BottomSheetDialogFragment {

    // נתונים להצגה
    private MoveRequest move;
    private MatchRequest pendingRequest;

    // מאזין לאירועים
    private OnActionListener listener;

    public interface OnActionListener {
        void onApprove(MatchRequest req);
        void onReject(MatchRequest req);
    }

    // ✅ עדכון שם הפונקציה הסטטית והטיפוס המוחזר
    public static MoveDetailsBottomSheetFragment newInstance(MoveRequest move, MatchRequest pendingRequest) {
        MoveDetailsBottomSheetFragment fragment = new MoveDetailsBottomSheetFragment();
        fragment.move = move;
        fragment.pendingRequest = pendingRequest;
        return fragment;
    }

    public void setListener(OnActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // משתמשים באותו קובץ XML שיצרנו קודם
        return inflater.inflate(R.layout.bottom_sheet_move_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- חיבור ל-UI (הצגת נתונים כלליים) ---
        TextView tvCustomer = view.findViewById(R.id.bsCustomerName);
        TextView tvSource = view.findViewById(R.id.bsSource);
        TextView tvDest = view.findViewById(R.id.bsDest);
        TextView tvDate = view.findViewById(R.id.bsDate);

        if (move != null) {
            tvSource.setText(move.getSourceAddress());
            tvDest.setText(move.getDestAddress());
            if (move.getMoveDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(move.getMoveDate())));
            }

            // שליפת שם הלקוח הראשי
            if (move.getCustomerId() != null) {
                new UserRepository().getUserNameById(move.getCustomerId())
                        .addOnSuccessListener(tvCustomer::setText);
            }
        }

        // --- הצגת שותף קיים (אם כבר אושר בעבר) ---
        LinearLayout layoutExisting = view.findViewById(R.id.layoutExistingPartner);
        TextView tvExName = view.findViewById(R.id.bsExistingPartnerName);
        TextView tvExAddr = view.findViewById(R.id.bsExistingPartnerAddress);

        if (move != null && move.getPartnerId() != null && !move.getPartnerId().isEmpty()) {
            layoutExisting.setVisibility(View.VISIBLE);
            tvExAddr.setText("איסוף מ: " + move.getIntermediateAddress());
            new UserRepository().getUserNameById(move.getPartnerId()).addOnSuccessListener(tvExName::setText);
        } else {
            layoutExisting.setVisibility(View.GONE);
        }

        // --- הצגת בקשה ממתינה (החלק של האישור - הריבוע הצהוב) ---
        CardView cardPending = view.findViewById(R.id.bsCardPendingRequest);
        TextView tvPendingInfo = view.findViewById(R.id.bsPendingInfo);
        Button btnApprove = view.findViewById(R.id.bsBtnApprove);
        Button btnReject = view.findViewById(R.id.bsBtnReject);

        // אם הועברה בקשה (כלומר לחצו על כפתור שיש בו נקודה אדומה)
        if (pendingRequest != null) {
            cardPending.setVisibility(View.VISIBLE);
            String info = "שם: " + pendingRequest.getFromUserName() + "\n" +
                    "כתובת איסוף: " + pendingRequest.getPartnerAddress();
            tvPendingInfo.setText(info);

            // לחיצה על אישור
            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(pendingRequest);
                dismiss(); // סגירת החלון
            });

            // לחיצה על דחייה
            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(pendingRequest);
                dismiss(); // סגירת החלון
            });
        } else {
            cardPending.setVisibility(View.GONE);
        }

        // כפתור סגירה כללי
        view.findViewById(R.id.bsBtnClose).setOnClickListener(v -> dismiss());
    }
}