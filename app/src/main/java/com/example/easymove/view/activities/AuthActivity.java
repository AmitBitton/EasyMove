package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.viewmodel.AuthViewModel;
// TODO: Replace Google Sign In prompt to the newer Credentials Manager
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

/**
 * Activity responsible for User Authentication (Login / Registration).
 * Handles:
 * 1. Email/Password Login & Registration.
 * 2. Google Sign-In.
 * 3. Collecting additional user details (User Type, Address) after Google Sign-In.
 * 4. Google Places Autocomplete for address selection.
 */
public class AuthActivity extends AppCompatActivity {

    // Constants for Address Selection Mode
    private static final int MODE_MOVER_ADDRESS = 0;
    private static final int MODE_CUSTOMER_SOURCE = 1;
    private static final int MODE_CUSTOMER_DEST = 2;

    private AuthViewModel viewModel;
    private GoogleSignInClient mGoogleSignInClient;

    // --- UI Components ---
    private EditText editEmail, editPassword, editName, editPhone;
    private EditText editMoverAddress;
    private TextView tvSource, tvDest;
    private LinearLayout layoutMoverRegistration;
    private LinearLayout layoutCustomerAddresses;
    private Button buttonAction;
    private TextView switchModeText, titleText, textUserTypeLabel;
    private RadioGroup radioUserType;
    private RadioButton radioCustomer;
    private SignInButton btnGoogleSignIn;

    // --- State Variables ---
    private boolean isLoginMode = true;
    private boolean isGoogleCompletionMode = false; // True if user logged in via Google but needs to fill profile
    private String pendingGoogleUid = null;
    private int addressRequestMode = MODE_MOVER_ADDRESS;

    // --- Data Holding Variables ---
    private Double moverLat = null;
    private Double moverLng = null;

    private String selectedSourceAddress = null;
    private Double selectedSourceLat = null;
    private Double selectedSourceLng = null;

    private String selectedDestAddress = null;
    private Double selectedDestLat = null;
    private Double selectedDestLng = null;

    // ------------------------------------------------------------------------
    // Activity Result Launchers
    // ------------------------------------------------------------------------

