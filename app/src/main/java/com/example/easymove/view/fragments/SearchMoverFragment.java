package com.example.easymove.view.fragments;

import android.app.AlertDialog;
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

import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.adapters.MoversAdapter;
import com.example.easymove.model.UserProfile;
import com.example.easymove.view.activities.ChatActivity;
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

/**
 * Fragment responsible for searching for Movers based on location (GeoFire).
 * Features:
 * 1. Google Places Autocomplete for Source/Destination.
 * 2. Date Picker for move date.
 * 3. "Fill from Profile" shortcut.
 * 4. Displays a list of Movers within a radius.
 * 5. Initiates Chat with a selected Mover.
 */
public class SearchMoverFragment extends Fragment implements MoversAdapter.OnMoverActionClickListener {

    private UserViewModel userViewModel;
    private ChatViewModel chatViewModel;
    private MoversAdapter adapter;
    private UserProfile myProfile;

    // UI Components
    private Button btnFillFromProfile;
    private TextView tvSource, tvDest;
    private Button btnSearch;
    private Button btnSelectDate;

    // Logic State
    private long selectedDate = 0;
    private UserProfile selectedMoverForChat;
    private LatLng sourceLatLng = null;
    private LatLng destLatLng = null;

    private boolean hasSearched = false;
    private boolean isSelectingSource = true; // Flag to determine which address is being picked

    /**
     * Launcher for Google Places Autocomplete.
     */
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        // 2. Initialize ViewModels
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // 3. Setup UI & Listeners
        initViews(view);
        setupListeners();
        observeViewModel();

        // 4. Load User Profile (for "Fill from Profile" feature)
        userViewModel.loadMyProfile();
    }

    private void initViews(View view) {
        tvSource = view.findViewById(R.id.tvSourceResult);
        tvDest = view.findViewById(R.id.tvDestResult);
        btnSearch = view.findViewById(R.id.btnSearchAction);
        btnSelectDate = view.findViewById(R.id.btnSelectDate);
        btnFillFromProfile = view.findViewById(R.id.btnFillFromProfile);

        RecyclerView recycler = view.findViewById(R.id.recyclerMovers);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MoversAdapter(this);
        recycler.setAdapter(adapter);
    }

    private void setupListeners() {
        btnFillFromProfile.setOnClickListener(v -> fillFromProfile());

        tvSource.setOnClickListener(v -> {
            isSelectingSource = true;
            openPlacePicker();
        });

        tvDest.setOnClickListener(v -> {
            isSelectingSource = false;
            openPlacePicker();
        });

        btnSelectDate.setOnClickListener(v -> openDatePicker());

        btnSearch.setOnClickListener(v -> performSearch());
    }

    private void observeViewModel() {
        // Observe Profile Data
        userViewModel.getMyProfileLiveData().observe(getViewLifecycleOwner(), profile ->
                myProfile = profile);

        // Observe Search Results
        userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
            adapter.setMovers(movers);
            if (hasSearched && movers.isEmpty()) {
                Toast.makeText(getContext(), "לא נמצאו מובילים ברדיוס הקרוב", Toast.LENGTH_LONG).show();
            }
        });

        // Observe Move Details Saving (Pre-requisite for Chat)
        userViewModel.getMoveDetailsSaved().observe(getViewLifecycleOwner(), saved -> {
            if (saved != null && saved) {
                if (selectedMoverForChat != null) {
                    chatViewModel.startChatWithMover(selectedMoverForChat);
                    selectedMoverForChat = null; // Reset
                }
            }
        });

        // Observe Navigation to Chat
        chatViewModel.getNavigateToChatId().observe(getViewLifecycleOwner(), chatId -> {
            if (chatId != null) {
                chatViewModel.onChatNavigated(); // Clear navigation event
                Intent intent = new Intent(requireContext(), ChatActivity.class);
                intent.putExtra("CHAT_ID", chatId);
                startActivity(intent);
            }
        });

        // Observe Errors
        userViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        chatViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    // ------------------------------------------------------------------------
    // Logic: Places & Address Selection
    // ------------------------------------------------------------------------

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext());
        placePickerLauncher.launch(intent);
    }

    /**
     * Validates if an address string contains a number (indicating a street number).
     */
    private boolean hasStreetNumber(String address) {
        if (address == null) return false;
        // Simple Regex to check if the string contains at least one digit
        return address.matches(".*\\d.*");
    }

    private void handleAddressSelection(Place place) {
        if (place.getLatLng() == null) return;

        // Prefer full address, fallback to name
        String fullAddress = place.getAddress();
        if (fullAddress == null) fullAddress = place.getName();

        // Validation: Ensure precise address
        if (!hasStreetNumber(fullAddress)) {
            Toast.makeText(getContext(), "חובה לבחור כתובת מדויקת עם מספר רחוב!", Toast.LENGTH_LONG).show();
            return;
        }

        if (isSelectingSource) {
            tvSource.setText(fullAddress);
            sourceLatLng = place.getLatLng();
        } else {
            tvDest.setText(fullAddress);
            destLatLng = place.getLatLng();
        }
        btnSearch.setEnabled(sourceLatLng != null && destLatLng != null);
    }

    // ------------------------------------------------------------------------
    // Logic: Search & Date
    // ------------------------------------------------------------------------

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth);
            selectedDate = chosen.getTimeInMillis();
            btnSelectDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void performSearch() {
        if (sourceLatLng != null) {
            hasSearched = true;
            // Clear previous results
            adapter.setMovers(new ArrayList<>());
            // Trigger search in ViewModel
            userViewModel.searchMoversByLocation(sourceLatLng);
        }
    }

    /**
     * Populates fields from the user's saved profile (if available).
     */
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

        tvSource.setText(from);
        tvDest.setText(to);

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
            selectedDate = 0;
            btnSelectDate.setText("📅 בחר תאריך הובלה");
        }

        btnSearch.setEnabled(true);
        Toast.makeText(getContext(), "הפרטים מולאו מהאזור האישי", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------------
    // Adapter Callbacks (OnMoverActionClickListener)
    // ------------------------------------------------------------------------

    @Override
    public void onChatClick(UserProfile mover) {
        if (selectedDate == 0) {
            Toast.makeText(getContext(), "אנא בחר תאריך הובלה לפני יצירת קשר", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tvSource.getText().toString().isEmpty() || tvDest.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "אנא בחר כתובות מוצא ויעד", Toast.LENGTH_SHORT).show();
            return;
        }

        this.selectedMoverForChat = mover;

        // Save move details first, then Observer triggers chat
        userViewModel.saveMoveDetails(
                FirebaseAuth.getInstance().getUid(),
                tvSource.getText().toString(),
                tvDest.getText().toString(),
                selectedDate
        );
    }

    @Override
    public void onDetailsClick(UserProfile mover) {
        new AlertDialog.Builder(getContext())
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
}