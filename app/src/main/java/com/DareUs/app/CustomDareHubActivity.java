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

public class CustomDareHubActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private PremiumManager premiumManager;
    private int pendingIncomingCount = 0;
    private int pendingOutgoingCount = 0;

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

        // ✅ Show loading while checking premium status
        showLoadingState();

        // Initialize premium manager with callback
        premiumManager = new PremiumManager(currentUser.getUid(), new PremiumManager.PremiumStatusListener() {
            @Override
            public void onPremiumStatusChanged(PremiumManager.PremiumTier tier) {
                // ✅ Only setup UI once premium status is known
                if (!isUISetup) {
                    setupHubUI();
                    loadNegotiationCounts();
                    isUISetup = true;
                } else {
                    updateAccessCards();
                }
            }

            @Override
            public void onUsageLimitReached(String category) {
                // Not needed here
            }
        });
    }

    private boolean isUISetup = false; // ✅ Add this field to track UI state

    private void showLoadingState() {
        // ✅ Show simple loading screen
        LinearLayout loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setBackgroundResource(R.drawable.premium_gradient_bg);
        loadingLayout.setPadding(24, 32, 24, 32);

        TextView loadingText = new TextView(this);
        loadingText.setText("Loading Custom Dares...");
        loadingText.setTextColor(0xFFFFFFFF);
        loadingText.setTextSize(18);
        loadingText.setGravity(Gravity.CENTER);

        loadingLayout.addView(loadingText);
        setContentView(loadingLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh counts when returning to this activity
        loadNegotiationCounts();
    }

    private void setupHubUI() {
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setPadding(24, 32, 24, 32);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("✨ Custom Dares Hub");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFB794F6);

        TextView subHeader = new TextView(this);
        subHeader.setText("Create, send, and negotiate personalized dares");
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

        // Add action cards
        updateAccessCards();

        scrollView.addView(contentLayout);
        setContentView(scrollView);
    }

    private void updateAccessCards() {
        // Remove existing action cards
        for (int i = contentLayout.getChildCount() - 1; i >= 0; i--) {
            if (contentLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout child = (LinearLayout) contentLayout.getChildAt(i);
                if (child.getTag() != null && "action_card".equals(child.getTag())) {
                    contentLayout.removeViewAt(i);
                }
            }
        }

        if (premiumManager != null && premiumManager.hasCustomDares()) {
            // User has access - show all options
            createAccessCard();
        } else {
            // No access - show upgrade option
            createUpgradeCard();
        }
    }

    private void createAccessCard() {
        // Create Custom Dare Card
        LinearLayout createCard = createActionCard(
                "🎨 Create Custom Dare",
                "Design a personalized challenge for your partner",
                "Premium Plus Feature",
                0xFF4CAF50,
                v -> {
                    Intent intent = new Intent(this, CustomDareActivity.class);
                    startActivity(intent);
                }
        );
        contentLayout.addView(createCard);

        // Negotiations Card
        LinearLayout negotiationsCard = createActionCard(
                "🤝 Negotiations (" + (pendingIncomingCount + pendingOutgoingCount) + ")",
                "Review and respond to custom dare proposals",
                pendingIncomingCount > 0 ? pendingIncomingCount + " need your response" : "All caught up",
                pendingIncomingCount > 0 ? 0xFFE91E63 : 0xFFBB86FC,
                v -> {
                    Intent intent = new Intent(this, CustomDareNegotiationActivity.class);
                    startActivity(intent);
                }
        );
        contentLayout.addView(negotiationsCard);

        // Info Card
        createInfoCard();
    }

    private void createUpgradeCard() {
        LinearLayout upgradeCard = new LinearLayout(this);
        upgradeCard.setOrientation(LinearLayout.VERTICAL);
        upgradeCard.setPadding(24, 20, 24, 20);
        upgradeCard.setBackgroundResource(R.drawable.glass_card);
        upgradeCard.setTag("action_card");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        upgradeCard.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText("🔒 Custom Dares Locked");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        String currentTier = premiumManager != null ? premiumManager.getTierDisplayName() : "Free";
        description.setText("Current Plan: " + currentTier + "\n\nCustom dares require Premium Plus or Premium Couple Plus");
        description.setTextColor(0xFFE8BBE8);
        description.setTextSize(14);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, 8, 0, 16);

        Button upgradeButton = new Button(this);
        upgradeButton.setText("🚀 Upgrade to Premium Plus");
        upgradeButton.setBackgroundResource(R.drawable.premium_button_gradient);
        upgradeButton.setTextColor(0xFFFFFFFF);
        upgradeButton.setTextSize(14);
        upgradeButton.setPadding(24, 12, 24, 12);

        upgradeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PremiumUpgradeActivity.class);
            startActivity(intent);
        });

        upgradeCard.addView(title);
        upgradeCard.addView(description);
        upgradeCard.addView(upgradeButton);

        contentLayout.addView(upgradeCard);
    }

    private LinearLayout createActionCard(String title, String description, String status,
                                          int statusColor, android.view.View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundResource(R.drawable.glass_card);
        card.setTag("action_card");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(0xFFFFFFFF);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setGravity(Gravity.CENTER);

        TextView descText = new TextView(this);
        descText.setText(description);
        descText.setTextColor(0xFFE8BBE8);
        descText.setTextSize(14);
        descText.setGravity(Gravity.CENTER);
        descText.setPadding(0, 4, 0, 8);

        TextView statusText = new TextView(this);
        statusText.setText(status);
        statusText.setTextColor(statusColor);
        statusText.setTextSize(12);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusText.setGravity(Gravity.CENTER);

        card.addView(titleText);
        card.addView(descText);
        card.addView(statusText);
        card.setOnClickListener(clickListener);

        return card;
    }

    private void createInfoCard() {
        LinearLayout infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(20, 16, 20, 16);
        // CORRECT: Use mutate() to avoid modifying shared drawable
        android.graphics.drawable.Drawable background = getResources().getDrawable(R.drawable.glass_card).mutate();
        background.setTint(0x304CAF50);
        infoCard.setBackground(background);
        infoCard.setTag("action_card");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 16, 0, 16);
        infoCard.setLayoutParams(params);

        TextView infoTitle = new TextView(this);
        infoTitle.setText("💡 How Custom Dares Work");
        infoTitle.setTextColor(0xFFFFFFFF);
        infoTitle.setTextSize(16);
        infoTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        infoTitle.setGravity(Gravity.CENTER);

        TextView infoText = new TextView(this);
        infoText.setText("1. Create your personalized dare\n" +
                "2. Set your proposed points (1-100)\n" +
                "3. Send for negotiation\n" +
                "4. Partner can accept, haggle, or reject\n" +
                "5. Once agreed, it becomes a regular dare!\n\n" +
                "✨ All custom content is encrypted and private");
        infoText.setTextColor(0xFFFFFFFF);
        infoText.setTextSize(12);
        infoText.setGravity(Gravity.CENTER);
        infoText.setPadding(0, 8, 0, 0);

        infoCard.addView(infoTitle);
        infoCard.addView(infoText);
        contentLayout.addView(infoCard);
    }

    private void loadNegotiationCounts() {
        if (currentUser == null) return;

        // Load incoming negotiations (I need to respond to)
        db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending_negotiation")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingIncomingCount = querySnapshot.size();
                    updateCountsInUI();
                });

        // Load outgoing negotiations (waiting for partner)
        db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereEqualTo("status", "pending_negotiation")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingOutgoingCount = querySnapshot.size();
                    updateCountsInUI();
                });
    }

    private void updateCountsInUI() {
        // Update the action cards with new counts
        if (premiumManager != null && premiumManager.hasCustomDares()) {
            updateAccessCards();
        }
    }
}
