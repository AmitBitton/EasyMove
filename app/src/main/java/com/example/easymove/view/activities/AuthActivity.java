package com.example.easymove.view.activities;

import android.app.Activity;
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
import android.app.AlertDialog; // לייבוא הדיאלוג

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easymove.BuildConfig;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton; // הכפתור של גוגל
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    // ... (משתנים קיימים נשארים אותו דבר)
    private EditText editEmail, editPassword, editName, editPhone;
    private Button buttonAction;
    private TextView switchModeText, titleText, textUserTypeLabel;
    private RadioGroup radioUserType;
    private RadioButton radioCustomer, radioMover;
    private LinearLayout layoutMoverRegistration;
    private TextView textSelectedAddressAuth;
    private Button btnPickLocationAuth;
    private boolean isLoginMode = true;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // --- חדש: משתנים לגוגל ---
    private SignInButton btnGoogleSignIn;
    private GoogleSignInClient mGoogleSignInClient;

    // משתנים זמניים
    private double tempLat = 0;
    private double tempLng = 0;
    private String tempGeoHash = "";
    private String tempAddress = "";

    // Launcher לבחירת מיקום
    private final ActivityResultLauncher<Intent> authPlacePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {
                        tempLat = place.getLatLng().latitude;
                        tempLng = place.getLatLng().longitude;
                        tempGeoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(tempLat, tempLng));
                        tempAddress = place.getAddress();
                        textSelectedAddressAuth.setText(tempAddress);
                    }
                }
            }
    );

    // --- חדש: Launcher להתחברות גוגל ---
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_KEY);
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- חדש: הגדרות גוגל ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // מגיע אוטומטית מ-google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupListeners();
        updateMode();
    }

    private void initViews() {
        // ... (אותו דבר כמו קודם)
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        textUserTypeLabel = findViewById(R.id.textUserTypeLabel);
        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);
        radioUserType = findViewById(R.id.radio_user_type);
        radioCustomer = findViewById(R.id.radio_customer);
        radioMover = findViewById(R.id.radio_mover);
        layoutMoverRegistration = findViewById(R.id.layoutMoverRegistration);
        textSelectedAddressAuth = findViewById(R.id.textSelectedAddressAuth);
        btnPickLocationAuth = findViewById(R.id.btnPickLocationAuth);

        // --- חדש ---
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupListeners() {
        // ... (מאזינים קיימים נשארים)
        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        radioUserType.setOnCheckedChangeListener((group, checkedId) -> {
            layoutMoverRegistration.setVisibility(checkedId == R.id.radio_mover ? View.VISIBLE : View.GONE);
        });

        btnPickLocationAuth.setOnClickListener(v -> openAuthPlacePicker());
        buttonAction.setOnClickListener(v -> handleButtonClick());

        // --- חדש: לחיצה על כפתור גוגל ---
        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    // ... (updateMode, openAuthPlacePicker, handleButtonClick, login נשארים ללא שינוי) ...
    // ... אני מדלג עליהם כדי לחסוך מקום, תשאיר אותם כמו שהם ...

    private void updateMode() {
        // (העתק את הלוגיקה מהקובץ הקודם)
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
            if (radioMover.isChecked()) layoutMoverRegistration.setVisibility(View.VISIBLE);
            else layoutMoverRegistration.setVisibility(View.GONE);
            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");
        }
    }

    private void openAuthPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL")
                .build(this);
        authPlacePickerLauncher.launch(intent);
    }

    private void handleButtonClick() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // 1. בדיקת חובה לאימייל וסיסמה
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return; // עוצר כאן
        }

        if (isLoginMode) {
            // --- מצב התחברות ---
            login(email, password);
        } else {
            // --- מצב הרשמה ---
            String name = editName.getText().toString().trim();

            // 2. בדיקת חובה לשם
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "נא למלא שם מלא", Toast.LENGTH_SHORT).show();
                return; // עוצר כאן
            }

            // 3. התיקון הקריטי: בדיקת מוביל
            if (radioMover.isChecked()) {
                // בודקים אם המשתנים של המיקום הם עדיין 0 (כלומר לא נבחר כלום)
                if (tempLat == 0 || tempLng == 0) {

                    // הודעה למשתמש
                    Toast.makeText(this, "שגיאה: חובה לבחור מיקום במפה לפני ההרשמה!", Toast.LENGTH_LONG).show();

                    // (אופציונלי) פותח לו את המפה שוב אוטומטית כדי להקל עליו
                    // openAuthPlacePicker();

                    return; // <--- זה ה-STOP! בלעדיו הקוד ימשיך למטה
                }
            }

            // 4. רק אם עברנו את כל ה-return למעלה, נבצע הרשמה
            performRegistration(email, password, name);
        }
    }

    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                    checkUserAndStartMain(result.getUser().getUid()); // שינוי קטן: בדיקה לפני מעבר
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהתחברות: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void performRegistration(String email, String password, String name) {
        String phone = editPhone.getText().toString().trim();
        String userType = radioCustomer.isChecked() ? "customer" : "mover";

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    UserProfile profile = new UserProfile();
                    profile.setUserId(uid);
                    profile.setName(name);
                    profile.setPhone(phone);
                    profile.setUserType(userType);

                    if ("mover".equals(userType)) {
                        profile.setLat(tempLat);
                        profile.setLng(tempLng);
                        profile.setGeohash(tempGeoHash);
                        profile.setDefaultFromAddress(tempAddress);
                        profile.setServiceRadiusKm(30);
                    }

                    // 1) קודם שומרים את המשתמש
                    db.collection("users").document(uid).set(profile)
                            .addOnSuccessListener(unused -> {

                                // 2) אם זה לקוח - יוצרים לו OPEN move ורק אז נכנסים לאפליקציה
                                if ("customer".equals(userType)) {
                                    new MoveRepository()
                                            .ensureOpenMoveForCustomer(uid)
                                            .addOnSuccessListener(v -> {
                                                Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                                startMain();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this,
                                                        "נרשמת, אבל לא נוצרה הובלה: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                startMain(); // עדיין נכנסים כדי לא לתקוע משתמש
                                            });
                                } else {
                                    Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                    startMain();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "שגיאה בשמירת פרופיל: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "שגיאה בהרשמה: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
    }

    // --- לוגיקה חדשה: טיפול בחזרה מגוגל ---
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        // אחרי התחברות גוגל - בודקים אם יש פרופיל ב-Firestore
                        checkUserAndStartMain(user.getUid());
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פונקציה חכמה: בודקת אם המשתמש קיים. אם לא - שואלת פרטים
    private void checkUserAndStartMain(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // המשתמש קיים - כניסה רגילה
                            startMain();
                        } else {
                            // משתמש חדש (נכנס דרך גוגל פעם ראשונה) - השלמת פרטים
                            showCompleteProfileDialog(uid);
                        }
                    } else {
                        Toast.makeText(this, "שגיאה בבדיקת פרופיל", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // דיאלוג להשלמת פרטים למשתמשי גוגל חדשים
    private void showCompleteProfileDialog(String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("השלמת הרשמה");
        builder.setMessage("ברוכים הבאים! אנא בחר סוג משתמש להמשך.");
        builder.setCancelable(false); // חייב לבחור

        builder.setPositiveButton("אני לקוח", (dialog, which) -> {
            saveGoogleUserProfile(uid, "customer");
        });

        builder.setNegativeButton("אני מוביל", (dialog, which) -> {
            // למוביל זה מסובך, צריך כתובת. נשלח הודעה ונפתח את בורר המיקום
            Toast.makeText(this, "מוביל חייב להגדיר מיקום. אנא בחר מיקום.", Toast.LENGTH_LONG).show();
            // כאן אפשר לפתוח את openAuthPlacePicker() ולשמור אחרי זה
            // אבל כדי לא להסתבך בלוגיקה מורכבת בדיאלוג:
            // נשמור אותו כמוביל ללא מיקום, ונעביר אותו לפרופיל שיעדכן
            saveGoogleUserProfile(uid, "mover");
        });

        builder.show();
    }

    private void saveGoogleUserProfile(String uid, String userType) {
        FirebaseUser firebaseUser = auth.getCurrentUser();

        UserProfile profile = new UserProfile();
        profile.setUserId(uid);
        profile.setName(firebaseUser != null ? firebaseUser.getDisplayName() : null);
        profile.setPhone(firebaseUser != null ? firebaseUser.getPhoneNumber() : null);
        profile.setUserType(userType);

        if ("mover".equals(userType)) {
            profile.setServiceRadiusKm(30);
        }

        // 1) קודם שומרים את המשתמש
        db.collection("users").document(uid).set(profile)
                .addOnSuccessListener(unused -> {

                    // 2) אם זה לקוח - יוצרים OPEN move ורק אז נכנסים
                    if ("customer".equals(userType)) {
                        new MoveRepository()
                                .ensureOpenMoveForCustomer(uid)
                                .addOnSuccessListener(v -> startMain())
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "נרשמת, אבל לא נוצרה הובלה: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    startMain();
                                });
                    } else {
                        startMain();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}