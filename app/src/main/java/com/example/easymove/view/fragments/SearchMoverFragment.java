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
import com.example.easymove.model.UserProfile;
import com.example.easymove.adapters.MoversAdapter; // Use the new MoverAdapter
import com.example.easymove.viewmodel.UserViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SearchMoverFragment extends Fragment {

    private UserViewModel userViewModel;
    private MoversAdapter adapter; // Updated Adapter class
    private TextView tvSource, tvDest;
    private Button btnSearch;

    private LatLng sourceLatLng = null;
    private LatLng destLatLng = null;

    private boolean hasSearched = false;
    private boolean isSelectingSource = true;

    // Place Picker Launcher
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

        // 1. Initialize Google Places
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_KEY);
        }

        // 2. Initialize ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        // 3. Initialize Views
        tvSource = view.findViewById(R.id.tvSourceResult);
        tvDest = view.findViewById(R.id.tvDestResult);
        btnSearch = view.findViewById(R.id.btnSearchAction);
        RecyclerView recycler = view.findViewById(R.id.recyclerMovers);

        // 4. Setup RecyclerView & Adapter with new Listener Logic
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MoversAdapter(getContext(), new ArrayList<>(), new MoversAdapter.OnMoverActionClickListener() {
            @Override
            public void onChatClick(UserProfile mover) {
                openChatWithMover(mover);
            }

            @Override
            public void onDetailsClick(UserProfile mover) {
                showMoverDetailsDialog(mover);
            }

            @Override
            public void onReviewsClick(UserProfile mover) {
                // Handle Reviews Click
                Toast.makeText(getContext(), "צפייה בביקורות - יפותח בהמשך", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReportClick(UserProfile mover) {
                // Handle Report Click
                Toast.makeText(getContext(), "דיווח נשלח לאדמין", Toast.LENGTH_SHORT).show();
            }
        });

        recycler.setAdapter(adapter);

        recycler.setAdapter(adapter);

        // 5. Setup Address Buttons
        view.findViewById(R.id.btnSourceAddress).setOnClickListener(v -> {
            isSelectingSource = true;
            openPlacePicker();
        });

        view.findViewById(R.id.btnDestAddress).setOnClickListener(v -> {
            isSelectingSource = false;
            openPlacePicker();
        });

        // 6. Setup Search Button
        btnSearch.setOnClickListener(v -> performSearch());

        // 7. Observe Data Changes
        userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
            // Convert list if necessary or pass directly
            // Assuming your ViewModel returns List<User> (or UserProfile that maps to User)
            // If there's a mismatch, you might need a converter loop here.
            // For now assuming compatibility:
            adapter.updateList(movers);

            if (hasSearched && movers.isEmpty()) {
                Toast.makeText(getContext(), "לא נמצאו מובילים ברדיוס הקרוב", Toast.LENGTH_LONG).show();
            }
        });

        userViewModel.getErrorMessageLiveData().observe(getViewLifecycleOwner(), error -> {
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

        // Enable search only if both locations are selected
        btnSearch.setEnabled(sourceLatLng != null && destLatLng != null);
    }

    private void performSearch() {
        if (sourceLatLng != null) {
            hasSearched = true;
            adapter.updateList(new ArrayList<>()); // Visual clear
            userViewModel.searchMoversByLocation(sourceLatLng);
        }
    }

    // --- Helper Logic for Actions ---

    private void openChatWithMover(UserProfile mover) {
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String moverId = mover.getUserId(); // Make sure your model has getUid() or getId()

        // Create Consistent Chat ID
        String chatId;
        if (currentUserId.compareTo(moverId) < 0) {
            chatId = currentUserId + "_" + moverId;
        } else {
            chatId = moverId + "_" + currentUserId;
        }

        // Start Chat Activity
        Intent intent = new Intent(getContext(), com.example.easymove.view.activities.ChatActivity.class);
        intent.putExtra("CHAT_ID", chatId);
        intent.putExtra("OTHER_USER_ID", moverId);
        intent.putExtra("OTHER_USER_NAME", mover.getName());
        intent.putExtra("OTHER_USER_IMAGE", mover.getProfileImageUrl());

        // --- PASS THE ADDRESSES ---
        // Make sure tvSource/tvDest are not null
        String source = tvSource.getText().toString();
        String dest = tvDest.getText().toString();

        intent.putExtra("SOURCE_ADDRESS", source);
        intent.putExtra("DEST_ADDRESS", dest);

        // Optional: We can pass coordinates too if you want to save lat/lng
        if (sourceLatLng != null) {
            intent.putExtra("SOURCE_LAT", sourceLatLng.latitude);
            intent.putExtra("SOURCE_LNG", sourceLatLng.longitude);
        }
        if (destLatLng != null) {
            intent.putExtra("DEST_LAT", destLatLng.latitude);
            intent.putExtra("DEST_LNG", destLatLng.longitude);
        }
        // -------------------------------

        startActivity(intent);
    }

    private void showMoverDetailsDialog(UserProfile mover) {
        // Simple dialog to show details
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("על " + mover.getName())

                .setMessage(mover.getAbout() != null ? mover.getAbout() : "אין מידע נוסף")
                .setPositiveButton("סגור", null)
                .show();
    }
}