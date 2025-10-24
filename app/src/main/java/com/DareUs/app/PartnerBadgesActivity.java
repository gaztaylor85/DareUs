package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class PartnerBadgesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout mainLayout;
    private List<String> partnerUnlockedBadges = new ArrayList<>();
    private String partnerId, partnerName;
    private int partnerTotalPoints = 0;

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

        setupPartnerBadgesUI();
        loadPartnerData();
    }

    private void setupPartnerBadgesUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);

        // Premium header with partner focus
        TextView headerText = new TextView(this);
        headerText.setText("💫 Partner's Achievements");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(20f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("✨ See what amazing things your partner has accomplished! ✨");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 16);

        // Partner summary card
        LinearLayout summaryCard = new LinearLayout(this);
        summaryCard.setOrientation(LinearLayout.HORIZONTAL);
        summaryCard.setPadding(24, 20, 24, 20);
        summaryCard.setBackgroundResource(R.drawable.glass_card);
        summaryCard.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        summaryParams.setMargins(0, 0, 0, 24);
        summaryCard.setLayoutParams(summaryParams);

        // Stats display
        createStatColumn(summaryCard, "🏆", "0", "Badges Earned");
        createStatColumn(summaryCard, "💎", "0", "Total Points");
        createStatColumn(summaryCard, "⭐", "0%", "Completion");

        mainLayout.addView(headerText);
        mainLayout.addView(subHeader);
        mainLayout.addView(summaryCard);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Achievements");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 24);
        backButton.setOnClickListener(v -> finish());

        mainLayout.addView(backButton);

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private void createStatColumn(LinearLayout parent, String emoji, String value, String label) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        column.setLayoutParams(columnParams);

        TextView emojiText = new TextView(this);
        emojiText.setText(emoji);
        emojiText.setTextSize(24);
        emojiText.setGravity(Gravity.CENTER);

        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextColor(0xFFFFFFFF);
        valueText.setTextSize(20);
        valueText.setTypeface(null, android.graphics.Typeface.BOLD);
        valueText.setGravity(Gravity.CENTER);
        valueText.setTag(label); // For updating later

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextColor(0xFFE8BBE8);
        labelText.setTextSize(12);
        labelText.setGravity(Gravity.CENTER);

        column.addView(emojiText);
        column.addView(valueText);
        column.addView(labelText);
        parent.addView(column);
    }

    private void loadPartnerData() {
        // First, get partner ID
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        partnerId = doc.getString("partnerId");

                        if (partnerId == null || partnerId.isEmpty()) {
                            showNoPartnerMessage();
                            return;
                        }

                        // Load partner's basic info
                        db.collection("dareus").document(partnerId)
                                .get()
                                .addOnSuccessListener(partnerDoc -> {
                                    if (partnerDoc.exists()) {
                                        partnerName = partnerDoc.getString("firstName");
                                        Long points = partnerDoc.getLong("points");
                                        partnerTotalPoints = points != null ? points.intValue() : 0;

                                        // Update header with partner name
                                        updateHeaderWithPartnerName();
                                        loadPartnerBadges();
                                    }
                                });
                    }
                });
    }

    private void updateHeaderWithPartnerName() {
        if (mainLayout.getChildCount() > 0 && mainLayout.getChildAt(0) instanceof TextView) {
            TextView headerText = (TextView) mainLayout.getChildAt(0);
            headerText.setText("💫 " + partnerName + "'s Achievements");
        }
    }

    private void loadPartnerBadges() {
        Log.d("PartnerBadges", "Loading badges for partner: " + partnerId);

        db.collection("userBadges")
                .whereEqualTo("userId", partnerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    partnerUnlockedBadges.clear();
                    Log.d("PartnerBadges", "Found " + querySnapshot.size() + " partner badges");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String badgeId = doc.getString("badgeId");
                        if (badgeId != null) {
                            partnerUnlockedBadges.add(badgeId);
                            Log.d("PartnerBadges", "Partner unlocked badge: " + badgeId);
                        }
                    }
                    updateStatsAndDisplayBadges();
                })
                .addOnFailureListener(e -> {
                    Log.e("PartnerBadges", "Error loading partner badges", e);
                    updateStatsAndDisplayBadges(); // Show badges anyway, just all locked
                });
    }

    private void updateStatsAndDisplayBadges() {
        // Update summary stats
        int totalBadges = BadgeSystem.BADGES.size();
        int earnedBadges = partnerUnlockedBadges.size();
        int completionPercent = (int) ((float) earnedBadges / totalBadges * 100);

        updateStatValue("Badges Earned", String.valueOf(earnedBadges));
        updateStatValue("Total Points", String.valueOf(partnerTotalPoints));
        updateStatValue("Completion", completionPercent + "%");

        displayPartnerBadges();
    }

    private void updateStatValue(String label, String newValue) {
        for (int i = 0; i < mainLayout.getChildCount(); i++) {
            if (mainLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout card = (LinearLayout) mainLayout.getChildAt(i);
                updateStatInCard(card, label, newValue);
            }
        }
    }

    private void updateStatInCard(LinearLayout parent, String label, String newValue) {
        if (parent.getChildCount() == 0) return;

        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) instanceof LinearLayout) {
                LinearLayout column = (LinearLayout) parent.getChildAt(i);
                for (int j = 0; j < column.getChildCount(); j++) {
                    if (column.getChildAt(j) instanceof TextView) {
                        TextView textView = (TextView) column.getChildAt(j);
                        if (label.equals(textView.getTag())) {
                            textView.setText(newValue);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void displayPartnerBadges() {
        // Show premium message about partner achievements
        LinearLayout partnerMessageCard = new LinearLayout(this);
        partnerMessageCard.setOrientation(LinearLayout.VERTICAL);
        partnerMessageCard.setPadding(24, 20, 24, 20);
        partnerMessageCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 16);
        partnerMessageCard.setLayoutParams(messageParams);

        TextView messageTitle = new TextView(this);
        messageTitle.setText("🤝 Achievement Sharing");
        messageTitle.setTextColor(0xFFFFFFFF);
        messageTitle.setTextSize(18);
        messageTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        messageTitle.setGravity(Gravity.CENTER);
        messageTitle.setPadding(0, 0, 0, 8);

        TextView messageText = new TextView(this);
        String message = partnerName + " has earned " + partnerUnlockedBadges.size() + " out of " +
                BadgeSystem.BADGES.size() + " total achievements!\n\n" +
                "💪 Keep challenging each other to unlock more badges together!";
        messageText.setText(message);
        messageText.setTextColor(0xFFE8BBE8);
        messageText.setTextSize(14);
        messageText.setGravity(Gravity.CENTER);

        partnerMessageCard.addView(messageTitle);
        partnerMessageCard.addView(messageText);
        mainLayout.addView(partnerMessageCard);

        // Display badge categories with partner's progress
        createPartnerBadgeCategory("🏃 Speed Demon", getSpeedBadges(), "Fast completion mastery!");
        createPartnerBadgeCategory("🎯 Category Master", getCategoryBadges(), "Dare variety expertise!");
        createPartnerBadgeCategory("🔥 Streak Legend", getStreakBadges(), "Consistency champion!");
        createPartnerBadgeCategory("💝 Power Couple", getPartnershipBadges(), "Teamwork achievements!");
        createPartnerBadgeCategory("🏆 Competition King/Queen", getCompetitionBadges(), "Monthly competition prowess!");
        createPartnerBadgeCategory("💎 Milestone Master", getMilestoneBadges(), "Point accumulation skills!");
        createPartnerBadgeCategory("🎲 Special Achiever", getSpecialBadges(), "Unique accomplishments!");
            }

    private void createPartnerBadgeCategory(String categoryName, List<String> badgeIds, String description) {
        LinearLayout categoryCard = new LinearLayout(this);
        categoryCard.setOrientation(LinearLayout.VERTICAL);
        categoryCard.setPadding(24, 20, 24, 20);
        categoryCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        categoryCard.setLayoutParams(params);

        TextView categoryTitle = new TextView(this);
        categoryTitle.setText(categoryName);
        categoryTitle.setTextColor(0xFFFFFFFF);
        categoryTitle.setTextSize(18);
        categoryTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryTitle.setGravity(Gravity.CENTER);
        categoryTitle.setPadding(0, 0, 0, 4);
        categoryTitle.setShadowLayer(10f, 0f, 0f, 0xFFFF6B9D);

        TextView categoryDesc = new TextView(this);
        categoryDesc.setText(description);
        categoryDesc.setTextColor(0xFFE8BBE8);
        categoryDesc.setTextSize(12);
        categoryDesc.setGravity(Gravity.CENTER);
        categoryDesc.setPadding(0, 0, 0, 16);

        categoryCard.addView(categoryTitle);
        categoryCard.addView(categoryDesc);

        // Calculate partner's progress
        int unlockedCount = 0;
        for (String badgeId : badgeIds) {
            if (partnerUnlockedBadges.contains(badgeId)) {
                unlockedCount++;
            }
        }

        // Premium progress display
        TextView progressText = new TextView(this);
        String progressStr = partnerName + " earned " + unlockedCount + " / " + badgeIds.size();
        if (unlockedCount == badgeIds.size()) {
            progressStr += " 🎉 MASTERED! 🎉";
            progressText.setTextColor(0xFFFFD700); // Gold for completion
        } else if (unlockedCount > 0) {
            progressStr += " ⚡ Great progress!";
            progressText.setTextColor(0xFF4CAF50);
        } else {
            progressStr += " 💪 Room to grow!";
            progressText.setTextColor(0xFFBB86FC);
        }

        progressText.setText(progressStr);
        progressText.setTextSize(14);
        progressText.setTypeface(null, android.graphics.Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, 0, 0, 8);

        // Visual progress bar
        LinearLayout progressBarContainer = new LinearLayout(this);
        progressBarContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressBarContainer.setGravity(Gravity.CENTER);
        progressBarContainer.setPadding(0, 0, 0, 16);

        for (int i = 0; i < badgeIds.size(); i++) {
            TextView progressBlock = new TextView(this);
            progressBlock.setText("●");
            progressBlock.setTextSize(20);

            if (i < unlockedCount) {
                if (unlockedCount == badgeIds.size()) {
                    progressBlock.setTextColor(0xFFFFD700); // Gold when complete
                } else {
                    progressBlock.setTextColor(0xFF4CAF50); // Green for earned
                }
                progressBlock.setShadowLayer(8f, 0f, 0f, 0xFF4CAF50);
            } else {
                progressBlock.setTextColor(0xFF555555); // Dark for unearned
            }

            progressBlock.setPadding(4, 0, 4, 0);
            progressBarContainer.addView(progressBlock);
        }

        categoryCard.addView(progressText);
        categoryCard.addView(progressBarContainer);

        // Show earned badges in this category
        for (String badgeId : badgeIds) {
            if (partnerUnlockedBadges.contains(badgeId)) {
                createPartnerBadgeRow(categoryCard, badgeId);
            }
        }

        mainLayout.addView(categoryCard);
    }

    private void createPartnerBadgeRow(LinearLayout parent, String badgeId) {
        BadgeSystem.Badge badge = BadgeSystem.BADGES.get(badgeId);
        if (badge == null) return;

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(16, 12, 16, 12);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        // Premium earned badge styling
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(16);
        shape.setColors(new int[]{0x404CAF50, 0x204CAF50});
        shape.setStroke(2, 0xFF4CAF50);
        badgeRow.setBackground(shape);
        badgeRow.setElevation(6);

        // Badge emoji with glow
        TextView emojiText = new TextView(this);
        emojiText.setText(badge.emoji);
        emojiText.setTextSize(32);
        emojiText.setPadding(0, 0, 20, 0);
        emojiText.setShadowLayer(10f, 0f, 0f, 0xFFFFD700);

        // Badge info
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        TextView nameText = new TextView(this);
        nameText.setText(badge.name);
        nameText.setTextColor(0xFFFFFFFF);
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setShadowLayer(6f, 0f, 0f, 0xFF4CAF50);

        TextView descText = new TextView(this);
        descText.setText(badge.description);
        descText.setTextColor(0xFFE8BBE8);
        descText.setTextSize(13);
        descText.setPadding(0, 4, 0, 0);

        infoLayout.addView(nameText);
        infoLayout.addView(descText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoLayout.setLayoutParams(infoParams);

        // Earned status
        TextView statusText = new TextView(this);
        statusText.setText("✅");
        statusText.setTextSize(24);
        statusText.setGravity(Gravity.CENTER);
        statusText.setShadowLayer(8f, 0f, 0f, 0xFF4CAF50);

        badgeRow.addView(emojiText);
        badgeRow.addView(infoLayout);
        badgeRow.addView(statusText);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 8);
        badgeRow.setLayoutParams(rowParams);

        parent.addView(badgeRow);
    }

    private void showNoPartnerMessage() {
        LinearLayout noPartnerCard = new LinearLayout(this);
        noPartnerCard.setOrientation(LinearLayout.VERTICAL);
        noPartnerCard.setPadding(24, 32, 24, 32);
        noPartnerCard.setBackgroundResource(R.drawable.glass_card);
        noPartnerCard.setGravity(Gravity.CENTER);

        TextView noPartnerText = new TextView(this);
        noPartnerText.setText("💔 No Partner Connected");
        noPartnerText.setTextColor(0xFFFFFFFF);
        noPartnerText.setTextSize(20);
        noPartnerText.setTypeface(null, android.graphics.Typeface.BOLD);
        noPartnerText.setGravity(Gravity.CENTER);

        TextView noPartnerDesc = new TextView(this);
        noPartnerDesc.setText("Connect with your partner to see their amazing achievements!");
        noPartnerDesc.setTextColor(0xFFE8BBE8);
        noPartnerDesc.setTextSize(14);
        noPartnerDesc.setGravity(Gravity.CENTER);
        noPartnerDesc.setPadding(0, 8, 0, 0);

        noPartnerCard.addView(noPartnerText);
        noPartnerCard.addView(noPartnerDesc);
        mainLayout.addView(noPartnerCard);
    }

    // Badge category methods (same as BadgesActivity)
    private List<String> getSpeedBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("lightning_lover");
        badges.add("flash_forward");
        badges.add("speed_racer");
        badges.add("instant_gratification");
        return badges;
    }

    private List<String> getCategoryBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("sweet_soul");
        badges.add("playful_spirit");
        badges.add("adventure_seeker");
        badges.add("passionate_heart");
        badges.add("wild_one");
        badges.add("renaissance_lover");
        return badges;
    }

    private List<String> getStreakBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("warm_up");
        badges.add("getting_hot");
        badges.add("on_fire");
        badges.add("blazing");
        badges.add("inferno");
        return badges;
    }

    private List<String> getPartnershipBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("perfect_match");
        badges.add("synchronized_souls");
        badges.add("power_couple");
        badges.add("dynamic_duo");
        return badges;
    }

    private List<String> getCompetitionBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("monthly_champion");
        badges.add("close_call");
        badges.add("comeback_kid");
        badges.add("dominator");
        badges.add("competitive_spirit");
        badges.add("hat_trick");
        badges.add("rivalry_master");
        badges.add("final_hour");
        badges.add("early_bird_winner");
        badges.add("photo_finish");
        return badges;
    }

    private List<String> getMilestoneBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("first_steps");
        badges.add("getting_started");
        badges.add("point_collector");
        badges.add("point_master");
        badges.add("point_legend");
        return badges;
    }

    private List<String> getSpecialBadges() {
        List<String> badges = new ArrayList<>();
        badges.add("generous_giver");
        badges.add("dare_devil");
        badges.add("night_owl");
        badges.add("early_bird");
        badges.add("weekend_warrior");
        badges.add("code_breaker");     // MOVED FROM SECRET
        badges.add("second_chance");    // MOVED FROM SECRET
        return badges;
    }
}