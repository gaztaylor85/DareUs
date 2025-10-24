package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextView confirmPasswordLabel;
    private Button buttonLogin, buttonSignUp;
    private TextView textViewForgotPassword;
    private boolean isCreateAccountMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_welcome);

            mAuth = FirebaseAuth.getInstance();

            // Initialize views with null checks
            initializeViews();

            // Only set up click listeners if all views were found
            if (areViewsValid()) {
                setupClickListeners();
            } else {
                Log.e("WelcomeActivity", "Some views were not found in layout");
                showCustomToast("App initialization error. Please restart the app.");
            }

        } catch (Exception e) {
            Log.e("WelcomeActivity", "Critical error in onCreate", e);
            showCustomToast("App startup error. Please restart the app.");
        }
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        confirmPasswordLabel = findViewById(R.id.textViewConfirmPasswordLabel);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        if (editTextConfirmPassword != null) {
            editTextConfirmPassword.setVisibility(android.view.View.GONE);
        }
        if (confirmPasswordLabel != null) {
            confirmPasswordLabel.setVisibility(android.view.View.GONE);
        }
    }

    private boolean areViewsValid() {
        return editTextEmail != null &&
                editTextPassword != null &&
                editTextConfirmPassword != null &&
                buttonLogin != null &&
                buttonSignUp != null &&
                textViewForgotPassword != null;
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            if (isCreateAccountMode) {
                // Switch back to sign in mode
                switchToSignInMode();
            } else {
                signInUser();
            }
        });

        buttonSignUp.setOnClickListener(v -> {
            if (isCreateAccountMode) {
                // Actually create the account
                signUpUser();
            } else {
                // Switch to create account mode
                switchToCreateAccountMode();
            }
        });

        textViewForgotPassword.setOnClickListener(v -> forgotPassword());
    }

    private void switchToCreateAccountMode() {
        isCreateAccountMode = true;

        if (editTextConfirmPassword != null) {
            editTextConfirmPassword.setVisibility(android.view.View.VISIBLE);
        }
        if (confirmPasswordLabel != null) {
            confirmPasswordLabel.setVisibility(android.view.View.VISIBLE);
        }

        // Update button texts
        buttonLogin.setText("← Back to Sign In");
        buttonSignUp.setText("Create Account");

        // Clear fields
        if (editTextPassword != null) editTextPassword.setText("");
        if (editTextConfirmPassword != null) editTextConfirmPassword.setText("");
    }

    private void switchToSignInMode() {
        isCreateAccountMode = false;

        if (editTextConfirmPassword != null) {
            editTextConfirmPassword.setVisibility(android.view.View.GONE);
            editTextConfirmPassword.setText("");
        }
        if (confirmPasswordLabel != null) {
            confirmPasswordLabel.setVisibility(android.view.View.GONE);
        }

        // Update button texts
        buttonLogin.setText("Sign In");
        buttonSignUp.setText("Create New Account");

        // Clear password field
        if (editTextPassword != null) editTextPassword.setText("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                goToMainActivity();
            }
        } catch (Exception e) {
            Log.e("WelcomeActivity", "Error in onStart", e);
        }
    }

    private void signInUser() {
        if (!areViewsValid()) return;

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }

        buttonLogin.setText("Signing In...");
        buttonLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (areViewsValid()) {
                        buttonLogin.setText("Sign In");
                        buttonLogin.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        showCustomToast("Welcome back! 💕");
                        goToMainActivity();
                    } else {
                        String errorMessage = "Sign in failed. Please check your connection and try again.";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null && exceptionMessage.contains("network")) {
                                errorMessage = "No internet connection. Please check your network and try again.";
                            }
                        }
                        showCustomToast(errorMessage);
                    }
                });

        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutHandler.postDelayed(() -> {
            if (areViewsValid() && "Signing In...".equals(buttonLogin.getText().toString())) {
                buttonLogin.setText("Sign In");
                buttonLogin.setEnabled(true);
                showCustomToast("Sign in timed out. Please check your connection and try again.");
            }
        }, 10000); // 10 second timeout
    }

    private void signUpUser() {
        if (!areViewsValid()) return;

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPassword.setError("Please confirm your password");
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords don't match");
            return;
        }

        // Proceed with account creation
        createAccount(email, password);
    }

    private void createAccount(String email, String password) {
        if (!areViewsValid()) return;

        buttonSignUp.setText("Creating Account...");
        buttonSignUp.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (areViewsValid()) {
                        buttonSignUp.setText("Create New Account");
                        buttonSignUp.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            showCustomToast("Welcome to DareUs! 🎉");
                            goToMainActivity();
                        } else {
                            showCustomToast("Account created! Please sign in.");
                        }
                    } else {
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        showCustomToast(errorMessage);
                    }
                });
    }

    private void forgotPassword() {
        if (!areViewsValid()) return;

        String email = editTextEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Enter your email first");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showCustomToast("Password reset email sent! Check your inbox.");
                    } else {
                        showCustomToast("Failed to send reset email. Please check your email address.");
                    }
                });
    }

    private void goToMainActivity() {
        try {
            Intent intent = new Intent(WelcomeActivity.this, ProfileSetupActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("WelcomeActivity", "Error starting MainActivity", e);
            showCustomToast("Navigation error. Please try again.");
        }
    }

    private void showCustomToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("WelcomeActivity", "Error showing toast: " + message, e);
        }
    }
}