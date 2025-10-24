package com.DareUs.app;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgeTracker {

    private FirebaseFirestore db;
    private String userId;
    private BadgeUnlockListener listener;

    public interface BadgeUnlockListener {
        void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge);
    }

    public BadgeTracker(String userId, BadgeUnlockListener listener) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        this.listener = listener;
    }

    // Check for badge unlocks after completing a dare
    public void checkDareCompletion(String category, long completedAt, long sentAt, int points) {
        // Calculate completion time
        long completionTime = completedAt - sentAt;
        int daysElapsed = (int) (completionTime / (24 * 60 * 60 * 1000)) + 1; // +1 so day 0 becomes day 1

        // Update daily streak FIRST
        updateDailyStreak(completedAt);

        // Check if this is first dare ever
        checkFirstDareCompleted();

        // Check speed badges for all fast completions (1-2 days)
        if (daysElapsed <= 2) {
            checkSpeedBadges();
        }

        // Check category badges
        checkCategoryBadges(category);

        // Check time-based badges
        checkTimeBadges(completedAt);

        // Check milestone badges
        checkMilestoneBadges(0);

        // Check partnership badges (including Power Couple)
        checkPartnershipBadges();

        // Check partnership timing badges
        checkPartnershipTimingBadges(completedAt);

        // Update user stats
        updateUserStats("daresCompleted", 1);
        updateCategoryStats(category, 1);
    }

    private void checkPartnershipBadges() {
        // Get current user's data to find partner
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String partnerId = doc.getString("partnerId");
                        if (partnerId != null && !partnerId.isEmpty()) {
                            // Load both user points to check Power Couple badge
                            checkPowerCouplePoints(partnerId);

                            // Load partner data for other partnership badges
                            checkOtherPartnershipBadges(partnerId);
                        }
                    }
                });
    }

    private void checkPowerCouplePoints(String partnerId) {
        // Get my points
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(myDoc -> {
                    if (myDoc.exists()) {
                        Long myPoints = myDoc.getLong("points");

                        // Get partner points
                        db.collection("dareus").document(partnerId)
                                .get()
                                .addOnSuccessListener(partnerDoc -> {
                                    if (partnerDoc.exists()) {
                                        Long partnerPoints = partnerDoc.getLong("points");

                                        int myTotalPoints = myPoints != null ? myPoints.intValue() : 0;
                                        int partnerTotalPoints = partnerPoints != null ? partnerPoints.intValue() : 0;
                                        int combinedPoints = myTotalPoints + partnerTotalPoints;

                                        Log.d("BadgeTracker", "Power Couple check: My points=" + myTotalPoints +
                                                ", Partner points=" + partnerTotalPoints +
                                                ", Combined=" + combinedPoints);

                                        // Check Power Couple badge (1000+ combined points)
                                        if (combinedPoints >= 1000 && !hasBadge("power_couple")) {
                                            unlockBadge("power_couple");
                                        }
                                    }
                                });
                    }
                });
    }

    private void checkOtherPartnershipBadges(String partnerId) {
        // Get both users' streak data for Dynamic Duo badge
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(myDoc -> {
                    if (myDoc.exists()) {
                        Long myStreak = myDoc.getLong("streakCount");

                        db.collection("dareus").document(partnerId)
                                .get()
                                .addOnSuccessListener(partnerDoc -> {
                                    if (partnerDoc.exists()) {
                                        Long partnerStreak = partnerDoc.getLong("streakCount");

                                        int myStreakCount = myStreak != null ? myStreak.intValue() : 0;
                                        int partnerStreakCount = partnerStreak != null ? partnerStreak.intValue() : 0;

                                        // Check Dynamic Duo badge (both have 7+ day streaks)
                                        if (myStreakCount >= 7 && partnerStreakCount >= 7 && !hasBadge("dynamic_duo")) {
                                            unlockBadge("dynamic_duo");
                                        }
                                    }
                                });
                    }
                });
    }

    private void checkFirstDareCompleted() {
        // Check if user already has first_steps badge
        if (!hasBadge("first_steps")) {
            // Count total completed dares
            db.collection("dares")
                    .whereEqualTo("toUserId", userId)
                    .whereEqualTo("status", "completed")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.size() == 1) {
                            // This is their first completion
                            unlockBadge("first_steps");
                        }
                    });
        }
    }

    private void checkPartnershipTimingBadges(long completedAt) {
        // Get partner ID first
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String partnerId = doc.getString("partnerId");
                        if (partnerId != null && !partnerId.isEmpty()) {
                            checkPerfectMatchBadge(partnerId, completedAt);
                            checkSynchronizedSoulsBadge(partnerId, completedAt);
                        }
                    }
                });
    }

    private void checkPerfectMatchBadge(String partnerId, long myCompletedAt) {
        // Check if partner completed any dare on the same day
        Calendar myCal = Calendar.getInstance();
        myCal.setTimeInMillis(myCompletedAt);
        myCal.set(Calendar.HOUR_OF_DAY, 0);
        myCal.set(Calendar.MINUTE, 0);
        myCal.set(Calendar.SECOND, 0);
        myCal.set(Calendar.MILLISECOND, 0);
        long startOfDay = myCal.getTimeInMillis();
        long endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1;

        db.collection("dares")
                .whereEqualTo("toUserId", partnerId)
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("completedAt", startOfDay)
                .whereLessThanOrEqualTo("completedAt", endOfDay)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Partner completed dare same day!
                        onSameDayCompletion();
                    }
                });
    }

    private void checkSynchronizedSoulsBadge(String partnerId, long myCompletedAt) {
        // Check if partner completed dare within 1 hour
        long oneHourBefore = myCompletedAt - (60 * 60 * 1000);
        long oneHourAfter = myCompletedAt + (60 * 60 * 1000);

        db.collection("dares")
                .whereEqualTo("toUserId", partnerId)
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("completedAt", oneHourBefore)
                .whereLessThanOrEqualTo("completedAt", oneHourAfter)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Partner completed dare within 1 hour!
                        onSynchronizedCompletion();
                    }
                });
    }

    // Method to check for competition badges when viewing leaderboard
    public void checkCompetitionBadges(int myMonthlyPoints, int partnerMonthlyPoints) {
        // Track participation
        onCompetitionParticipation();

        // Check if I won this month
        if (myMonthlyPoints > partnerMonthlyPoints) {
            // I won! Check win-related badges
            onMonthlyWin(myMonthlyPoints, partnerMonthlyPoints);

            // Check consecutive wins
            checkConsecutiveWinCount();
        } else if (myMonthlyPoints == partnerMonthlyPoints && myMonthlyPoints > 0) {
            // Tie result
            onTiedResult();
        }

        // Check advanced competition badges with historical data
        checkFinalHourBadge(myMonthlyPoints, partnerMonthlyPoints);
        checkEarlyBirdWinnerBadge(myMonthlyPoints, partnerMonthlyPoints);
        checkComebackKidBadge(myMonthlyPoints, partnerMonthlyPoints);
    }

    /**
     * Final Hour Badge: Take the lead in the last day of competition
     * Checks if user won but was behind 24 hours before month end
     */
    private void checkFinalHourBadge(int myMonthlyPoints, int partnerMonthlyPoints) {
        if (myMonthlyPoints <= partnerMonthlyPoints || hasBadge("final_hour")) {
            return; // Only for winners who don't already have the badge
        }

        String currentMonthCode = getCurrentMonthCode();

        // Get the current month's competition document
        db.collection("monthlyCompetitions")
                .whereEqualTo("coupleId", getCurrentCoupleId())
                .whereEqualTo("monthCode", currentMonthCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String user1Id = doc.getString("user1Id");

                        // Check scores from 24 hours before month end
                        // We'll store daily snapshots in a subcollection called "dailySnapshots"
                        doc.getReference()
                                .collection("dailySnapshots")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                .limit(2) // Get last 2 days
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    if (snapshots.size() >= 2) {
                                        // Get second-to-last day (24 hours before end)
                                        com.google.firebase.firestore.DocumentSnapshot yesterdaySnapshot = snapshots.getDocuments().get(1);
                                        Long user1PointsYesterday = yesterdaySnapshot.getLong("user1Points");
                                        Long user2PointsYesterday = yesterdaySnapshot.getLong("user2Points");

                                        if (user1PointsYesterday != null && user2PointsYesterday != null) {
                                            boolean wasBehindYesterday;
                                            if (userId.equals(user1Id)) {
                                                wasBehindYesterday = user1PointsYesterday < user2PointsYesterday;
                                            } else {
                                                wasBehindYesterday = user2PointsYesterday < user1PointsYesterday;
                                            }

                                            if (wasBehindYesterday) {
                                                // Was behind yesterday but won today! Final hour victory!
                                                unlockBadge("final_hour");
                                            }
                                        }
                                    }
                                });
                    }
                });
    }

    /**
     * Early Bird Winner Badge: Lead the entire month and win
     * Checks if user was ahead every day of the month
     */
    private void checkEarlyBirdWinnerBadge(int myMonthlyPoints, int partnerMonthlyPoints) {
        if (myMonthlyPoints <= partnerMonthlyPoints || hasBadge("early_bird_winner")) {
            return; // Only for winners
        }

        String currentMonthCode = getCurrentMonthCode();

        db.collection("monthlyCompetitions")
                .whereEqualTo("coupleId", getCurrentCoupleId())
                .whereEqualTo("monthCode", currentMonthCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String user1Id = doc.getString("user1Id");

                        // Check all daily snapshots to see if always ahead
                        doc.getReference()
                                .collection("dailySnapshots")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    boolean alwaysAhead = true;

                                    for (com.google.firebase.firestore.DocumentSnapshot snapshot : snapshots) {
                                        Long user1Points = snapshot.getLong("user1Points");
                                        Long user2Points = snapshot.getLong("user2Points");

                                        if (user1Points != null && user2Points != null && user1Points > 0 && user2Points > 0) {
                                            boolean wasAhead;
                                            if (userId.equals(user1Id)) {
                                                wasAhead = user1Points >= user2Points;
                                            } else {
                                                wasAhead = user2Points >= user1Points;
                                            }

                                            if (!wasAhead) {
                                                alwaysAhead = false;
                                                break;
                                            }
                                        }
                                    }

                                    if (alwaysAhead && snapshots.size() >= 3) {
                                        // Led entire month (at least 3 days tracked)
                                        unlockBadge("early_bird_winner");
                                    }
                                });
                    }
                });
    }

    /**
     * Comeback Kid Badge: Win after being behind by 50+ points
     * Checks if user was ever 50+ points behind during the month
     */
    private void checkComebackKidBadge(int myMonthlyPoints, int partnerMonthlyPoints) {
        if (myMonthlyPoints <= partnerMonthlyPoints || hasBadge("comeback_kid")) {
            return; // Only for winners
        }

        String currentMonthCode = getCurrentMonthCode();

        db.collection("monthlyCompetitions")
                .whereEqualTo("coupleId", getCurrentCoupleId())
                .whereEqualTo("monthCode", currentMonthCode)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String user1Id = doc.getString("user1Id");

                        // Check if ever had a deficit of 50+ points
                        doc.getReference()
                                .collection("dailySnapshots")
                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .get()
                                .addOnSuccessListener(snapshots -> {
                                    boolean hadBigDeficit = false;

                                    for (com.google.firebase.firestore.DocumentSnapshot snapshot : snapshots) {
                                        Long user1Points = snapshot.getLong("user1Points");
                                        Long user2Points = snapshot.getLong("user2Points");

                                        if (user1Points != null && user2Points != null) {
                                            int deficit;
                                            if (userId.equals(user1Id)) {
                                                deficit = user2Points.intValue() - user1Points.intValue();
                                            } else {
                                                deficit = user1Points.intValue() - user2Points.intValue();
                                            }

                                            if (deficit >= 50) {
                                                hadBigDeficit = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (hadBigDeficit) {
                                        // Was 50+ points behind but came back to win!
                                        unlockBadge("comeback_kid");
                                    }
                                });
                    }
                });
    }

    private void checkConsecutiveWinCount() {
        // Check historical monthly competitions to count real consecutive wins
        String currentMonthCode = getCurrentMonthCode();

        db.collection("monthlyCompetitions")
                .whereEqualTo("coupleId", getCurrentCoupleId())
                .orderBy("monthCode", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(12) // Check last 12 months
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int consecutiveWins = 0;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String monthCode = doc.getString("monthCode");
                        Long user1Points = doc.getLong("user1Points");
                        Long user2Points = doc.getLong("user2Points");
                        String user1Id = doc.getString("user1Id");

                        if (user1Points == null || user2Points == null) continue;

                        // Check if I won this month
                        boolean iWon = false;
                        if (userId.equals(user1Id)) {
                            iWon = user1Points > user2Points;
                        } else {
                            iWon = user2Points > user1Points;
                        }

                        if (iWon) {
                            consecutiveWins++;
                        } else {
                            break; // Streak broken
                        }
                    }

                    // Award badges based on actual consecutive wins
                    if (consecutiveWins >= 3 && !hasBadge("hat_trick")) {
                        unlockBadge("hat_trick");
                    }
                    if (consecutiveWins >= 5 && !hasBadge("rivalry_master")) {
                        unlockBadge("rivalry_master");
                    }
                });
    }

    private String getCurrentMonthCode() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return cal.get(java.util.Calendar.YEAR) + "-" + (cal.get(java.util.Calendar.MONTH) + 1);
    }

    private String getCurrentCoupleId() {
        // You'll need to implement this to get the couple ID
        // For now, return a placeholder
        return userId + "_couple"; // Implement proper couple ID logic
    }
    // FIXED: Calculate and update daily completion streak
    // FIXED: Calculate and update daily completion streak
    public void updateDailyStreak(long completedAt) {
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long lastCompletionTime = doc.getLong("lastCompletionDate");
                        Long currentStreak = doc.getLong("streakCount");

                        int streak = currentStreak != null ? currentStreak.intValue() : 0;

                        // Get today's date (midnight)
                        Calendar today = Calendar.getInstance();
                        today.setTimeInMillis(completedAt);
                        today.set(Calendar.HOUR_OF_DAY, 0);
                        today.set(Calendar.MINUTE, 0);
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);
                        long todayMidnight = today.getTimeInMillis();

                        // FIXED: Calculate final streak value
                        final int finalStreak;
                        if (lastCompletionTime != null) {
                            // Get last completion date (midnight)
                            Calendar lastCompletion = Calendar.getInstance();
                            lastCompletion.setTimeInMillis(lastCompletionTime);
                            lastCompletion.set(Calendar.HOUR_OF_DAY, 0);
                            lastCompletion.set(Calendar.MINUTE, 0);
                            lastCompletion.set(Calendar.SECOND, 0);
                            lastCompletion.set(Calendar.MILLISECOND, 0);
                            long lastMidnight = lastCompletion.getTimeInMillis();

                            long daysDifference = (todayMidnight - lastMidnight) / (24 * 60 * 60 * 1000);

                            if (daysDifference == 0) {
                                // Same day - don't change streak
                                Log.d("BadgeTracker", "Same day completion - streak unchanged: " + streak);
                                return;
                            } else if (daysDifference == 1) {
                                // Yesterday - increment streak
                                finalStreak = streak + 1;
                                Log.d("BadgeTracker", "Consecutive day - streak increased to: " + finalStreak);
                            } else {
                                // Missed days - reset streak to 1
                                finalStreak = 1;
                                Log.d("BadgeTracker", "Streak broken - reset to 1");
                            }
                        } else {
                            // First ever completion
                            finalStreak = 1;
                            Log.d("BadgeTracker", "First completion - streak set to 1");
                        }

                        // Update both streak and last completion date
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("streakCount", finalStreak);
                        updates.put("lastCompletionDate", completedAt);

                        db.collection("dareus").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("BadgeTracker", "✅ Streak updated to " + finalStreak);
                                    // Check streak badges with new value
                                    checkStreakBadgesWithValue(finalStreak);
                                });
                    }
                });
    }

    // Helper method to check streak badges with specific value
    private void checkStreakBadgesWithValue(int streak) {
        if (streak >= 3 && !hasBadge("warm_up")) {
            unlockBadge("warm_up");
        }
        if (streak >= 7 && !hasBadge("getting_hot")) {
            unlockBadge("getting_hot");
        }
        if (streak >= 14 && !hasBadge("on_fire")) {
            unlockBadge("on_fire");
        }
        if (streak >= 30 && !hasBadge("blazing")) {
            unlockBadge("blazing");
        }
        if (streak >= 60 && !hasBadge("inferno")) {
            unlockBadge("inferno");
        }
    }

    // Check for badge unlocks after sending a dare
    public void checkDareSent(String category) {
        checkSenderBadges();
        updateUserStats("daresSent", 1);
    }

    private void checkSpeedBadges() {
        // Get all completed dares to analyze timing patterns
        db.collection("dares")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "completed")
                .orderBy("completedAt")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int day1Completions = 0;
                    int first2DayCompletions = 0;
                    
                    // For time-based badges, collect completion timestamps
                    List<Long> completionTimes = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Long sentAt = doc.getLong("sentAt");
                        Long completedAt = doc.getLong("completedAt");

                        if (sentAt != null && completedAt != null) {
                            completionTimes.add(completedAt);
                            
                            // Calculate days elapsed from sent to completed
                            long elapsed = completedAt - sentAt;
                            int daysElapsed = (int) (elapsed / (24 * 60 * 60 * 1000)) + 1;

                            // Count by speed of completion (for flash_forward and speed_racer)
                            if (daysElapsed == 1) {
                                day1Completions++;
                            }
                            if (daysElapsed <= 2) {
                                first2DayCompletions++;
                            }
                        }
                    }

                    // Check for Lightning Lover: 5 dares within any 24-hour window
                    int maxIn24Hours = findMaxCompletionsInWindow(completionTimes, 24 * 60 * 60 * 1000L);
                    
                    // Check for Instant Gratification: 15 dares within any 48-hour window
                    int maxIn48Hours = findMaxCompletionsInWindow(completionTimes, 48 * 60 * 60 * 1000L);

                    Log.d("BadgeTracker", "Speed stats - Max24h: " + maxIn24Hours +
                            ", Max48h: " + maxIn48Hours +
                            ", Day1: " + day1Completions +
                            ", First2Days: " + first2DayCompletions);

                    // LIGHTNING LOVER: 5 dares completed within any 24-hour window
                    if (maxIn24Hours >= 5 && !hasBadge("lightning_lover")) {
                        unlockBadge("lightning_lover");
                    }

                    // FLASH FORWARD: 10 dares completed on Day 1 (same day sent)
                    if (day1Completions >= 10 && !hasBadge("flash_forward")) {
                        unlockBadge("flash_forward");
                    }

                    // SPEED RACER: 10 dares completed within first 2 days
                    if (first2DayCompletions >= 10 && !hasBadge("speed_racer")) {
                        unlockBadge("speed_racer");
                    }

                    // INSTANT GRATIFICATION: 15 dares completed within any 48-hour window
                    if (maxIn48Hours >= 15 && !hasBadge("instant_gratification")) {
                        unlockBadge("instant_gratification");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BadgeTracker", "Error checking speed badges", e);
                });
    }
    
    private int findMaxCompletionsInWindow(List<Long> completionTimes, long windowMillis) {
        if (completionTimes.size() < 2) return completionTimes.size();
        
        int maxCount = 1;
        
        for (int i = 0; i < completionTimes.size(); i++) {
            long startTime = completionTimes.get(i);
            int count = 1;
            
            for (int j = i + 1; j < completionTimes.size(); j++) {
                if (completionTimes.get(j) - startTime <= windowMillis) {
                    count++;
                } else {
                    break; // Times are sorted, no need to continue
                }
            }
            
            maxCount = Math.max(maxCount, count);
        }
        
        return maxCount;
    }

    private void checkCategoryBadges(String category) {
        db.collection("dares")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", "completed")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();

                    switch (category) {
                        case "Sweet":
                            if (count >= 10 && !hasBadge("sweet_soul")) {
                                unlockBadge("sweet_soul");
                            }
                            break;
                        case "Playful":
                            if (count >= 10 && !hasBadge("playful_spirit")) {
                                unlockBadge("playful_spirit");
                            }
                            break;
                        case "Adventure":
                            if (count >= 10 && !hasBadge("adventure_seeker")) {
                                unlockBadge("adventure_seeker");
                            }
                            break;
                        case "Passionate":
                            if (count >= 10 && !hasBadge("passionate_heart")) {
                                unlockBadge("passionate_heart");
                            }
                            break;
                        case "Wild":
                            if (count >= 10 && !hasBadge("wild_one")) {
                                unlockBadge("wild_one");
                            }
                            break;
                    }

                    // Check renaissance lover badge
                    checkRenaissanceLover();
                });
    }

    private void checkRenaissanceLover() {
        // Check if user has 5+ completions in each category
        String[] categories = {"Sweet", "Playful", "Adventure", "Passionate", "Wild"};
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (String category : categories) {
            db.collection("dares")
                    .whereEqualTo("toUserId", userId)
                    .whereEqualTo("status", "completed")
                    .whereEqualTo("category", category)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        categoryCounts.put(category, querySnapshot.size());

                        if (categoryCounts.size() == 5) {
                            boolean allAbove5 = true;
                            for (int count : categoryCounts.values()) {
                                if (count < 5) {
                                    allAbove5 = false;
                                    break;
                                }
                            }

                            if (allAbove5 && !hasBadge("renaissance_lover")) {
                                unlockBadge("renaissance_lover");
                            }
                        }
                    });
        }
    }

    private void checkTimeBadges(long completedAt) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(completedAt);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Night Owl badge (after 10 PM)
        if (hour >= 22) {
            updateTimeBadgeCount("nightOwlCount", "night_owl", 5);
        }

        // Early Bird badge (before 8 AM)
        if (hour < 8) {
            updateTimeBadgeCount("earlyBirdCount", "early_bird", 5);
        }

        // Weekend Warrior badge (Saturday or Sunday)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            updateTimeBadgeCount("weekendWarriorCount", "weekend_warrior", 10);
        }
    }

    private void checkMilestoneBadges(int pointsEarned) {
        // Get current total points from Firebase
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long totalPoints = doc.getLong("points");
                        int currentTotal = totalPoints != null ? totalPoints.intValue() : 0;

                        // Check each milestone
                        if (currentTotal >= 100 && !hasBadge("getting_started")) {
                            unlockBadge("getting_started");
                        }
                        if (currentTotal >= 500 && !hasBadge("point_collector")) {
                            unlockBadge("point_collector");
                        }
                        if (currentTotal >= 1000 && !hasBadge("point_master")) {
                            unlockBadge("point_master");
                        }
                        if (currentTotal >= 2500 && !hasBadge("point_legend")) {
                            unlockBadge("point_legend");
                        }
                    }
                });
    }


    private void checkSenderBadges() {
        db.collection("dares")
                .whereEqualTo("fromUserId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();

                    if (count >= 25 && !hasBadge("generous_giver")) {
                        unlockBadge("generous_giver");
                    }
                    if (count >= 50 && !hasBadge("dare_devil")) {
                        unlockBadge("dare_devil");
                    }
                });
    }

    private void updateTimeBadgeCount(String field, String badgeId, int requirement) {
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long count = doc.getLong(field);
                        int currentCount = count != null ? count.intValue() : 0;
                        int newCount = currentCount + 1;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put(field, newCount);

                        db.collection("dareus").document(userId).update(updates);

                        if (newCount >= requirement && !hasBadge(badgeId)) {
                            unlockBadge(badgeId);
                        }
                    }
                });
    }

    private void updateUserStats(String field, int increment) {
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long current = doc.getLong(field);
                        int newValue = (current != null ? current.intValue() : 0) + increment;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put(field, newValue);

                        db.collection("dareus").document(userId).update(updates);
                    }
                });
    }

    private void updateCategoryStats(String category, int increment) {
        String field = category.toLowerCase() + "Completed";
        updateUserStats(field, increment);
    }

    private boolean hasBadge(String badgeId) {
        // Note: This method is used for quick checks, actual verification happens in unlockBadge
        // The unlockBadge method performs the real database check to prevent duplicates
        return false; // Actual check performed in unlockBadge to avoid duplicate badges
    }

    private void unlockBadge(String badgeId) {
        // 🔒 SECURE BADGE UNLOCK via Cloud Functions (awards 50 bonus points + prevents duplication)
        FirebaseFunctions functions = FirebaseFunctions.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("badgeId", badgeId);

        functions.getHttpsCallable("awardBadgeBonus")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    boolean success = (boolean) response.get("success");

                    if (success) {
                        // 🎊 BADGE UNLOCKED! Server validated and awarded points
                        int pointsAwarded = ((Long) response.get("pointsAwarded")).intValue();
                        String message = (String) response.get("message");

                        Log.d("BadgeTracker", "🎉 " + message + " (+" + pointsAwarded + " points)");

                        // 🎊 TRIGGER EPIC CELEBRATION!
                        BadgeSystem.Badge badge = BadgeSystem.BADGES.get(badgeId);
                        if (badge != null && listener != null) {
                            listener.onBadgeUnlocked(badgeId, badge);
                        }

                        // 💝 NOTIFY PARTNER!
                        notifyPartnerOfBadge(badgeId, badge);

                        // ✅ Save badge to userBadges collection for UI display
                        Map<String, Object> badgeData = new HashMap<>();
                        badgeData.put("badgeId", badgeId);
                        badgeData.put("unlockedAt", System.currentTimeMillis());
                        badgeData.put("userId", userId);

                        db.collection("userBadges").add(badgeData);
                    } else {
                        // Badge already unlocked (not an error)
                        Log.d("BadgeTracker", "Badge already exists: " + badgeId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BadgeTracker", "Failed to unlock badge: " + badgeId, e);
                });
    }

    private void notifyPartnerOfBadge(String badgeId, BadgeSystem.Badge badge) {
        if (badge == null) return;

        // Get current user's name and partner ID
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String myName = doc.getString("firstName");
                        String partnerId = doc.getString("partnerId");

                        if (partnerId != null && !partnerId.isEmpty() && myName != null) {
                            // Create badge notification for partner
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("type", "badge_unlock");
                            notification.put("fromUserId", userId);
                            notification.put("toUserId", partnerId);
                            notification.put("badgeId", badgeId);
                            notification.put("badgeName", badge.name);
                            notification.put("badgeEmoji", badge.emoji);
                            notification.put("userName", myName);
                            notification.put("timestamp", System.currentTimeMillis());
                            notification.put("read", false);

                            db.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(notificationRef -> {
                                        Log.d("BadgeTracker", "Partner notified of badge unlock");
                                    });
                        }
                    }
                });
    }

    // 🌟 SPECIAL BADGE TRIGGERS with ENHANCED tracking!
    public void onCodeShared() {
        updateUserStats("codeShares", 1);
        // Check social butterfly badge
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long shares = doc.getLong("codeShares");
                        if (shares != null && shares >= 3) {
                            unlockBadge("social_butterfly");
                        }
                    }
                });
    }

    public void onCodeRegenerated() {
        unlockBadge("second_chance");
    }

    public void onPrizeRevealed() {
        unlockBadge("code_breaker");
    }

    public void onFirstDareCompleted() {
        unlockBadge("first_steps");
    }

    // 🏆 NEW COMPETITION BADGE TRIGGERS!
    public void onMonthlyWin(int myPoints, int partnerPoints) {
        unlockBadge("monthly_champion");

        int difference = Math.abs(myPoints - partnerPoints);
        if (difference <= 5) {
            unlockBadge("close_call");
        } else if (difference >= 100) {
            unlockBadge("dominator");
        }
    }

    public void onComeback(boolean wasTrailing, int finalDifference) {
        if (wasTrailing && finalDifference >= 50) {
            unlockBadge("comeback_kid");
        }
    }

    public void onCompetitionParticipation() {
        updateUserStats("competitionsParticipated", 1);
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long competitions = doc.getLong("competitionsParticipated");
                        if (competitions != null && competitions >= 3) {
                            unlockBadge("competitive_spirit");
                        }
                    }
                });
    }

    public void onConsecutiveWins(int consecutiveWins) {
        if (consecutiveWins >= 3) {
            unlockBadge("hat_trick");
        }
        if (consecutiveWins >= 5) {
            unlockBadge("rivalry_master");
        }
    }

    public void onLastDayTakeover() {
        unlockBadge("final_hour");
    }

    public void onLeadEntireMonth() {
        unlockBadge("early_bird_winner");
    }

    public void onTiedResult() {
        unlockBadge("photo_finish");
    }

    // 🤝 PARTNERSHIP BADGE TRIGGERS!
    public void onSameDayCompletion() {
        updateUserStats("sameDayCompletions", 1);
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long sameDays = doc.getLong("sameDayCompletions");
                        if (sameDays != null && sameDays >= 5) {
                            unlockBadge("perfect_match");
                        }
                    }
                });
    }

    public void onSynchronizedCompletion() {
        updateUserStats("synchronizedCompletions", 1);
        db.collection("dareus").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long synced = doc.getLong("synchronizedCompletions");
                        if (synced != null && synced >= 10) {
                            unlockBadge("synchronized_souls");
                        }
                    }
                });
    }

    public void onCouplePointsMilestone(int combinedPoints) {
        if (combinedPoints >= 1000) {
            unlockBadge("power_couple");
        }
    }

    public void onBothHighStreaks(int myStreak, int partnerStreak) {
        if (myStreak >= 7 && partnerStreak >= 7) {
            unlockBadge("dynamic_duo");
        }
    }
}