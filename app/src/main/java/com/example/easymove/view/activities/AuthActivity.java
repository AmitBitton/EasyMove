package com.example.easymove.view.activities;

import android.app.AlertDialog;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
// --- ייבואים של גוגל מפות ---
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private AuthViewModel viewModel;

    private EditText editEmail, editPassword, editName, editPhone;

    // שדות כתובת למוביל וללקוח
    private EditText editMoverAddress;
    private TextView tvSource, tvDest;
    private LinearLayout layoutMoverRegistration;
    private LinearLayout layoutCustomerAddresses;

    private Button buttonAction;
    private TextView switchModeText, titleText, textUserTypeLabel;
    private RadioGroup radioUserType;
    private RadioButton radioCustomer, radioMover;

    private SignInButton btnGoogleSignIn;

    private boolean isLoginMode = true;
    private GoogleSignInClient mGoogleSignInClient;

    // --- משתנים לשמירת הכתובות שנבחרו ---
    // מצבי בחירה: 0=מוביל, 1=לקוח-מוצא, 2=לקוח-יעד
    private int addressRequestMode = 0;

    // נתוני מוביל
    private Double moverLat = null;
    private Double moverLng = null;

    // נתוני לקוח
    private String selectedSourceAddress = null;
    private Double selectedSourceLat = null;
    private Double selectedSourceLng = null;

    private String selectedDestAddress = null;
    private Double selectedDestLat = null;
    private Double selectedDestLng = null;

    // --- Launcher לגוגל התחברות ---
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        viewModel.handleGoogleSignIn(account);
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // ✅ Launcher אחד חכם לכל סוגי הכתובות
    private final ActivityResultLauncher<Intent> addressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {

                        // בדיקה איזה שדה עדכנו
                        if (addressRequestMode == 0) {
                            // מוביל
                            moverLat = place.getLatLng().latitude;
                            moverLng = place.getLatLng().longitude;
                            editMoverAddress.setText(place.getAddress());

                        } else if (addressRequestMode == 1) {
                            // לקוח - מוצא
                            selectedSourceLat = place.getLatLng().latitude;
                            selectedSourceLng = place.getLatLng().longitude;
                            selectedSourceAddress = place.getAddress(); // או place.getName()
                            tvSource.setText(selectedSourceAddress);
                            tvSource.setTextColor(getColor(android.R.color.black));

                        } else if (addressRequestMode == 2) {
                            // לקוח - יעד
                            selectedDestLat = place.getLatLng().latitude;
                            selectedDestLng = place.getLatLng().longitude;
                            selectedDestAddress = place.getAddress();
                            tvDest.setText(selectedDestAddress);
                            tvDest.setTextColor(getColor(android.R.color.black));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // אתחול Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_KEY);
        }

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

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
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);

        // שדות כתובת מוביל
        layoutMoverRegistration = findViewById(R.id.layoutMoverRegistration);
        editMoverAddress = findViewById(R.id.editMoverAddress);

        // שדות כתובת לקוח
        layoutCustomerAddresses = findViewById(R.id.layoutCustomerAddresses);
        tvSource = findViewById(R.id.tvRegSourceAddress);
        tvDest = findViewById(R.id.tvRegDestAddress);

        textUserTypeLabel = findViewById(R.id.textUserTypeLabel);
        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);
        radioUserType = findViewById(R.id.radio_user_type);
        radioCustomer = findViewById(R.id.radio_customer);
        radioMover = findViewById(R.id.radio_mover);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        // שינוי תצוגה לפי בחירת לקוח/מוביל
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

        // בחירת כתובת מוביל
        if (editMoverAddress != null) {
            editMoverAddress.setFocusable(false);
            editMoverAddress.setClickable(true);
            editMoverAddress.setOnClickListener(v -> openPlacePicker(0));
        }

        // בחירת כתובת מוצא לקוח
        tvSource.setOnClickListener(v -> openPlacePicker(1));

        // בחירת כתובת יעד לקוח
        tvDest.setOnClickListener(v -> openPlacePicker(2));
    }

    private void openPlacePicker(int mode) {
        this.addressRequestMode = mode;
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        addressLauncher.launch(intent);
    }

    private void observeViewModel() {
        viewModel.getNavigateToMain().observe(this, shouldNavigate -> {
            if (shouldNavigate) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            buttonAction.setEnabled(!isLoading);
            btnGoogleSignIn.setEnabled(!isLoading);
            if (isLoading) buttonAction.setText("טוען...");
            else updateMode();
        });

        viewModel.getShowGoogleTypeDialog().observe(this, this::showCompleteProfileDialog);
    }

    private void handleButtonClick() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // --- מצב התחברות (Login) ---
        if (isLoginMode) {
            if (TextUtils.isEmpty(email)) {
                editEmail.setError("נא להזין אימייל");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editPassword.setError("נא להזין סיסמה");
                return;
            }
            viewModel.login(email, password);
        }

        // --- מצב הרשמה (Register) ---
        else {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name)) {
                Toast.makeText(this, "נא למלא את כל שדות החובה", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isCustomer = radioCustomer.isChecked();

            // ✅ ולידציה ללקוח (שני הכתובות חובה!)
            if (isCustomer) {
                if (selectedSourceAddress == null || selectedDestAddress == null) {
                    Toast.makeText(this, "חובה לבחור כתובת מוצא ויעד להרשמה", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            // ✅ ולידציה למוביל (כתובת בסיס חובה!)
            else {
                if (moverLat == null || moverLng == null) {
                    Toast.makeText(this, "מוביל חייב לבחור כתובת בסיס מהרשימה", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // הכנת נתונים להעברה
            // שים לב: אנחנו מעבירים את הנתונים הרלוונטיים לפי סוג המשתמש
            String addressToSend = isCustomer ? selectedSourceAddress : editMoverAddress.getText().toString();
            Double latToSend = isCustomer ? selectedSourceLat : moverLat;
            Double lngToSend = isCustomer ? selectedSourceLng : moverLng;

            // אזהרה: עליך לעדכן את AuthViewModel כך שיקבל גם את נתוני ה-Destination של הלקוח!
            // כרגע אני מעביר את זה לפונקציה הקיימת, אבל מומלץ להוסיף ל-ViewModel גם destAddress, destLat, destLng
            // כדי שזה יישמר במקום הנכון (defaultToAddress).

            viewModel.register(email, password, name, phone, isCustomer,
                    addressToSend, latToSend, lngToSend,
                    // נתונים נוספים של הלקוח (יש לעדכן את הפונקציה ב-ViewModel לקבל אותם)
                    selectedDestAddress, selectedDestLat, selectedDestLng);
        }
    }

    private void updateMode() {
        if (isLoginMode) {
            titleText.setText("התחברות");
            editName.setVisibility(View.GONE);
            editPhone.setVisibility(View.GONE);
            textUserTypeLabel.setVisibility(View.GONE);
            radioUserType.setVisibility(View.GONE);
            layoutMoverRegistration.setVisibility(View.GONE);
            layoutCustomerAddresses.setVisibility(View.GONE);
            buttonAction.setText("התחבר");
            switchModeText.setText("אין לך חשבון? להרשמה לחצי כאן");
        } else {
            titleText.setText("הרשמה");
            editName.setVisibility(View.VISIBLE);
            editPhone.setVisibility(View.VISIBLE);
            textUserTypeLabel.setVisibility(View.VISIBLE);
            radioUserType.setVisibility(View.VISIBLE);

            // הצגת השדות הנכונים לפי הבחירה הנוכחית ברדיו
            if (radioCustomer.isChecked()) {
                layoutCustomerAddresses.setVisibility(View.VISIBLE);
                layoutMoverRegistration.setVisibility(View.GONE);
            } else {
                layoutCustomerAddresses.setVisibility(View.GONE);
                layoutMoverRegistration.setVisibility(View.VISIBLE);
            }

            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");
        }
    }

    // ... (פונקציית הדיאלוג נשארת ללא שינוי)
    private void showCompleteProfileDialog(String uid) {
        new AlertDialog.Builder(this)
                .setTitle("השלמת הרשמה")
                .setMessage("ברוכים הבאים! אנא בחר סוג משתמש להמשך.")
                .setCancelable(false)
                .setPositiveButton("אני לקוח", (dialog, which) -> viewModel.completeGoogleRegistration(uid, "customer"))
                .setNegativeButton("אני מוביל", (dialog, which) -> viewModel.completeGoogleRegistration(uid, "mover"))
                .show();
    }
}