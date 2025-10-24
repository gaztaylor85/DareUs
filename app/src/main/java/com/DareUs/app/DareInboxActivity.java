package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import androidx.appcompat.app.AlertDialog;
import android.view.View;

public class DareInboxActivity extends AppCompatActivity implements BadgeTracker.BadgeUnlockListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private BadgeTracker badgeTracker;
    private String currentPartnerId; // 🎯 ADD THIS LINE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        // Initialize badge tracker
        badgeTracker = new BadgeTracker(currentUser.getUid(), this);

// Load partner ID first
        loadPartnerInfo();

        setupInboxUI();
        loadReceivedDares();
    }

    @Override
    public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
        // Show EPIC badge celebration dialog!
        BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(this, badge);
        celebrationDialog.show();
    }
    private void loadPartnerInfo() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentPartnerId = doc.getString("partnerId");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DareInbox", "Error loading partner info", e);
                });
    }

    private void createEmptyPartnerSection() {
        LinearLayout emptyCard = new LinearLayout(this);
        emptyCard.setOrientation(LinearLayout.VERTICAL);
        emptyCard.setPadding(24, 20, 24, 20);
        emptyCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        emptyCard.setLayoutParams(params);

        TextView emptyText = new TextView(this);
        emptyText.setText("📭 No Recent Partner Activity");
        emptyText.setTextColor(0xFFFFFFFF);
        emptyText.setTextSize(16);
        emptyText.setTypeface(null, android.graphics.Typeface.BOLD);
        emptyText.setGravity(Gravity.CENTER);

        TextView emptyDesc = new TextView(this);
        emptyDesc.setText("Your partner hasn't completed any of your dares recently.\n\nSend them some challenges!");
        emptyDesc.setTextColor(0xFFE8BBE8);
        emptyDesc.setTextSize(14);
        emptyDesc.setGravity(Gravity.CENTER);
        emptyDesc.setPadding(0, 8, 0, 0);

        emptyCard.addView(emptyText);
        emptyCard.addView(emptyDesc);
        contentLayout.addView(emptyCard);
    }

    private void setupInboxUI() {
        // Create main ScrollView with proper parameters
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        // Create content layout inside ScrollView
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setPadding(24, 32, 24, 32);

        // FIXED: Back button at the top
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000); // Transparent background
        backButton.setPadding(0, 0, 0, 16);
        backButton.setOnClickListener(v -> finish());

        LinearLayout.LayoutParams backButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backButtonParams.gravity = Gravity.START;
        backButton.setLayoutParams(backButtonParams);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("🎯 Your Dares");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("Complete dares to earn points and unlock badges!");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 24);

        // Add views to content layout
        contentLayout.addView(backButton);
        contentLayout.addView(headerText);
        contentLayout.addView(subHeader);

        // Add content layout to scroll view and set as main view
        scrollView.addView(contentLayout);
        setContentView(scrollView);
    }

    private void loadReceivedDares() {
        try {
            clearContent();

            db.collection("dares")
                    .whereEqualTo("toUserId", currentUser.getUid())
                    .whereIn("status", Arrays.asList("pending", "completed", "rejected"))
                    .get()  // ✅ Remove expiration filter here to get ALL dares
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        try {
                            java.util.List<QueryDocumentSnapshot> currentDares = new java.util.ArrayList<>();
                            java.util.List<QueryDocumentSnapshot> completedDares = new java.util.ArrayList<>();
                            java.util.List<QueryDocumentSnapshot> uncompletedDares = new java.util.ArrayList<>();

                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                String status = document.getString("status");
                                Long expiresAt = document.getLong("expiresAt");
                                boolean isExpired = expiresAt != null && expiresAt <= System.currentTimeMillis();

                                if ("completed".equals(status)) {
                                    completedDares.add(document);
                                } else if ("pending".equals(status) && !isExpired) {
                                    // ✅ Only show non-expired pending dares in current
                                    currentDares.add(document);
                                } else if ("rejected".equals(status) || ("pending".equals(status) && isExpired)) {
                                    // ✅ Show both rejected AND expired dares in uncompleted
                                    uncompletedDares.add(document);
                                }
                            }

                            // Sort and display as before...
                            completedDares.sort((a, b) -> {
                                try {
                                    Long timeA = a.getLong("completedAt");
                                    Long timeB = b.getLong("completedAt");
                                    if (timeA == null) timeA = a.getLong("sentAt");
                                    if (timeB == null) timeB = b.getLong("sentAt");
                                    if (timeA == null) timeA = 0L;
                                    if (timeB == null) timeB = 0L;
                                    return timeB.compareTo(timeA);
                                } catch (Exception e) {
                                    return 0;
                                }
                            });

                            uncompletedDares.sort((a, b) -> {
                                try {
                                    Long timeA = a.getLong("rejectedAt");
                                    Long timeB = b.getLong("rejectedAt");
                                    if (timeA == null) timeA = a.getLong("expiresAt"); // ✅ Sort expired by expiration time
                                    if (timeB == null) timeB = b.getLong("expiresAt");
                                    if (timeA == null) timeA = a.getLong("sentAt");
                                    if (timeB == null) timeB = b.getLong("sentAt");
                                    if (timeA == null) timeA = 0L;
                                    if (timeB == null) timeB = 0L;
                                    return timeB.compareTo(timeA);
                                } catch (Exception e) {
                                    return 0;
                                }
                            });

                            java.util.List<QueryDocumentSnapshot> limitedCompleted = completedDares.size() > 20 ?
                                    completedDares.subList(0, 20) : completedDares;
                            java.util.List<QueryDocumentSnapshot> limitedUncompleted = uncompletedDares.size() > 20 ?
                                    uncompletedDares.subList(0, 20) : uncompletedDares;

                            displayDareSections(currentDares, limitedCompleted, limitedUncompleted);

                        } catch (Exception e) {
                            Log.e("DareInbox", "Error processing dares list", e);
                            showCustomToast("Error processing dares - please try again! 😅");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DareInbox", "Error loading dares", e);
                        showCustomToast("Error loading dares - please try again! 😅");
                    });

        } catch (Exception e) {
            Log.e("DareInbox", "Error in loadReceivedDares", e);
            showCustomToast("Error loading dares - please try again! 😅");
        }
    }

    private void clearContent() {
        // Keep only the back button, header, and subheader (first 3 children), remove everything else
        int childCount = contentLayout.getChildCount();
        if (childCount > 3) {
            contentLayout.removeViews(3, childCount - 3);
        }
    }

    private void displayDareSections(java.util.List<QueryDocumentSnapshot> currentDares,
                                     java.util.List<QueryDocumentSnapshot> completedDares,
                                     java.util.List<QueryDocumentSnapshot> uncompletedDares) {
        // Display current dares section
        if (!currentDares.isEmpty()) {
            createSectionHeader("🔥 Active Dares", "Complete or reject these dares!");
            for (QueryDocumentSnapshot document : currentDares) {
                try {
                    createDareCard(document, false);
                } catch (Exception e) {
                    Log.e("DareInbox", "Error creating dare card for: " + document.getId(), e);
                }
            }
        } else {
            createEmptyActiveCard();
        }

        // FIXED: Display completed dares section BEFORE uncompleted dares
        if (!completedDares.isEmpty()) {
            createSectionHeader("✅ Completed Dares", "Great job! You've conquered these challenges!");
            for (QueryDocumentSnapshot document : completedDares) {
                try {
                    createDareCard(document, true);
                } catch (Exception e) {
                    Log.e("DareInbox", "Error creating completed dare card for: " + document.getId(), e);
                }
            }
        }

        // Display uncompleted dares section AFTER completed dares
        if (!uncompletedDares.isEmpty()) {
            createSectionHeader("❌ Missed & Rejected Dares", "These dares expired or were rejected");
            for (QueryDocumentSnapshot document : uncompletedDares) {
                try {
                    createUncompletedDareCard(document);
                } catch (Exception e) {
                    Log.e("DareInbox", "Error creating uncompleted dare card for: " + document.getId(), e);
                }
            }
        }
        // Show partner's completed dares
        displayPartnerCompletedDares(); // 🎯 ADD THIS LINE
        // Handle completely empty state
        if (currentDares.isEmpty() && completedDares.isEmpty() && uncompletedDares.isEmpty()) {
            showNoDaresMessage();
        }
    }

    private void createSectionHeader(String title, String subtitle) {
        LinearLayout headerCard = new LinearLayout(this);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setPadding(20, 16, 20, 16);
        headerCard.setBackgroundResource(R.drawable.section_header_bg); // FIXED: Curved edges

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
        subtitleText.setTextColor(0xFFBB86FC);
        subtitleText.setTextSize(12);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setPadding(0, 4, 0, 0);

        headerCard.addView(titleText);
        headerCard.addView(subtitleText);
        contentLayout.addView(headerCard);
    }


    private void createDareCard(QueryDocumentSnapshot document, boolean isCompleted) {
        try {
            String dareText = document.getString("dareText");
            String category = document.getString("category");
            String status = document.getString("status");
            Long points = document.getLong("points");
            Long expiresAt = document.getLong("expiresAt");
            Long sentAt = document.getLong("sentAt");
            String dareId = document.getId();
            Boolean isCustom = document.getBoolean("isCustom");

            // Safety checks
            if (dareText == null) dareText = "Mystery dare";
            if (category == null) category = "Unknown";
            if (points == null) points = 5L;
            if (sentAt == null) sentAt = System.currentTimeMillis();
            if (expiresAt == null) expiresAt = sentAt + (7 * 24 * 60 * 60 * 1000L);

            // FIXED: Make variables final for lambda use
            final String finalCategory = category;
            final long finalSentAt = sentAt;
            final int basePoints = points.intValue();
            final String finalDareId = dareId;
            final String finalDareText = dareText;
            final int bonusPoints;
            final int totalPoints;

            if (!isCompleted) {
                long timeElapsed = System.currentTimeMillis() - finalSentAt;
                int daysElapsed = (int) TimeUnit.MILLISECONDS.toDays(timeElapsed) + 1;

                // Clean bonus system - flat point bonuses!
                switch (daysElapsed) {
                    case 1: bonusPoints = basePoints; break; // Double points day 1!
                    case 2: bonusPoints = (int)(basePoints * 0.75); break; // 1.75x points day 2
                    case 3: bonusPoints = (int)(basePoints * 0.5); break; // 1.5x points day 3
                    case 4: bonusPoints = (int)(basePoints * 0.25); break; // 1.25x points day 4
                    default: bonusPoints = 0; break; // Base points after day 4
                }
                totalPoints = basePoints + bonusPoints;
            } else {
                bonusPoints = 0;
                totalPoints = basePoints;
            }

            // FIXED: Create dare card with consistent glass design
            LinearLayout dareCard = new LinearLayout(this);
            dareCard.setTag(dareId); // 🏷️ TAG FOR FINDING LATER
            dareCard.setOrientation(LinearLayout.VERTICAL);
            dareCard.setPadding(20, 16, 20, 16);
            dareCard.setBackgroundResource(R.drawable.glass_card);

            // FIXED: Use consistent opacity for both completed and active dares
            if (isCompleted) {
                android.graphics.drawable.Drawable background = getResources().getDrawable(R.drawable.glass_card).mutate();
                background.setTint(0x204CAF50);
                dareCard.setBackground(background);
            } else {
                android.graphics.drawable.Drawable background = getResources().getDrawable(R.drawable.glass_card).mutate();
                background.setTint(0x20B794F6);
                dareCard.setBackground(background);
            }

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 12);
            dareCard.setLayoutParams(cardParams);

            // Category and points
            TextView categoryText = new TextView(this);
            String pointsText = finalCategory;
            if (Boolean.TRUE.equals(isCustom)) {
                pointsText += " (Custom)";
            }

            if (!isCompleted) {
                if (bonusPoints > 0) {
                    pointsText += " • " + basePoints + "+" + bonusPoints + " = " + totalPoints + " points";
                } else {
                    pointsText += " • " + totalPoints + " points";
                }
            } else {
                pointsText += " • " + totalPoints + " points";
            }

            categoryText.setText(pointsText);
            categoryText.setTextColor(isCompleted ? 0xFF4CAF50 : 0xFFBB86FC);
            categoryText.setTextSize(11);
            categoryText.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryText.setGravity(Gravity.CENTER);

            // Dare text
            TextView dareTextView = new TextView(this);
            dareTextView.setText(dareText);
            dareTextView.setTextColor(isCompleted ? 0xFFBBBBBB : 0xFFFFFFFF);
            dareTextView.setTextSize(16);
            dareTextView.setGravity(Gravity.CENTER);
            dareTextView.setPadding(0, 8, 0, 8);

            // Status and time
            TextView statusText = new TextView(this);
            if (isCompleted) {
                Long completedAt = document.getLong("completedAt");
                if (completedAt != null) {
                    long daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - completedAt);
                    if (daysAgo == 0) {
                        statusText.setText("✅ Completed today!");
                    } else if (daysAgo == 1) {
                        statusText.setText("✅ Completed yesterday");
                    } else {
                        statusText.setText("✅ Completed " + daysAgo + " days ago");
                    }
                } else {
                    statusText.setText("✅ COMPLETED");
                }
                statusText.setTextColor(0xFF4CAF50);
            } else {
                long timeRemaining = expiresAt - System.currentTimeMillis();
                long days = TimeUnit.MILLISECONDS.toDays(timeRemaining);
                long hours = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24;

                long timeElapsed = System.currentTimeMillis() - finalSentAt;
                int daysElapsed = (int) TimeUnit.MILLISECONDS.toDays(timeElapsed) + 1;

                statusText.setText("⏰ Day " + daysElapsed + " • " + days + " days, " + hours + " hours left");
                statusText.setTextColor(0xFFE8BBE8);
            }
            statusText.setTextSize(12);
            statusText.setGravity(Gravity.CENTER);

            dareCard.addView(categoryText);
            dareCard.addView(dareTextView);
            dareCard.addView(statusText);

            // Add action buttons if not completed
            if (!isCompleted) {
                LinearLayout buttonLayout = new LinearLayout(this);
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                buttonLayout.setGravity(Gravity.CENTER);
                buttonLayout.setPadding(0, 12, 0, 0);

                Button completeButton = new Button(this);
                completeButton.setText("✅ Complete (" + totalPoints + " pts)");
                completeButton.setBackgroundResource(R.drawable.premium_button_gradient);
                completeButton.setTextColor(0xFFFFFFFF);
                completeButton.setTextSize(12);
                completeButton.setPadding(16, 8, 16, 8);

                Button rejectButton = new Button(this);
                rejectButton.setText("❌ Reject");
                rejectButton.setBackgroundResource(R.drawable.reject_button_bg);
                rejectButton.setTextColor(0xFFFFFFFF);
                rejectButton.setTextSize(12);
                rejectButton.setPadding(16, 8, 16, 8);

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                );
                buttonParams.setMargins(4, 0, 4, 0);
                completeButton.setLayoutParams(buttonParams);
                rejectButton.setLayoutParams(buttonParams);

                completeButton.setOnClickListener(v -> completeDare(finalDareId, totalPoints, finalCategory, finalSentAt));
                rejectButton.setOnClickListener(v -> showRejectConfirmation(finalDareId, finalDareText));

                buttonLayout.addView(completeButton);
                buttonLayout.addView(rejectButton);
                dareCard.addView(buttonLayout);
            }

            contentLayout.addView(dareCard);

        } catch (Exception e) {
            Log.e("DareInbox", "Error creating dare card", e);
        }
    }
    private void createEmptyActiveCard() {
        LinearLayout noActiveCard = new LinearLayout(this);
        noActiveCard.setOrientation(LinearLayout.VERTICAL);
        noActiveCard.setPadding(24, 20, 24, 20);
        noActiveCard.setBackgroundResource(R.drawable.glass_card); // FIXED: Curved edges

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        noActiveCard.setLayoutParams(params);

        TextView noActiveText = new TextView(this);
        noActiveText.setText("📭 No Active Dares");
        noActiveText.setTextColor(0xFFFFFFFF);
        noActiveText.setTextSize(16);
        noActiveText.setTypeface(null, android.graphics.Typeface.BOLD);
        noActiveText.setGravity(Gravity.CENTER);

        TextView noActiveDesc = new TextView(this);
        noActiveDesc.setText("No active dares - time for your partner to send more! 😉");
        noActiveDesc.setTextColor(0xFFE8BBE8);
        noActiveDesc.setTextSize(13);
        noActiveDesc.setGravity(Gravity.CENTER);
        noActiveDesc.setPadding(0, 6, 0, 0);

        noActiveCard.addView(noActiveText);
        noActiveCard.addView(noActiveDesc);
        contentLayout.addView(noActiveCard);
    }
    private void displayPartnerCompletedDares() {
        // Load partner ID first, then show button
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String partnerId = doc.getString("partnerId");
                        if (partnerId != null && !partnerId.isEmpty()) {
                            // Check for completed dares
                            checkForPartnerCompletedDares();
                        }
                    }
                });
    }
    private void createPartnerDaresButton() {
        LinearLayout buttonCard = new LinearLayout(this);
        buttonCard.setOrientation(LinearLayout.VERTICAL);
        buttonCard.setPadding(24, 20, 24, 20);
        buttonCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        buttonCard.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("💕 Partner Activity");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 8);

        Button viewButton = new Button(this);
        viewButton.setText("👀 View Partner's Completed Dares");
        viewButton.setBackgroundResource(R.drawable.premium_button_gradient);
        viewButton.setTextColor(0xFFFFFFFF);
        viewButton.setTextSize(14);
        viewButton.setPadding(24, 12, 24, 12);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER;
        buttonParams.setMargins(0, 8, 0, 0);
        viewButton.setLayoutParams(buttonParams);

        viewButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PartnerDaresActivity.class);
            startActivity(intent);
        });

        buttonCard.addView(title);
        buttonCard.addView(viewButton);
        contentLayout.addView(buttonCard);
    }

    private void checkForPartnerCompletedDares() {
        db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid()) // Dares I sent
                .whereEqualTo("status", "completed")
                .limit(1) // Just check if any exist
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        createPartnerDaresButton();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DareInbox", "Error checking partner completed dares", e);
                });
    }



    private void createUncompletedDareCard(QueryDocumentSnapshot document) {
        try {
            String dareText = document.getString("dareText");
            String category = document.getString("category");
            Long points = document.getLong("points");
            Long rejectedAt = document.getLong("rejectedAt");
            Boolean isCustom = document.getBoolean("isCustom");

            // Safety checks
            if (dareText == null) dareText = "Mystery dare";
            if (category == null) category = "Unknown";
            if (points == null) points = 5L;

            LinearLayout dareCard = new LinearLayout(this);
            dareCard.setTag(document.getId());
            dareCard.setOrientation(LinearLayout.VERTICAL);
            dareCard.setPadding(20, 16, 20, 16);
            dareCard.setBackgroundResource(R.drawable.glass_card); // FIXED: Use same glass card background

            // FIXED: Apply consistent opacity tint like other cards
            android.graphics.drawable.Drawable background = getResources().getDrawable(R.drawable.glass_card).mutate();
            background.setTint(0x20666666);
            dareCard.setBackground(background); // Same opacity pattern (0x20)

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 12);
            dareCard.setLayoutParams(cardParams);

            // Category
            TextView categoryText = new TextView(this);
            String categoryDisplay = category;
            if (Boolean.TRUE.equals(isCustom)) {
                categoryDisplay += " (Custom)";
            }
            categoryDisplay += " • " + points + " points (missed)";

            categoryText.setText(categoryDisplay);
            categoryText.setTextColor(0xFF999999);
            categoryText.setTextSize(11);
            categoryText.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryText.setGravity(Gravity.CENTER);

            // Dare text
            TextView dareTextView = new TextView(this);
            dareTextView.setText(dareText);
            dareTextView.setTextColor(0xFF999999);
            dareTextView.setTextSize(16);
            dareTextView.setGravity(Gravity.CENTER);
            dareTextView.setPadding(0, 8, 0, 8);

            // Status
            TextView statusText = new TextView(this);
            if (rejectedAt != null) {
                long daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - rejectedAt);
                if (daysAgo == 0) {
                    statusText.setText("❌ Rejected today");
                } else if (daysAgo == 1) {
                    statusText.setText("❌ Rejected yesterday");
                } else {
                    statusText.setText("❌ Rejected " + daysAgo + " days ago");
                }
            } else {
                statusText.setText("❌ Not completed");
            }
            statusText.setTextColor(0xFF999999);
            statusText.setTextSize(12);
            statusText.setGravity(Gravity.CENTER);

            dareCard.addView(categoryText);
            dareCard.addView(dareTextView);
            dareCard.addView(statusText);

            contentLayout.addView(dareCard);

        } catch (Exception e) {
            Log.e("DareInbox", "Error creating uncompleted dare card", e);
        }
    }

    private void showNoDaresMessage() {
        LinearLayout noDaresCard = new LinearLayout(this);
        noDaresCard.setOrientation(LinearLayout.VERTICAL);
        noDaresCard.setPadding(24, 32, 24, 32);
        noDaresCard.setBackgroundResource(R.drawable.glass_card); // FIXED: Already curved
        noDaresCard.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        noDaresCard.setLayoutParams(params);

        TextView noDaresText = new TextView(this);
        noDaresText.setText("🎯 No dares yet!");
        noDaresText.setTextColor(0xFFFFFFFF);
        noDaresText.setTextSize(18);
        noDaresText.setTypeface(null, android.graphics.Typeface.BOLD);
        noDaresText.setGravity(Gravity.CENTER);

        TextView noDaresDesc = new TextView(this);
        noDaresDesc.setText("Ask your partner to send you some exciting dares! 💕");
        noDaresDesc.setTextColor(0xFFE8BBE8);
        noDaresDesc.setTextSize(14);
        noDaresDesc.setGravity(Gravity.CENTER);
        noDaresDesc.setPadding(0, 8, 0, 0);

        noDaresCard.addView(noDaresText);
        noDaresCard.addView(noDaresDesc);
        contentLayout.addView(noDaresCard);
    }

    private void showRejectConfirmation(String dareId, String dareText) {
        new AlertDialog.Builder(this)
                .setTitle("Reject This Dare?")
                .setMessage("Are you sure you want to reject this dare?\n\n\"" + dareText + "\"\n\nYou won't earn any points and it will move to your uncompleted dares.")
                .setPositiveButton("Yes, Reject", (dialog, which) -> rejectDare(dareId))
                .setNegativeButton("Keep It", null)
                .show();
    }

    private void rejectDare(String dareId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("rejectedAt", System.currentTimeMillis());

        db.collection("dares").document(dareId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showCustomToast("Dare rejected and moved to uncompleted section");
                    // FIXED: Force immediate refresh to update counters and move dares
                    loadReceivedDares();
                })
                .addOnFailureListener(e -> {
                    Log.e("DareInbox", "Error rejecting dare", e);
                    showCustomToast("Error rejecting dare - please try again! 😅");
                });
    }

    private void completeDare(String dareId, int totalPoints, String category, long sentAt) {
        try {
            long completedAt = System.currentTimeMillis();

            // Show completing message
            showCustomToast("Completing dare...");

            // Update Firebase immediately
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "completed");
            updates.put("completedAt", completedAt);
            updates.put("earnedPoints", totalPoints);

            db.collection("dares").document(dareId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // Points are awarded automatically by backend (onDareCompleted trigger)
                        checkFirstDareCompletion();
                        badgeTracker.checkDareCompletion(category, completedAt, sentAt, totalPoints);
                        showCustomToast("🎉 Dare completed! Points will be awarded shortly.");

                        // 🚀 SMOOTH UPDATE - NO MORE FULL REFRESH!
                        updateDareCardToCompleted(dareId, totalPoints);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DareInbox", "Error completing dare", e);
                        showCustomToast("Error completing dare - please try again! 😅");
                    });

        } catch (Exception e) {
            Log.e("DareInbox", "Error in completeDare", e);
            showCustomToast("Error completing dare - please try again! 😅");
        }
    }

    private void updateCardToCompletedStyle(LinearLayout card, int points) {
        // Find and update specific elements in the card
        for (int i = 0; i < card.getChildCount(); i++) {
            View child = card.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                String text = tv.getText().toString();
                // Update status text
                if (text.contains("⏰") || text.contains("Day")) {
                    tv.setText("✅ COMPLETED - Earned " + points + " points!");
                    tv.setTextColor(0xFF4CAF50);
                }
                // Gray out the dare text to show it's completed
                if (text.length() > 20 && !text.contains("✅") && !text.contains("⏰")) {
                    tv.setTextColor(0xFFBBBBBB);
                }
            } else if (child instanceof LinearLayout) {
                // Hide button layout
                LinearLayout buttonLayout = (LinearLayout) child;
                if (buttonLayout.getChildCount() >= 2) {
                    View firstChild = buttonLayout.getChildAt(0);
                    if (firstChild instanceof Button) {
                        buttonLayout.setVisibility(View.GONE);
                    }
                }
            }
        }

        // 🎯 MOVE CARD TO COMPLETED SECTION AFTER A SHORT DELAY
        card.postDelayed(() -> {
            moveDareToCompletedSection(card, points);
        }, 2000); // Wait 2 seconds so user sees the completion clearly
    }
    private LinearLayout findDareCardById(String dareId) {
        // Search through contentLayout children to find the card with this dareId
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            if (child instanceof LinearLayout && dareId.equals(child.getTag())) {
                return (LinearLayout) child;
            }
        }
        return null;
    }
    private void updateDareCardToCompleted(String dareId, int points) {
        // Find the specific dare card and update it smoothly
        LinearLayout dareCard = findDareCardById(dareId);
        if (dareCard != null) {
            // Smooth animation
            dareCard.animate()
                    .alpha(0.7f)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        // Update card appearance
                        updateCardToCompletedStyle(dareCard, points);
                        // Animate back
                        dareCard.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();
                    })
                    .start();
        }
    }

    private void moveDareToCompletedSection(LinearLayout completedCard, int points) {
        try {
            // Find the "Active Dares" section and remove the card
            int cardIndex = -1;
            for (int i = 0; i < contentLayout.getChildCount(); i++) {
                if (contentLayout.getChildAt(i) == completedCard) {
                    cardIndex = i;
                    break;
                }
            }

            if (cardIndex != -1) {
                // Remove from current position with animation
                completedCard.animate()
                        .alpha(0f)
                        .translationX(-200f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            contentLayout.removeView(completedCard);

                            // Add to completed section
                            addToCompletedSection(completedCard, points);
                        })
                        .start();
            }
        } catch (Exception e) {
            Log.e("DareInbox", "Error moving card to completed section", e);
        }
    }

    private void addToCompletedSection(LinearLayout completedCard, int points) {
        try {
            // Find or create the "Completed Dares" section
            int completedSectionIndex = findCompletedSectionIndex();

            if (completedSectionIndex == -1) {
                // No completed section exists, create it ONLY
                createSectionHeader("✅ Completed Dares", "Great job! You've conquered these challenges!");
                completedSectionIndex = contentLayout.getChildCount() - 1;
            }

            // Reset card appearance and add to completed section
            completedCard.setAlpha(1f);
            completedCard.setTranslationX(0f);

            // Find the correct insertion point (after the header, before any uncompleted section)
            int insertIndex = completedSectionIndex + 1;

            // Skip over existing completed dares to add at the end of completed section
            while (insertIndex < contentLayout.getChildCount()) {
                View nextView = contentLayout.getChildAt(insertIndex);
                if (nextView instanceof LinearLayout) {
                    LinearLayout nextContainer = (LinearLayout) nextView;
                    // Check if this is the start of uncompleted or partner section
                    boolean isUncompletedSection = false;
                    boolean isPartnerSection = false;

                    for (int j = 0; j < nextContainer.getChildCount(); j++) {
                        if (nextContainer.getChildAt(j) instanceof TextView) {
                            TextView tv = (TextView) nextContainer.getChildAt(j);
                            String text = tv.getText().toString();
                            if (text.contains("❌ Uncompleted") || text.contains("💕 Partner")) {
                                isUncompletedSection = true;
                                isPartnerSection = true;
                                break;
                            }
                        }
                    }

                    if (isUncompletedSection || isPartnerSection) {
                        break; // Insert before uncompleted/partner section
                    }
                }
                insertIndex++;
            }

            // Add the completed dare card at the correct position
            contentLayout.addView(completedCard, insertIndex);

            // Slide in animation
            completedCard.setTranslationX(200f);
            completedCard.animate()
                    .translationX(0f)
                    .setDuration(400)
                    .start();

        } catch (Exception e) {
            Log.e("DareInbox", "Error adding to completed section", e);
        }
    }
    private boolean hasCompletedSection() {
        return findCompletedSectionIndex() != -1;
    }
    private int findCompletedSectionIndex() {
        // Look for the "✅ Completed Dares" header inside LinearLayout containers
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) child;
                // Check if this LinearLayout contains the completed dares header
                for (int j = 0; j < container.getChildCount(); j++) {
                    View innerChild = container.getChildAt(j);
                    if (innerChild instanceof TextView) {
                        TextView tv = (TextView) innerChild;
                        String text = tv.getText().toString();
                        if (text.contains("✅ Completed Dares") || text.contains("Completed Dares")) {
                            return i; // Return the index of the container LinearLayout
                        }
                    }
                }
            }
        }
        return -1; // Not found
    }




    private void checkFirstDareCompletion() {
        try {
            db.collection("dares")
                    .whereEqualTo("toUserId", currentUser.getUid())
                    .whereEqualTo("status", "completed")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.size() == 1) {
                            badgeTracker.onFirstDareCompleted();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DareInbox", "Error checking first dare completion", e);
                    });
        } catch (Exception e) {
            Log.e("DareInbox", "Error in checkFirstDareCompletion", e);
        }
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