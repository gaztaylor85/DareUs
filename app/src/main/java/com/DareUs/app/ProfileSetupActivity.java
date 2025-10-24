package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.HashMap;
import java.util.Map;

public class ProfileSetupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText editTextFirstName;
    private Button buttonContinue;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_profile_setup);

            // Initialize Firebase
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            currentUser = mAuth.getCurrentUser();

            // Check if user is logged in
            if (currentUser == null) {
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
                return;
            }

            // Initialize views
            editTextFirstName = findViewById(R.id.editTextFirstName);
            buttonContinue = findViewById(R.id.buttonContinue);

            // Set click listener
            buttonContinue.setOnClickListener(v -> saveProfile());

            // Check if profile already exists with timeout
            checkExistingProfile();

        } catch (Exception e) {
            Log.e("ProfileSetup", "Critical error in onCreate", e);
            // Emergency fallback
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        }
    }

    // Replace your ProfileSetupActivity.java checkExistingProfile method with this enhanced version:

    private void checkExistingProfile() {
        Log.d("ProfileSetup", "=== CHECKING EXISTING PROFILE ===");
        Log.d("ProfileSetup", "Checking for user: " + currentUser.getUid());

        // Show loading message
        if (editTextFirstName != null) {
            editTextFirstName.setHint("Checking your profile...");
            editTextFirstName.setEnabled(false);
        }
        if (buttonContinue != null) {
            buttonContinue.setText("Loading...");
            buttonContinue.setEnabled(false);
        }

        // Set up a timeout handler
        android.os.Handler timeoutHandler = new android.os.Handler();
        final boolean[] hasCompleted = {false};

        // 10 second timeout
        Runnable timeoutRunnable = () -> {
            if (!hasCompleted[0]) {
                Log.w("ProfileSetup", "Profile check timed out - allowing manual setup");
                hasCompleted[0] = true;
                enableProfileSetup();
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 10000);

        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (hasCompleted[0]) return; // Already timed out
                    hasCompleted[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    Log.d("ProfileSetup", "Profile check SUCCESS");
                    Log.d("ProfileSetup", "Document exists: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        Log.d("ProfileSetup", "Document data: " + documentSnapshot.getData());
                        Log.d("ProfileSetup", "Has firstName: " + documentSnapshot.contains("firstName"));

                        String firstName = documentSnapshot.getString("firstName");
                        if (firstName != null && !firstName.trim().isEmpty()) {
                            Log.d("ProfileSetup", "✅ PROFILE EXISTS - REDIRECTING!");
                            goToMainActivityFixed();
                        } else {
                            Log.d("ProfileSetup", "Profile exists but no valid firstName - enabling setup");
                            enableProfileSetup();
                        }
                    } else {
                        Log.d("ProfileSetup", "No profile document found - enabling setup");
                        enableProfileSetup();
                    }
                })
                .addOnFailureListener(e -> {
                    if (hasCompleted[0]) return; // Already timed out
                    hasCompleted[0] = true;
                    timeoutHandler.removeCallbacks(timeoutRunnable);

                    Log.e("ProfileSetup", "❌ PROFILE CHECK FAILED: " + e.getMessage());
                    Log.d("ProfileSetup", "Network error - allowing manual setup");
                    enableProfileSetup();
                });
    }

    private void enableProfileSetup() {
        Log.d("ProfileSetup", "Enabling profile setup UI");

        if (editTextFirstName != null) {
            editTextFirstName.setHint("Enter your first name");
            editTextFirstName.setEnabled(true);
            editTextFirstName.requestFocus();
        }
        if (buttonContinue != null) {
            buttonContinue.setText("Continue");
            buttonContinue.setEnabled(true);
        }
    }

    private void goToMainActivityFixed() {
        Log.d("ProfileSetup", "Profile exists - redirecting to MainActivity");
        showCustomToast("Welcome back! Loading dashboard...");

        // Small delay to show the toast
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(ProfileSetupActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 500);
    }

    // Also update your saveProfile method to be more robust:
    private void saveProfile() {
        String firstName = editTextFirstName.getText().toString().trim();

        Log.d("ProfileSetup", "=== STARTING PROFILE SAVE ===");
        Log.d("ProfileSetup", "First name: '" + firstName + "'");

        if (TextUtils.isEmpty(firstName)) {
            editTextFirstName.setError("First name is required");
            return;
        }

        if (firstName.length() < 2) {
            editTextFirstName.setError("Name must be at least 2 characters");
            return;
        }

        if (firstName.length() > 15) {
            editTextFirstName.setError("Name must be 15 characters or less");
            return;
        }

        // Show loading state
        buttonContinue.setText("Setting up...");
        buttonContinue.setEnabled(false);
        editTextFirstName.setEnabled(false);

        // Create user profile
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("firstName", firstName);
        userProfile.put("email", currentUser.getEmail());
        userProfile.put("uid", currentUser.getUid());
        userProfile.put("createdAt", System.currentTimeMillis());
        userProfile.put("points", 0);
        userProfile.put("partnerId", null);
        userProfile.put("streakCount", 0);
        userProfile.put("totalDares", 0);

        Log.d("ProfileSetup", "Saving to Firestore...");

        // Set up timeout for save operation
        android.os.Handler saveTimeoutHandler = new android.os.Handler();
        final boolean[] saveCompleted = {false};

        Runnable saveTimeoutRunnable = () -> {
            if (!saveCompleted[0]) {
                Log.w("ProfileSetup", "Save operation timed out");
                saveCompleted[0] = true;
                buttonContinue.setText("Continue");
                buttonContinue.setEnabled(true);
                editTextFirstName.setEnabled(true);
                showCustomToast("Save timed out. Please try again.");
            }
        };
        saveTimeoutHandler.postDelayed(saveTimeoutRunnable, 15000);

        // Save to Firestore
        db.collection("dareus").document(currentUser.getUid())
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    if (saveCompleted[0]) return;
                    saveCompleted[0] = true;
                    saveTimeoutHandler.removeCallbacks(saveTimeoutRunnable);

                    Log.d("ProfileSetup", "✅ SAVE SUCCESS!");
                    showCustomToast("Welcome to DareUs, " + firstName + "! 💕");
                    goToPartnerLinking();
                })
                .addOnFailureListener(e -> {
                    if (saveCompleted[0]) return;
                    saveCompleted[0] = true;
                    saveTimeoutHandler.removeCallbacks(saveTimeoutRunnable);

                    Log.e("ProfileSetup", "❌ SAVE FAILED: " + e.getMessage());
                    buttonContinue.setText("Continue");
                    buttonContinue.setEnabled(true);
                    editTextFirstName.setEnabled(true);
                    showCustomToast("Failed to save profile. Please try again.");
                });
    }


    private void goToPartnerLinking() {
        Log.d("ProfileSetup", "Profile saved successfully - going to partner linking");

        showCustomToast("Profile saved! Setting up partner linking...");

        // Small delay to ensure Firestore write is complete
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(ProfileSetupActivity.this, PartnerLinkingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, 800); // Slightly longer delay to ensure save is complete
    }


    private void showCustomToast(String message) {
        LinearLayout toastLayout = new LinearLayout(this);
        toastLayout.setOrientation(LinearLayout.HORIZONTAL);
        toastLayout.setBackgroundColor(0xFF2A2A2A);
        toastLayout.setPadding(24, 16, 24, 16);
        toastLayout.setGravity(Gravity.CENTER);

        TextView toastText = new TextView(this);
        toastText.setText(message);
        toastText.setTextColor(0xFFFFFFFF);
        toastText.setTextSize(14);

        toastLayout.addView(toastText);

        Toast customToast = new Toast(this);
        customToast.setDuration(Toast.LENGTH_SHORT);
        customToast.setView(toastLayout);
        customToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        customToast.show();
    }
}