package com.example.easymove.view.fragments;

import android.app.DatePickerDialog;
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
import com.example.easymove.view.fragments.MoverReviewsFragment;


import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.adapters.MoversAdapter;
import com.example.easymove.model.UserProfile;
import com.example.easymove.viewmodel.ChatViewModel;
import com.example.easymove.viewmodel.UserViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class SearchMoverFragment extends Fragment implements MoversAdapter.OnMoverActionClickListener {

    private UserViewModel userViewModel;
    private ChatViewModel chatViewModel;
    private MoversAdapter adapter;
    private Button btnFillFromProfile;
    private UserProfile myProfile;


    private TextView tvSource, tvDest;
    private Button btnSearch;

    // --- משתנים חדשים לתאריך ולוגיקה ---
    private Button btnSelectDate; // כפתור בחירת תאריך
    private long selectedDate = 0; // שמירת התאריך שנבחר
    private UserProfile selectedMoverForChat; // ✅ הנה המשתנה שהיה חסר לך!
    // ------------------------------------

    private LatLng sourceLatLng = null;
    private LatLng destLatLng = null;

    private boolean hasSearched = false;
    private boolean isSelectingSource = true;

    // משגר לבחירת כתובת (נשאר ללא שינוי)
    private final ActivityResultLauncher<Intent> placePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
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

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        // אתחול מפות גוגל
//        if (!Places.isInitialized()) {
//            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
//        }
//
//        // אתחול ViewModels
//        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
//        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
//
//        // חיבור ל-XML
//        tvSource = view.findViewById(R.id.tvSourceResult); // הטקסט שרואים
//        tvDest = view.findViewById(R.id.tvDestResult);     // הטקסט שרואים
//        btnSearch = view.findViewById(R.id.btnSearchAction);
//        btnSelectDate = view.findViewById(R.id.btnSelectDate);
//
//        RecyclerView recycler = view.findViewById(R.id.recyclerMovers);
//        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        adapter = new MoversAdapter(this);
//        recycler.setAdapter(adapter);
//
//        // --- מאזינים לכפתורים (התיקון כאן!) ---
//
//        // ✅ מאזינים ל-TextView עצמו (במקום לכפתור המוסתר btnSourceAddress)
//        tvSource.setOnClickListener(v -> {
//            isSelectingSource = true;
//            openPlacePicker();
//        });
//
//        // ✅ מאזינים ל-TextView עצמו (במקום לכפתור המוסתר btnDestAddress)
//        tvDest.setOnClickListener(v -> {
//            isSelectingSource = false;
//            openPlacePicker();
//        });
//
//        // בחירת תאריך
//        btnSelectDate.setOnClickListener(v -> {
//            Calendar calendar = Calendar.getInstance();
//            new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
//                Calendar chosen = Calendar.getInstance();
//                chosen.set(year, month, dayOfMonth);
//                selectedDate = chosen.getTimeInMillis();
//
//                // עדכון הטקסט על הכפתור
//                btnSelectDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
//            },
//                    calendar.get(Calendar.YEAR),
//                    calendar.get(Calendar.MONTH),
//                    calendar.get(Calendar.DAY_OF_MONTH)).show();
//        });
//
//        btnSearch.setOnClickListener(v -> performSearch());
//
//        // --- האזנה לנתונים (Observers) ---
//
//        // 1. תוצאות חיפוש
//        userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
//            adapter.setMovers(movers);
//            if (hasSearched && movers.isEmpty()) {
//                Toast.makeText(getContext(), "לא נמצאו מובילים ברדיוס הקרוב", Toast.LENGTH_LONG).show();
//            }
//        });
//
//        // 2. האזנה לשמירת פרטי ההובלה
//        userViewModel.getMoveDetailsSaved().observe(getViewLifecycleOwner(), saved -> {
//            if (saved != null && saved) {
//                if (selectedMoverForChat != null) {
//                    chatViewModel.startChatWithMover(selectedMoverForChat);
//                    selectedMoverForChat = null;
//                }
//            }
//        });
//
//        // 3. האזנה לפתיחת הצ'אט
//        chatViewModel.getNavigateToChatId().observe(getViewLifecycleOwner(), chatId -> {
//            if (chatId != null) {
//                chatViewModel.onChatNavigated();
//                Intent intent = new Intent(getContext(), com.example.easymove.view.activities.ChatActivity.class);
//                intent.putExtra("CHAT_ID", chatId);
//                startActivity(intent);
//            }
//        });
//
//        // הצגת שגיאות
//        userViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), error -> {
//            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
//        });
//
//        chatViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
//            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
//        });
//    }
@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // אתחול מפות גוגל
    if (!Places.isInitialized()) {
        Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
    }

    // אתחול ViewModels
    userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
    chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

    // חיבור ל-XML
    tvSource = view.findViewById(R.id.tvSourceResult); // הטקסט שרואים
    tvDest = view.findViewById(R.id.tvDestResult);     // הטקסט שרואים
    btnSearch = view.findViewById(R.id.btnSearchAction);
    btnSelectDate = view.findViewById(R.id.btnSelectDate);

    // ✅ חדש: כפתור מילוי אוטומטי מהאזור האישי
    btnFillFromProfile = view.findViewById(R.id.btnFillFromProfile);

    RecyclerView recycler = view.findViewById(R.id.recyclerMovers);
    recycler.setLayoutManager(new LinearLayoutManager(getContext()));

    adapter = new MoversAdapter(this);
    recycler.setAdapter(adapter);

    // ✅ חדש: טעינת פרופיל + שמירה מקומית כדי שנוכל למלא אוטומטית
    userViewModel.loadMyProfile();
    userViewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
        myProfile = profile; // שימי לב: myProfile הוא שדה במחלקה
    });

    // ✅ חדש: מאזין לכפתור מילוי אוטומטי
    btnFillFromProfile.setOnClickListener(v -> fillFromProfile());

    // --- מאזינים לכפתורים ---

    // ✅ מאזינים ל-TextView עצמו (במקום לכפתור המוסתר btnSourceAddress)
    tvSource.setOnClickListener(v -> {
        isSelectingSource = true;
        openPlacePicker();
    });

    // ✅ מאזינים ל-TextView עצמו (במקום לכפתור המוסתר btnDestAddress)
    tvDest.setOnClickListener(v -> {
        isSelectingSource = false;
        openPlacePicker();
    });

    // בחירת תאריך
    btnSelectDate.setOnClickListener(v -> {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth);
            selectedDate = chosen.getTimeInMillis();

            // עדכון הטקסט על הכפתור
            btnSelectDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    });

    btnSearch.setOnClickListener(v -> performSearch());

    // --- האזנה לנתונים (Observers) ---

    // 1. תוצאות חיפוש
    userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
        adapter.setMovers(movers);
        if (hasSearched && movers.isEmpty()) {
            Toast.makeText(getContext(), "לא נמצאו מובילים ברדיוס הקרוב", Toast.LENGTH_LONG).show();
        }
    });

    // 2. האזנה לשמירת פרטי ההובלה
    userViewModel.getMoveDetailsSaved().observe(getViewLifecycleOwner(), saved -> {
        if (saved != null && saved) {
            if (selectedMoverForChat != null) {
                chatViewModel.startChatWithMover(selectedMoverForChat);
                selectedMoverForChat = null;
            }
        }
    });

    // 3. האזנה לפתיחת הצ'אט
    chatViewModel.getNavigateToChatId().observe(getViewLifecycleOwner(), chatId -> {
        if (chatId != null) {
            chatViewModel.onChatNavigated();
            Intent intent = new Intent(getContext(), com.example.easymove.view.activities.ChatActivity.class);
            intent.putExtra("CHAT_ID", chatId);
            startActivity(intent);
        }
    });

    // הצגת שגיאות
    userViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), error -> {
        if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
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
        if (place.getLatLng() == null) return;

        if (isSelectingSource) {
            tvSource.setText(place.getName()); // או place.getAddress()
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
            adapter.setMovers(new ArrayList<>());
            userViewModel.searchMoversByLocation(sourceLatLng);
        }
    }

    // --- OnClick: כשלוחצים על "צור קשר" בכרטיס מוביל ---
    @Override
    public void onChatClick(UserProfile mover) {
        // 1. בדיקת תקינות
        if (selectedDate == 0) {
            Toast.makeText(getContext(), "אנא בחר תאריך הובלה לפני יצירת קשר", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tvSource.getText().toString().isEmpty() || tvDest.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "אנא בחר כתובות מוצא ויעד", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. שמירת המוביל בצד (במשתנה שהגדרנו למעלה)
        this.selectedMoverForChat = mover;

        // 3. שליחת פקודה ל-ViewModel לשמור את הפרטים
        userViewModel.saveMoveDetails(
                FirebaseAuth.getInstance().getUid(),
                tvSource.getText().toString(),
                tvDest.getText().toString(),
                selectedDate
        );
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

        String moverId = mover.getUserId();
        String moverName = mover.getName();

        if (moverId == null || moverId.trim().isEmpty()) {
            Toast.makeText(getContext(), "שגיאה: חסר moverId", Toast.LENGTH_SHORT).show();
            return;
        }

        MoverReviewsFragment fragment = MoverReviewsFragment.newInstance(moverId, moverName);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }



    @Override
    public void onReportClick(UserProfile mover) {
        Toast.makeText(getContext(), "דיווח על " + mover.getName() + " נשלח לאדמין", Toast.LENGTH_LONG).show();
    }

    private void fillFromProfile() {
        if (myProfile == null) {
            Toast.makeText(getContext(), "הפרופיל עדיין נטען, נסי שוב בעוד רגע", Toast.LENGTH_SHORT).show();
            return;
        }

        String from = myProfile.getDefaultFromAddress();
        String to = myProfile.getDefaultToAddress();

        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            Toast.makeText(getContext(), "בבקשה הגדרי כתובת מוצא ויעד באזור האישי קודם", Toast.LENGTH_SHORT).show();
            return;
        }

        // טקסטים
        tvSource.setText(from);
        tvDest.setText(to);

        // קואורדינטות
        Double flt = myProfile.getFromLat();
        Double fln = myProfile.getFromLng();
        Double tlt = myProfile.getToLat();
        Double tln = myProfile.getToLng();

        if (flt == null || fln == null || tlt == null || tln == null) {
            Toast.makeText(getContext(), "חסרים קואורדינטות לכתובות. בחרי כתובות מחדש באזור האישי.", Toast.LENGTH_SHORT).show();
            return;
        }

        sourceLatLng = new LatLng(flt, fln);
        destLatLng = new LatLng(tlt, tln);

        // תאריך
        Long date = myProfile.getDefaultMoveDate();
        if (date != null && date > 0) {
            selectedDate = date;

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(date);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH) + 1;
            int year = c.get(Calendar.YEAR);
            btnSelectDate.setText(day + "/" + month + "/" + year);
        } else {
            // לא חובה, אבל נחמד שיהיה ברור
            selectedDate = 0;
            btnSelectDate.setText("📅 בחר תאריך הובלה");
        }

        btnSearch.setEnabled(true);
        Toast.makeText(getContext(), "הפרטים מולאו מהאזור האישי", Toast.LENGTH_SHORT).show();
    }

}