package com.example.easymove.view.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.example.easymove.R;
import com.example.easymove.adapters.MoversAdapter;
import com.example.easymove.viewmodel.UserViewModel;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchMoverFragment extends Fragment {

    private UserViewModel userViewModel;
    private MoversAdapter adapter;

    private TextView tvSource, tvDest;
    private Button btnSearch;

    // משתנים לשמירת האזורים שזוהו
    private String sourceRegion = null;
    private String destRegion = null;

    // משתנה עזר לדעת איזה כפתור נלחץ (מוצא או יעד)
    private boolean isSelectingSource = true;

    // ה-Launcher לבחירת כתובת
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

        // אתחול Places API
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        tvSource = view.findViewById(R.id.tvSourceResult);
        tvDest = view.findViewById(R.id.tvDestResult);
        btnSearch = view.findViewById(R.id.btnSearchAction);
        RecyclerView recycler = view.findViewById(R.id.recyclerMovers);

        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MoversAdapter(mover -> {
            Toast.makeText(getContext(), "פתיחת צ'אט עם " + mover.getName(), Toast.LENGTH_SHORT).show();
        });
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

        // האזנה לתוצאות מה-ViewModel
        userViewModel.getMoversListLiveData().observe(getViewLifecycleOwner(), movers -> {
            if (movers.isEmpty()) {
                Toast.makeText(getContext(), "לא נמצאו מובילים באזורים אלו", Toast.LENGTH_SHORT).show();
            }
            adapter.setMovers(movers);
        });
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(requireContext());
        placePickerLauncher.launch(intent);
    }

    private void handleAddressSelection(Place place) {
        // חילוץ שם העיר מהכתובת
        String city = extractCity(place);

        // "קסם": המרה מעיר לאזור (לוגיקה פשוטה לדוגמה)
        String region = mapCityToRegion(city);

        if (isSelectingSource) {
            tvSource.setText(city + " (" + region + ")");
            sourceRegion = region;
        } else {
            tvDest.setText(city + " (" + region + ")");
            destRegion = region;
        }

        // אפשר לחפש רק אם נבחרו שתי הכתובות
        btnSearch.setEnabled(sourceRegion != null && destRegion != null);
    }

    private void performSearch() {
        List<String> areasToSearch = new ArrayList<>();
        if (sourceRegion != null) areasToSearch.add(sourceRegion);
        if (destRegion != null && !destRegion.equals(sourceRegion)) areasToSearch.add(destRegion);

        Toast.makeText(getContext(), "מחפש מובילים ב: " + areasToSearch, Toast.LENGTH_SHORT).show();
        userViewModel.loadMoversByAreas(areasToSearch);
    }

    // --- פונקציות עזר ללוגיקה הגיאוגרפית ---

    private String extractCity(Place place) {
        if (place.getAddressComponents() != null) {
            for (var component : place.getAddressComponents().asList()) {
                if (component.getTypes().contains("locality")) {
                    return component.getName();
                }
            }
        }
        return place.getName(); // Fallback
    }

    /**
     * מיפוי חכם של ערים לאזורים - תומך עברית ואנגלית
     */
    private String mapCityToRegion(String city) {
        if (city == null) return "מרכז"; // ברירת מחדל

        // נרמל את הטקסט כדי למנוע בעיות של אותיות גדולות/קטנות
        String normalized = city.toLowerCase().trim();

        // --- אזור המרכז ---
        if (containsAny(normalized, "tel aviv", "תל אביב", "ramat gan", "רמת גן",
                "givatayim", "גבעתיים", "holon", "חולון", "bat yam", "בת ים",
                "rishon", "ראשון", "tln")) {
            return "מרכז";
        }

        // --- אזור השרון ---
        if (containsAny(normalized, "netanya", "נתניה", "herzliya", "הרצליה",
                "kefar sava", "kfar saba", "כפר סבא", "ra'anana", "raanana", "רעננה",
                "hod hasharon", "הוד השרון", "ramat hasharon", "רמת השרון")) {
            return "השרון";
        }

        // --- אזור הדרום ---
        if (containsAny(normalized, "beer sheva", "be'er sheva", "באר שבע",
                "ashdod", "אשדוד", "ashkelon", "אשקלון", "eilat", "אילת",
                "sderot", "שדרות", "netivot", "נתיבות")) {
            return "דרום";
        }

        // --- אזור הצפון ---
        if (containsAny(normalized, "haifa", "חיפה", "akko", "acre", "עכו",
                "tiberias", "טבריה", "nahariya", "נהריה", "krayot", "קריות",
                "karmiel", "כרמיאל", "nazareth", "נצרת")) {
            return "צפון";
        }

        // --- ירושלים ---
        if (containsAny(normalized, "jerusalem", "yerushalayim", "ירושלים",
                "ma'ale adumim", "מעלה אדומים", "mevaseret", "מבשרת")) {
            return "ירושלים";
        }

        // --- שפלה ---
        if (containsAny(normalized, "rehovot", "רחובות", "ness ziona", "נס ציונה",
                "lod", "לוד", "ramla", "רמלה", "modi'in", "מודיעין")) {
            return "שפלה";
        }

        // --- ברירת מחדל חכמה ---
        // אם לא זיהינו, נניח שזה מרכז (או שתחזיר null ותטפל בזה)
        return "מרכז";
    }

    // פונקציית עזר לבדיקה אם מחרוזת מכילה אחד מהערכים
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}