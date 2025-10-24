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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public class PartnerLinkingActivity extends AppCompatActivity implements BadgeTracker.BadgeUnlockListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextView textViewYourCode, textViewWelcome;
    private EditText editTextPartnerCode;
    private Button buttonGenerateCode, buttonConnectPartner, buttonSkipForNow;
    private String myInviteCode;
    private ListenerRegistration partnerListener;
    private BadgeTracker badgeTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_linking);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Initialize badge tracker
        badgeTracker = new BadgeTracker(currentUser.getUid(), this);

        // Initialize views
        textViewWelcome = findViewById(R.id.textViewWelcome);
        textViewYourCode = findViewById(R.id.textViewYourCode);
        editTextPartnerCode = findViewById(R.id.editTextPartnerCode);
        buttonGenerateCode = findViewById(R.id.buttonGenerateCode);
        buttonConnectPartner = findViewById(R.id.buttonConnectPartner);
        buttonSkipForNow = findViewById(R.id.buttonSkipForNow);

        // Set click listeners
        buttonGenerateCode.setOnClickListener(v -> generateInviteCode());
        buttonConnectPartner.setOnClickListener(v -> connectToPartner());
        buttonSkipForNow.setOnClickListener(v -> skipToMainActivity());

        // Load user data and check if they already have a partner or code
        loadUserData();

        // Start listening for partner connections
        startPartnerListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener when activity is destroyed
        if (partnerListener != null) {
            partnerListener.remove();
        }
    }

    @Override
    public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
        // Show EPIC badge celebration dialog!
        BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(this, badge);
        celebrationDialog.show();
    }


    private void startPartnerListener() {
        // Listen for changes to current user's document (specifically partnerId field)
        partnerListener = db.collection("dareus").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("PartnerLinking", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String partnerId = documentSnapshot.getString("partnerId");

                        // If partnerId just got set (someone connected to us)
                        if (partnerId != null && !partnerId.isEmpty()) {
                            Log.d("PartnerLinking", "Partner connected! Redirecting...");

                            // Get partner's name and show success message
                            db.collection("dareus").document(partnerId)
                                    .get()
                                    .addOnSuccessListener(partnerDoc -> {
                                        String partnerName = "Your Partner";
                                        if (partnerDoc.exists() && partnerDoc.contains("firstName")) {
                                            partnerName = partnerDoc.getString("firstName");
                                        }
                                        showCustomToast("🎉 " + partnerName + " connected to you!");

                                        // Small delay to show the toast, then redirect
                                        new android.os.Handler().postDelayed(() -> {
                                            goToMainActivity();
                                        }, 1500);
                                    })
                                    .addOnFailureListener(ex -> {
                                        showCustomToast("🎉 Partner connected!");
                                        new android.os.Handler().postDelayed(() -> {
                                            goToMainActivity();
                                        }, 1500);
                                    });
                        }
                    }
                });
    }

    private void loadUserData() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String partnerId = doc.getString("partnerId");
                        String inviteCode = doc.getString("inviteCode");

                        textViewWelcome.setText("Hey " + firstName + "! 💕");

                        // Check if already has partner
                        if (partnerId != null && !partnerId.isEmpty()) {
                            // Already has partner, go to main activity
                            showCustomToast("Partner already connected! 🎉");
                            goToMainActivity();
                            return;
                        }

                        // Check if already has invite code
                        if (inviteCode != null && !inviteCode.isEmpty()) {
                            myInviteCode = inviteCode;
                            textViewYourCode.setText("Your couple code: " + myInviteCode);
                            buttonGenerateCode.setText("Share Code");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PartnerLinking", "Error loading user data: " + e.getMessage());
                });
    }

    private void generateInviteCode() {
        if (myInviteCode != null) {
            // Already have code, just share it
            shareInviteCode();
            return;
        }

        buttonGenerateCode.setText("Generating...");
        buttonGenerateCode.setEnabled(false);

        // Call secure Cloud Function to generate invite code
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();

        functions.getHttpsCallable("generateInviteCode")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    boolean success = (boolean) response.get("success");

                    if (success) {
                        String inviteCode = (String) response.get("inviteCode");
                        myInviteCode = inviteCode;
                        textViewYourCode.setText("Your couple code: " + myInviteCode);
                        buttonGenerateCode.setText("Share Code");
                        buttonGenerateCode.setEnabled(true);
                        showCustomToast("Code generated! Share it with your partner.");
                        shareInviteCode();
                    }
                })
                .addOnFailureListener(e -> {
                    buttonGenerateCode.setText("Generate Code");
                    buttonGenerateCode.setEnabled(true);

                    String errorMessage = "Error generating code";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("resource-exhausted")) {
                            errorMessage = "You can only generate one code per day. Try again tomorrow!";
                        } else if (e.getMessage().contains("Try again in")) {
                            errorMessage = e.getMessage();
                        } else {
                            errorMessage = "Error: " + e.getMessage();
                        }
                    }
                    showCustomToast(errorMessage);
                });
    }

    private void shareInviteCode() {
        if (myInviteCode == null) return;

        // Track code sharing for Social Butterfly badge
        badgeTracker.onCodeShared();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey! Join me on DareUs 💕\n\n" +
                        "Use my couple code: " + myInviteCode + "\n\n" +
                        "Download the app and let's start our adventure together! 🎉");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join me on DareUs!");

        startActivity(Intent.createChooser(shareIntent, "Share couple code"));
    }

    private void connectToPartner() {
        String partnerCode = editTextPartnerCode.getText().toString().trim().toUpperCase();

        if (TextUtils.isEmpty(partnerCode)) {
            editTextPartnerCode.setError("Enter your partner's code");
            return;
        }

        if (partnerCode.length() != 12) {
            editTextPartnerCode.setError("Code must be 12 characters");
            return;
        }

        if (partnerCode.equals(myInviteCode)) {
            editTextPartnerCode.setError("You can't use your own code!");
            return;
        }

        buttonConnectPartner.setText("Connecting...");
        buttonConnectPartner.setEnabled(false);

        connectToPartnerByCode(partnerCode);
    }

    private void connectToPartnerByCode(String partnerCode) {
        Log.d("PartnerLinking", "Searching for partner with code: " + partnerCode);

        // Check if it's user's own code
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String myCode = doc.getString("inviteCode");
                    if (partnerCode.equals(myCode)) {
                        buttonConnectPartner.setText("Connect Partner");
                        buttonConnectPartner.setEnabled(true);
                        showCustomToast("You can't use your own code!");
                        return;
                    }

                    // Find partner by code - with better error handling
                    db.collection("dareus")
                            .whereEqualTo("inviteCode", partnerCode)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (!task.getResult().isEmpty()) {
                                        // Found partner
                                        String partnerUid = task.getResult().getDocuments().get(0).getId();
                                        String partnerName = task.getResult().getDocuments().get(0).getString("firstName");
                                        String partnerCurrentPartnerId = task.getResult().getDocuments().get(0).getString("partnerId");

                                        Log.d("PartnerLinking", "Found partner: " + partnerName + " (" + partnerUid + ")");
                                        Log.d("PartnerLinking", "Partner's current partner: " + partnerCurrentPartnerId);

                                        // Check if partner already has a partner
                                        if (partnerCurrentPartnerId != null && !partnerCurrentPartnerId.isEmpty()) {
                                            buttonConnectPartner.setText("Connect Partner");
                                            buttonConnectPartner.setEnabled(true);
                                            showCustomToast("This person is already partnered with someone else!");
                                            return;
                                        }

                                        // Send partner link request (requires mutual consent)
                                        sendPartnerLinkRequest(partnerUid, partnerName);
                                    } else {
                                        Log.d("PartnerLinking", "No partner found with code: " + partnerCode);
                                        buttonConnectPartner.setText("Connect Partner");
                                        buttonConnectPartner.setEnabled(true);
                                        showCustomToast("Invalid code. Check with your partner!");

                                    }
                                } else {
                                    Log.e("PartnerLinking", "Error querying partner", task.getException());
                                    buttonConnectPartner.setText("Connect Partner");
                                    buttonConnectPartner.setEnabled(true);
                                    showCustomToast("Error searching for partner. Try again!");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("PartnerLinking", "Error loading user data", e);
                    buttonConnectPartner.setText("Connect Partner");
                    buttonConnectPartner.setEnabled(true);
                    showCustomToast("Error loading your profile. Try again!");
                });
    }

    private void sendPartnerLinkRequest(String partnerUid, String partnerName) {
        // Call secure Cloud Function to send partner link request
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("partnerId", partnerUid);

        functions.getHttpsCallable("sendPartnerLinkRequest")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    boolean success = (boolean) response.get("success");

                    if (success) {
                        buttonConnectPartner.setText("Connect Partner");
                        buttonConnectPartner.setEnabled(true);

                        showCustomToast("Link request sent to " + partnerName + "! Waiting for their approval.");

                        // Go to main activity - they'll see the pending request
                        new android.os.Handler().postDelayed(() -> {
                            goToMainActivity();
                        }, 2000);
                    }
                })
                .addOnFailureListener(e -> {
                    buttonConnectPartner.setText("Connect Partner");
                    buttonConnectPartner.setEnabled(true);

                    String errorMessage = "Connection failed";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("already linked")) {
                            errorMessage = "You are already linked with a partner!";
                        } else if (e.getMessage().contains("already sent")) {
                            errorMessage = "You already sent a request to this person!";
                        } else if (e.getMessage().contains("linked with someone else")) {
                            errorMessage = "This person is already partnered with someone else!";
                        } else {
                            errorMessage = e.getMessage();
                        }
                    }
                    showCustomToast(errorMessage);
                });
    }

    private void skipToMainActivity() {
        showCustomToast("You can connect your partner later from settings! 💕");
        goToMainActivity();
    }

    private void goToMainActivity() {
        Intent intent = new Intent(PartnerLinkingActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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