package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class CustomDareNegotiationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private String partnerId, partnerName;

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

        setupNegotiationUI();
        loadPartnerInfo();
    }

    private void setupNegotiationUI() {
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);
        scrollView.setScrollBarStyle(ScrollView.SCROLLBARS_INSIDE_OVERLAY);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setPadding(24, 32, 24, 32);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("🤝 Custom Dare Negotiations");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(26);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFB794F6);

        TextView subHeader = new TextView(this);
        subHeader.setText("Negotiate points and approve custom dares! ✨");
        subHeader.setTextColor(0xFFD6BCFA);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 32);

        contentLayout.addView(headerText);
        contentLayout.addView(subHeader);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFB794F6);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 24);
        backButton.setOnClickListener(v -> finish());

        contentLayout.addView(backButton);

        scrollView.addView(contentLayout);
        setContentView(scrollView);
    }

    private void loadPartnerInfo() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        partnerId = doc.getString("partnerId");

                        if (partnerId != null && !partnerId.isEmpty()) {
                            db.collection("dareus").document(partnerId)
                                    .get()
                                    .addOnSuccessListener(partnerDoc -> {
                                        if (partnerDoc.exists()) {
                                            partnerName = partnerDoc.getString("firstName");
                                        } else {
                                            partnerName = "Your Partner";
                                        }
                                        loadNegotiations();
                                    });
                        } else {
                            showNoPartnerMessage();
                        }
                    }
                });
    }

    private void loadNegotiations() {
        // Load dares I need to respond to (sent TO me)
        db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending_negotiation")
                .get()
                .addOnSuccessListener(incomingSnapshot -> {
                    // Load dares waiting for partner's response (sent FROM me)
                    db.collection("dares")
                            .whereEqualTo("fromUserId", currentUser.getUid())
                            .whereEqualTo("status", "pending_negotiation")
                            .get()
                            .addOnSuccessListener(outgoingSnapshot -> {

                                boolean hasIncoming = !incomingSnapshot.isEmpty();
                                boolean hasOutgoing = !outgoingSnapshot.isEmpty();

                                if (!hasIncoming && !hasOutgoing) {
                                    showNoNegotiationsMessage();
                                    return;
                                }

                                // Show incoming negotiations (I need to respond)
                                if (hasIncoming) {
                                    createSectionHeader("📥 Dares for You to Review (" + incomingSnapshot.size() + ")",
                                            "These custom dares need your approval!");

                                    for (QueryDocumentSnapshot doc : incomingSnapshot) {
                                        createIncomingNegotiationCard(doc);
                                    }
                                }

                                // Show outgoing negotiations (waiting for partner)
                                if (hasOutgoing) {
                                    createSectionHeader("⏳ Waiting for " + partnerName + " (" + outgoingSnapshot.size() + ")",
                                            "Custom dares you sent that are being reviewed");

                                    for (QueryDocumentSnapshot doc : outgoingSnapshot) {
                                        createOutgoingNegotiationCard(doc);
                                    }
                                }
                            });
                });
    }

    private void createSectionHeader(String title, String subtitle) {
        LinearLayout headerCard = new LinearLayout(this);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setPadding(20, 16, 20, 16);
        headerCard.setBackgroundColor(0x30FFFFFF);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 8);
        headerCard.setLayoutParams(params);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);

        TextView subtitleText = new TextView(this);
        subtitleText.setText(subtitle);
        subtitleText.setTextColor(0xFFB794F6);
        subtitleText.setTextSize(12);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setPadding(0, 4, 0, 0);

        headerCard.addView(titleText);
        headerCard.addView(subtitleText);
        contentLayout.addView(headerCard);
    }

    private void createIncomingNegotiationCard(QueryDocumentSnapshot document) {
        String dareText = document.getString("dareText");
        Long proposedPoints = document.getLong("proposedPoints");
        String dareId = document.getId();
        Long negotiationCount = document.getLong("negotiationCount");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        TextView headerText = new TextView(this);
        String headerMsg = "💌 New Custom Dare from " + partnerName;
        if (negotiationCount != null && negotiationCount > 0) {
            headerMsg += " (Round " + (negotiationCount + 1) + ")";
        }
        headerText.setText(headerMsg);
        headerText.setTextColor(0xFFFF6B9D);
        headerText.setTextSize(14);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);

        TextView dareTextView = new TextView(this);
        dareTextView.setText("\"" + dareText + "\"");
        dareTextView.setTextColor(0xFFFFFFFF);
        dareTextView.setTextSize(16);
        dareTextView.setGravity(Gravity.CENTER);
        dareTextView.setPadding(8, 8, 8, 16);
        dareTextView.setBackgroundResource(R.drawable.glass_card);

        // Points info
        TextView pointsText = new TextView(this);
        String pointsMsg = "💎 Current Offer: " + proposedPoints + " points";
        if (negotiationCount != null && negotiationCount > 0) {
            pointsMsg += "\n🔄 Counter-offer from " + partnerName;
        }
        pointsText.setText(pointsMsg);
        pointsText.setTextColor(0xFFFFFFFF);
        pointsText.setTextSize(14);
        pointsText.setTypeface(null, android.graphics.Typeface.BOLD);
        pointsText.setGravity(Gravity.CENTER);
        pointsText.setPadding(0, 8, 0, 16);

        // Buttons layout
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER);

        Button acceptButton = new Button(this);
        acceptButton.setText("✅ Accept");
        acceptButton.setBackgroundResource(R.drawable.accept_button_gradient);
        acceptButton.setTextColor(0xFFFFFFFF);
        acceptButton.setTextSize(13);
        acceptButton.setPadding(16, 8, 16, 8);

        Button haggleButton = new Button(this);
        haggleButton.setText("💰 Haggle");
        haggleButton.setBackgroundResource(R.drawable.haggle_button_gradient);
        haggleButton.setTextColor(0xFFFFFFFF);
        haggleButton.setTextSize(13);
        haggleButton.setPadding(16, 8, 16, 8);

        Button rejectButton = new Button(this);
        rejectButton.setText("❌ Reject");
        rejectButton.setBackgroundResource(R.drawable.reject_button_gradient);
        rejectButton.setTextColor(0xFFFFFFFF);
        rejectButton.setTextSize(13);
        rejectButton.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        buttonParams.setMargins(4, 0, 4, 0);
        acceptButton.setLayoutParams(buttonParams);
        haggleButton.setLayoutParams(buttonParams);
        rejectButton.setLayoutParams(buttonParams);

        acceptButton.setOnClickListener(v -> acceptCustomDare(dareId, proposedPoints.intValue()));
        haggleButton.setOnClickListener(v -> showHaggleDialog(dareId, proposedPoints.intValue()));
        rejectButton.setOnClickListener(v -> rejectCustomDare(dareId));

        buttonsLayout.addView(acceptButton);
        buttonsLayout.addView(haggleButton);
        buttonsLayout.addView(rejectButton);

        card.addView(headerText);
        card.addView(dareTextView);
        card.addView(pointsText);
        card.addView(buttonsLayout);

        contentLayout.addView(card);
    }

    private void createOutgoingNegotiationCard(QueryDocumentSnapshot document) {
        String dareText = document.getString("dareText");
        Long proposedPoints = document.getLong("proposedPoints");
        String dareId = document.getId();
        Long negotiationCount = document.getLong("negotiationCount");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        TextView headerText = new TextView(this);
        String headerMsg = "⏳ Awaiting " + partnerName + "'s Response";
        if (negotiationCount != null && negotiationCount > 0) {
            headerMsg += " (Round " + (negotiationCount + 1) + ")";
        }
        headerText.setText(headerMsg);
        headerText.setTextColor(0xFFB794F6);
        headerText.setTextSize(14);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);

        TextView dareTextView = new TextView(this);
        dareTextView.setText("\"" + dareText + "\"");
        dareTextView.setTextColor(0xFFFFFFFF);
        dareTextView.setTextSize(15);
        dareTextView.setGravity(Gravity.CENTER);
        dareTextView.setPadding(8, 8, 8, 8);
        dareTextView.setBackgroundResource(R.drawable.glass_card);

        TextView pointsText = new TextView(this);
        pointsText.setText("💎 Current Offer: " + proposedPoints + " points");
        pointsText.setTextColor(0xFFFFFFFF);
        pointsText.setTextSize(14);
        pointsText.setTypeface(null, android.graphics.Typeface.BOLD);
        pointsText.setGravity(Gravity.CENTER);
        pointsText.setPadding(0, 8, 0, 0);

        card.addView(headerText);
        card.addView(dareTextView);
        card.addView(pointsText);

        contentLayout.addView(card);
    }
    private void sendCustomDareNotification(String title, String message) {
        if (currentUser == null || partnerId == null) return;

        // Get partner's FCM token and send notification
        db.collection("dareus").document(partnerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String partnerToken = doc.getString("fcmToken");
                        if (partnerToken != null) {
                            Map<String, Object> notif = new HashMap<>();
                            notif.put("toToken", partnerToken);
                            notif.put("title", title);
                            notif.put("body", message);
                            notif.put("timestamp", System.currentTimeMillis());
                            notif.put("sent", false);

                            db.collection("notifications").add(notif);
                        }
                    }
                });
    }
    private void acceptCustomDare(String dareId, int points) {
        db.collection("dares").document(dareId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String originalTargetUserId = document.getString("originalTargetUserId");
                        String originalCreatorUserId;

                        if (originalTargetUserId == null) {
                            String currentFromUserId = document.getString("fromUserId");
                            String currentToUserId = document.getString("toUserId");

                            if (currentUser.getUid().equals(currentToUserId)) {
                                originalTargetUserId = currentFromUserId;
                                originalCreatorUserId = currentToUserId;
                            } else {
                                originalTargetUserId = currentToUserId;
                                originalCreatorUserId = currentFromUserId;
                            }
                        } else {
                            String currentFromUserId = document.getString("fromUserId");
                            String currentToUserId = document.getString("toUserId");
                            originalCreatorUserId = originalTargetUserId.equals(currentFromUserId) ? currentToUserId : currentFromUserId;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "pending");
                        updates.put("points", points);
                        updates.put("negotiatedPoints", points);
                        updates.put("expiresAt", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L));
                        updates.put("acceptedAt", System.currentTimeMillis());

                        updates.put("fromUserId", originalCreatorUserId);
                        updates.put("toUserId", originalTargetUserId);

                        db.collection("dares").document(dareId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    showStyledToast("🎉 Custom dare accepted! It will appear in the correct person's received dares!");
                                    sendCustomDareNotification("✅ Custom Dare Accepted!",
                                            "Your partner accepted your custom dare for " + points + " points!");
                                    recreate();
                                })
                                .addOnFailureListener(e -> showStyledToast("Error accepting dare - try again! 😅"));
                    }
                });
    }

    private void showHaggleDialog(String dareId, int currentPoints) {
        EditText pointsInput = new EditText(this);
        pointsInput.setHint("Enter your counter-offer");
        pointsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        pointsInput.setTextColor(0xFFFFFFFF); // White text for input
        pointsInput.setHintTextColor(0xFF9F7AEA);
        pointsInput.setText(String.valueOf(currentPoints));
        pointsInput.setPadding(16, 16, 16, 16);
        pointsInput.setBackgroundResource(R.drawable.premium_input_field);

        pointsInput.selectAll();

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("💰 Negotiate Points")
                .setMessage("Current offer: " + currentPoints + " points\n\nWhat would you like to counter-offer?\n(You can negotiate up or down)")
                .setView(pointsInput)
                .setPositiveButton("Send Counter-Offer", (dialogInterface, which) -> {
                    String newPointsStr = pointsInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(newPointsStr)) {
                        try {
                            int newPoints = Integer.parseInt(newPointsStr);
                            if (newPoints >= 1 && newPoints <= 100) {
                                if (newPoints == currentPoints) {
                                    showStyledToast("That's the same amount! Try a different value 😄");
                                } else {
                                    sendCounterOffer(dareId, newPoints);
                                }
                            } else {
                                showStyledToast("Points must be between 1 and 100! 📏");
                            }
                        } catch (NumberFormatException e) {
                            showStyledToast("Please enter a valid number! 🔢");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void sendCounterOffer(String dareId, int newPoints) {
        db.collection("dares").document(dareId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String originalFromUserId = document.getString("fromUserId");
                        String originalToUserId = document.getString("toUserId");

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("proposedPoints", newPoints);
                        updates.put("negotiatedPoints", newPoints);
                        updates.put("lastNegotiatedBy", currentUser.getUid());
                        updates.put("negotiationCount", FieldValue.increment(1));
                        updates.put("lastCounterOfferAt", System.currentTimeMillis());

                        updates.put("fromUserId", originalToUserId);
                        updates.put("toUserId", originalFromUserId);

                        db.collection("dares").document(dareId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    showStyledToast("💰 Counter-offer sent! " + partnerName + " will see your new request!");
                                    sendCustomDareNotification("💰 Custom Dare Counter-Offer!",
                                            "Your partner wants " + newPoints + " points for your custom dare!");
                                    recreate();
                                })
                                .addOnFailureListener(e -> {
                                    showStyledToast("Error sending counter-offer - try again! 😅");
                                });
                    }
                });
    }

    private void rejectCustomDare(String dareId) {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("❌ Reject Custom Dare?")
                .setMessage("Are you sure you want to reject this custom dare?\n\nThis action cannot be undone.")
                .setPositiveButton("Yes, Reject", (dialogInterface, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "rejected");
                    updates.put("rejectedAt", System.currentTimeMillis());
                    updates.put("rejectedBy", currentUser.getUid());

                    db.collection("dares").document(dareId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                showStyledToast("❌ Custom dare rejected!");
                                sendCustomDareNotification("❌ Custom Dare Rejected",
                                        "Your partner rejected your custom dare");
                                recreate();
                            })
                            .addOnFailureListener(e -> showStyledToast("Error rejecting dare - try again! 😅"));
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void showNoNegotiationsMessage() {
        LinearLayout noNegCard = new LinearLayout(this);
        noNegCard.setOrientation(LinearLayout.VERTICAL);
        noNegCard.setPadding(24, 32, 24, 32);
        noNegCard.setBackgroundResource(R.drawable.glass_card);
        noNegCard.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        noNegCard.setLayoutParams(params);

        TextView noNegText = new TextView(this);
        noNegText.setText("🤝 No negotiations yet!");
        noNegText.setTextColor(0xFFFFFFFF);
        noNegText.setTextSize(18);
        noNegText.setTypeface(null, android.graphics.Typeface.BOLD);
        noNegText.setGravity(Gravity.CENTER);

        TextView noNegDesc = new TextView(this);
        noNegDesc.setText("Create custom dares to start negotiating! ✨");
        noNegDesc.setTextColor(0xFFD6BCFA);
        noNegDesc.setTextSize(14);
        noNegDesc.setGravity(Gravity.CENTER);
        noNegDesc.setPadding(0, 8, 0, 0);

        noNegCard.addView(noNegText);
        noNegCard.addView(noNegDesc);
        contentLayout.addView(noNegCard);
    }

    private void showNoPartnerMessage() {
        LinearLayout noPartnerCard = new LinearLayout(this);
        noPartnerCard.setOrientation(LinearLayout.VERTICAL);
        noPartnerCard.setPadding(24, 32, 24, 32);
        noPartnerCard.setBackgroundResource(R.drawable.glass_card);
        noPartnerCard.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        noPartnerCard.setLayoutParams(params);

        TextView noPartnerText = new TextView(this);
        noPartnerText.setText("💔 No partner linked");
        noPartnerText.setTextColor(0xFFFFFFFF);
        noPartnerText.setTextSize(18);
        noPartnerText.setTypeface(null, android.graphics.Typeface.BOLD);
        noPartnerText.setGravity(Gravity.CENTER);

        TextView noPartnerDesc = new TextView(this);
        noPartnerDesc.setText("Connect with your partner to negotiate custom dares!");
        noPartnerDesc.setTextColor(0xFFD6BCFA);
        noPartnerDesc.setTextSize(14);
        noPartnerDesc.setGravity(Gravity.CENTER);
        noPartnerDesc.setPadding(0, 8, 0, 0);

        noPartnerCard.addView(noPartnerText);
        noPartnerCard.addView(noPartnerDesc);
        contentLayout.addView(noPartnerCard);
    }

    private void showStyledToast(String message) {
        LinearLayout toastLayout = new LinearLayout(this);
        toastLayout.setOrientation(LinearLayout.HORIZONTAL);
        toastLayout.setBackgroundColor(0xFF2D2E37);
        toastLayout.setPadding(24, 16, 24, 16);
        toastLayout.setGravity(Gravity.CENTER);

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(16);
        shape.setColor(0xFF2D2E37);
        shape.setStroke(2, 0xFFB794F6);
        toastLayout.setBackground(shape);

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