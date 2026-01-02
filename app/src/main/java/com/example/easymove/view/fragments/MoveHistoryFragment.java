package com.example.easymove.view.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.R;
import com.example.easymove.adapters.MoveHistoryAdapter; // ✅ שים לב: עכשיו אנחנו משתמשים באדפטר הנכון
import com.example.easymove.model.repository.MoveRepository;

public class MoveHistoryFragment extends Fragment {

    // בנאי שמגדיר את ה-XML של המסך
    public MoveHistoryFragment() {
        super(R.layout.fragment_move_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. קישור לרכיבים ב-XML
        RecyclerView recyclerView = view.findViewById(R.id.recyclerHistory);
        TextView tvEmpty = view.findViewById(R.id.tvEmptyHistory);
        ProgressBar progressBar = view.findViewById(R.id.progressBarHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. יצירת האדפטר
        MoveHistoryAdapter adapter = new MoveHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // 3. הצגת טעינה התחלתית
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // 4. לוגיקה חכמה: קודם בודקים ב-Session מי המשתמש (לקוח/מוביל)
        com.example.easymove.model.UserSession.getInstance().ensureStarted()
                .addOnSuccessListener(profile -> {
                    if (profile == null) return;

                    String uid = profile.getUserId();
                    String userType = profile.getUserType(); // נחזיר "customer" או "mover"

                    // 5. קריאה ל-Repository עם הסוג הנכון
                    new MoveRepository().getMoveHistory(uid, userType)
                            .addOnSuccessListener(moves -> {
                                // בדיקת הגנה: האם אנחנו עדיין במסך?
                                if (!isAdded()) return;

                                progressBar.setVisibility(View.GONE);

                                if (moves.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    recyclerView.setVisibility(View.VISIBLE);
                                    adapter.setMoves(moves);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                progressBar.setVisibility(View.GONE);
                                // Toast.makeText(getContext(), "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                });
    }
}