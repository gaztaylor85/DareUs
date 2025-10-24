package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Calendar;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class DareSelectionActivity extends AppCompatActivity implements PremiumManager.PremiumStatusListener, BadgeTracker.BadgeUnlockListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentPartnerId, partnerName;
    private PremiumManager premiumManager;
    private BadgeTracker badgeTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dare_selection);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Initialize premium manager and badge tracker
        premiumManager = new PremiumManager(currentUser.getUid(), this);
        badgeTracker = new BadgeTracker(currentUser.getUid(), this);

        // Check if user has partner
        checkPartnerStatus();
    }

    @Override
    public void onPremiumStatusChanged(PremiumManager.PremiumTier tier) {
        // Handle premium status changes if needed
    }

    @Override
    public void onUsageLimitReached(String category) {
        showPremiumUpgradeDialog(category);
    }

    @Override
    public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
        // Show EPIC badge celebration dialog!
        BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(this, badge);
        celebrationDialog.show();
    }


    private void showPremiumUpgradeDialog(String category) {
        String message;
        if ("Sweet".equals(category) || "Playful".equals(category)) {
            message = "You've reached your weekly limit of 5 " + category + " dares.\n\nUpgrade to Premium for unlimited dares!";
        } else {
            message = "You've reached your weekly limit of 1 " + category + " dare.\n\nUpgrade to Premium for unlimited access to all categories!";
        }

        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Upgrade to Premium")
                .setMessage(message)
                .setPositiveButton("Upgrade Now", (dialog, which) -> {
                    Intent intent = new Intent(DareSelectionActivity.this, PremiumUpgradeActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Maybe Later", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void checkPartnerStatus() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentPartnerId = doc.getString("partnerId");

                        if (currentPartnerId == null || currentPartnerId.isEmpty()) {
                            // No partner - redirect to partner linking
                            showCustomToast("You need a partner to send dares");
                            startActivity(new Intent(this, PartnerLinkingActivity.class));
                            finish();
                            return;
                        }

                        // Get partner name and setup UI
                        loadPartnerNameAndSetupUI();
                    }
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Error loading profile. Try again.");
                    finish();
                });
    }

    private void loadPartnerNameAndSetupUI() {
        db.collection("dareus").document(currentPartnerId)
                .get()
                .addOnSuccessListener(partnerDoc -> {
                    if (partnerDoc.exists()) {
                        partnerName = partnerDoc.getString("firstName");
                        setupPremiumDareCategories();
                    } else {
                        partnerName = "Your Partner";
                        setupPremiumDareCategories();
                    }
                })
                .addOnFailureListener(e -> {
                    partnerName = "Your Partner";
                    setupPremiumDareCategories();
                });
    }

    private void setupPremiumDareCategories() {
        // Create main layout programmatically with premium design
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(24, 32, 24, 32);
        mainLayout.setBackgroundResource(R.drawable.premium_gradient_bg);

        // Premium Header with shadow
        TextView headerText = new TextView(this);
        headerText.setText("Send " + partnerName + " a Dare");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        if (premiumManager.isPremiumUser()) {
            subHeader.setText("Choose an intensity level - Unlimited access!");
        } else {
            subHeader.setText("Choose an intensity level - Free: 5 Sweet/Playful + 1 other per week");
        }
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 40);


        mainLayout.addView(headerText);
        mainLayout.addView(subHeader);
        // FIXED: Standardized back button at TOP
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 16);
        backButton.setOnClickListener(v -> {
            startActivity(new Intent(DareSelectionActivity.this, MainActivity.class));
            finish();
        });

        LinearLayout.LayoutParams backButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backButtonParams.gravity = Gravity.START;
        backButton.setLayoutParams(backButtonParams);

        mainLayout.addView(backButton);



        // Create floating glass card categories
        createFloatingCategoryCard(mainLayout, "Sweet", "Romantic & tender moments", 1, true);
        createFloatingCategoryCard(mainLayout, "Playful", "Fun & flirty challenges", 2, true);

        // These categories are premium for free users
        boolean isPremium = premiumManager.isPremiumUser();
        // All categories are unlocked for clicking - limits are handled in the logic
        createFloatingCategoryCard(mainLayout, "Adventure", "Bold & daring experiences", 3, true);
        createFloatingCategoryCard(mainLayout, "Passionate", "Intense & intimate moments", 4, true);
        createFloatingCategoryCard(mainLayout, "Wild", "Unleash your desires", 5, true);

        // REMOVED: Custom dares button - this should only be on the main dashboard


        setContentView(mainLayout);
    }

    private void createFloatingCategoryCard(LinearLayout parent, String categoryName, String description, int points, boolean isUnlocked) {
        // Create floating glass card exactly like login page
        LinearLayout cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.setPadding(24, 20, 24, 20);
        cardContainer.setBackgroundResource(R.drawable.glass_card);

        // Set margins for floating effect
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        cardContainer.setLayoutParams(cardParams);

        // Category name with proper typography
        TextView categoryTitle = new TextView(this);
        String titleText = categoryName;
        if (!isUnlocked) {
            titleText += " 🔒";
        }
        categoryTitle.setText(titleText);
        categoryTitle.setTextColor(isUnlocked ? 0xFFFFFFFF : 0xFF888888);
        categoryTitle.setTextSize(18);
        categoryTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        categoryTitle.setGravity(Gravity.CENTER);

        // Description
        // Description
        TextView categoryDesc = new TextView(this);
// Show different text for premium categories for free users
        if (!premiumManager.isPremiumUser() &&
                ("Adventure".equals(categoryName) || "Passionate".equals(categoryName) || "Wild".equals(categoryName))) {
            categoryDesc.setText(description + " (1 free per week)");
        } else {
            categoryDesc.setText(description);
        }
        categoryDesc.setTextColor(0xFFE8BBE8);
        categoryDesc.setTextSize(12);
        categoryDesc.setGravity(Gravity.CENTER);
        categoryDesc.setPadding(0, 4, 0, 8);

        // Points and time display
        TextView pointsText = new TextView(this);
        if (isUnlocked) {
            pointsText.setText(points + " base points • Complete faster for bonus points!");
            pointsText.setTextColor(0xFFBB86FC);
        } else {
            pointsText.setText("Premium required");
            pointsText.setTextColor(0xFFFF6B9D);
        }
        pointsText.setTextSize(11);
        pointsText.setTypeface(null, android.graphics.Typeface.BOLD);
        pointsText.setGravity(Gravity.CENTER);

        cardContainer.addView(categoryTitle);
        cardContainer.addView(categoryDesc);
        cardContainer.addView(pointsText);

        // Make entire card clickable if unlocked
        if (isUnlocked) {
            cardContainer.setOnClickListener(v -> selectDareFromCategory(categoryName, points));
        } else {
            cardContainer.setOnClickListener(v -> showPremiumUpgradeDialog(categoryName));
        }

        parent.addView(cardContainer);
    }

    private void selectDareFromCategory(String category, int points) {
        Log.d("DareSelection", "Attempting to send " + category + " dare");

        // Check usage limits for free users FIRST
        if (!premiumManager.isPremiumUser()) {
            checkUsageLimitsAndProceed(category, points);
        } else {
            proceedWithDareSelection(category, points);
        }
    }

    private void checkUsageLimitsAndProceed(String category, int points) {
        if (premiumManager != null) {
        }

        // FIXED: Proper weekly limit checking with new tier system
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long weekStart = calendar.getTimeInMillis();

        db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("sentAt", weekStart)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                })
                .addOnFailureListener(e -> {
                    Log.e("DareSelection", "Error checking limits", e);
                    // If it's a network error, show connection message. Otherwise, suggest upgrade.
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("network")) {
                        showPremiumUpgradeDialog("Connection error. Please check your internet and try again!");
                    } else {
                        // Database error or other issue - suggest upgrade as failsafe
                        showPremiumUpgradeDialog("Unable to verify your usage limits.\n\nUpgrade to Premium for unlimited dares and no restrictions!");
                    }
                });


        db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereGreaterThanOrEqualTo("sentAt", weekStart)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int sweetCount = 0;
                    int playfulCount = 0;
                    int premiumCount = 0;
                    int totalCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String dareCategory = document.getString("category");
                        totalCount++;

                        if ("Sweet".equals(dareCategory)) {
                            sweetCount++;
                        } else if ("Playful".equals(dareCategory)) {
                            playfulCount++;
                        } else if ("Adventure".equals(dareCategory) || "Passionate".equals(dareCategory) || "Wild".equals(dareCategory)) {
                            premiumCount++;
                        }
                    }

                    Log.d("DareSelection", "This week - Sweet: " + sweetCount + ", Playful: " + playfulCount + ", Premium: " + premiumCount + ", Total: " + totalCount);

                    boolean canSend = false;
                    String errorMessage = "";

                    PremiumManager.PremiumTier currentTier = premiumManager != null ? premiumManager.getCurrentTier() : PremiumManager.PremiumTier.FREE;

                    switch (currentTier) {
                        case FREE:
                            // Free: Sweet/Playful 5 COMBINED per week + 1 premium per week
                            if ("Sweet".equals(category) || "Playful".equals(category)) {
                                int freeCount = sweetCount + playfulCount;
                                if (freeCount < 5) {
                                    canSend = true;
                                } else {
                                    canSend = false;
                                    errorMessage = "You've used your 5 free Sweet/Playful dares this week. Upgrade for more!";
                                }
                            } else {
                                // Premium categories (Adventure, Passionate, Wild)
                                if (premiumCount < 1) {
                                    canSend = true;
                                } else {
                                    canSend = false;
                                    errorMessage = "You've used your 1 free premium dare this week. Upgrade for more!";
                                }
                            }

                            // CRITICAL: Make sure we actually proceed if canSend is true!
                            if (canSend) {
                                Log.d("DareSelection", "✅ FREE USER CAN SEND " + category + " DARE");
                            } else {
                                Log.d("DareSelection", "❌ FREE USER BLOCKED: " + errorMessage);
                            }
                            break;

                        case PREMIUM_SINGLE:
                            // Premium Single: 50 total dares per week, all categories (just for you)
                            if (totalCount < 50) {
                                canSend = true;
                            } else {
                                errorMessage = "You've reached your weekly limit of 50 dares. Upgrade to Premium Couple Plus for unlimited!";
                            }
                            break;

                        case PREMIUM_COUPLE:
                            // Premium Couple: 50 total dares per week, all categories (both partners)
                            if (totalCount < 50) {
                                canSend = true;
                            } else {
                                errorMessage = "You've reached your weekly limit of 50 dares. Upgrade to Premium Couple Plus for unlimited!";
                            }
                            break;

                        case PREMIUM_COUPLE_PLUS:
                            // Premium Couple Plus: Unlimited everything
                            canSend = true;
                            break;
                    }

                    if (canSend) {
                        proceedWithDareSelection(category, points);
                    } else {
                        showPremiumUpgradeDialog(category); // ✅ PASS CATEGORY, NOT MESSAGE
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DareSelection", "Error checking limits", e);
                    showPremiumUpgradeDialog("Connection error. Please check your internet and try again, or upgrade for unlimited dares!");
                });
    }

    private void proceedWithDareSelection(String category, int points) {
        // Get random dare but DON'T show it to sender
        List<String> dares = getDaresByCategory(category);

        if (dares.isEmpty()) {
            showCustomToast("No dares available in this category yet");
            return;
        }

        // Select random dare but keep it hidden
        Random random = new Random();
        String selectedDare = dares.get(random.nextInt(dares.size()));

        // Show confirmation WITHOUT revealing the dare content
        showPremiumDareConfirmation(category, points, selectedDare);
    }

    private List<String> getDaresByCategory(String category) {
        List<String> dares = new ArrayList<>();

        switch (category) {
            case "Sweet":
                dares.addAll(Arrays.asList(
                        "Give your partner a 10-second hug",
                        "Send your partner a loving message",
                        "Tell your partner something that will make them smile",
                        "Start a tickle fight",
                        "Write a love note and hide it somewhere they'll find it",
                        "Compliment 3 things you love about them",
                        "Make them their favorite drink",
                        "Give them a gentle forehead kiss",
                        "Tell them why you're grateful for them today",
                        "Hold their hand for 5 minutes without talking",
                        "Give them a surprise back rub",
                        "Leave a sweet voice message on their phone",
                        "Bring them breakfast in bed",
                        "Write down 5 reasons why they make you smile",
                        "Give them your undivided attention for 15 minutes",
                        "Surprise them with their favorite snack",
                        "Tell them about your favorite memory together",
                        "Give them a long, soft kiss",
                        "Write 'I love you' in steam on the bathroom mirror",
                        "Cuddle with them while watching their favorite show",
                        "Give them a gentle foot massage",
                        "Tell them what you love most about their personality",
                        "Surprise them with flowers or their favorite treat",
                        "Give them a 20-second passionate hug",
                        "Write them a poem (even if it's silly)",
                        "Tell them how they've made you a better person",
                        "Give them butterfly kisses on their eyelids",
                        "Make a playlist of songs that remind you of them",
                        "Give them a shoulder massage while they work",
                        "Leave love notes in their coat pockets",
                        "Tell them your favorite thing about their smile",
                        "Give them a gentle scalp massage",
                        "Surprise them by doing one of their chores",
                        "Tell them about the moment you knew you loved them",
                        "Give them a soft kiss on the neck",
                        "Write down what you want to do together this year",
                        "Give them a warm oil hand massage",
                        "Tell them how attractive they looked today",
                        "Surprise them with their favorite coffee or tea",
                        "Give them 10 tiny kisses all over their face",
                        "Tell them what you're most excited about in your future together",
                        "Give them a gentle temple massage",
                        "Write them a list of things you adore about them",
                        "Give them a loving kiss on their palm",
                        "Tell them about a dream you had about them",
                        "Surprise them by wearing something they love on you",
                        "Give them a soft earlobe kiss",
                        "Tell them how they make you feel safe",
                        "Give them a gentle finger massage",
                        "Write them a thank you note for something specific",
                        "Give them a tender wrist kiss",
                        "Tell them what you love about their laugh",
                        "End the day by telling them your favorite moment from today"
                ));
                break;

            case "Playful":
                dares.addAll(Arrays.asList(
                        "Give your partner a 5-minute head massage",
                        "Slow dance to your favorite song together",
                        "Play 20 questions about each other",
                        "Give each other silly nicknames for the day",
                        "Have a staring contest - first to laugh loses",
                        "Feed each other dessert with your eyes closed",
                        "Give them a playful lip bite",
                        "Have a thumb wrestling competition",
                        "Whisper sweet nothings in their ear",
                        "Give them a flirty wink across the room",
                        "Play a game where you can only communicate through touch",
                        "Give them a teasing almost-kiss",
                        "Have a pillow fight in your underwear",
                        "Trace your name on their back with your finger",
                        "Give them a playful spank",
                        "Blindfold them and feed them different foods to guess",
                        "Give them a flirty text while you're in the same room",
                        "Have a tickle fight",
                        "Write something naughty on their skin with your finger",
                        "Give them a sexy look from across the room",
                        "Play a game of truth or dare with just you two",
                        "Give them a nibble on their ear",
                        "Take turns describing what you want to do to each other",
                        "Give them a sensual hand kiss",
                        "Play 'guess where I'm touching you' with eyes closed",
                        "Give them a flirty bite on their shoulder",
                        "Whisper your favorite thing about their body",
                        "Give them a teasing kiss just below their ear",
                        "Play a game where every touch has to last 10 seconds",
                        "Give them a playful tug on their clothes",
                        "Describe in detail how you want to kiss them",
                        "Give them a soft bite on their bottom lip",
                        "Play 'find the hidden kiss' on their body",
                        "Give them a flirty squeeze",
                        "Tell them exactly what you're thinking about them right now",
                        "Give them a teasing touch and walk away",
                        "Play 'guess the body part' while blindfolded",
                        "Give them a lingering kiss on their wrist",
                        "Describe your perfect romantic evening together",
                        "Give them a playful hip bump",
                        "Tell them what you want to do after dark",
                        "Give them a cheeky grin and a wink",
                        "Play 'copy my touch' - mirror each other's movements",
                        "Give them a gentle tug toward you",
                        "Whisper something that makes them blush",
                        "Give them a kiss and tell them to guess the flavor of your lip balm",
                        "Play 'finish this sentence' with romantic prompts",
                        "Give them a playful push against the wall",
                        "Tell them your favorite way they touch you",
                        "End with a kiss that leaves them wanting more"
                ));
                break;

            case "Adventure":
                dares.addAll(Arrays.asList(
                        "Kiss somewhere semi-public",
                        "Try a new restaurant together",
                        "Go for a sunset walk holding hands",
                        "Take a selfie somewhere you've never been",
                        "Ask strangers for directions to somewhere you already know",
                        "Give your partner a passionate kiss in an elevator",
                        "Hold hands under the table during dinner out",
                        "Whisper something naughty in their ear at a coffee shop",
                        "Give them a lingering hug in a bookstore",
                        "Share an intimate moment in a photo booth",
                        "Kiss them passionately in the rain",
                        "Hold their gaze intensely across a crowded room",
                        "Give them a secret touch while talking to others",
                        "Sneak away together during a social gathering",
                        "Share a romantic moment on a balcony or rooftop",
                        "Give them a meaningful look during a movie",
                        "Hold hands while walking through a market",
                        "Steal a moment alone in a garden or park",
                        "Give them a quick passionate kiss when no one's looking",
                        "Share an intimate whisper during a group conversation",
                        "Take a romantic photo together in an unusual location",
                        "Give them a lingering touch while ordering food",
                        "Share a secret smile across a room full of people",
                        "Hold their hand during a scenic drive",
                        "Give them a passionate embrace at a viewpoint",
                        "Whisper your plans for later while in public",
                        "Share a meaningful moment watching a sunset",
                        "Give them an unexpected kiss while shopping",
                        "Hold their waist while waiting in line",
                        "Share an intimate moment in a quiet corner",
                        "Give them a loving look during a toast",
                        "Trace their hand while sitting together",
                        "Share a passionate moment before entering a party",
                        "Give them a lingering goodbye kiss",
                        "Hold them close while listening to live music",
                        "Share a romantic moment on public transport",
                        "Give them an unexpected hug from behind in public",
                        "Whisper sweet words during a group dinner",
                        "Share an intimate laugh at an inside joke",
                        "Give them a meaningful touch during a ceremony",
                        "Hold their hand during a beautiful moment",
                        "Share a loving glance during a celebration",
                        "Give them a gentle caress while no one's watching",
                        "Whisper how much you want them right now",
                        "Share a passionate moment before going home",
                        "Give them a promise of what's coming later",
                        "Hold them close during a slow song",
                        "Share an intimate moment under the stars",
                        "Give them a kiss that makes them weak in the knees",
                        "End the adventure by telling them it's just the beginning"
                ));
                break;

            case "Passionate":
                dares.addAll(Arrays.asList(
                        "Send your partner a picture you'd never send your friends",
                        "Tell them your favorite thing about their body",
                        "Give them a massage somewhere unexpected",
                        "Whisper something naughty in their ear in public",
                        "Show them something you've never shown anyone else",
                        "Tell them exactly what you want to do to them later",
                        "Give them a massage using only your lips",
                        "Describe in detail your favorite intimate memory together",
                        "Show them how you like to be touched",
                        "Tell them what you think about when you're alone",
                        "Give them a full body massage with warm oil",
                        "Describe what you want them to do to you",
                        "Show them your most sensitive spot",
                        "Tell them about a fantasy you've never shared",
                        "Give them a sensual dance",
                        "Describe how they make you feel when they touch you",
                        "Show them something that drives you wild",
                        "Tell them what you love most about being intimate",
                        "Give them a massage that leads to more",
                        "Describe your perfect intimate evening",
                        "Show them a new way to kiss you",
                        "Tell them what you want them to whisper to you",
                        "Give them control for 10 minutes",
                        "Describe what you want to wake up to",
                        "Show them how you want to be held",
                        "Tell them about your deepest desire",
                        "Give them a surprise in your favorite lingerie",
                        "Describe what you want them to do with their hands",
                        "Show them your favorite way to be kissed",
                        "Tell them what makes you feel most desired",
                        "Give them a taste of what's coming tonight",
                        "Describe how you want to be seduced",
                        "Show them something that makes you feel confident",
                        "Tell them what you want to try together",
                        "Give them a preview of your evening plans",
                        "Describe what you love about their touch",
                        "Show them how you want to be surprised",
                        "Tell them what makes you feel most alive",
                        "Give them a hint about your hidden talents",
                        "Describe your most intense moment together",
                        "Show them what you wear when you're thinking of them",
                        "Tell them what you want them to remember forever",
                        "Give them something to think about all day",
                        "Describe what you want to explore together",
                        "Show them what makes you feel beautiful",
                        "Tell them what you want to hear in the dark",
                        "Give them a reason to come home early",
                        "Describe what you want to discover about each other",
                        "Show them how much you want them right now",
                        "Promise them something unforgettable tonight"
                ));
                break;

            case "Wild":
                dares.addAll(Arrays.asList(
                        "Spend 2 minutes kissing your partner's favorite part of their body",
                        "Do something together you've both been curious about",
                        "Give them a full body massage with their favorite scent",
                        "Share your deepest fantasy with them",
                        "Create your own intimate moment together",
                        "Show them something that makes you feel incredibly attractive",
                        "Recreate your most passionate moment together",
                        "Give them complete control over what happens next",
                        "Show them exactly how you want to be pleased",
                        "Create a new intimate tradition just for you two",
                        "Show them what you've been dreaming about",
                        "Give them an experience they'll never forget",
                        "Show them something that makes your heart race",
                        "Create your own private celebration",
                        "Show them what makes you feel most alive",
                        "Give them a surprise that exceeds their wildest dreams",
                        "Show them how much passion you've been hiding",
                        "Create an adventure in your own space",
                        "Show them what you've been wanting to try",
                        "Give them the most intense experience you can imagine",
                        "Show them what drives you absolutely crazy",
                        "Create a moment you'll both remember forever",
                        "Show them what you think about in your most private moments",
                        "Give them something that will make them yours completely",
                        "Show them what you've never dared to show anyone",
                        "Create your own definition of perfection",
                        "Show them what makes you feel most desired",
                        "Give them access to your most guarded secrets",
                        "Show them what you want to be remembered for",
                        "Create an experience that belongs only to you two",
                        "Ask them to tell you their favourite time alone with you and recreate it",
                        "Give them something worth waiting for",
                        "Show them what makes you feel most confident",
                        "Send them a voice note explaining why they should be with you right now",
                        "Show them what you want to explore without limits",
                        "Send them 3 new things to try, pick one together",
                        "Show them what you've been saving for the right moment",
                        "Create something that will deepen your bond forever",
                        "Show them what you want to discover together",
                        "Give them proof of how much you trust them",
                        "Show them what you want your love to become",
                        "Create an experience that words can't describe",
                        "Show them what you want to remember when you're old",
                        "Show them how much you enjoy being unselfish",
                        "Show them what you've been building up to",
                        "Show off a body part you think they love the most",
                        "Show them what you want to share with no one else",
                        "Go to another room and show them why they should join you",
                        "Send them a picture only they should ever see",
                        "Create a moment where nothing else exists but you two"
                ));
                break;
        }

        return dares;
    }

    private void showPremiumDareConfirmation(String category, int points, String dare) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialogTheme);
        builder.setTitle("Send " + category + " Dare?");
        builder.setMessage("You're about to send a " + category.toLowerCase() + " dare to " + partnerName +
                "\n\nYou'll earn " + points + " points for sending this dare." +
                "\n\nThey'll have 7 days to complete it, but get bonus points for completing it faster!" +
                "\n\nThe dare content will remain a surprise until they receive it!");

        builder.setPositiveButton("Send Dare", (dialog, which) -> {
            sendDareToPartner(dare, category, points);
        });
        builder.setNegativeButton("Choose Different Category", null);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.show();
    }

    private void sendDareToPartner(String dare, String category, int points) {
        Log.d("DareSelection", "🎯 Starting to send dare: " + category);

        // Create dare document in Firestore - always 7 days
        Map<String, Object> dareData = new HashMap<>();
        dareData.put("dareText", dare);
        dareData.put("category", category);
        dareData.put("points", points);
        dareData.put("senderPoints", points);
        dareData.put("timeLimit", 7);
        dareData.put("fromUserId", currentUser.getUid());
        dareData.put("toUserId", currentPartnerId);
        dareData.put("status", "pending");
        dareData.put("sentAt", System.currentTimeMillis());
        dareData.put("expiresAt", System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L));

        Log.d("DareSelection", "🎯 About to save dare to database...");

        db.collection("dares")
                .add(dareData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("DareSelection", "✅ Dare saved successfully! Now sending notification...");

                    // Points are awarded automatically by backend (onDareSent trigger)

                    // Track badge achievements for sending
                    badgeTracker.checkDareSent(category);

                    // 🔔 NEW: Send notification to partner
                    sendNotificationToPartner(category);

                    showCustomToast("Dare sent to " + partnerName + "! Points will be awarded shortly.");
                })
                .addOnFailureListener(e -> {
                    Log.e("DareSelection", "❌ Failed to save dare: " + e.getMessage());
                    showCustomToast("Failed to send dare. Try again!");
                });
    }

    // REPLACE the verbose sendNotificationToPartner() with this clean version:
    private void sendNotificationToPartner(String category) {
        if (currentUser == null || currentPartnerId == null || db == null) {
            Log.e("Notification", "Missing required data for notification");
            return;
        }

        // Get my name first
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(myDoc -> {
                    String myName = myDoc.getString("firstName");
                    final String senderName = (myName != null) ? myName : "Your partner";

                    // Get partner's FCM token
                    db.collection("dareus").document(currentPartnerId)
                            .get()
                            .addOnSuccessListener(partnerDoc -> {
                                String partnerToken = partnerDoc.getString("fcmToken");

                                if (partnerToken != null) {
                                    // Create notification document
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("toToken", partnerToken);
                                    notif.put("title", "💕 New Dare!");
                                    notif.put("body", senderName + " sent you a " + category + " dare!");
                                    notif.put("timestamp", System.currentTimeMillis());
                                    notif.put("sent", false);

                                    db.collection("notifications")
                                            .add(notif)
                                            .addOnSuccessListener(doc -> {
                                                Log.d("Notification", "Notification queued successfully");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("Notification", "Failed to queue notification", e);
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Notification", "Failed to get partner data", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Notification", "Failed to get user data", e);
                });
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