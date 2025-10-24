package com.DareUs.app;

import java.util.HashMap;
import java.util.Map;

public class BadgeSystem {

    public static final Map<String, Badge> BADGES = new HashMap<>();

    static {
        // Speed Demon Badges
        BADGES.put("lightning_lover", new Badge("⚡", "Lightning Lover",
                "Complete 5 dares within 24 hours", "speed", 5));
        BADGES.put("flash_forward", new Badge("🏃‍♂️", "Flash Forward",
                "Complete 10 dares on Day 1 (100% bonus)", "speed", 10));
        BADGES.put("speed_racer", new Badge("🏎️", "Speed Racer",
                "Complete 10 dares within first 2 days", "speed", 10));
        BADGES.put("instant_gratification", new Badge("💨", "Instant Gratification",
                "Complete 15 dares within 48 hours", "speed", 15));

        // Category Master Badges
        BADGES.put("sweet_soul", new Badge("🍯", "Sweet Soul",
                "Complete 10 Sweet dares", "category_sweet", 10));
        BADGES.put("playful_spirit", new Badge("🎭", "Playful Spirit",
                "Complete 10 Playful dares", "category_playful", 10));
        BADGES.put("adventure_seeker", new Badge("🗺️", "Adventure Seeker",
                "Complete 10 Adventure dares", "category_adventure", 10));
        BADGES.put("passionate_heart", new Badge("❤️‍🔥", "Passionate Heart",
                "Complete 10 Passionate dares", "category_passionate", 10));
        BADGES.put("wild_one", new Badge("🦁", "Wild One",
                "Complete 10 Wild dares", "category_wild", 10));
        BADGES.put("renaissance_lover", new Badge("🎨", "Renaissance Lover",
                "Complete 5 dares in each category", "category_mixed", 25));

        // Streak Badges
        BADGES.put("warm_up", new Badge("🔥", "Warm-Up",
                "3-day completion streak", "streak", 3));
        BADGES.put("getting_hot", new Badge("🌡️", "Getting Hot",
                "7-day completion streak", "streak", 7));
        BADGES.put("on_fire", new Badge("🔥", "On Fire",
                "14-day completion streak", "streak", 14));
        BADGES.put("blazing", new Badge("🌋", "Blazing",
                "30-day completion streak", "streak", 30));
        BADGES.put("inferno", new Badge("☄️", "Inferno",
                "60-day completion streak", "streak", 60));

        // Partnership Badges
        BADGES.put("perfect_match", new Badge("💑", "Perfect Match",
                "Both partners complete dares on same day 5 times", "partnership", 5));
        BADGES.put("synchronized_souls", new Badge("⚡", "Synchronized Souls",
                "Complete dares within 1 hour of each other 10 times", "partnership", 10));
        BADGES.put("power_couple", new Badge("💪", "Power Couple",
                "Combined 1000 points as a couple", "partnership", 1000));
        BADGES.put("dynamic_duo", new Badge("👥", "Dynamic Duo",
                "Both maintain 7+ day streaks simultaneously", "partnership", 1));

        // Competition Badges - NEW CATEGORY!
        BADGES.put("monthly_champion", new Badge("🏆", "Monthly Champion",
                "Win a monthly competition", "competition", 1));
        BADGES.put("close_call", new Badge("😅", "Close Call",
                "Win by 5 points or less", "competition", 1));
        BADGES.put("comeback_kid", new Badge("🔄", "Comeback Kid",
                "Win after being behind by 50+ points", "competition", 1));
        BADGES.put("dominator", new Badge("👑", "Dominator",
                "Win by 100+ points", "competition", 1));
        BADGES.put("competitive_spirit", new Badge("⚔️", "Competitive Spirit",
                "Participate in 3 monthly competitions", "competition", 3));
        BADGES.put("hat_trick", new Badge("🎩", "Hat Trick",
                "Win 3 months in a row", "competition", 3));
        BADGES.put("rivalry_master", new Badge("🥊", "Rivalry Master",
                "Win 5 monthly competitions", "competition", 5));
        BADGES.put("final_hour", new Badge("⏰", "Final Hour",
                "Take the lead in the last day of competition", "competition", 1));
        BADGES.put("early_bird_winner", new Badge("🐦", "Early Bird Winner",
                "Lead the entire month and win", "competition", 1));
        BADGES.put("photo_finish", new Badge("📸", "Photo Finish",
                "Tie in monthly points (both win)", "competition", 1));

        // Milestone Badges
        BADGES.put("first_steps", new Badge("👶", "First Steps",
                "Complete your first dare", "milestone", 1));
        BADGES.put("getting_started", new Badge("🌱", "Getting Started",
                "Earn 100 points", "milestone", 100));
        BADGES.put("point_collector", new Badge("💎", "Point Collector",
                "Earn 500 points", "milestone", 500));
        BADGES.put("point_master", new Badge("🏅", "Point Master",
                "Earn 1000 points", "milestone", 1000));
        BADGES.put("point_legend", new Badge("🌟", "Point Legend",
                "Earn 2500 points", "milestone", 2500));

        // Special Behavior Badges
        BADGES.put("generous_giver", new Badge("🎁", "Generous Giver",
                "Send 25 dares to partner", "sender", 25));
        BADGES.put("dare_devil", new Badge("😈", "Dare Devil",
                "Send 50 dares to partner", "sender", 50));
        BADGES.put("night_owl", new Badge("🦉", "Night Owl",
                "Complete 5 dares after 10 PM", "time", 5));
        BADGES.put("early_bird", new Badge("🐦", "Early Bird",
                "Complete 5 dares before 8 AM", "time", 5));
        BADGES.put("weekend_warrior", new Badge("⚔️", "Weekend Warrior",
                "Complete 10 dares on weekends", "time", 10));

        // Secret/Hidden Badges
        BADGES.put("social_butterfly", new Badge("🦋", "Social Butterfly",
                "Share your invite code 3 times", "special", 3));
        BADGES.put("code_breaker", new Badge("🔍", "Code Breaker",
                "Use the 'Peek at Prize' feature", "secret", 1));
        BADGES.put("second_chance", new Badge("🔄", "Second Chance",
                "Regenerate your invite code", "secret", 1));
        BADGES.put("explorer", new Badge("🧭", "Explorer",
                "Access every screen in the app", "secret", 1));
    }

    public static class Badge {
        public String emoji;
        public String name;
        public String description;
        public String type;
        public int requirement;
        public boolean isHidden;

        public Badge(String emoji, String name, String description, String type, int requirement) {
            this.emoji = emoji;
            this.name = name;
            this.description = description;
            this.type = type;
            this.requirement = requirement;
            this.isHidden = type.equals("secret");
        }
    }
}