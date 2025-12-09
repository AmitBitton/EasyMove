package com.example.easymove.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.easymove.view.activities.MainActivity;
import com.example.easymove.R;
import com.example.easymove.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editName, editPhone;
    private Button buttonAction;
    private TextView switchModeText, titleText;

    private RadioGroup radioUserType;
    private RadioButton radioCustomer, radioMover;

    private boolean isLoginMode = true;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור ל־Views
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);

        buttonAction = findViewById(R.id.buttonAction);
        switchModeText = findViewById(R.id.switchModeText);
        titleText = findViewById(R.id.textTitle);

        radioUserType = findViewById(R.id.radio_user_type);
        radioCustomer = findViewById(R.id.radio_customer);
        radioMover = findViewById(R.id.radio_mover);

        updateMode();

        switchModeText.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateMode();
        });

        buttonAction.setOnClickListener(v -> {
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
                String phone = editPhone.getText().toString().trim();
                String userType = radioCustomer.isChecked() ? "customer" : "mover";

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(this, "נא למלא שם מלא", Toast.LENGTH_SHORT).show();
                    return;
                }

                register(email, password, name, phone, userType);
            }
        });
    }

    private void updateMode() {
        if (isLoginMode) {
            titleText.setText("התחברות");
            editName.setVisibility(View.GONE);
            editPhone.setVisibility(View.GONE);
            radioUserType.setVisibility(View.GONE);
            buttonAction.setText("התחבר");
            switchModeText.setText("אין לך חשבון? להרשמה לחצי כאן");
        } else {
            titleText.setText("הרשמה");
            editName.setVisibility(View.VISIBLE);
            editPhone.setVisibility(View.VISIBLE);
            radioUserType.setVisibility(View.VISIBLE);
            buttonAction.setText("הרשמה");
            switchModeText.setText("יש לך כבר חשבון? התחברות");
        }
    }

    private void login(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                    startMain();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהתחברות: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void register(String email,
                          String password,
                          String name,
                          String phone,
                          String userType) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();

                    UserProfile profile = new UserProfile();
                    profile.setUserId(uid);
                    profile.setName(name);
                    profile.setPhone(phone);
                    profile.setUserType(userType);

                    db.collection("users")
                            .document(uid)
                            .set(profile)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                startMain();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "שגיאה בשמירת פרופיל: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהרשמה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
