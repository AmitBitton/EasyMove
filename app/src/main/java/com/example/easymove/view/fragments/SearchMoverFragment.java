package com.example.easymove.view.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.adapters.MoversAdapter;
import com.example.easymove.model.UserProfile;
import com.example.easymove.viewmodel.ChatViewModel; // הוספנו את ה-ViewModel החדש
import com.example.easymove.viewmodel.UserViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchMoverFragment extends Fragment implements MoversAdapter.OnMoverActionClickListener {

    private UserViewModel userViewModel;
    private ChatViewModel chatViewModel; // משתנה לניהול הצ'אט
    private MoversAdapter adapter;

    private TextView tvSource, tvDest;
    private Button btnSearch;

    private LatLng sourceLatLng = null;
    private LatLng destLatLng = null;

    private boolean hasSearched = false;
    private boolean isSelectingSource = true;

    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    handleAddressSelection(place);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_mover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        // אתחול ה-ViewModels
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class); // אתחול ChatViewModel

        tvSource = view.findViewById(R.id.tvSourceResult);
        tvDest = view.findViewById(R.id.tvDestResult);
        btnSearch = view.findViewById(R.id.btnSearchAction);

        RecyclerView recycler = view.findViewById(R.id.recyclerMovers);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MoversAdapter(this);
        recycler.setAdapter(adapter);

        // מאזינים לכפתורים
        view.findViewById(R.id.btnSourceAddress).setOnClickListener(v -> {
            isSelectingSource = true;
            openPlacePicker();
        });

        view.findViewById(R.id.btnDestAddress).setOnClickListener(v -> {
            isSelectingSource = false;
            openPlacePicker();
        });

        btnSearch.setOnClickListener(v -> performSearch());

        // --- האזנה לנתונים (Observers) ---

        // 1. תוצאות חיפוש מובילים
        userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
            adapter.setMovers(movers);
            if (hasSearched && movers.isEmpty()) {
                Toast.makeText(getContext(), "לא נמצאו מובילים ברדיוס הקרוב", Toast.LENGTH_LONG).show();
            }
        });

        userViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        // 2. האזנה ליצירת צ'אט (ChatViewModel)
        chatViewModel.getNavigateToChatId().observe(getViewLifecycleOwner(), chatId -> {
            if (chatId != null) {
                chatViewModel.onChatNavigated(); // איפוס כדי שלא יקפוץ שוב

                // כרגע רק מציגים הודעה, בשלב הבא נפתח את מסך הצ'אט הממשי
                Toast.makeText(getContext(), "צ'אט נוצר בהצלחה! ID: " + chatId, Toast.LENGTH_SHORT).show();

                // מעבר ל-ChatActivity עם ה-ID
                Intent intent = new Intent(getContext(), com.example.easymove.view.activities.ChatActivity.class);
                intent.putExtra("CHAT_ID", chatId);
                startActivity(intent);
            }
        });

        chatViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext());
        placePickerLauncher.launch(intent);
    }

    private void handleAddressSelection(Place place) {
        if (place.getLatLng() == null) {
            Toast.makeText(getContext(), "שגיאה: לא התקבל מיקום", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSelectingSource) {
            tvSource.setText(place.getName());
            sourceLatLng = place.getLatLng();
        } else {
            tvDest.setText(place.getName());
            destLatLng = place.getLatLng();
        }

        btnSearch.setEnabled(sourceLatLng != null && destLatLng != null);
    }

    private void performSearch() {
        if (sourceLatLng != null) {
            hasSearched = true;
            adapter.setMovers(new ArrayList<>()); // איפוס הרשימה לחווי ויזואלי של טעינה
            userViewModel.searchMoversByLocation(sourceLatLng);
        }
    }

    // --- מימוש הלחיצות מהאדפטר ---

    @Override
    public void onChatClick(UserProfile mover) {
        // קריאה ל-ViewModel ליצירת/פתיחת הצ'אט
        chatViewModel.startChatWithMover(mover);
    }

    @Override
    public void onDetailsClick(UserProfile mover) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("על " + mover.getName())
                .setMessage(mover.getAbout() != null ? mover.getAbout() : "אין מידע נוסף")
                .setPositiveButton("סגור", null)
                .show();
    }

    @Override
    public void onReviewsClick(UserProfile mover) {
        Toast.makeText(getContext(), "חלון ביקורות (ימומש בהמשך)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReportClick(UserProfile mover) {
        Toast.makeText(getContext(), "דיווח על " + mover.getName() + " נשלח לאדמין", Toast.LENGTH_LONG).show();
    }
}