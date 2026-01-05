package com.example.quizletproject2; // CHANGE THIS TO YOUR ACTUAL PACKAGE NAME

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    // Define UI elements
    private EditText emailField, passwordField;
    private Button loginButton;
    private TextView goToSignup, forgotPassword;

    // Define Firebase Auth instance
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in (Session Management)
        if (mAuth.getCurrentUser() != null) {
            // User is already logged in, go to Main/Dashboard
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return; // Return to prevent the rest of the method from running
        }

        setContentView(R.layout.activity_login);

        // Link Java objects to XML IDs
        emailField = findViewById(R.id.etLoginEmail);
        passwordField = findViewById(R.id.etLoginPassword);
        loginButton = findViewById(R.id.btnLogin);
        goToSignup = findViewById(R.id.tvGoToSignup);
        forgotPassword = findViewById(R.id.tvForgotPassword);

        // Click Listener for Login Button
        loginButton.setOnClickListener(v -> loginUser());

        // Click Listener to switch to Signup screen
        goToSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));

        // Click Listener to go to Forgot Password screen
        forgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        // Validation: Ensure fields are not empty
        if (TextUtils.isEmpty(email)) {
            emailField.setError("Email is required");
            emailField.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Please enter a valid email");
            emailField.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordField.setError("Password is required");
            passwordField.requestFocus();
            return;
        }

        // Firebase Sign In Method
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Prevent going back to login
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
