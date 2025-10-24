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
import java.util.HashMap;
import java.util.Map;

public class CustomDareActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private EditText editTextCustomDare, editTextProposedPoints;
    private Button buttonSendCustomDare, buttonBackToDashboard;
    private String currentPartnerId, partnerName;
    private PremiumManager premiumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        premiumManager = new PremiumManager(currentUser.getUid(), new PremiumManager.PremiumStatusListener() {
            @Override
            public void onPremiumStatusChanged(PremiumManager.PremiumTier tier) {
                checkCustomDareAccess();
            }

            @Override
            public void onUsageLimitReached(String category) {
                // Not needed for custom dares
            }
        });

        setupCustomDareUI();
        loadPartnerInfo();
    }

    private void checkCustomDareAccess() {
        if (!premiumManager.hasCustomDares()) {
            String message = "Custom dares require Premium Couple Plus! ✨\n\n";
            message += "Current status: " + premiumManager.getTierDisplayName() + "\n";
            message += premiumManager.getTierDescription() + "\n\n";
            message += "Upgrade to Premium Couple Plus ($4.99/month) to unlock custom dares! 💎";

            showCustomToast(message);

            // And redirect to upgrade activity
            Intent intent = new Intent(this, PremiumUpgradeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
    }

    private void setupCustomDareUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);
        mainLayout.setBackgroundResource(R.drawable.premium_gradient_bg);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("✨ Create Custom Dare");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("Design a personalized challenge for your partner! 🎯");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 16);

        // Premium status indicator
        TextView premiumStatus = new TextView(this);
        String statusText = "✅ Premium Plus Access: " + premiumManager.getTierDisplayName();
        String tierDescription = premiumManager.getTierDescription();
        if (tierDescription != null && !tierDescription.isEmpty()) {
            statusText += "\n" + tierDescription;
        }
        premiumStatus.setText(statusText);
        premiumStatus.setTextColor(0xFF4CAF50);
        premiumStatus.setTextSize(12);
        premiumStatus.setGravity(Gravity.CENTER);
        premiumStatus.setPadding(8, 8, 8, 8);
        premiumStatus.setBackgroundColor(0x20FFFFFF);

        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 0, 0, 16);
        premiumStatus.setLayoutParams(statusParams);

        // How it works info
        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(16, 12, 16, 12);
        infoCard.setBackgroundColor(0x304CAF50);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 16);
        infoCard.setLayoutParams(infoParams);

        TextView infoText = new TextView(this);
        infoText.setText("🤝 How it works:\n1. Create your custom dare\n2. Set your proposed points\n3. Your partner can accept, haggle, or reject\n4. Once agreed, it becomes a regular dare!");
        infoText.setTextColor(0xFFFFFFFF);
        infoText.setTextSize(12);
        infoText.setGravity(Gravity.CENTER);

        infoCard.addView(infoText);

        // Disclaimer
        LinearLayout disclaimerCard = new LinearLayout(this);
        disclaimerCard.setOrientation(LinearLayout.VERTICAL);
        disclaimerCard.setPadding(16, 12, 16, 12);
        disclaimerCard.setBackgroundColor(0x30FF6B6B);

        LinearLayout.LayoutParams disclaimerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        disclaimerParams.setMargins(0, 0, 0, 24);
        disclaimerCard.setLayoutParams(disclaimerParams);

        TextView disclaimerText = new TextView(this);
        disclaimerText.setText("⚠️ DISCLAIMER: Custom dares are encrypted and private. DareUs is not responsible for user-generated content. Keep it consensual and legal.");
        disclaimerText.setTextColor(0xFFFFFFFF);
        disclaimerText.setTextSize(10);
        disclaimerText.setGravity(Gravity.CENTER);

        disclaimerCard.addView(disclaimerText);

        // Custom dare input card
        LinearLayout dareCard = new LinearLayout(this);
        dareCard.setOrientation(LinearLayout.VERTICAL);
        dareCard.setPadding(24, 20, 24, 20);
        dareCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        dareCard.setLayoutParams(cardParams);

        TextView dareLabel = new TextView(this);
        dareLabel.setText("📝 Your Custom Dare");
        dareLabel.setTextColor(0xFFFFFFFF);
        dareLabel.setTextSize(16);
        dareLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        dareLabel.setPadding(0, 0, 0, 8);

        editTextCustomDare = new EditText(this);
        editTextCustomDare.setHint("Describe your custom dare... (be creative!) 🎨");
        editTextCustomDare.setTextColor(0xFFFFFFFF);
        editTextCustomDare.setHintTextColor(0xFF9575CD);
        editTextCustomDare.setBackgroundResource(R.drawable.premium_input_field);
        editTextCustomDare.setPadding(16, 12, 16, 12);
        editTextCustomDare.setMinLines(3);
        editTextCustomDare.setMaxLines(5);

        TextView pointsLabel = new TextView(this);
        pointsLabel.setText("💎 Proposed Points (your partner can negotiate!)");
        pointsLabel.setTextColor(0xFFE8BBE8);
        pointsLabel.setTextSize(14);
        pointsLabel.setPadding(0, 16, 0, 8);

        editTextProposedPoints = new EditText(this);
        editTextProposedPoints.setHint("e.g. 10");
        editTextProposedPoints.setTextColor(0xFFFFFFFF);
        editTextProposedPoints.setHintTextColor(0xFF9575CD);
        editTextProposedPoints.setBackgroundResource(R.drawable.premium_input_field);
        editTextProposedPoints.setPadding(16, 12, 16, 12);
        editTextProposedPoints.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        buttonSendCustomDare = new Button(this);
        buttonSendCustomDare.setText("🚀 Send for Negotiation");
        buttonSendCustomDare.setBackgroundResource(R.drawable.premium_button_gradient);
        buttonSendCustomDare.setTextColor(0xFFFFFFFF);
        buttonSendCustomDare.setTextSize(16);
        buttonSendCustomDare.setPadding(24, 16, 24, 16);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(0, 20, 0, 0);
        buttonSendCustomDare.setLayoutParams(buttonParams);

        dareCard.addView(dareLabel);
        dareCard.addView(editTextCustomDare);
        dareCard.addView(pointsLabel);
        dareCard.addView(editTextProposedPoints);
        dareCard.addView(buttonSendCustomDare);

        // Back button
        buttonBackToDashboard = new Button(this);
        buttonBackToDashboard.setText("← Back to Custom Dares Hub");
        buttonBackToDashboard.setTextColor(0xFFBB86FC);
        buttonBackToDashboard.setTextSize(14);
        buttonBackToDashboard.setBackgroundColor(0x00000000);
        buttonBackToDashboard.setPadding(0, 24, 0, 0);

        // Set click listeners
        buttonSendCustomDare.setOnClickListener(v -> sendCustomDare());
        buttonBackToDashboard.setOnClickListener(v -> finish());

        mainLayout.addView(headerText);
        mainLayout.addView(subHeader);
        mainLayout.addView(premiumStatus);
        mainLayout.addView(infoCard);
        mainLayout.addView(disclaimerCard);
        mainLayout.addView(dareCard);
        mainLayout.addView(buttonBackToDashboard);

        setContentView(mainLayout);
    }

    private void loadPartnerInfo() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentPartnerId = doc.getString("partnerId");

                        if (currentPartnerId == null || currentPartnerId.isEmpty()) {
                            showCustomToast("You need a partner to send custom dares! 💔");
                            finish();
                            return;
                        }

                        // Get partner name
                        db.collection("dareus").document(currentPartnerId)
                                .get()
                                .addOnSuccessListener(partnerDoc -> {
                                    if (partnerDoc.exists()) {
                                        partnerName = partnerDoc.getString("firstName");
                                    } else {
                                        partnerName = "Your Partner";
                                    }
                                });
                    }
                });
    }

    private void sendCustomDare() {
        // Double-check premium access at send time
        if (!premiumManager.hasCustomDares()) {
            showCustomToast("Premium Plus required for custom dares! 🚀");
            finish();
            return;
        }

        String dareText = editTextCustomDare.getText().toString().trim();
        String pointsStr = editTextProposedPoints.getText().toString().trim();

        if (TextUtils.isEmpty(dareText)) {
            if (editTextCustomDare != null) {
                editTextCustomDare.setError("Please enter a dare");
            }
            return;
        }
        if (containsInappropriateContent(dareText)) {
            if (editTextCustomDare != null) {
                editTextCustomDare.setError("Content not allowed - please keep it appropriate and consensual!");
            }
            return;
        }


        if (TextUtils.isEmpty(pointsStr)) {
            if (editTextProposedPoints != null) {
                editTextProposedPoints.setError("Please enter proposed points");
            }
            return;
        }

        int proposedPoints;
        try {
            proposedPoints = Integer.parseInt(pointsStr);
            if (proposedPoints < 1 || proposedPoints > 100) {
                if (editTextProposedPoints != null) {
                    editTextProposedPoints.setError("Points must be between 1 and 100");
                }
                return;
            }
        } catch (NumberFormatException e) {
            if (editTextProposedPoints != null) {
                editTextProposedPoints.setError("Please enter a valid number");
            }
            return;
        }

        if (buttonSendCustomDare != null) {
            buttonSendCustomDare.setText("Sending... 🚀");
            buttonSendCustomDare.setEnabled(false);
        }

        // Create custom dare document for NEGOTIATION
        Map<String, Object> customDareData = new HashMap<>();
        customDareData.put("dareText", dareText);
        customDareData.put("category", "Custom");
        customDareData.put("proposedPoints", proposedPoints);
        customDareData.put("negotiatedPoints", proposedPoints);
        customDareData.put("fromUserId", currentUser.getUid());
        customDareData.put("toUserId", currentPartnerId);
        customDareData.put("originalTargetUserId", currentPartnerId);
        customDareData.put("status", "pending_negotiation");
        customDareData.put("sentAt", System.currentTimeMillis());
        customDareData.put("isCustom", true);
        customDareData.put("encrypted", true);
        customDareData.put("negotiationCount", 0);

        db.collection("dares")
                .add(customDareData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("CustomDare", "Dare created successfully, about to send notification");
                    
                    // Send notification to partner
                    sendCustomDareNotification("💌 New Custom Dare!", 
                            "You have a new custom dare to review from your partner!");
                    
                    showCustomToast("🎉 Custom dare sent for negotiation!\n" +
                            partnerName + " will review and respond! 🤝");
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (buttonSendCustomDare != null) {
                        buttonSendCustomDare.setText("🚀 Send for Negotiation");
                        buttonSendCustomDare.setEnabled(true);
                    }
                    showCustomToast("Failed to send custom dare - please try again! 😅");
                });
    }
    
    private void sendCustomDareNotification(String title, String message) {
        if (currentUser == null || currentPartnerId == null) {
            Log.e("CustomDare", "Cannot send notification - missing user or partner ID");
            return;
        }

        Log.d("CustomDare", "Sending notification to partner: " + currentPartnerId);

        // Get partner's FCM token and send notification
        db.collection("dareus").document(currentPartnerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String partnerToken = doc.getString("fcmToken");
                        Log.d("CustomDare", "Partner token: " + (partnerToken != null ? "found" : "null"));
                        
                        if (partnerToken != null) {
                            Map<String, Object> notif = new HashMap<>();
                            notif.put("toToken", partnerToken);
                            notif.put("title", title);
                            notif.put("body", message);
                            notif.put("timestamp", System.currentTimeMillis());
                            notif.put("sent", false);

                            db.collection("notifications").add(notif)
                                    .addOnSuccessListener(docRef -> {
                                        Log.d("CustomDare", "Notification added to Firestore: " + docRef.getId());
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CustomDare", "Failed to add notification", e);
                                    });
                        } else {
                            Log.e("CustomDare", "Partner FCM token is null");
                        }
                    } else {
                        Log.e("CustomDare", "Partner document doesn't exist");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CustomDare", "Failed to get partner document", e);
                });
    }
    
    private boolean containsInappropriateContent(String text) {
        String lowerText = text.toLowerCase();

        // Words that violate Google policies
        String[] bannedWords = {
                "kill", "die", "death", "suicide", "murder", "hurt", "harm", "abuse",
                "child", "kid", "minor", "baby", "infant", "teen", "young",
                "racist", "nazi", "hitler", "genocide", "ethnic", "racial",
                "illegal", "drug", "cocaine", "heroin", "meth",
                "violence", "weapon", "gun", "knife", "bomb",
                "hate", "discrimination", "torture", "rape", "assault"
        };

        for (String word : bannedWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }

        return false;
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
        customToast.setDuration(Toast.LENGTH_LONG);
        customToast.setView(toastLayout);
        customToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
        customToast.show();
    }
}