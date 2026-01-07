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

import com.example.easymove.BuildConfig; // וודא שיש לך את זה או תשתמש ב-String ישירות
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

    // ✅ הוספנו את המשתנה ל-View של הכתובת
    private EditText editMoverAddress;

    private Button buttonAction;
    private TextView switchModeText, titleText, textUserTypeLabel;
    private RadioGroup radioUserType;
    private RadioButton radioCustomer, radioMover;
    private LinearLayout layoutMoverRegistration;
    private SignInButton btnGoogleSignIn;

    private boolean isLoginMode = true;
    private GoogleSignInClient mGoogleSignInClient;

    // ✅ משתנים לשמירת המיקום שנבחר (זמני עד ללחיצה על הרשמה)
    private Double selectedLat = null;
    private Double selectedLng = null;

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

    // ✅ Launcher לחיפוש כתובת (התיקון שלך)
    private final ActivityResultLauncher<Intent> addressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {
                        selectedLat = place.getLatLng().latitude;
                        selectedLng = place.getLatLng().longitude;
                        editMoverAddress.setText(place.getAddress()); // מציג את הכתובת למשתמש
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // ✅ אתחול Places (חובה!)
        if (!Places.isInitialized()) {
            // שים לב: עדיף לשמור את המפתח ב-local.properties או BuildConfig
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

        // ✅ וודא שבקובץ XML יש לך EditText עם ה-ID הזה בתוך layoutMoverRegistration
        editMoverAddress = findViewById(R.id.editMoverAddress);

        textUserTypeLabel = findViewById(R.id.textUserTypeLabel);
        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);
        radioUserType = findViewById(R.id.radio_user_type);
        radioCustomer = findViewById(R.id.radio_customer);
        radioMover = findViewById(R.id.radio_mover);
        layoutMoverRegistration = findViewById(R.id.layoutMoverRegistration);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        radioUserType.setOnCheckedChangeListener((group, checkedId) ->
                layoutMoverRegistration.setVisibility(checkedId == R.id.radio_mover ? View.VISIBLE : View.GONE)
        );

        buttonAction.setOnClickListener(v -> handleButtonClick());

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // ✅ המאזין החסר! זה מה שיתקן את הבאג
        if (editMoverAddress != null) {
            // מונע מהמקלדת לקפוץ כשלוחצים על השדה
            editMoverAddress.setFocusable(false);
            editMoverAddress.setClickable(true);

            editMoverAddress.setOnClickListener(v -> {
                // פתיחת חלון החיפוש של גוגל
                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .build(this);
                addressLauncher.launch(intent);
            });
        }
    }

    // ... (observeViewModel נשאר אותו דבר) ...

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
            String phone = editPhone.getText().toString().trim(); // טלפון הוא אופציונלי, לא בודקים אותו

            // 1. בדיקות שדות חובה לכולם (לקוח ומוביל)
            if (TextUtils.isEmpty(email)) {
                editEmail.setError("אימייל הוא שדה חובה");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                editPassword.setError("סיסמה היא שדה חובה");
                return;
            }
            if (TextUtils.isEmpty(name)) {
                editName.setError("שם מלא הוא שדה חובה");
                return;
            }

            boolean isCustomer = radioCustomer.isChecked();

            // 2. בדיקת חובה ספציפית למוביל: כתובת בסיס
            if (!isCustomer) {
                // אנחנו בודקים אם יש קואורדינטות (selectedLat/Lng)
                // זה מבטיח שהמשתמש באמת בחר מהרשימה של גוגל ולא סתם דילג
                if (selectedLat == null || selectedLng == null || TextUtils.isEmpty(editMoverAddress.getText())) {
                    Toast.makeText(this, "מוביל חייב לבחור כתובת בסיס מהרשימה", Toast.LENGTH_LONG).show();
                    return; // עוצר את ההרשמה
                }
            }

            String address = editMoverAddress.getText().toString();

            // אם הגענו לפה - כל השדות תקינים, מבצעים הרשמה
            viewModel.register(email, password, name, phone, isCustomer, address, selectedLat, selectedLng);
        }
    }

    // ... (שאר הפונקציות ללא שינוי, observeViewModel, updateMode, showCompleteProfileDialog) ...
    // רק שים לב: ב-observeViewModel וודא שאתה לא מוחק שום דבר חיוני.
    // את הפונקציה onConfirmClicked ושאר הקוד שלא קשור לכאן הסרנו כי הוא שייך ל-ChatActivity.

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

    // ... (שאר הדיאלוגים והעזרים נשארים זהים למה ששלחתי קודם) ...
    private void updateMode() {
        if (isLoginMode) {
            titleText.setText("התחברות");
            editName.setVisibility(View.GONE);
            editPhone.setVisibility(View.GONE);
            textUserTypeLabel.setVisibility(View.GONE);
            radioUserType.setVisibility(View.GONE);
            layoutMoverRegistration.setVisibility(View.GONE);
            buttonAction.setText("התחבר");
            switchModeText.setText("אין לך חשבון? להרשמה לחצי כאן");
        } else {
            titleText.setText("הרשמה");
            editName.setVisibility(View.VISIBLE);
            editPhone.setVisibility(View.VISIBLE);
            textUserTypeLabel.setVisibility(View.VISIBLE);
            radioUserType.setVisibility(View.VISIBLE);
            // מציגים רק אם זה מוביל
            layoutMoverRegistration.setVisibility(radioMover.isChecked() ? View.VISIBLE : View.GONE);
            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");
        }
    }

    private void showCompleteProfileDialog(String uid) {
        // ... (אותו קוד כמו קודם) ...
        // הערה: אם אתה רוצה לאפשר למוביל להשלים פרטים בגוגל, זה מורכב יותר
        // ודורש דיאלוג מותאם אישית עם בחירת כתובת.
        // לבינתיים הקוד הקודם שומר אותו בלי כתובת וזה בסדר (הוא יערוך בפרופיל).
        new AlertDialog.Builder(this)
                .setTitle("השלמת הרשמה")
                .setMessage("ברוכים הבאים! אנא בחר סוג משתמש להמשך.")
                .setCancelable(false)
                .setPositiveButton("אני לקוח", (dialog, which) ->
                        viewModel.completeGoogleRegistration(uid, "customer")
                )
                .setNegativeButton("אני מוביל", (dialog, which) -> {
                    Toast.makeText(this, "מוביל נרשם בלי מיקום כרגע. יש להשלים בפרופיל.", Toast.LENGTH_LONG).show();
                    viewModel.completeGoogleRegistration(uid, "mover");
                })
                .show();
    }
}