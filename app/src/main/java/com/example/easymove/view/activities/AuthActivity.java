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
import android.app.AlertDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.example.easymove.model.repository.MoveRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editName, editPhone;
    private Button buttonAction;
    private TextView switchModeText, titleText, textUserTypeLabel;
    private RadioGroup radioUserType;
    private RadioButton radioCustomer, radioMover;
    private LinearLayout layoutMoverRegistration;

    private boolean isLoginMode = true;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private SignInButton btnGoogleSignIn;
    private GoogleSignInClient mGoogleSignInClient;

    // --- Launcher להתחברות גוגל ---
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            Toast.makeText(this, "Google sign in failed: account is null", Toast.LENGTH_SHORT).show();
                        }
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

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- הגדרות גוגל ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        initViews();
        setupListeners();
        updateMode();
    }

    private void initViews() {
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

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
        }
    }

    private void setupListeners() {
        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        radioUserType.setOnCheckedChangeListener((group, checkedId) -> {
            layoutMoverRegistration.setVisibility(checkedId == R.id.radio_mover ? View.VISIBLE : View.GONE);
        });

        buttonAction.setOnClickListener(v -> handleButtonClick());

        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

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

            if (radioMover.isChecked()) layoutMoverRegistration.setVisibility(View.VISIBLE);
            else layoutMoverRegistration.setVisibility(View.GONE);

            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");
        }
    }

    private void handleButtonClick() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoginMode) {
            login(email, password);
        } else {
            String name = editName.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "נא למלא שם מלא", Toast.LENGTH_SHORT).show();
                return;
            }

            performRegistration(email, password, name);
        }
    }

    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                    checkUserAndStartMain(result.getUser().getUid());
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

                    // ✅ בלי מיקום כאן. מקסימום רדיוס שירות למוביל
                    if ("mover".equals(userType)) {
                        profile.setServiceRadiusKm(30);
                    }

                    db.collection("users").document(uid).set(profile)
                            .addOnSuccessListener(unused -> {
                                if ("customer".equals(userType)) {
                                    new MoveRepository()
                                            .ensureActiveMoveForCustomer(uid)
                                            .addOnSuccessListener(v -> {
                                                Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                                startMain();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this,
                                                        "נרשמת, אבל לא נוצרה הובלה: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                startMain();
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

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) checkUserAndStartMain(user.getUid());
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserAndStartMain(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            startMain();
                        } else {
                            showCompleteProfileDialog(uid);
                        }
                    } else {
                        Toast.makeText(this, "שגיאה בבדיקת פרופיל", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCompleteProfileDialog(String uid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("השלמת הרשמה");
        builder.setMessage("ברוכים הבאים! אנא בחר סוג משתמש להמשך.");
        builder.setCancelable(false);

        builder.setPositiveButton("אני לקוח", (dialog, which) -> saveGoogleUserProfile(uid, "customer"));

        builder.setNegativeButton("אני מוביל", (dialog, which) -> {
            Toast.makeText(this, "מוביל נרשם בלי מיקום. אפשר להשלים בפרופיל אחר כך.", Toast.LENGTH_LONG).show();
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

        db.collection("users").document(uid).set(profile)
                .addOnSuccessListener(unused -> {
                    if ("customer".equals(userType)) {
                        new MoveRepository()
                                .ensureActiveMoveForCustomer(uid)
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
