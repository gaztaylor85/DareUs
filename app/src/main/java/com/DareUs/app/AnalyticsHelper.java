package com.DareUs.app;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Helper class for Firebase Analytics tracking
 * Tracks user behavior, engagement, and premium conversion metrics
 */
public class AnalyticsHelper {

    private static final String TAG = "AnalyticsHelper";
    private static FirebaseAnalytics analytics;

    /**
     * Initialize analytics (call from Application or MainActivity)
     */
    public static void initialize(Context context) {
        if (analytics == null) {
            analytics = FirebaseAnalytics.getInstance(context);
            Log.d(TAG, "Firebase Analytics initialized");
        }
    }

    // ========================================
    // USER EVENTS
    // ========================================

    public static void logUserLogin(String method) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.METHOD, method);
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN, params);
    }

    public static void logUserSignUp(String method) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.METHOD, method);
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params);
    }

    public static void logProfileSetup(String firstName) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("profile_name", firstName);
        analytics.logEvent("profile_setup_complete", params);
    }

    // ========================================
    // PARTNER LINKING EVENTS
    // ========================================

    public static void logPartnerLinkInitiated() {
        if (analytics == null) return;
        analytics.logEvent("partner_link_initiated", new Bundle());
    }

    public static void logPartnerLinkSuccess() {
        if (analytics == null) return;
        analytics.logEvent("partner_link_success", new Bundle());
    }

    public static void logInviteCodeGenerated() {
        if (analytics == null) return;
        analytics.logEvent("invite_code_generated", new Bundle());
    }

    // ========================================
    // DARE EVENTS
    // ========================================

    public static void logDareSent(String category, int points, boolean isCustom) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("dare_category", category);
        params.putInt("dare_points", points);
        params.putBoolean("is_custom", isCustom);
        analytics.logEvent("dare_sent", params);
    }

    public static void logDareCompleted(String category, int points, int daysToComplete, boolean gotBonus) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("dare_category", category);
        params.putInt("dare_points", points);
        params.putInt("days_to_complete", daysToComplete);
        params.putBoolean("got_bonus", gotBonus);
        analytics.logEvent("dare_completed", params);
    }

    public static void logDareRejected(String category) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("dare_category", category);
        analytics.logEvent("dare_rejected", params);
    }

    public static void logDareNegotiationStarted() {
        if (analytics == null) return;
        analytics.logEvent("dare_negotiation_started", new Bundle());
    }

    // ========================================
    // BADGE EVENTS
    // ========================================

    public static void logBadgeUnlocked(String badgeId, String badgeName) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("badge_id", badgeId);
        params.putString("badge_name", badgeName);
        params.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, badgeId);
        analytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, params);
    }

    public static void logBadgesViewed(int totalBadges, int unlockedBadges) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("total_badges", totalBadges);
        params.putInt("unlocked_badges", unlockedBadges);
        params.putDouble("unlock_percentage", (unlockedBadges * 100.0 / totalBadges));
        analytics.logEvent("badges_viewed", params);
    }

    // ========================================
    // COMPETITION EVENTS
    // ========================================

    public static void logLeaderboardViewed(int myPoints, int partnerPoints) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("my_points", myPoints);
        params.putInt("partner_points", partnerPoints);
        params.putInt("point_difference", Math.abs(myPoints - partnerPoints));
        params.putBoolean("is_winning", myPoints > partnerPoints);
        analytics.logEvent("leaderboard_viewed", params);
    }

    public static void logMonthlyWin(int myPoints, int partnerPoints) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("winning_points", myPoints);
        params.putInt("losing_points", partnerPoints);
        params.putInt("victory_margin", myPoints - partnerPoints);
        analytics.logEvent("monthly_competition_won", params);
    }

    public static void logPrizeSet(String prize) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("prize_description", prize.substring(0, Math.min(100, prize.length())));
        analytics.logEvent("prize_set", params);
    }

    public static void logPrizeRevealed(int pointsCost) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("points_spent", pointsCost);
        analytics.logEvent("prize_revealed", params);
    }

    // ========================================
    // PREMIUM / MONETIZATION EVENTS
    // ========================================

    public static void logPremiumScreenViewed() {
        if (analytics == null) return;
        analytics.logEvent("premium_screen_viewed", new Bundle());
    }

    public static void logPremiumPurchaseInitiated(String tier, double price) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("premium_tier", tier);
        params.putDouble(FirebaseAnalytics.Param.VALUE, price);
        params.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
        analytics.logEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, params);
    }

    public static void logPremiumPurchaseSuccess(String tier, double price) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("premium_tier", tier);
        params.putDouble(FirebaseAnalytics.Param.VALUE, price);
        params.putString(FirebaseAnalytics.Param.CURRENCY, "USD");
        params.putString(FirebaseAnalytics.Param.TRANSACTION_ID, String.valueOf(System.currentTimeMillis()));
        analytics.logEvent(FirebaseAnalytics.Event.PURCHASE, params);
    }

    public static void logPremiumPurchaseFailure(String tier, String error) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("premium_tier", tier);
        params.putString("error_message", error);
        analytics.logEvent("premium_purchase_failed", params);
    }

    public static void logRateLimitHit(String type, int limit) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("limit_type", type);
        params.putInt("limit_value", limit);
        analytics.logEvent("rate_limit_hit", params);
    }

    // ========================================
    // ENGAGEMENT EVENTS
    // ========================================

    public static void logScreenView(String screenName, String screenClass) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass);
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
    }

    public static void logStreakMilestone(int streakDays) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("streak_days", streakDays);
        analytics.logEvent("streak_milestone", params);
    }

    public static void logPointsMilestone(int totalPoints) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putInt("total_points", totalPoints);
        analytics.logEvent("points_milestone", params);
    }

    // ========================================
    // ERROR TRACKING
    // ========================================

    public static void logError(String errorType, String errorMessage) {
        if (analytics == null) return;
        Bundle params = new Bundle();
        params.putString("error_type", errorType);
        params.putString("error_message", errorMessage.substring(0, Math.min(100, errorMessage.length())));
        analytics.logEvent("app_error", params);
    }

    // ========================================
    // USER PROPERTIES
    // ========================================

    public static void setUserProperty(String name, String value) {
        if (analytics == null) return;
        analytics.setUserProperty(name, value);
    }

    public static void setUserId(String userId) {
        if (analytics == null) return;
        analytics.setUserId(userId);
    }

    public static void setPremiumTier(String tier) {
        setUserProperty("premium_tier", tier);
    }

    public static void setHasPartner(boolean hasPartner) {
        setUserProperty("has_partner", hasPartner ? "true" : "false");
    }
}
