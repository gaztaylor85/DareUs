package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentPartnerId, partnerName, firstName;
    private LinearLayout mainLayout;
    private int myPoints = 0, partnerPoints = 0, myMonthlyPoints = 0, partnerMonthlyPoints = 0;
    private String currentMonth;
    private String coupleId;
    private PremiumManager premiumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase FIRST
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // NOW you can initialize things that need Firebase
        premiumManager = new PremiumManager(currentUser.getUid(), null);

        // Get current month
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().getTime());

        setupLeaderboardUI();
        loadUserData();
    }

    private void setupLeaderboardUI() {
        // Create main layout with premium styling
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);
        mainLayout.setBackgroundResource(R.drawable.premium_gradient_bg);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("🏆 Leaderboard");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        // Month display
        String monthName = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        TextView monthText = new TextView(this);
        monthText.setText(monthName + " Competition");
        monthText.setTextColor(0xFFE8BBE8);
        monthText.setTextSize(16);
        monthText.setGravity(Gravity.CENTER);
        monthText.setPadding(0, 0, 0, 32);

        mainLayout.addView(headerText);
        mainLayout.addView(monthText);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 24);
        backButton.setOnClickListener(v -> finish());

        mainLayout.addView(backButton);
        setContentView(mainLayout);
    }

    private void loadUserData() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        firstName = doc.getString("firstName");
                        currentPartnerId = doc.getString("partnerId");
                        Long points = doc.getLong("points");
                        myPoints = points != null ? points.intValue() : 0;

                        if (currentPartnerId == null || currentPartnerId.isEmpty()) {
                            showNoPartnerMessage();
                            return;
                        }

                        // Create couple ID (sorted for consistency)
                        coupleId = currentUser.getUid().compareTo(currentPartnerId) < 0 ?
                                currentUser.getUid() + "_" + currentPartnerId :
                                currentPartnerId + "_" + currentUser.getUid();

                        loadPartnerData();
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Error loading data");
                });
    }

    private void loadPartnerData() {
        db.collection("dareus").document(currentPartnerId)
                .get()
                .addOnSuccessListener(partnerDoc -> {
                    if (partnerDoc.exists()) {
                        partnerName = partnerDoc.getString("firstName");
                        Long partnerPointsLong = partnerDoc.getLong("points");
                        partnerPoints = partnerPointsLong != null ? partnerPointsLong.intValue() : 0;

                        loadMonthlyCompetition();
                    } else {
                        showCustomToast("Partner data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Error loading partner data");
                });
    }

    private void loadMonthlyCompetition() {
        db.collection("monthlyCompetitions").document(coupleId + "_" + currentMonth)
                .get()
                .addOnSuccessListener(competitionDoc -> {
                    if (competitionDoc.exists()) {
                        // Check if current user has set their prize
                        boolean isUser1 = currentUser.getUid().compareTo(currentPartnerId) < 0;
                        Boolean myPrizeSet = isUser1 ? competitionDoc.getBoolean("user1PrizeSet") : competitionDoc.getBoolean("user2PrizeSet");

                        if (myPrizeSet == null || !myPrizeSet) {
                            // Current user hasn't set their prize yet
                            showPrizeSelectionDialog();
                        } else {
                            // Competition exists and user has set prize, load points
                            loadMonthlyPoints(competitionDoc);
                        }
                    } else {
                        // No competition yet, create one
                        showPrizeSelectionDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error loading competition", e);
                    showPrizeSelectionDialog();
                });
    }

    private void showPrizeSelectionDialog() {
        android.widget.EditText prizeInput = new android.widget.EditText(this);
        prizeInput.setHint("What do you want if you win this month?");
        prizeInput.setTextColor(0xFFFFFFFF);
        prizeInput.setHintTextColor(0xFF9F7AEA);
        prizeInput.setBackgroundResource(R.drawable.premium_input_field);
        prizeInput.setMaxLines(3);
        prizeInput.setPadding(16, 16, 16, 16);

        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Set Your Prize")
                .setMessage("Choose what you want if you win " + new SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().getTime()) + "'s competition!\n\nThis will stay hidden until the last 7 days.")
                .setView(prizeInput)
                .setPositiveButton("Set Prize", (dialog, which) -> {
                    String prize = prizeInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(prize)) {
                        savePrizeChoice(prize);
                    } else {
                        showCustomToast("Please enter a prize!");
                    }
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    // Create competition without prize
                    savePrizeChoice("Winner's choice");
                })
                .show();
    }

    private void savePrizeChoice(String prize) {
        // Calculate month start/end
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long monthEnd = cal.getTimeInMillis();

        boolean isUser1 = currentUser.getUid().compareTo(currentPartnerId) < 0;

        // Check if competition document already exists
        db.collection("monthlyCompetitions").document(coupleId + "_" + currentMonth)
                .get()
                .addOnSuccessListener(existingDoc -> {
                    Map<String, Object> competitionData = new HashMap<>();

                    if (existingDoc.exists()) {
                        // Update existing competition with current user's prize
                        if (isUser1) {
                            competitionData.put("user1Prize", prize);
                            competitionData.put("user1PrizeSet", true);
                        } else {
                            competitionData.put("user2Prize", prize);
                            competitionData.put("user2PrizeSet", true);
                        }

                        db.collection("monthlyCompetitions").document(coupleId + "_" + currentMonth)
                                .update(competitionData)
                                .addOnSuccessListener(aVoid -> {
                                    showCustomToast("Prize set!");
                                    loadMonthlyPoints(null);
                                })
                                .addOnFailureListener(e -> {
                                    showCustomToast("Error saving prize. Try again!");
                                });
                    } else {
                        // Create new competition document
                        competitionData.put("coupleId", coupleId);
                        competitionData.put("month", currentMonth);
                        competitionData.put("startDate", monthStart);
                        competitionData.put("endDate", monthEnd);
                        competitionData.put("user1Id", currentUser.getUid().compareTo(currentPartnerId) < 0 ? currentUser.getUid() : currentPartnerId);
                        competitionData.put("user2Id", currentUser.getUid().compareTo(currentPartnerId) < 0 ? currentPartnerId : currentUser.getUid());

                        // Set current user's prize
                        if (isUser1) {
                            competitionData.put("user1Prize", prize);
                            competitionData.put("user1PrizeSet", true);
                            competitionData.put("user2PrizeSet", false);
                        } else {
                            competitionData.put("user2Prize", prize);
                            competitionData.put("user2PrizeSet", true);
                            competitionData.put("user1PrizeSet", false);
                        }

                        competitionData.put("user1Revealed", false);
                        competitionData.put("user2Revealed", false);

                        db.collection("monthlyCompetitions").document(coupleId + "_" + currentMonth)
                                .set(competitionData)
                                .addOnSuccessListener(aVoid -> {
                                    showCustomToast("Prize set! Waiting for " + partnerName + " to set theirs.");
                                    loadMonthlyPoints(null);
                                })
                                .addOnFailureListener(e -> {
                                    showCustomToast("Error saving prize. Try again!");
                                });
                    }
                });
    }

    private void loadMonthlyPoints(com.google.firebase.firestore.DocumentSnapshot competitionDoc) {
        // Calculate month start timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStart = cal.getTimeInMillis();

        Log.d("Leaderboard", "Loading monthly points since: " + new java.util.Date(monthStart));

        // Use arrays to avoid lambda final variable issue
        final int[] myCompletedPoints = {0};
        final int[] mySentPoints = {0};
        final int[] partnerCompletedPoints = {0};
        final int[] partnerSentPoints = {0};

        // Load MY completed dares
        db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .whereGreaterThan("completedAt", monthStart)
                .get()
                .addOnSuccessListener(myCompletedDares -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot dare : myCompletedDares) {
                        Long points = dare.getLong("earnedPoints");
                        if (points != null) {
                            myCompletedPoints[0] += points.intValue();
                        }
                    }

                    // Load MY sent dares
                    db.collection("dares")
                            .whereEqualTo("fromUserId", currentUser.getUid())
                            .whereGreaterThan("sentAt", monthStart)
                            .get()
                            .addOnSuccessListener(mySentDares -> {
                                for (com.google.firebase.firestore.QueryDocumentSnapshot dare : mySentDares) {
                                    Long points = dare.getLong("senderPoints");
                                    if (points != null) {
                                        mySentPoints[0] += points.intValue();
                                    }
                                }

                                // Load PARTNER completed dares
                                db.collection("dares")
                                        .whereEqualTo("toUserId", currentPartnerId)
                                        .whereEqualTo("status", "completed")
                                        .whereGreaterThan("completedAt", monthStart)
                                        .get()
                                        .addOnSuccessListener(partnerCompletedDares -> {
                                            for (com.google.firebase.firestore.QueryDocumentSnapshot dare : partnerCompletedDares) {
                                                Long points = dare.getLong("earnedPoints");
                                                if (points != null) {
                                                    partnerCompletedPoints[0] += points.intValue();
                                                }
                                            }

                                            // Load PARTNER sent dares
                                            db.collection("dares")
                                                    .whereEqualTo("fromUserId", currentPartnerId)
                                                    .whereGreaterThan("sentAt", monthStart)
                                                    .get()
                                                    .addOnSuccessListener(partnerSentDares -> {
                                                        for (com.google.firebase.firestore.QueryDocumentSnapshot dare : partnerSentDares) {
                                                            Long points = dare.getLong("senderPoints");
                                                            if (points != null) {
                                                                partnerSentPoints[0] += points.intValue();
                                                            }
                                                        }

                                                        // NOW assign to class fields
                                                        myMonthlyPoints = myCompletedPoints[0] + mySentPoints[0];
                                                        partnerMonthlyPoints = partnerCompletedPoints[0] + partnerSentPoints[0];

                                                        Log.d("Leaderboard", "My total: " + myMonthlyPoints + " (completed: " + myCompletedPoints[0] + ", sent: " + mySentPoints[0] + ")");
                                                        Log.d("Leaderboard", "Partner total: " + partnerMonthlyPoints + " (completed: " + partnerCompletedPoints[0] + ", sent: " + partnerSentPoints[0] + ")");

                                                        // Update competition with current points
                                                        updateCompetitionWithCurrentPoints();

                                                        // Display the leaderboard
                                                        displayLeaderboard(competitionDoc);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("Leaderboard", "Error loading partner sent dares", e);
                                                        // Set partner points and continue
                                                        myMonthlyPoints = myCompletedPoints[0] + mySentPoints[0];
                                                        partnerMonthlyPoints = partnerCompletedPoints[0];
                                                        displayLeaderboard(competitionDoc);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Leaderboard", "Error loading partner completed dares", e);
                                            // Set points and continue
                                            myMonthlyPoints = myCompletedPoints[0] + mySentPoints[0];
                                            partnerMonthlyPoints = 0;
                                            displayLeaderboard(competitionDoc);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Leaderboard", "Error loading my sent dares", e);
                                // Continue with just completed points
                                myMonthlyPoints = myCompletedPoints[0];
                                loadPartnerPointsOnly(competitionDoc, monthStart);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error loading my completed dares", e);
                    myMonthlyPoints = 0;
                    partnerMonthlyPoints = 0;
                    displayLeaderboard(competitionDoc);
                });
    }

    // Helper method for error handling
    private void loadPartnerPointsOnly(com.google.firebase.firestore.DocumentSnapshot competitionDoc, long monthStart) {
        db.collection("dares")
                .whereEqualTo("toUserId", currentPartnerId)
                .whereEqualTo("status", "completed")
                .whereGreaterThan("completedAt", monthStart)
                .get()
                .addOnSuccessListener(partnerDares -> {
                    partnerMonthlyPoints = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot dare : partnerDares) {
                        Long points = dare.getLong("earnedPoints");
                        if (points != null) {
                            partnerMonthlyPoints += points.intValue();
                        }
                    }
                    displayLeaderboard(competitionDoc);
                })
                .addOnFailureListener(e -> {
                    partnerMonthlyPoints = 0;
                    displayLeaderboard(competitionDoc);
                });
    }

    private void updateCompetitionWithCurrentPoints() {
        // Update the competition document with current month's points
        boolean isUser1 = currentUser.getUid().compareTo(currentPartnerId) < 0;

        Map<String, Object> updates = new HashMap<>();
        if (isUser1) {
            updates.put("currentUser1Points", myMonthlyPoints);
            updates.put("currentUser2Points", partnerMonthlyPoints);
        } else {
            updates.put("currentUser1Points", partnerMonthlyPoints);
            updates.put("currentUser2Points", myMonthlyPoints);
        }
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("monthlyCompetitions").document(coupleId + "_" + currentMonth)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Leaderboard", "Updated competition with current points");
                })
                .addOnFailureListener(e -> {
                    Log.w("Leaderboard", "Could not update competition points", e);
                });
    }

    private void displayLeaderboard(com.google.firebase.firestore.DocumentSnapshot competitionDoc) {
        // CLEAR existing competition cards ONLY (keep header and back button)
        // Count from the end and remove cards added after the back button
        int startIndex = 3; // Header, month text, back button = 3 items
        while (mainLayout.getChildCount() > startIndex) {
            mainLayout.removeViewAt(startIndex);
        }

        // Monthly points breakdown card
        LinearLayout pointsCard = createDetailedPointsCard();
        mainLayout.addView(pointsCard);

        // Prize section if competition exists
        if (competitionDoc != null && competitionDoc.exists()) {
            LinearLayout prizeCard = createPrizeCard(competitionDoc);
            mainLayout.addView(prizeCard);
        }

        // Enhanced countdown with premium styling
        LinearLayout countdownCard = createPremiumCountdownCard();
        mainLayout.addView(countdownCard);

        // Historical winners
        LinearLayout historyCard = createHistoryCard();
        mainLayout.addView(historyCard);

// ADD THIS: Check competition badges when viewing leaderboard
        Log.d("Leaderboard", "Current scores - Me: " + myMonthlyPoints + ", Partner: " + partnerMonthlyPoints);
    }

    private LinearLayout createDetailedPointsCard() {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 20, 24, 20);
            card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("🏆 Current Competition");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        // Create table-like structure
        LinearLayout tableHeader = new LinearLayout(this);
        tableHeader.setOrientation(LinearLayout.HORIZONTAL);
        tableHeader.setPadding(0, 0, 0, 8);

        TextView headerName = new TextView(this);
        headerName.setText("Name");
        headerName.setTextColor(0xFFBB86FC);
        headerName.setTextSize(12);
        headerName.setTypeface(null, android.graphics.Typeface.BOLD);
        headerName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        TextView headerBase = new TextView(this);
        headerBase.setText("Base");
        headerBase.setTextColor(0xFFBB86FC);
        headerBase.setTextSize(12);
        headerBase.setTypeface(null, android.graphics.Typeface.BOLD);
        headerBase.setGravity(Gravity.CENTER);
        headerBase.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView headerBonus = new TextView(this);
        headerBonus.setText("Bonus");
        headerBonus.setTextColor(0xFFBB86FC);
        headerBonus.setTextSize(12);
        headerBonus.setTypeface(null, android.graphics.Typeface.BOLD);
        headerBonus.setGravity(Gravity.CENTER);
        headerBonus.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView headerTotal = new TextView(this);
        headerTotal.setText("Total");
        headerTotal.setTextColor(0xFFBB86FC);
        headerTotal.setTextSize(12);
        headerTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        headerTotal.setGravity(Gravity.CENTER);
        headerTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        tableHeader.addView(headerName);
        tableHeader.addView(headerBase);
        tableHeader.addView(headerBonus);
        tableHeader.addView(headerTotal);

        // Calculate detailed points (for now using simplified logic)
        int myBasePoints = (int)(myMonthlyPoints * 0.7); // Approximate base points
        int myBonusPoints = myMonthlyPoints - myBasePoints;
        int partnerBasePoints = (int)(partnerMonthlyPoints * 0.7);
        int partnerBonusPoints = partnerMonthlyPoints - partnerBasePoints;

        // Determine winner and create rows in winner-first order
        boolean imWinning = myMonthlyPoints >= partnerMonthlyPoints;

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.setPadding(0, 4, 0, 4);

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setPadding(0, 4, 0, 4);

        if (imWinning) {
            // I'm winning, show my row first
            createPlayerRow(firstRow, firstName, myBasePoints, myBonusPoints, myMonthlyPoints, true);
            createPlayerRow(secondRow, partnerName, partnerBasePoints, partnerBonusPoints, partnerMonthlyPoints, false);
        } else {
            // Partner is winning, show partner row first
            createPlayerRow(firstRow, partnerName, partnerBasePoints, partnerBonusPoints, partnerMonthlyPoints, true);
            createPlayerRow(secondRow, firstName, myBasePoints, myBonusPoints, myMonthlyPoints, false);
        }

        // Points difference
        TextView difference = new TextView(this);
        int diff = Math.abs(myMonthlyPoints - partnerMonthlyPoints);
        if (diff == 0) {
            difference.setText("🤝 Perfect tie! Both win at month-end! 🎉");
        } else {
            String leader = myMonthlyPoints > partnerMonthlyPoints ? firstName : partnerName;
            difference.setText(leader + " leads by " + diff + " points");
        }
        difference.setTextColor(0xFFFFFFFF);
        difference.setTextSize(12);
        difference.setGravity(Gravity.CENTER);
        difference.setPadding(0, 12, 0, 0);

        card.addView(title);
        card.addView(tableHeader);

        // Add divider line
        TextView divider = new TextView(this);
        divider.setBackgroundColor(0x30FFFFFF);
        divider.setHeight(1);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
        );
        dividerParams.setMargins(0, 4, 0, 8);
        divider.setLayoutParams(dividerParams);
        card.addView(divider);

        card.addView(firstRow);
        card.addView(secondRow);
        card.addView(difference);

        return card;
    }

    private void createPlayerRow(LinearLayout row, String playerName, int basePoints, int bonusPoints, int totalPoints, boolean isWinner) {
        TextView nameView = new TextView(this);
        // Show trophy for winner, but special handling for ties
        boolean isTie = myMonthlyPoints == partnerMonthlyPoints && myMonthlyPoints > 0;
        if (isTie) {
            nameView.setText(playerName + " 🏆"); // Both get trophy in a tie
        } else {
            nameView.setText(playerName + (isWinner && totalPoints > 0 ? " 🏆" : ""));
        }
        nameView.setTextColor(isWinner || isTie ? 0xFF4CAF50 : 0xFFFFFFFF);
        nameView.setTextSize(14);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        TextView baseView = new TextView(this);
        baseView.setText(String.valueOf(basePoints));
        baseView.setTextColor(0xFFFFFFFF);
        baseView.setTextSize(14);
        baseView.setGravity(Gravity.CENTER);
        baseView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView bonusView = new TextView(this);
        bonusView.setText("+" + bonusPoints);
        bonusView.setTextColor(0xFFFF6B9D);
        bonusView.setTextSize(14);
        bonusView.setGravity(Gravity.CENTER);
        bonusView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView totalView = new TextView(this);
        totalView.setText(String.valueOf(totalPoints));
        totalView.setTextColor(isWinner || isTie ? 0xFF4CAF50 : 0xFFFFFFFF);
        totalView.setTextSize(14);
        totalView.setTypeface(null, android.graphics.Typeface.BOLD);
        totalView.setGravity(Gravity.CENTER);
        totalView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(nameView);
        row.addView(baseView);
        row.addView(bonusView);
        row.addView(totalView);
    }

    private LinearLayout createPremiumCountdownCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("⏰ Competition Countdown");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        // Calculate days remaining in month
        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_MONTH);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysLeft = lastDay - today + 1;

        // Premium styled countdown with better visual design
        LinearLayout countdownContainer = new LinearLayout(this);
        countdownContainer.setOrientation(LinearLayout.VERTICAL);
        countdownContainer.setPadding(16, 16, 16, 16);
        countdownContainer.setBackgroundColor(0x20FFFFFF);

        android.graphics.drawable.GradientDrawable countdownShape = new android.graphics.drawable.GradientDrawable();
        countdownShape.setCornerRadius(12);
        countdownShape.setColor(0x20FFFFFF);
        countdownShape.setStroke(2, 0xFFB794F6);
        countdownContainer.setBackground(countdownShape);

        TextView countdown = new TextView(this);
        String countdownText;
        int countdownColor;

        if (daysLeft == 1) {
            countdownText = "🚨 FINAL DAY!";
            countdownColor = 0xFFE91E63;
        } else if (daysLeft <= 3) {
            countdownText = "🔥 " + daysLeft + " days left";
            countdownColor = 0xFFFF6B9D;
        } else if (daysLeft <= 7) {
            countdownText = "⚡ " + daysLeft + " days remaining";
            countdownColor = 0xFFED8936;
        } else {
            countdownText = "📅 " + daysLeft + " days left";
            countdownColor = 0xFFBB86FC;
        }

        countdown.setText(countdownText);
        countdown.setTextColor(countdownColor);
        countdown.setTextSize(20);
        countdown.setTypeface(null, android.graphics.Typeface.BOLD);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(0, 0, 0, 12);

        // Premium progress bar with gradient and glow effect
        LinearLayout progressContainer = new LinearLayout(this);
        progressContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressContainer.setPadding(8, 8, 8, 8);

        LinearLayout progressBar = new LinearLayout(this);
        progressBar.setOrientation(LinearLayout.HORIZONTAL);

        // Create gradient progress bar
        int progress = (int)(((float)(today - 1) / (lastDay - 1)) * 20);
        for (int i = 0; i < 20; i++) {
            TextView progressBlock = new TextView(this);
            progressBlock.setText("█");
            progressBlock.setTextSize(16);
            progressBlock.setPadding(1, 0, 1, 0);

            if (i < progress) {
                // Gradient effect - more intense for recent progress
                float intensity = 1.0f - (float)i / progress;
                int color = i < progress * 0.3f ? 0xFF4CAF50 :
                        i < progress * 0.7f ? 0xFFFFEB3B : 0xFFFF6B9D;
                progressBlock.setTextColor(color);
                progressBlock.setShadowLayer(8f, 0f, 0f, color);
            } else {
                progressBlock.setTextColor(0x40FFFFFF);
            }
            progressBar.addView(progressBlock);
        }

        progressContainer.addView(progressBar);
        progressContainer.setGravity(Gravity.CENTER);

        TextView progressLabel = new TextView(this);
        progressLabel.setText("Day " + today + " of " + lastDay);
        progressLabel.setTextColor(0xFFE8BBE8);
        progressLabel.setTextSize(12);
        progressLabel.setGravity(Gravity.CENTER);
        progressLabel.setPadding(0, 8, 0, 0);

        countdownContainer.addView(countdown);
        countdownContainer.addView(progressContainer);
        countdownContainer.addView(progressLabel);

        card.addView(title);
        card.addView(countdownContainer);

        return card;
    }

    private LinearLayout createPrizeCard(com.google.firebase.firestore.DocumentSnapshot competitionDoc) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("🎁 Monthly Prizes");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        // Check if within 7 days of month end
        Calendar cal = Calendar.getInstance();
        int daysLeft = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - cal.get(Calendar.DAY_OF_MONTH) + 1;
        boolean withinRevealPeriod = daysLeft <= 7;

        // Get reveal status
        Boolean user1Revealed = competitionDoc.getBoolean("user1Revealed");
        Boolean user2Revealed = competitionDoc.getBoolean("user2Revealed");
        if (user1Revealed == null) user1Revealed = false;
        if (user2Revealed == null) user2Revealed = false;

        boolean isUser1 = currentUser.getUid().compareTo(currentPartnerId) < 0;
        boolean myPrizeRevealed = isUser1 ? user1Revealed : user2Revealed;
        boolean partnerPrizeRevealed = isUser1 ? user2Revealed : user1Revealed;

        // My prize
        TextView myPrize = new TextView(this);
        String myPrizeText = isUser1 ? competitionDoc.getString("user1Prize") : competitionDoc.getString("user2Prize");
        myPrize.setText("Your prize: " + (myPrizeText != null ? myPrizeText : "Not set"));
        myPrize.setTextColor(0xFFFFFFFF);
        myPrize.setTextSize(14);
        myPrize.setGravity(Gravity.CENTER);

        // Partner prize
        TextView partnerPrize = new TextView(this);
        String partnerPrizeText = isUser1 ? competitionDoc.getString("user2Prize") : competitionDoc.getString("user1Prize");

        if (withinRevealPeriod || partnerPrizeRevealed) {
            partnerPrize.setText(partnerName + "'s prize: " + (partnerPrizeText != null ? partnerPrizeText : "Not set"));
            partnerPrize.setTextColor(0xFFFFFFFF);
        } else {
            partnerPrize.setText(partnerName + "'s prize: 🔒 HIDDEN (" + daysLeft + " days until reveal)");
            partnerPrize.setTextColor(0xFF9575CD);
        }
        partnerPrize.setTextSize(14);
        partnerPrize.setGravity(Gravity.CENTER);
        partnerPrize.setPadding(0, 8, 0, 0);

        card.addView(title);
        card.addView(myPrize);
        card.addView(partnerPrize);

        // Reveal button if not within 7 days and not revealed
        if (!withinRevealPeriod && !partnerPrizeRevealed && myPoints >= 25) {
            Button revealButton = new Button(this);
            revealButton.setText("Peek at Prize (25 points)");
            revealButton.setBackgroundResource(R.drawable.premium_button_gradient);
            revealButton.setTextColor(0xFFFFFFFF);
            revealButton.setTextSize(14);
            revealButton.setPadding(24, 12, 24, 12);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.gravity = Gravity.CENTER;
            buttonParams.setMargins(0, 16, 0, 0);
            revealButton.setLayoutParams(buttonParams);

            revealButton.setOnClickListener(v -> revealPartnerPrize(competitionDoc));
            card.addView(revealButton);
        }

        return card;
    }

    private void revealPartnerPrize(com.google.firebase.firestore.DocumentSnapshot competitionDoc) {
        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Reveal Partner's Prize?")
                .setMessage("This will cost you 25 points to see what " + partnerName + " wants if they win.\n\nAre you sure?")
                .setPositiveButton("Yes, Reveal", (dialog, which) -> {
                    // Call secure Cloud Function to deduct points and reveal
                    FirebaseFunctions functions = FirebaseFunctions.getInstance();
                    Map<String, Object> data = new HashMap<>();
                    data.put("competitionId", coupleId + "_" + currentMonth);

                    functions.getHttpsCallable("revealPartnerPrize")
                            .call(data)
                            .addOnSuccessListener(result -> {
                                Map<String, Object> response = (Map<String, Object>) result.getData();
                                boolean success = (boolean) response.get("success");

                                if (success) {
                                    int newBalance = ((Long) response.get("newBalance")).intValue();

                                    // Track Code Breaker badge
                                    BadgeTracker badgeTracker = new BadgeTracker(currentUser.getUid(), new BadgeTracker.BadgeUnlockListener() {
                                        @Override
                                        public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
                                            // Show EPIC badge celebration dialog!
                                            BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(LeaderboardActivity.this, badge);
                                            celebrationDialog.show();
                                        }
                                    });
                                    badgeTracker.onPrizeRevealed();

                                    showCustomToast("Prize revealed! You lost 25 points. New balance: " + newBalance);
                                    recreate(); // Refresh screen
                                }
                            })
                            .addOnFailureListener(e -> {
                                String errorMessage = "Error revealing prize";
                                if (e.getMessage() != null) {
                                    if (e.getMessage().contains("Insufficient points")) {
                                        errorMessage = "Not enough points! Need 25 points.";
                                    } else if (e.getMessage().contains("Competition not found")) {
                                        errorMessage = "Competition not found. Try again!";
                                    } else {
                                        errorMessage = e.getMessage();
                                    }
                                }
                                showCustomToast(errorMessage);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }



    private LinearLayout createHistoryCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("📊 Competition History");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);

        // Load historical data
        loadHistoricalWinners(card);

        card.addView(title);
        return card;
    }

    private void loadHistoricalWinners(LinearLayout card) {
        // Query past competitions
        db.collection("monthlyCompetitions")
                .whereEqualTo("coupleId", coupleId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView noHistory = new TextView(this);
                        noHistory.setText("No past competitions yet\n\nStart competing to build your history!");
                        noHistory.setTextColor(0xFF9575CD);
                        noHistory.setTextSize(12);
                        noHistory.setGravity(Gravity.CENTER);
                        noHistory.setPadding(0, 8, 0, 0);
                        card.addView(noHistory);
                        return;
                    }

                    // Process competitions...
                    List<com.google.firebase.firestore.QueryDocumentSnapshot> completedCompetitions = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String month = doc.getString("month");
                        if (month != null && !month.equals(currentMonth)) {
                            completedCompetitions.add(doc);
                        }
                    }

                    if (completedCompetitions.isEmpty()) {
                        TextView noHistory = new TextView(this);
                        noHistory.setText("First month competing!\n\nHistory will appear next month!");
                        noHistory.setTextColor(0xFF9575CD);
                        noHistory.setTextSize(12);
                        noHistory.setGravity(Gravity.CENTER);
                        noHistory.setPadding(0, 8, 0, 0);
                        card.addView(noHistory);
                        return;
                    }

                    // Add "History" label
                    TextView historyLabel = new TextView(this);
                    historyLabel.setText("📊 Past Competitions");
                    historyLabel.setTextColor(0xFFFFFFFF);
                    historyLabel.setTextSize(14);
                    historyLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    historyLabel.setGravity(Gravity.CENTER);
                    historyLabel.setPadding(0, 0, 0, 12);
                    card.addView(historyLabel);

                    // Show last 3 competitions
                    int count = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : completedCompetitions) {
                        if (count >= 3) break;

                        String month = doc.getString("month");
                        // Try multiple field names for backwards compatibility
                        Long user1Points = doc.getLong("currentUser1Points");
                        Long user2Points = doc.getLong("currentUser2Points");

// Fallback to older field names if current ones don't exist
                        if (user1Points == null) {
                            user1Points = doc.getLong("finalUser1Points");
                        }
                        if (user2Points == null) {
                            user2Points = doc.getLong("finalUser2Points");
                        }
// Another fallback - use user points directly
                        if (user1Points == null) {
                            user1Points = doc.getLong("user1Points");
                        }
                        if (user2Points == null) {
                            user2Points = doc.getLong("user2Points");
                        }

                        TextView competitionText = new TextView(this);
                        String displayText = getMonthDisplayName(month) + ": ";

// Check if this month is actually finished
                        boolean isMonthFinished = isMonthComplete(month);

                        if (user1Points != null && user2Points != null && (user1Points > 0 || user2Points > 0)) {
                            if (user1Points > user2Points) {
                                String user1Id = doc.getString("user1Id");
                                String winner = user1Id != null && user1Id.equals(currentUser.getUid()) ? "You" : partnerName;
                                displayText += winner + " won (" + user1Points + "-" + user2Points + ")";
                            } else if (user2Points > user1Points) {
                                String user2Id = doc.getString("user2Id");
                                String winner = user2Id != null && user2Id.equals(currentUser.getUid()) ? "You" : partnerName;
                                displayText += winner + " won (" + user2Points + "-" + user1Points + ")";
                            } else {
                                displayText += "Tie (" + user1Points + " each)";
                            }
                        } else if (isMonthFinished) {
                            // Month is finished but no points - probably no dares completed
                            displayText += "No activity (0-0)";
                        } else {
                            // Month is still in progress
                            displayText += "Month in progress (0-0)";
                        }

                        competitionText.setText(displayText);
                        competitionText.setTextColor(0xFFE8BBE8);
                        competitionText.setTextSize(11);
                        competitionText.setGravity(Gravity.CENTER);
                        competitionText.setPadding(0, 2, 0, 2);
                        card.addView(competitionText);

                        count++;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Leaderboard", "Error loading history", e);
                    TextView errorText = new TextView(this);
                    errorText.setText("Unable to load competition history");
                    errorText.setTextColor(0xFFFF6B6B);
                    errorText.setTextSize(12);
                    errorText.setGravity(Gravity.CENTER);
                    card.addView(errorText);
                });
    }
    private boolean isMonthComplete(String monthCode) {
        try {
            String[] parts = monthCode.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]); // 1-12

            Calendar monthEnd = Calendar.getInstance();
            monthEnd.set(Calendar.YEAR, year);
            monthEnd.set(Calendar.MONTH, month - 1); // Calendar months are 0-11
            monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
            monthEnd.set(Calendar.HOUR_OF_DAY, 23);
            monthEnd.set(Calendar.MINUTE, 59);
            monthEnd.set(Calendar.SECOND, 59);

            long monthEndTime = monthEnd.getTimeInMillis();
            long now = System.currentTimeMillis();

            return now > monthEndTime;
        } catch (Exception e) {
            // If we can't parse, assume it's old and finished
            return true;
        }
    }
    private String getMonthDisplayName(String monthCode) {
        try {
            String[] parts = monthCode.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month - 1);

            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            return monthCode;
        }
    }




    private void showNoPartnerMessage() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 32, 24, 32);
        card.setBackgroundResource(R.drawable.glass_card);
        card.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("No Competition Yet");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView desc = new TextView(this);
        desc.setText("Connect with your partner to start competing!");
        desc.setTextColor(0xFFE8BBE8);
        desc.setTextSize(14);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 8, 0, 0);

        card.addView(title);
        card.addView(desc);
        mainLayout.addView(card);
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