package com.DareUs.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.os.Looper;

public class MainActivity extends AppCompatActivity implements BadgeTracker.BadgeUnlockListener {
    private static final String TAG = "DareUs";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout mainLayout;
    private int pendingNegotiationsCount = 0;
    private int receivedDaresCount = 0;
    private boolean dashboardLoaded = false;

    // Store button references for updating badges
    private LinearLayout negotiationsButton;
    private LinearLayout receivedDaresButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();

            // Initialize Firebase Analytics
            AnalyticsHelper.initialize(this);

            // Check if user is logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Intent intent = new Intent(this, WelcomeActivity.class);
                startActivity(intent);
                finish();
                return;
            }

            // Track user ID and screen view for analytics
            AnalyticsHelper.setUserId(currentUser.getUid());
            AnalyticsHelper.logScreenView("Dashboard", "MainActivity");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
                }
            }

            // Start notification service
            Intent serviceIntent = new Intent(MainActivity.this, NotificationService.class);
            startService(serviceIntent);

            // FIXED: Enhanced profile loading with better error handling
            db.collection("dareus").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        try {
                            if (doc.exists()) {

                                // FIXED: Better firstName check
                                String firstName = doc.getString("firstName");
                                boolean hasValidFirstName = firstName != null && !firstName.trim().isEmpty();


                                if (hasValidFirstName) {

                                    String partnerId = doc.getString("partnerId");
                                    if (partnerId == null || partnerId.isEmpty()) {
                                        showCleanDashboard(currentUser, firstName, "Your Partner");
                                    } else {
                                        loadPartnerInfoAndShowDashboard(currentUser, firstName, partnerId);
                                    }

                                    // Setup real-time listeners AFTER UI is created
                                    setupRealTimeListeners();
                                } else {
                                    goToProfileSetup();
                                }
                            } else {
                                goToProfileSetup();
                            }

                        } catch (Exception e) {
                            showCleanDashboard(currentUser, "User", "Your Partner");
                            setupRealTimeListeners();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showCleanDashboard(currentUser, "User", "Your Partner");
                        setupRealTimeListeners();
                    });

        } catch (Exception e) {
            finish();
        }
        checkNotificationPermissions();
        setupNotifications();

    }

    private void goToProfileSetup() {
        Intent intent = new Intent(MainActivity.this, ProfileSetupActivity.class);
        startActivity(intent);
        finish();
    }
    private void checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }
    private void setupNotifications() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                                return;
                        }

                        String token = task.getResult();

                        // Save token to user profile
                        saveFCMToken(token);
                    }
                });
    }

    private void saveFCMToken(String token) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);

            db.collection("dareus").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        // FCM token saved successfully
                    })
                    .addOnFailureListener(e -> {
                        // FCM token save failed
                    });
        }
    }

    private void setupRealTimeListeners() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;


        // FIXED: Add a small delay to prevent initial flicker
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Real-time listener for pending negotiations (custom dares)
            db.collection("dares")
                    .whereEqualTo("toUserId", currentUser.getUid())
                    .whereEqualTo("status", "pending_negotiation")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            return;
                        }

                        if (snapshots != null) {
                            int newCount = snapshots.size();
                            if (newCount != pendingNegotiationsCount) {
                                pendingNegotiationsCount = newCount;
                                if (negotiationsButton != null) {
                                    runOnUiThread(() -> updateButtonWithCount(negotiationsButton, pendingNegotiationsCount, "Custom Dares"));
                                }
                            }
                        }
                    });

            // Real-time listener for received regular dares
            db.collection("dares")
                    .whereEqualTo("toUserId", currentUser.getUid())
                    .whereEqualTo("status", "pending")
                    .whereGreaterThan("expiresAt", System.currentTimeMillis())
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            return;
                        }

                        if (snapshots != null) {
                            int newCount = snapshots.size();
                            if (newCount != receivedDaresCount) {
                                receivedDaresCount = newCount;
                                if (receivedDaresButton != null) {
                                    updateButtonWithCount(receivedDaresButton, receivedDaresCount, "Received Dares");
                                }
                            }
                        }
                    });
        }, 500); // 500ms delay to let UI settle
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && dashboardLoaded) {
            return;
        }

        if (currentUser != null && !dashboardLoaded) {

            db.collection("dareus").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String firstName = doc.getString("firstName");
                            boolean hasValidFirstName = firstName != null && !firstName.trim().isEmpty();

                            if (hasValidFirstName) {
                                String partnerId = doc.getString("partnerId");
                                if (partnerId == null || partnerId.isEmpty()) {
                                    showCleanDashboard(currentUser, firstName, "Your Partner");
                                } else {
                                    loadPartnerInfoAndShowDashboard(currentUser, firstName, partnerId);
                                }
                                dashboardLoaded = true; // Mark as loaded
                            }
                        }
                    })
                    .addOnFailureListener(e -> {});
        }
    }


    @Override
    public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
        // 🎉 SHOW EPIC CELEBRATION DIALOG!
        BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(this, badge);
        celebrationDialog.show();

        // Award bonus points for badge unlock
        awardBadgeBonusPoints();
    }

    private void awardBadgeBonusPoints() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentPoints = doc.getLong("points");
                        if (currentPoints == null) currentPoints = 0L;

                        // Award 50 bonus points for badge unlock
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("points", currentPoints + 50);

                        db.collection("dareus").document(currentUser.getUid())
                                .update(updates);
                    }
                });
    }

    private void loadPartnerInfoAndShowDashboard(FirebaseUser currentUser, String firstName, String partnerId) {
        FirebaseFirestore.getInstance()
                .collection("dareus")
                .document(partnerId)
                .get()
                .addOnSuccessListener(partnerDoc -> {
                    String partnerName = "Your Partner";
                    if (partnerDoc.exists() && partnerDoc.contains("firstName")) {
                        partnerName = partnerDoc.getString("firstName");
                    }
                    showCleanDashboard(currentUser, firstName, partnerName);
                })
                .addOnFailureListener(e -> {
                    showCleanDashboard(currentUser, firstName, "Your Partner");
                });
        dashboardLoaded = true;
    }
    private void updateButtonWithCount(LinearLayout button, int count, String baseTitle) {
        if (button == null) return;

        // Find the title TextView and update it with clean bracket notation
        for (int i = 0; i < button.getChildCount(); i++) {
            if (button.getChildAt(i) instanceof TextView) {
                TextView titleView = (TextView) button.getChildAt(i);

                // Update title with count if > 0
                if (count > 0) {
                    titleView.setText(baseTitle + " (" + count + ")");
                    titleView.setTextColor(0xFFFF6B9D); // Premium highlight color
                } else {
                    titleView.setText(baseTitle);
                    titleView.setTextColor(0xFFFFFFFF); // Normal color
                }
                break; // Only update the first (title) TextView
            }
        }
    }
    private void showCleanDashboard(FirebaseUser currentUser, String firstName, String partnerName) {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        mainLayout.setPadding(24, 8, 24, 32);
        mainLayout.setBackgroundResource(R.drawable.premium_gradient_bg);

        android.widget.ImageView logoView = new android.widget.ImageView(this);
        logoView.setImageResource(R.drawable.dareus_logo);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(
                500, // Slightly smaller when no partner
                500
        );
        logoParams.setMargins(0, 0, 0, 16);
        logoParams.gravity = Gravity.CENTER;
        logoView.setLayoutParams(logoParams);
        logoView.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        logoView.setAdjustViewBounds(true);

        if (partnerName.equals("Your Partner")) {
            TextView welcomeText = new TextView(this);
            welcomeText.setText("Welcome " + firstName);
            welcomeText.setTextColor(0xFFFFFFFF);
            welcomeText.setTextSize(32);
            welcomeText.setTypeface(null, android.graphics.Typeface.BOLD);
            welcomeText.setGravity(Gravity.CENTER);
            welcomeText.setPadding(0, 0, 0, 8);
            welcomeText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

            TextView subText = new TextView(this);
            subText.setText("Connect with your partner to start your adventure");
            subText.setTextColor(0xFFE8BBE8);
            subText.setTextSize(14);
            subText.setGravity(Gravity.CENTER);
            subText.setPadding(0, 0, 0, 40);

            mainLayout.addView(logoView);
            mainLayout.addView(welcomeText);
            mainLayout.addView(subText);
        } else {
            TextView heartEmoji = new TextView(this);
            heartEmoji.setText("💕");
            heartEmoji.setTextSize(28);
            heartEmoji.setGravity(Gravity.CENTER);
            heartEmoji.setPadding(0, 0, 0, 8);
            heartEmoji.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

            TextView coupleText = new TextView(this);
            coupleText.setText(firstName + " & " + partnerName);
            coupleText.setTextColor(0xFFFFFFFF);
            coupleText.setTextSize(32);
            coupleText.setTypeface(null, android.graphics.Typeface.BOLD);
            coupleText.setGravity(Gravity.CENTER);
            coupleText.setPadding(0, 0, 0, 40);
            coupleText.setShadowLayer(20f, 0f, 0f, 0xFFFF6B9D);

            mainLayout.addView(logoView);
            mainLayout.addView(heartEmoji);
            mainLayout.addView(coupleText);
        }

        // Clean dashboard with only 6 main buttons
        createPremiumDashboardButton(mainLayout, "Send Dares", "Challenge your partner", v -> {
            Intent intent = new Intent(MainActivity.this, DareSelectionActivity.class);
            startActivity(intent);
        });

        receivedDaresButton = createPremiumDashboardButtonView("Received Dares", "Complete challenges", v -> {
            Intent intent = new Intent(MainActivity.this, DareInboxActivity.class);
            startActivity(intent);
        });
        mainLayout.addView(receivedDaresButton);

        // Custom Dares - Combined send and receive page
        negotiationsButton = createPremiumDashboardButtonView("Custom Dares", "Create & negotiate custom challenges", v -> {
            Intent intent = new Intent(MainActivity.this, CustomDareHubActivity.class);
            startActivity(intent);
        });
        mainLayout.addView(negotiationsButton);

        createPremiumDashboardButton(mainLayout, "📖 How to Play", "Learn the rules & get started", v -> {
            Intent intent = new Intent(MainActivity.this, InstructionsActivity.class);
            startActivity(intent);
        });

        createPremiumDashboardButton(mainLayout, "Leaderboard & Competition", "See who's winning this month", v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        createPremiumDashboardButton(mainLayout, "Achievements", "View your badges & progress", v -> {
            Intent intent = new Intent(MainActivity.this, BadgesActivity.class);
            startActivity(intent);
        });

        createPremiumDashboardButton(mainLayout, "Profile & Premium", "Manage account & upgrade", v -> {
            Intent intent = new Intent(MainActivity.this, ProfileManagementActivity.class);
            startActivity(intent);
        });

        ScrollView dashboardScrollView = new ScrollView(this);
        dashboardScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        dashboardScrollView.setFillViewport(true);
        dashboardScrollView.addView(mainLayout);

        setContentView(dashboardScrollView);

        // Load notification counts after UI is ready
        loadNotificationCounts();

        dashboardLoaded = true;
    }
    private void loadNotificationCounts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Load pending negotiations count
        db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending_negotiation")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingNegotiationsCount = querySnapshot.size();
                    updateButtonWithCount(negotiationsButton, pendingNegotiationsCount, "Custom Dares");
                });

        // Load received dares count (only pending, not completed) - one-time check
        db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    receivedDaresCount = querySnapshot.size();
                    if (receivedDaresButton != null) {
                        updateButtonWithCount(receivedDaresButton, receivedDaresCount, "Received Dares");
                    }
                });
    }

    private void createPremiumDashboardButton(LinearLayout parent, String title, String description,
                                              android.view.View.OnClickListener clickListener) {
        LinearLayout buttonCard = createPremiumDashboardButtonView(title, description, clickListener);
        parent.addView(buttonCard);
    }

    private LinearLayout createPremiumDashboardButtonView(String title, String description,
                                                          android.view.View.OnClickListener clickListener) {
        LinearLayout buttonCard = new LinearLayout(this);
        buttonCard.setOrientation(LinearLayout.VERTICAL);
        buttonCard.setPadding(28, 24, 28, 24);
        buttonCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(0, 0, 0, 16);
        buttonCard.setLayoutParams(buttonParams);

        TextView buttonTitle = new TextView(this);
        buttonTitle.setText(title);
        buttonTitle.setTextColor(0xFFFFFFFF);
        buttonTitle.setTextSize(18);
        buttonTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        buttonTitle.setGravity(Gravity.CENTER);

        TextView buttonDesc = new TextView(this);
        buttonDesc.setText(description);
        buttonDesc.setTextColor(0xFFE8BBE8);
        buttonDesc.setTextSize(14);
        buttonDesc.setGravity(Gravity.CENTER);
        buttonDesc.setPadding(0, 4, 0, 0);

        buttonCard.addView(buttonTitle);
        buttonCard.addView(buttonDesc);
        buttonCard.setOnClickListener(clickListener);

        return buttonCard;
    }
}