    /**
     * Launcher for Google Sign-In Intent.
     */
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        viewModel.handleGoogleSignIn(account.getIdToken(), account.getDisplayName());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    /**
     * Launcher for Google Places Autocomplete Intent.
     * Handles address selection for Mover Base, Customer Source, and Customer Destination.
     */
    private final ActivityResultLauncher<Intent> addressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {
                        updateAddressFields(place);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_KEY);
        }

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupListeners();
        observeViewModel();
        updateMode();
    }

    private void initViews() {
        // Basic Info
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);

        // Mover Specific
        layoutMoverRegistration = findViewById(R.id.layoutMoverRegistration);
        editMoverAddress = findViewById(R.id.editMoverAddress);

        // Customer Specific
        layoutCustomerAddresses = findViewById(R.id.layoutCustomerAddresses);
        tvSource = findViewById(R.id.tvRegSourceAddress);
        tvDest = findViewById(R.id.tvRegDestAddress);

        // Labels & Buttons
        textUserTypeLabel = findViewById(R.id.textUserTypeLabel);
        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);
        radioUserType = findViewById(R.id.radio_user_type);
        radioCustomer = findViewById(R.id.radio_customer);
        RadioButton radioMover = findViewById(R.id.radio_mover);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
        }
    }

    private void setupListeners() {
        // Toggle Login / Register mode
        switchModeText.setOnClickListener(v -> {
            if (isGoogleCompletionMode) return; // Prevent switching if completing Google profile
            isLoginMode = !isLoginMode;
            updateMode();
        });

        // Toggle User Type (Customer / Mover) UI
        radioUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_mover) {
                layoutMoverRegistration.setVisibility(View.VISIBLE);
                layoutCustomerAddresses.setVisibility(View.GONE);
            } else {
                layoutMoverRegistration.setVisibility(View.GONE);
                layoutCustomerAddresses.setVisibility(View.VISIBLE);
            }
        });

        buttonAction.setOnClickListener(v -> handleButtonClick());

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // Address Pickers
        if (editMoverAddress != null) {
            editMoverAddress.setFocusable(false);
            editMoverAddress.setClickable(true);
            editMoverAddress.setOnClickListener(v -> openPlacePicker(MODE_MOVER_ADDRESS));
        }

        tvSource.setOnClickListener(v -> openPlacePicker(MODE_CUSTOMER_SOURCE));
        tvDest.setOnClickListener(v -> openPlacePicker(MODE_CUSTOMER_DEST));
    }

    /**
     * Updates the UI variables based on the result from the Place Picker.
     */
    private void updateAddressFields(Place place) {
        if (place.getLatLng() == null) return;

        switch (addressRequestMode) {
            case MODE_MOVER_ADDRESS:
                moverLat = place.getLatLng().latitude;
                moverLng = place.getLatLng().longitude;
                editMoverAddress.setText(place.getAddress());
                break;

            case MODE_CUSTOMER_SOURCE:
                selectedSourceLat = place.getLatLng().latitude;
                selectedSourceLng = place.getLatLng().longitude;
                selectedSourceAddress = place.getAddress();
                tvSource.setText(selectedSourceAddress);
                tvSource.setTextColor(getColor(android.R.color.black));
                break;

            case MODE_CUSTOMER_DEST:
                selectedDestLat = place.getLatLng().latitude;
                selectedDestLng = place.getLatLng().longitude;
                selectedDestAddress = place.getAddress();
                tvDest.setText(selectedDestAddress);
                tvDest.setTextColor(getColor(android.R.color.black));
                break;
        }
    }

    private void openPlacePicker(int mode) {
        this.addressRequestMode = mode;
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        addressLauncher.launch(intent);
    }

    private void observeViewModel() {
        // Navigation Observer
        viewModel.getNavigateToMain().observe(this, shouldNavigate -> {
            if (shouldNavigate) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        // Error Observer
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });

        // Loading State Observer
        viewModel.getIsLoading().observe(this, isLoading -> {
            buttonAction.setEnabled(!isLoading);
            btnGoogleSignIn.setEnabled(!isLoading);
            if (isLoading) {
                buttonAction.setText("טוען...");
            } else {
                updateMode(); // Restores the correct button text
            }
        });

        // Google Completion Observer (User logged in via Google, but has no DB profile)
        viewModel.getGoogleUserIncomplete().observe(this, account -> {
            if (account != null) {
                isGoogleCompletionMode = true;
                isLoginMode = false; // Effectively a registration flow

                // Store UID for the completion step
                if (viewModel.getAuthInstance().getCurrentUser() != null) {
                    pendingGoogleUid = viewModel.getAuthInstance().getCurrentUser().getUid();
                }

                // Auto-fill name from Google Account
                if (account.getName() != null) {
                    editName.setText(account.getName());
                }

                Toast.makeText(this, "התחברת בהצלחה! אנא השלם את פרטי הכתובת.", Toast.LENGTH_LONG).show();
                updateMode();
            }
        });
    }

    private void handleButtonClick() {
        // --- CASE 1: Standard Login (Email/Password) ---
        if (isLoginMode && !isGoogleCompletionMode) {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                editEmail.setError("נא להזין אימייל");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editPassword.setError("נא להזין סיסמה");
                return;
            }
            viewModel.login(email, password);
            return;
        }

        // --- CASE 2: Registration / Google Completion ---
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Email/Password validation only for standard registration
        if (!isGoogleCompletionMode) {
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (TextUtils.isEmpty(name)) {
            editName.setError("חובה למלא שם");
            return;
        }

        boolean isCustomer = radioCustomer.isChecked();

        // Address Validation
        if (isCustomer) {
            if (selectedSourceAddress == null || selectedDestAddress == null) {
                Toast.makeText(this, "חובה לבחור כתובת מוצא ויעד להרשמה", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            if (moverLat == null || moverLng == null) {
                Toast.makeText(this, "מוביל חייב לבחור כתובת בסיס מהרשימה", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Prepare Data
        String addressToSend = isCustomer ? selectedSourceAddress : editMoverAddress.getText().toString();
        Double latToSend = isCustomer ? selectedSourceLat : moverLat;
        Double lngToSend = isCustomer ? selectedSourceLng : moverLng;

        if (isGoogleCompletionMode) {
            // Complete Google Sign-Up
            viewModel.completeGoogleRegistration(
                    pendingGoogleUid,
                    isCustomer ? "customer" : "mover",
                    phone,
                    addressToSend, latToSend, lngToSend,
                    selectedDestAddress, selectedDestLat, selectedDestLng
            );
        } else {
            // Standard Registration
            viewModel.register(email, password, name, phone, isCustomer,
                    addressToSend, latToSend, lngToSend,
                    selectedDestAddress, selectedDestLat, selectedDestLng);
        }
    }

    /**
     * Updates the UI visibility based on the current state:
     * 1. Google Completion (Missing details).
     * 2. Standard Login.
     * 3. Standard Registration.
     */
    private void updateMode() {
        // --- Mode: Google Completion ---
        if (isGoogleCompletionMode) {
            titleText.setText("השלמת פרטים");
            buttonAction.setText("סיום הרשמה");

            // Hide auth fields
            editEmail.setVisibility(View.GONE);
            editPassword.setVisibility(View.GONE);
            btnGoogleSignIn.setVisibility(View.GONE);
            switchModeText.setVisibility(View.GONE);

            // Show profile fields
            editName.setVisibility(View.VISIBLE);
            editPhone.setVisibility(View.VISIBLE);
            textUserTypeLabel.setVisibility(View.VISIBLE);
            radioUserType.setVisibility(View.VISIBLE);

            // Toggle Address Layouts
            if (radioCustomer.isChecked()) {
                layoutCustomerAddresses.setVisibility(View.VISIBLE);
                layoutMoverRegistration.setVisibility(View.GONE);
            } else {
                layoutCustomerAddresses.setVisibility(View.GONE);
                layoutMoverRegistration.setVisibility(View.VISIBLE);
            }
            return;
        }

        // --- Mode: Login ---
        if (isLoginMode) {
            titleText.setText("התחברות");
            buttonAction.setText("התחבר");
            switchModeText.setText("אין לך חשבון? להרשמה לחצי כאן");

            // Show Login Fields
            editEmail.setVisibility(View.VISIBLE);
            editPassword.setVisibility(View.VISIBLE);
            btnGoogleSignIn.setVisibility(View.VISIBLE);
            switchModeText.setVisibility(View.VISIBLE);

            // Hide Registration Fields
            editName.setVisibility(View.GONE);
            editPhone.setVisibility(View.GONE);
            textUserTypeLabel.setVisibility(View.GONE);
            radioUserType.setVisibility(View.GONE);
            layoutMoverRegistration.setVisibility(View.GONE);
            layoutCustomerAddresses.setVisibility(View.GONE);

        } else {
            // --- Mode: Register ---
            titleText.setText("הרשמה");
            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");

            // Show All Fields
            editName.setVisibility(View.VISIBLE);
            editPhone.setVisibility(View.VISIBLE);
            textUserTypeLabel.setVisibility(View.VISIBLE);
            radioUserType.setVisibility(View.VISIBLE);
            editEmail.setVisibility(View.VISIBLE);
            editPassword.setVisibility(View.VISIBLE);
            btnGoogleSignIn.setVisibility(View.VISIBLE);
            switchModeText.setVisibility(View.VISIBLE);

            // Toggle Address Layouts
            if (radioCustomer.isChecked()) {
                layoutCustomerAddresses.setVisibility(View.VISIBLE);
                layoutMoverRegistration.setVisibility(View.GONE);
            } else {
                layoutCustomerAddresses.setVisibility(View.GONE);
                layoutMoverRegistration.setVisibility(View.VISIBLE);
            }
        }
    }
}