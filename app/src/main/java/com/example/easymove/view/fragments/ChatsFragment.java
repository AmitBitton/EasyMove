package com.example.easymove.view.fragments; // Fragments

import android.content.Intent; // מעבר למסך אחר
import android.os.Bundle; // נתונים
import android.view.LayoutInflater; // inflate
import android.view.View; // view
import android.view.ViewGroup; // container
import android.widget.ProgressBar; // טעינה
import android.widget.TextView; // טקסט

import androidx.annotation.NonNull; // לא null
import androidx.annotation.Nullable; // יכול להיות null
import androidx.fragment.app.Fragment; // Fragment
import androidx.lifecycle.ViewModelProvider; // ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager; // סידור רשימה
import androidx.recyclerview.widget.RecyclerView; // RecyclerView

import com.example.easymove.R; // משאבים
import com.example.easymove.adapters.ChatsListAdapter; // אדפטר רשימת צ'אטים
import com.example.easymove.view.activities.ChatActivity; // מסך צ'אט
import com.example.easymove.viewmodel.ChatViewModel; // ViewModel של צ'אטים

import java.util.ArrayList; // רשימה ריקה כשאין צ'אטים

public class ChatsFragment extends Fragment {

    private ChatViewModel chatViewModel; // מנהל נתונים ולוגיקה למסך צ'אטים
    private ChatsListAdapter adapter; // אדפטר לרשימה
    private TextView tvEmpty; // טקסט "אין צ'אטים"
    private ProgressBar progressBar; // ספינר טעינה

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // מנפחת את layout של המסך
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // נקרא אחרי שיש view, כאן מחברים UI, אדפטרים, observers
        super.onViewCreated(view, savedInstanceState);

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        // יצירת/קבלת ViewModel למסך זה

        RecyclerView recyclerView = view.findViewById(R.id.recyclerChatsList); // רשימת צ'אטים
        tvEmpty = view.findViewById(R.id.tvEmptyChats); // טקסט כשאין צ'אטים
        progressBar = view.findViewById(R.id.progressChats); // טעינה

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // רשימה אנכית

        // לחיצה על צ'אט ברשימה פותחת את מסך ההודעות
        adapter = new ChatsListAdapter(chat -> {
            Intent intent = new Intent(getContext(), ChatActivity.class); // מעבר למסך שיחה
            intent.putExtra("CHAT_ID", chat.getId()); // שולחים את מזהה הצ'אט כדי לטעון הודעות
            startActivity(intent); // פתיחת activity
        });
        recyclerView.setAdapter(adapter); // חיבור אדפטר

        // האזנה לשינויים ברשימה
        chatViewModel.getUserChatsLiveData().observe(getViewLifecycleOwner(), chats -> {
            // observer שמופעל כש-ViewModel מעדכן את רשימת הצ'אטים
            if (chats == null || chats.isEmpty()) {
                adapter.setChats(new ArrayList<>()); // מציגים רשימה ריקה
                tvEmpty.setVisibility(View.VISIBLE); // מציגים "אין צ'אטים"
            } else {
                tvEmpty.setVisibility(View.GONE); // מסתירים "אין צ'אטים"
                adapter.setChats(chats); // מעבירים רשימה לאדפטר
            }
        });

        chatViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );
        // אם isLoading = true -> מציגים progressBar, אחרת מסתירים

        // טעינת הנתונים
        chatViewModel.loadUserChats(); // קריאה שמביאה מהמסד את כל הצ'אטים של המשתמשת
    }

    @Override
    public void onResume() {
        // נקרא בכל פעם שחוזרים למסך הזה (כולל אחרי שחוזרים מ-ChatActivity)
        super.onResume();
        // רענון הרשימה כשחוזרים ממסך השיחה (כדי לעדכן את "ההודעה האחרונה")
        chatViewModel.loadUserChats(); // מביא מחדש כדי שה-lastMessage יהיה מעודכן
    }
}
