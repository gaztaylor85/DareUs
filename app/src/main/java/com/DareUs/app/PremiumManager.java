package com.DareUs.app;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class PremiumManager {

    public enum PremiumTier {
        FREE,
        PREMIUM_SINGLE,      // $2.99 - unlimited sends for you only
        PREMIUM_COUPLE,      // $3.99 - unlimited sends for both partners
        PREMIUM_COUPLE_PLUS  // $4.99 - unlimited + custom dares for both
    }

    private FirebaseFirestore db;
    private String userId;
    private PremiumTier currentTier;
    private PremiumTier partnerTier;
    private String partnerId;
    private PremiumStatusListener listener;

    public interface PremiumStatusListener {
        void onPremiumStatusChanged(PremiumTier tier);
        void onUsageLimitReached(String category);
    }

    public PremiumManager(String userId, PremiumStatusListener listener) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        this.listener = listener;
        this.currentTier = PremiumTier.FREE; // Default to free
        this.partnerTier = PremiumTier.FREE; // Default to free
        loadPremiumStatus();
    }

    public void loadPremiumStatus() {
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Load user's tier
                        String tierString = doc.getString("premiumTier");
                        Long expiresAt = doc.getLong("premiumExpiresAt");
                        this.partnerId = doc.getString("partnerId");

                        if (tierString != null && expiresAt != null && expiresAt > System.currentTimeMillis()) {
                            try {
                                currentTier = PremiumTier.valueOf(tierString);
                            } catch (IllegalArgumentException e) {
                                currentTier = PremiumTier.FREE;
                            }
                        } else {
                            currentTier = PremiumTier.FREE;
                        }

                        Log.d("PremiumManager", "User tier: " + currentTier.name());

                        // Load partner's tier if they have a partner
                        if (partnerId != null && !partnerId.isEmpty()) {
                            loadPartnerPremiumStatus();
                        } else {
                            // No partner, notify with current tier
                            if (listener != null) {
                                listener.onPremiumStatusChanged(currentTier);
                            }
                        }
                    } else {
                        currentTier = PremiumTier.FREE;
                        if (listener != null) {
                            listener.onPremiumStatusChanged(currentTier);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PremiumManager", "Error loading premium status", e);
                    currentTier = PremiumTier.FREE;
                    if (listener != null) {
                        listener.onPremiumStatusChanged(currentTier);
                    }
                });
    }

    private void loadPartnerPremiumStatus() {
        if (partnerId == null || partnerId.isEmpty()) {
            partnerTier = PremiumTier.FREE;
            if (listener != null) {
                listener.onPremiumStatusChanged(currentTier);
            }
            return;
        }

        db.collection("dareus").document(partnerId)
                .get()
                .addOnSuccessListener(partnerDoc -> {
                    if (partnerDoc.exists()) {
                        String partnerTierString = partnerDoc.getString("premiumTier");
                        Long partnerExpiresAt = partnerDoc.getLong("premiumExpiresAt");

                        if (partnerTierString != null && partnerExpiresAt != null && partnerExpiresAt > System.currentTimeMillis()) {
                            try {
                                partnerTier = PremiumTier.valueOf(partnerTierString);
                            } catch (IllegalArgumentException e) {
                                partnerTier = PremiumTier.FREE;
                            }
                        } else {
                            partnerTier = PremiumTier.FREE;
                        }

                        Log.d("PremiumManager", "Partner tier: " + partnerTier.name());
                    } else {
                        partnerTier = PremiumTier.FREE;
                    }

                    if (listener != null) {
                        listener.onPremiumStatusChanged(getEffectiveTier());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PremiumManager", "Error loading partner premium status", e);
                    partnerTier = PremiumTier.FREE;
                    if (listener != null) {
                        listener.onPremiumStatusChanged(currentTier);
                    }
                });
    }

    public boolean canSendDare(String category) {
        if (isPremiumUser()) {
            return true;
        }

        // For free users, check usage limits
        checkFreeUsageLimit(category);
        return true; // Allow attempt, limits enforced in checkFreeUsageLimit via listener
    }

    private void checkFreeUsageLimit(String category) {
        // Get current week's usage (Monday to Sunday)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long weekStart = cal.getTimeInMillis();

        Log.d("PremiumManager", "Checking usage since: " + new java.util.Date(weekStart));

        db.collection("dares")
                .whereEqualTo("fromUserId", userId)
                .whereGreaterThan("sentAt", weekStart)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int sweetCount = 0;
                    int playfulCount = 0;
                    int otherCount = 0;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String dareCategory = doc.getString("category");
                        if ("Sweet".equals(dareCategory)) {
                            sweetCount++;
                        } else if ("Playful".equals(dareCategory)) {
                            playfulCount++;
                        } else if (!"Custom".equals(dareCategory)) { // Don't count custom dares
                            otherCount++;
                        }
                    }

                    Log.d("PremiumManager", "Usage this week - Sweet: " + sweetCount + ", Playful: " + playfulCount + ", Other: " + otherCount);

                    boolean canSend = false;
                    String limitMessage = "";

                    if ("Sweet".equals(category)) {
                        if (sweetCount < 5) {
                            canSend = true;
                        } else {
                            limitMessage = "You've reached your weekly limit of 5 Sweet dares. Upgrade to Premium for unlimited access!";
                        }
                    } else if ("Playful".equals(category)) {
                        if (playfulCount < 5) {
                            canSend = true;
                        } else {
                            limitMessage = "You've reached your weekly limit of 5 Playful dares. Upgrade to Premium for unlimited access!";
                        }
                    } else {
                        // Adventure, Passionate, Wild categories
                        if (otherCount < 1) {
                            canSend = true;
                        } else {
                            limitMessage = "You've reached your weekly limit of 1 " + category + " dare. Upgrade to Premium for unlimited access!";
                        }
                    }

                    if (!canSend && listener != null) {
                        Log.d("PremiumManager", "Limit reached for " + category + ": " + limitMessage);
                        listener.onUsageLimitReached(category);
                    } else {
                        Log.d("PremiumManager", "Can send " + category + " dare");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PremiumManager", "Error checking usage limits", e);
                    // On error, allow the dare to be sent
                });
    }

    public boolean isPremiumUser() {
        PremiumTier effectiveTier = getEffectiveTier();
        return effectiveTier != PremiumTier.FREE;
    }

    public boolean hasCustomDares() {
        PremiumTier effectiveTier = getEffectiveTier();
        // Only Premium Couple Plus has custom dares
        return effectiveTier == PremiumTier.PREMIUM_COUPLE_PLUS;
    }
    /**
     * Get the effective premium tier considering both user and partner subscriptions
     */
    private PremiumTier getEffectiveTier() {
        // If user has any premium, use their tier
        if (currentTier != PremiumTier.FREE) {
            return currentTier;
        }

        // If partner has couples subscription, user benefits from it
        if (partnerTier == PremiumTier.PREMIUM_COUPLE || partnerTier == PremiumTier.PREMIUM_COUPLE_PLUS) {
            return partnerTier;
        }

        // Both are free
        return PremiumTier.FREE;
    }

    public PremiumTier getCurrentTier() {
        return currentTier;
    }

    public String getTierDisplayName() {
        PremiumTier effectiveTier = getEffectiveTier();
        switch (effectiveTier) {
            case PREMIUM_SINGLE: return "Premium Single";
            case PREMIUM_COUPLE: return "Premium Couple";
            case PREMIUM_COUPLE_PLUS: return "Premium Couple Plus";
            default: return "Free";
        }
    }

    public String getTierDescription() {
        PremiumTier effectiveTier = getEffectiveTier();

        // Add indicator if benefits come from partner
        String source = "";
        if (currentTier == PremiumTier.FREE && partnerTier != PremiumTier.FREE) {
            source = " (via partner)";
        }

        switch (effectiveTier) {
            case PREMIUM_SINGLE:
                return "Unlimited dares from all categories" + source;
            case PREMIUM_COUPLE:
                return "Unlimited dares for both partners" + source;
            case PREMIUM_COUPLE_PLUS:
                return "Unlimited dares + Custom dares for both partners" + source;
            default:
                return "5 Sweet/Playful dares + 1 other per week";
        }
    }

    public void upgradeToTier(PremiumTier tier) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("premiumTier", tier.name());
        updates.put("premiumExpiresAt", System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // 30 days
        updates.put("premiumPurchasedAt", System.currentTimeMillis());

        db.collection("dareus").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    currentTier = tier;
                    Log.d("PremiumManager", "Upgraded to: " + tier.name());
                    if (listener != null) {
                        listener.onPremiumStatusChanged(tier);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PremiumManager", "Error upgrading tier", e);
                });
    }

    /**
     * For debugging - get partner tier
     */
    public PremiumTier getPartnerTier() {
        return partnerTier;
    }
}