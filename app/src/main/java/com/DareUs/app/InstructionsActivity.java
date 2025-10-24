package com.DareUs.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;

public class InstructionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupInstructionsUI();
    }

    private void setupInstructionsUI() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("📖 How to Use DareUs");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 24);
        backButton.setOnClickListener(v -> finish());

        // Instructions content
        createInstructionCard(mainLayout, "🚀 Getting Started",
                "1. Set up your profile with your name\n" +
                        "2. Share your 12-character invite code with your partner\n" +
                        "3. Enter your partner's code to link your accounts\n" +
                        "4. Start sending dares to each other!\n" +
                        "5. Complete dares to earn points and unlock achievements");

        createInstructionCard(mainLayout, "📅 Free User Limits (Resets Every Monday)",
                "• Sweet & Playful categories: 5 dares each per week\n" +
                        "• Adventure, Passionate & Wild: 1 dare each per week\n" +
                        "• Weekly limits reset every Monday at midnight\n" +
                        "• Upgrade to Premium for unlimited access!");

        createInstructionCard(mainLayout, "💖 Sending Dares",
                "• Choose from 5 categories with different point values:\n" +
                        "  - Sweet (5-15 pts): Romantic & caring challenges\n" +
                        "  - Playful (10-20 pts): Fun & lighthearted activities\n" +
                        "  - Adventure (15-25 pts): Exciting outdoor experiences\n" +
                        "  - Passionate (20-30 pts): Intimate & romantic\n" +
                        "  - Wild (25-35 pts): Bold & daring challenges");

        createInstructionCard(mainLayout, "✅ Completing Dares",
                "• Check your 'Received Dares' for new challenges\n" +
                        "• Dares expire after 7 days if not completed\n" +
                        "• Mark dares as complete when finished\n" +
                        "• Earn points and maintain your completion streak!\n" +
                        "• Build daily streaks for bonus achievement badges");

        createInstructionCard(mainLayout, "🏆 Achievements & Competition",
                "• Unlock 40+ achievement badges across 7 categories:\n" +
                        "  - Speed Demon: Fast completion badges\n" +
                        "  - Category Master: Complete dares in each type\n" +
                        "  - Streak Legend: Daily completion streaks\n" +
                        "  - Power Couple: Partnership achievements\n" +
                        "  - Competition Champion: Monthly leaderboard wins\n" +
                        "  - Milestone Master: Point-based achievements\n" +
                        "  - Special Achiever: Unique accomplishments");

        createInstructionCard(mainLayout, "🏅 Monthly Competition",
                "• Compete with your partner each month for points\n" +
                        "• View leaderboard to see who's winning\n" +
                        "• Earn competition badges for wins and achievements\n" +
                        "• Competition resets at the start of each month\n" +
                        "• Special badges for close wins, comebacks & dominance!");

        createInstructionCard(mainLayout, "💎 Premium Tiers",
                "• Premium Single ($2.99): Unlimited dares for you only\n" +
                        "• Premium Couple ($3.99): Unlimited for both partners\n" +
                        "• Premium Couple Plus ($4.99): + Custom dare creation\n" +
                        "• Partner's couple subscription benefits both users\n" +
                        "• Upgrade anytime from Profile & Premium section");

        createInstructionCard(mainLayout, "🎯 Custom Dares (Premium Couple Plus)",
                "• Create completely personalized dares\n" +
                        "• Set your own point values (1-100 points)\n" +
                        "• Send for negotiation with your partner\n" +
                        "• Partner can accept, haggle points, or reject\n" +
                        "• Negotiate back and forth until agreement\n" +
                        "• Once accepted, becomes a regular dare to complete");

        createInstructionCard(mainLayout, "🔥 Streaks & Timing",
                "• Daily streaks: Complete at least 1 dare per day\n" +
                        "• Streaks reset if you skip a day\n" +
                        "• Speed badges for fast completions (same day/next day)\n" +
                        "• Time-based badges: Night Owl, Early Bird, Weekend Warrior\n" +
                        "• Partnership badges for synchronized completions");

        createInstructionCard(mainLayout, "💡 Tips for Success",
                "• Complete dares quickly for speed achievement badges\n" +
                        "• Try dares from all categories for variety badges\n" +
                        "• Coordinate with your partner for timing badges\n" +
                        "• Check achievements gallery to see all available badges\n" +
                        "• Maintain daily streaks for the most impressive badges\n" +
                        "• Use custom dares to create personal challenges!");

        mainLayout.addView(headerText);
        mainLayout.addView(backButton);

        scrollView.addView(mainLayout);
        setContentView(scrollView);
    }

    private void createInstructionCard(LinearLayout parent, String title, String content) {
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

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setPadding(0, 0, 0, 12);

        TextView contentText = new TextView(this);
        contentText.setText(content);
        contentText.setTextColor(0xFFE8BBE8);
        contentText.setTextSize(14);

        card.addView(titleText);
        card.addView(contentText);
        parent.addView(card);
    }
}