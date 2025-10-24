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
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class BadgesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout mainLayout;
    private List<String> unlockedBadges = new ArrayList<>();
    private int totalPoints = 0;

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

        setupBadgesUI();
        loadUserData();
    }

    private void setupBadgesUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);

        // 🎉 ENHANCED HEADER with celebration
        TextView headerText = new TextView(this);
        headerText.setText("🏆 Achievement Gallery");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(20f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("✨ Show off your accomplishments! Every badge tells your love story! ✨");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 16);

        // 💎 ACHIEVEMENT SUMMARY CARD
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

        // 💫 PREMIUM PARTNER BADGES BUTTON
        Button partnerBadgesButton = new Button(this);
        partnerBadgesButton.setText("💫 View Partner's Achievements");
        partnerBadgesButton.setBackgroundResource(R.drawable.premium_button_gradient);
        partnerBadgesButton.setTextColor(0xFFFFFFFF);
        partnerBadgesButton.setTextSize(16);
        partnerBadgesButton.setTypeface(null, android.graphics.Typeface.BOLD);
        partnerBadgesButton.setPadding(24, 16, 24, 16);
        partnerBadgesButton.setElevation(8);

        LinearLayout.LayoutParams partnerButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        partnerButtonParams.setMargins(0, 0, 0, 24);
        partnerBadgesButton.setLayoutParams(partnerButtonParams);

        partnerBadgesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PartnerBadgesActivity.class);
            startActivity(intent);
        });

        // Stats display
        createStatColumn(summaryCard, "🏆", "0", "Badges Earned");
        createStatColumn(summaryCard, "💎", "0", "Total Points");
        createStatColumn(summaryCard, "🎯", "0%", "Completion");

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 24);
        backButton.setOnClickListener(v -> finish());

        mainLayout.addView(backButton);
        mainLayout.addView(headerText);
        mainLayout.addView(subHeader);
        mainLayout.addView(summaryCard);
        mainLayout.addView(partnerBadgesButton);



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

    private void loadUserData() {
        // Load user points and stats
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long points = doc.getLong("points");
                        totalPoints = points != null ? points.intValue() : 0;
                        loadUserBadges();
                    }
                });
    }

    private void loadUserBadges() {
        Log.d("BadgesActivity", "Loading badges for user: " + currentUser.getUid());

        db.collection("userBadges")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    unlockedBadges.clear();
                    Log.d("BadgesActivity", "Found " + querySnapshot.size() + " unlocked badges");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String badgeId = doc.getString("badgeId");
                        if (badgeId != null) {
                            unlockedBadges.add(badgeId);
                            Log.d("BadgesActivity", "Unlocked badge: " + badgeId);
                        }
                    }
                    updateStatsAndDisplayBadges();
                })
                .addOnFailureListener(e -> {
                    Log.e("BadgesActivity", "Error loading badges", e);
                    updateStatsAndDisplayBadges(); // Show badges anyway, just all locked
                });
    }

    private void updateStatsAndDisplayBadges() {
        // Update summary stats
        int totalBadges = BadgeSystem.BADGES.size();
        int earnedBadges = unlockedBadges.size();
        int completionPercent = (int) ((float) earnedBadges / totalBadges * 100);

        updateStatValue("Badges Earned", String.valueOf(earnedBadges));
        updateStatValue("Total Points", String.valueOf(totalPoints));
        updateStatValue("Completion", completionPercent + "%");

        displayBadges();
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

    private void displayBadges() {
        // 🎉 ALL BADGES VISIBLE - NO MORE HIDING!
        createBadgeCategory("🏃 Speed Demon", getSpeedBadges(), "Complete dares quickly for massive bonuses!");
        createBadgeCategory("🎯 Category Master", getCategoryBadges(), "Conquer every type of dare challenge!");
        createBadgeCategory("🔥 Streak Legend", getStreakBadges(), "Build unstoppable completion streaks!");
        createBadgeCategory("💝 Power Couple", getPartnershipBadges(), "Perfect teamwork with your partner!");
        createBadgeCategory("🏆 Competition King/Queen", getCompetitionBadges(), "Dominate monthly competitions!");
        createBadgeCategory("💎 Milestone Master", getMilestoneBadges(), "Reach incredible point milestones!");
        createBadgeCategory("🎲 Special Achiever", getSpecialBadges(), "Unique and rare accomplishments!");
    }

    private void createBadgeCategory(String categoryName, List<String> badgeIds, String description) {
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

        // Calculate progress with AWESOME visual feedback
        int unlockedCount = 0;
        for (String badgeId : badgeIds) {
            if (unlockedBadges.contains(badgeId)) {
                unlockedCount++;
            }
        }

        // 🎉 ENHANCED Progress display
        TextView progressText = new TextView(this);
        String progressStr = unlockedCount + " / " + badgeIds.size() + " earned";
        if (unlockedCount == badgeIds.size()) {
            progressStr += " 🎉 COMPLETED! 🎉";
            progressText.setTextColor(0xFFFFD700); // Gold for completion
        } else if (unlockedCount > 0) {
            progressStr += " ⚡ Keep going!";
            progressText.setTextColor(0xFF4CAF50);
        } else {
            progressStr += " 💪 Ready to start!";
            progressText.setTextColor(0xFFBB86FC);
        }

        progressText.setText(progressStr);
        progressText.setTextSize(14);
        progressText.setTypeface(null, android.graphics.Typeface.BOLD);
        progressText.setGravity(Gravity.CENTER);
        progressText.setPadding(0, 0, 0, 8);

        // ✨ STUNNING visual progress bar
        LinearLayout progressBarContainer = new LinearLayout(this);
        progressBarContainer.setOrientation(LinearLayout.HORIZONTAL);
        progressBarContainer.setGravity(Gravity.CENTER);
        progressBarContainer.setPadding(0, 0, 0, 16);

        for (int i = 0; i < badgeIds.size(); i++) {
            TextView progressBlock = new TextView(this);
            progressBlock.setText("●");
            progressBlock.setTextSize(20);

            if (i < unlockedCount) {
                // Gradient effect for earned badges
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

        // 🏆 ALL BADGES SHOWN - LOCKED AND UNLOCKED!
        LinearLayout badgeGrid = new LinearLayout(this);
        badgeGrid.setOrientation(LinearLayout.VERTICAL);

        for (String badgeId : badgeIds) {
            createEnhancedBadgeRow(badgeGrid, badgeId);
        }

        categoryCard.addView(badgeGrid);
        mainLayout.addView(categoryCard);
    }

    private void createEnhancedBadgeRow(LinearLayout parent, String badgeId) {
        BadgeSystem.Badge badge = BadgeSystem.BADGES.get(badgeId);
        if (badge == null) {
            Log.w("BadgesActivity", "Badge not found: " + badgeId);
            return;
        }

        boolean isUnlocked = unlockedBadges.contains(badgeId);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(16, 16, 16, 16);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        // 🎉 ENHANCED backgrounds with celebration vibes
        if (isUnlocked) {
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setCornerRadius(16);
            shape.setColors(new int[]{0x404CAF50, 0x204CAF50}); // Green gradient for unlocked
            shape.setStroke(3, 0xFF4CAF50);
            badgeRow.setBackground(shape);

            // Add glow effect for unlocked badges
            badgeRow.setElevation(8);
        } else {
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setCornerRadius(16);
            shape.setColors(new int[]{0x20666666, 0x10666666}); // Gray gradient for locked
            shape.setStroke(2, 0xFF666666);
            badgeRow.setBackground(shape);
        }

        // 🏆 BIGGER, BETTER badge emoji
        TextView emojiText = new TextView(this);
        if (isUnlocked) {
            emojiText.setText(badge.emoji);
            emojiText.setShadowLayer(12f, 0f, 0f, 0xFFFFD700); // Gold glow for unlocked
        } else {
            emojiText.setText("🔒");
        }
        emojiText.setTextSize(36); // BIGGER!
        emojiText.setPadding(0, 0, 24, 0);

        // 💫 ENHANCED badge info
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);

        TextView nameText = new TextView(this);
        nameText.setText(badge.name); // ALWAYS SHOW NAME!
        nameText.setTextColor(isUnlocked ? 0xFFFFFFFF : 0xFFCCCCCC);
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        if (isUnlocked) {
            nameText.setShadowLayer(8f, 0f, 0f, 0xFF4CAF50);
        }

        TextView descText = new TextView(this);
        descText.setText(badge.description); // ALWAYS SHOW DESCRIPTION!
        descText.setTextColor(isUnlocked ? 0xFFE8BBE8 : 0xFF999999);
        descText.setTextSize(13);
        descText.setPadding(0, 4, 0, 0);

        // 🎯 Show progress toward unlocking
        if (!isUnlocked) {
            TextView progressText = new TextView(this);
            progressText.setText("🎯 Goal: " + badge.requirement + " " + getRequirementText(badge.type));
            progressText.setTextColor(0xFFBB86FC);
            progressText.setTextSize(11);
            progressText.setTypeface(null, android.graphics.Typeface.BOLD);
            progressText.setPadding(0, 4, 0, 0);
            infoLayout.addView(progressText);
        }

        infoLayout.addView(nameText);
        infoLayout.addView(descText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        infoLayout.setLayoutParams(infoParams);

        badgeRow.addView(emojiText);
        badgeRow.addView(infoLayout);

        // 💎 ENHANCED reward display
        LinearLayout rewardLayout = new LinearLayout(this);
        rewardLayout.setOrientation(LinearLayout.VERTICAL);
        rewardLayout.setGravity(Gravity.CENTER);

        if (isUnlocked) {
            TextView statusText = new TextView(this);
            statusText.setText("✅");
            statusText.setTextSize(24);
            statusText.setGravity(Gravity.CENTER);
            statusText.setShadowLayer(8f, 0f, 0f, 0xFF4CAF50);

            TextView earnedText = new TextView(this);
            earnedText.setText("EARNED!");
            earnedText.setTextColor(0xFF4CAF50);
            earnedText.setTextSize(10);
            earnedText.setTypeface(null, android.graphics.Typeface.BOLD);
            earnedText.setGravity(Gravity.CENTER);

            rewardLayout.addView(statusText);
            rewardLayout.addView(earnedText);
        } else {
            TextView lockText = new TextView(this);
            lockText.setText("🔓");
            lockText.setTextSize(20);
            lockText.setGravity(Gravity.CENTER);

            TextView unlockText = new TextView(this);
            unlockText.setText("UNLOCK");
            unlockText.setTextColor(0xFF666666);
            unlockText.setTextSize(10);
            unlockText.setTypeface(null, android.graphics.Typeface.BOLD);
            unlockText.setGravity(Gravity.CENTER);

            rewardLayout.addView(lockText);
            rewardLayout.addView(unlockText);
        }

        badgeRow.addView(rewardLayout);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 12);
        badgeRow.setLayoutParams(rowParams);

        parent.addView(badgeRow);
    }

    private String getRequirementText(String type) {
        switch (type) {
            case "speed": return "fast completions";
            case "category_sweet": return "sweet dares";
            case "category_playful": return "playful dares";
            case "category_adventure": return "adventure dares";
            case "category_passionate": return "passionate dares";
            case "category_wild": return "wild dares";
            case "category_mixed": return "mixed categories";
            case "streak": return "day streak";
            case "partnership": return "couple activities";
            case "competition": return "competitions";
            case "milestone": return "points";
            case "sender": return "dares sent";
            case "time": return "time-based dares";
            case "secret": return "secret actions";
            default: return "completions";
        }
    }

    // Keep all the existing badge category methods...
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
        badges.add("social_butterfly");
        badges.add("code_breaker");
        badges.add("second_chance");
        return badges;
    }
}