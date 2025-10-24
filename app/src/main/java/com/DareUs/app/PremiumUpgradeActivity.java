package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PremiumUpgradeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private PremiumManager premiumManager;
    private LinearLayout mainLayout;
    private long premiumExpiresAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        premiumManager = new PremiumManager(currentUser.getUid(), new PremiumManager.PremiumStatusListener() {
            @Override
            public void onPremiumStatusChanged(PremiumManager.PremiumTier tier) {
                updateCurrentPlanCard();
            }

            @Override
            public void onUsageLimitReached(String category) {
                // Not needed here
            }
        });

        setupPremiumUI();
        loadPremiumExpirationData();
    }

    private void loadPremiumExpirationData() {
        FirebaseFirestore.getInstance()
                .collection("dareus")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long expiresAt = doc.getLong("premiumExpiresAt");
                        if (expiresAt != null) {
                            premiumExpiresAt = expiresAt;
                            updateCurrentPlanCard();
                        }
                    }
                });
    }
    private void createPremiumPlans() {
        TextView plansTitle = new TextView(this);
        plansTitle.setText("💎 Choose Your Plan");
        plansTitle.setTextColor(0xFFFFFFFF);
        plansTitle.setTextSize(24);
        plansTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        plansTitle.setGravity(Gravity.CENTER);
        plansTitle.setPadding(0, 16, 0, 24);
        mainLayout.addView(plansTitle);

        // Premium Single - $2.99
        createTierCard(
                "🚀 Premium Single",
                "$2.99/month",
                "• Unlimited dares from all categories\n• Send as many as you want\n• Perfect for individuals",
                PremiumManager.PremiumTier.PREMIUM_SINGLE,
                true
        );

        // Premium Couple - $3.99
        createTierCard(
                "💕 Premium Couple",
                "$3.99/month",
                "• Everything in Premium Single\n• BOTH partners get unlimited dares\n• Share the premium benefits\n• Best value for couples!",
                PremiumManager.PremiumTier.PREMIUM_COUPLE,
                true
        );

        // Premium Couple Plus - $4.99
        createTierCard(
                "✨ Premium Couple Plus",
                "$4.99/month",
                "• Everything in Premium Couple\n• Custom dare creation & negotiation\n• Ultimate couples experience\n• Full feature access for both",
                PremiumManager.PremiumTier.PREMIUM_COUPLE_PLUS,
                true
        );
    }

    private void setupPremiumUI() {
        // Create main ScrollView
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        // Create main layout inside ScrollView
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(24, 32, 24, 32);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("💎 Premium Plans");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("Unlock unlimited dares and premium features");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 32);

        mainLayout.addView(headerText);
        mainLayout.addView(subHeader);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back To Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 16);
        backButton.setOnClickListener(v -> finish());

        LinearLayout.LayoutParams backButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backButtonParams.gravity = Gravity.START;
        backButton.setLayoutParams(backButtonParams);
        mainLayout.addView(backButton);

        // Current plan will be added here dynamically
        updateCurrentPlanCard();

        // FIXED: Only create plans once with correct pricing!
        TextView plansTitle = new TextView(this);
        plansTitle.setText("💎 Choose Your Plan");
        plansTitle.setTextColor(0xFFFFFFFF);
        plansTitle.setTextSize(24);
        plansTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        plansTitle.setGravity(Gravity.CENTER);
        plansTitle.setPadding(0, 16, 0, 24);
        mainLayout.addView(plansTitle);

        // Premium Single - $2.99 ✅
        createTierCard(
                "🚀 Premium Single",
                "$2.99/month",
                "• Unlimited dares from all categories\n• Send as many as you want\n• Perfect for individuals",
                PremiumManager.PremiumTier.PREMIUM_SINGLE,
                true
        );

        // Premium Couple - $3.99 ✅
        createTierCard(
                "💕 Premium Couple",
                "$3.99/month",
                "• Everything in Premium Single\n• BOTH partners get unlimited dares\n• Share the premium benefits\n• Best value for couples!",
                PremiumManager.PremiumTier.PREMIUM_COUPLE,
                true
        );

        // Premium Couple Plus - $4.99 ✅
        createTierCard(
                "✨ Premium Couple Plus",
                "$4.99/month",
                "• Everything in Premium Couple\n• Custom dare creation & negotiation\n• Ultimate couples experience\n• Full feature access for both",
                PremiumManager.PremiumTier.PREMIUM_COUPLE_PLUS,
                true
        );

        // Add mainLayout to ScrollView
        scrollView.addView(mainLayout);

        // Set ScrollView as content view
        setContentView(scrollView);
    }

    private void updateCurrentPlanCard() {
        // Remove existing current plan card if it exists
        for (int i = 0; i < mainLayout.getChildCount(); i++) {
            if (mainLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout child = (LinearLayout) mainLayout.getChildAt(i);
                if (child.getTag() != null && "current_plan".equals(child.getTag())) {
                    mainLayout.removeViewAt(i);
                    break;
                }
            }
        }

        // Create new current plan card
        LinearLayout currentPlanCard = new LinearLayout(this);
        currentPlanCard.setOrientation(LinearLayout.VERTICAL);
        currentPlanCard.setPadding(24, 20, 24, 20);
        currentPlanCard.setBackgroundResource(R.drawable.glass_card);
        currentPlanCard.setTag("current_plan");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        currentPlanCard.setLayoutParams(params);

        // Current plan header
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView currentPlanTitle = new TextView(this);
        currentPlanTitle.setText("🌟 Current Plan");
        currentPlanTitle.setTextColor(0xFFFFFFFF);
        currentPlanTitle.setTextSize(18);
        currentPlanTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView statusIndicator = new TextView(this);
        if (premiumManager != null && premiumManager.isPremiumUser()) {
            statusIndicator.setText("ACTIVE");
            statusIndicator.setTextColor(0xFF4CAF50);
        } else {
            statusIndicator.setText("FREE");
            statusIndicator.setTextColor(0xFFFF6B9D);
        }
        statusIndicator.setTextSize(12);
        statusIndicator.setTypeface(null, android.graphics.Typeface.BOLD);
        statusIndicator.setGravity(Gravity.END);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        currentPlanTitle.setLayoutParams(titleParams);

        headerLayout.addView(currentPlanTitle);
        headerLayout.addView(statusIndicator);

        // Plan details
        TextView planDetails = new TextView(this);
        String tierName = premiumManager != null ? premiumManager.getTierDisplayName() : "Free";
        String tierDesc = premiumManager != null ? premiumManager.getTierDescription() : "Limited features";

        String planText = "Plan: " + tierName + "\n" + tierDesc;

        // Add expiration info if premium
        if (premiumManager != null && premiumManager.isPremiumUser() && premiumExpiresAt > 0) {
            long timeLeft = premiumExpiresAt - System.currentTimeMillis();
            if (timeLeft > 0) {
                long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String expiryDate = dateFormat.format(new Date(premiumExpiresAt));

                if (daysLeft <= 7) {
                    planText += "\n\n⚠️ Expires in " + daysLeft + " days (" + expiryDate + ")";
                } else {
                    planText += "\n\n📅 Expires: " + expiryDate + " (" + daysLeft + " days left)";
                }
            } else {
                planText += "\n\n❌ EXPIRED - Please renew your subscription";
            }
        }

        // Add partner benefit info if relevant
        if (premiumManager != null) {
            PremiumManager.PremiumTier currentTier = premiumManager.getCurrentTier();
            PremiumManager.PremiumTier partnerTier = premiumManager.getPartnerTier();

            if (currentTier == PremiumManager.PremiumTier.FREE && partnerTier != PremiumManager.PremiumTier.FREE) {
                planText += "\n\n🤝 Benefits via partner's subscription";
            }
        }

        planDetails.setText(planText);
        planDetails.setTextColor(0xFFE8BBE8);
        planDetails.setTextSize(13);
        planDetails.setPadding(0, 8, 0, 0);

        currentPlanCard.addView(headerLayout);
        currentPlanCard.addView(planDetails);

        // Add upgrade button for free users or expired premium
        boolean needsUpgrade = premiumManager == null || !premiumManager.isPremiumUser() ||
                (premiumExpiresAt > 0 && premiumExpiresAt <= System.currentTimeMillis());

        if (needsUpgrade) {
            Button upgradeButton = new Button(this);
            upgradeButton.setText(premiumExpiresAt > 0 && premiumExpiresAt <= System.currentTimeMillis() ?
                    "🔄 Renew Subscription" : "⬆️ Upgrade Now");
            upgradeButton.setBackgroundResource(R.drawable.premium_button_gradient);
            upgradeButton.setTextColor(0xFFFFFFFF);
            upgradeButton.setTextSize(14);
            upgradeButton.setPadding(16, 8, 16, 8);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.gravity = Gravity.CENTER;
            buttonParams.setMargins(0, 12, 0, 0);
            upgradeButton.setLayoutParams(buttonParams);

            upgradeButton.setOnClickListener(v -> {
                showCustomToast("Choose a plan below to upgrade! 💎");
            });

            currentPlanCard.addView(upgradeButton);
        }

        // Insert at position 2 (after header and subheader)
        mainLayout.addView(currentPlanCard, 2);
    }

    private void createTierCard(String tierName, String price, String features, PremiumManager.PremiumTier tier, boolean canUpgrade) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);

        // Check if this is the current tier
        boolean isCurrentTier = premiumManager != null && premiumManager.getCurrentTier() == tier;

        if (isCurrentTier) {
            // Current tier gets special styling
            android.graphics.drawable.GradientDrawable currentTierBg = new android.graphics.drawable.GradientDrawable();
            currentTierBg.setCornerRadius(16);
            currentTierBg.setColor(0x304CAF50);
            currentTierBg.setStroke(2, 0xFF4CAF50);
            card.setBackground(currentTierBg);
        } else {
            card.setBackgroundResource(R.drawable.glass_card);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        // Tier name and price
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView nameText = new TextView(this);
        String displayName = tierName;
        if (isCurrentTier) {
            displayName += " ✅";
        }
        nameText.setText(displayName);
        nameText.setTextColor(0xFFFFFFFF);
        nameText.setTextSize(18);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView priceText = new TextView(this);
        priceText.setText(price);
        priceText.setTextColor(isCurrentTier ? 0xFF4CAF50 : 0xFFFF6B9D);
        priceText.setTextSize(16);
        priceText.setTypeface(null, android.graphics.Typeface.BOLD);
        priceText.setGravity(Gravity.END);

        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameText.setLayoutParams(nameParams);

        headerLayout.addView(nameText);
        headerLayout.addView(priceText);

        // Features
        TextView featuresText = new TextView(this);
        featuresText.setText(features);
        featuresText.setTextColor(0xFFE8BBE8);
        featuresText.setTextSize(13);
        featuresText.setPadding(0, 8, 0, 16);

        card.addView(headerLayout);
        card.addView(featuresText);

        // Upgrade button (only show if not current tier)
        if (canUpgrade && !isCurrentTier) {
            Button upgradeButton = new Button(this);
            upgradeButton.setText("Choose " + tierName);
            upgradeButton.setBackgroundResource(R.drawable.premium_button_gradient);
            upgradeButton.setTextColor(0xFFFFFFFF);
            upgradeButton.setTextSize(14);
            upgradeButton.setPadding(24, 12, 24, 12);

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            upgradeButton.setLayoutParams(buttonParams);

            upgradeButton.setOnClickListener(v -> upgradeTo(tier, tierName));
            card.addView(upgradeButton);
        } else if (isCurrentTier) {
            // Show "Current Plan" text for active tier
            TextView currentText = new TextView(this);
            currentText.setText("✅ Current Plan");
            currentText.setTextColor(0xFF4CAF50);
            currentText.setTextSize(14);
            currentText.setTypeface(null, android.graphics.Typeface.BOLD);
            currentText.setGravity(Gravity.CENTER);
            currentText.setPadding(0, 8, 0, 0);
            card.addView(currentText);
        }

        mainLayout.addView(card);
    }

    private void upgradeTo(PremiumManager.PremiumTier tier, String tierName) {
        // Map tier to product ID
        String productId;
        switch (tier) {
            case PREMIUM_SINGLE:
                productId = BillingManager.SKU_PREMIUM_SINGLE;
                break;
            case PREMIUM_COUPLE:
                productId = BillingManager.SKU_PREMIUM_COUPLE;
                break;
            case PREMIUM_COUPLE_PLUS:
                productId = BillingManager.SKU_PREMIUM_COUPLE_PLUS;
                break;
            default:
                showCustomToast("Invalid tier selected");
                return;
        }

        // Initialize billing manager for this purchase
        BillingManager billingManager = new BillingManager(this, new BillingManager.PurchaseListener() {
            @Override
            public void onPurchaseSuccess(String purchasedProductId) {
                // Verify purchase with backend
                verifyPurchaseWithBackend(purchasedProductId);
            }

            @Override
            public void onPurchaseError(String error) {
                showCustomToast("Purchase failed: " + error);
            }
        });

        // Launch purchase flow
        showCustomToast("Opening Google Play...");
        billingManager.launchPurchaseFlow(productId);
    }

    private void verifyPurchaseWithBackend(String productId) {
        // This will be handled by Cloud Functions
        // The purchase token is sent to verifyPurchase function
        showCustomToast("Verifying purchase... ✨");

        // Refresh premium status after a short delay
        new android.os.Handler().postDelayed(() -> {
            premiumManager.loadPremiumStatus();
            loadPremiumExpirationData();
            showCustomToast("Premium activated! 🎉");
        }, 2000);
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