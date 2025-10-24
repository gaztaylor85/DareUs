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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.widget.EditText;
import android.text.TextUtils;

public class ProfileManagementActivity extends AppCompatActivity implements BadgeTracker.BadgeUnlockListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private String currentPartnerId, partnerName;
    private BadgeTracker badgeTracker;
    private PremiumManager premiumManager;
    private long premiumExpiresAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        // Initialize badge tracker
        badgeTracker = new BadgeTracker(currentUser.getUid(), this);

        // Initialize premium manager
        premiumManager = new PremiumManager(currentUser.getUid(), new PremiumManager.PremiumStatusListener() {
            @Override
            public void onPremiumStatusChanged(PremiumManager.PremiumTier tier) {
                updatePremiumSection();
            }

            @Override
            public void onUsageLimitReached(String category) {
                // Not needed here
            }
        });

        setupProfileUI();
        loadUserData();
        loadPremiumExpirationData();
    }

    @Override
    public void onBadgeUnlocked(String badgeId, BadgeSystem.Badge badge) {
        // Show EPIC badge celebration dialog!
        BadgeCelebrationDialog celebrationDialog = new BadgeCelebrationDialog(this, badge);
        celebrationDialog.show();

        // Award bonus points for badge unlock
        awardBadgeBonusPoints();
    }

    private void awardBadgeBonusPoints() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance().collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long currentPoints = doc.getLong("points");
                        if (currentPoints == null) currentPoints = 0L;

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("points", currentPoints + 50);

                        FirebaseFirestore.getInstance().collection("dareus").document(currentUser.getUid())
                                .update(updates);
                    }
                });
    }



    private void setupProfileUI() {
        // Create the basic UI structure with back button and headers
        scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true);
        scrollView.setBackgroundResource(R.drawable.premium_gradient_bg);

        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(24, 32, 24, 32);

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("⚙️ Profile & Settings");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("Manage your account and premium features");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 32);

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Dashboard");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 16);
        backButton.setOnClickListener(v -> finish());

        contentLayout.addView(headerText);
        contentLayout.addView(subHeader);
        contentLayout.addView(backButton);

        scrollView.addView(contentLayout);
        setContentView(scrollView);
    }

    private void loadPremiumExpirationData() {
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long expiresAt = doc.getLong("premiumExpiresAt");
                        if (expiresAt != null) {
                            premiumExpiresAt = expiresAt;
                            updatePremiumSection();
                        }
                    }
                });
    }

    private void loadUserData() {
        // FIXED: Clear only profile cards, keep header and back button
        if (contentLayout != null) {
            // Count how many views are headers + back button (should be 3: headerText, subHeader, backButton)
            int headerCount = 3;

            // Remove everything AFTER the back button
            while (contentLayout.getChildCount() > headerCount) {
                contentLayout.removeViewAt(headerCount);
            }
        }

        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String email = doc.getString("email");
                        currentPartnerId = doc.getString("partnerId");
                        String inviteCode = doc.getString("inviteCode");
                        Long points = doc.getLong("points");
                        Long streakCount = doc.getLong("streakCount");

                        // Create profile info section
                        createProfileInfoCard(firstName, email, inviteCode, points, streakCount);

                        // Create partner section
                        if (currentPartnerId != null && !currentPartnerId.isEmpty()) {
                            loadPartnerData();
                        } else {
                            createPartnerSection(null);
                        }

                        // Create premium section
                        updatePremiumSection();

                        // Create account actions section
                        createAccountActionsSection();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileManagement", "Error loading user data: " + e.getMessage());
                    showCustomToast("Error loading profile data");
                });
        // FIXED: Standardized back button



    }

    private void createProfileInfoCard(String firstName, String email, String inviteCode, Long points, Long streakCount) {
        LinearLayout profileCard = new LinearLayout(this);
        profileCard.setOrientation(LinearLayout.VERTICAL);
        profileCard.setPadding(24, 20, 24, 20);
        profileCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        profileCard.setLayoutParams(params);

        TextView profileTitle = new TextView(this);
        profileTitle.setText("👤 Your Profile");
        profileTitle.setTextColor(0xFFFFFFFF);
        profileTitle.setTextSize(18);
        profileTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        profileTitle.setGravity(Gravity.CENTER);
        profileTitle.setPadding(0, 0, 0, 16);

        // Make name clickable to edit
        TextView nameInfo = new TextView(this);
        nameInfo.setText("Name: " + firstName + " (tap to edit)");
        nameInfo.setTextColor(0xFF4CAF50);
        nameInfo.setTextSize(14);
        nameInfo.setGravity(Gravity.CENTER);
        nameInfo.setPadding(8, 4, 8, 4);
        nameInfo.setBackgroundColor(0x20FFFFFF);
        nameInfo.setClickable(true);
        nameInfo.setOnClickListener(v -> showEditNameDialog(firstName));

        TextView otherInfo = new TextView(this);
        String otherInfoText = "Email: " + email + "\n" +
                "Total Points: " + (points != null ? points : 0) + "\n" +
                "Daily Completion Streak: " + (streakCount != null ? streakCount : 0) + " days\n" +
                "Your Code: " + (inviteCode != null ? inviteCode : "None");
        otherInfo.setText(otherInfoText);
        otherInfo.setTextColor(0xFFE8BBE8);
        otherInfo.setTextSize(14);
        otherInfo.setGravity(Gravity.CENTER);
        otherInfo.setPadding(0, 8, 0, 0);

        Button regenerateCodeButton = new Button(this);
        regenerateCodeButton.setText("Regenerate My Code");
        regenerateCodeButton.setBackgroundResource(R.drawable.premium_button_gradient); // CHANGED
        regenerateCodeButton.setTextColor(0xFFFFFFFF); // CHANGED
        regenerateCodeButton.setTextSize(12);
        regenerateCodeButton.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams regenParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        regenParams.gravity = Gravity.CENTER;
        regenParams.setMargins(0, 16, 0, 0);
        regenerateCodeButton.setLayoutParams(regenParams);

        regenerateCodeButton.setOnClickListener(v -> regenerateMyCode());

        profileCard.addView(profileTitle);
        profileCard.addView(nameInfo);
        profileCard.addView(otherInfo);
        profileCard.addView(regenerateCodeButton);

        contentLayout.addView(profileCard);
    }
    private void showEditNameDialog(String currentName) {
        EditText nameInput = new EditText(this);
        nameInput.setText(currentName);
        nameInput.setTextColor(0xFFFFFFFF);
        nameInput.setHintTextColor(0xFF9575CD);
        nameInput.setBackgroundResource(R.drawable.premium_input_field);
        nameInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(15)});
        nameInput.setPadding(12, 12, 12, 12);
        nameInput.selectAll(); // Select all text for easy editing

        new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setTitle("Edit Name")
                .setMessage("Update your display name:")
                .setView(nameInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName) && newName.length() >= 2) {
                        updateUserName(newName);
                    } else {
                        showCustomToast("Name must be at least 2 characters!");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUserName(String newName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", newName);

        db.collection("dareus").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showCustomToast("Name updated to " + newName + "! 🎉");
                    // Refresh the display
                    // 🚀 SMART REFRESH - Keep back button and header!
                    setupProfileUI(); // Re-create header and back button first
                    loadUserData();   // Then load the content
                })
                .addOnFailureListener(e -> {
                    showCustomToast("Error updating name. Try again!");
                });
    }
    private void loadPartnerData() {
        if (currentPartnerId == null || currentPartnerId.isEmpty()) return;

        db.collection("dareus").document(currentPartnerId)
                .get()
                .addOnSuccessListener(partnerDoc -> {
                    if (partnerDoc.exists()) {
                        partnerName = partnerDoc.getString("firstName");
                        String partnerEmail = partnerDoc.getString("email");
                        Long partnerPoints = partnerDoc.getLong("points");
                        Long partnerStreak = partnerDoc.getLong("streakCount");

                        Map<String, Object> partnerData = new HashMap<>();
                        partnerData.put("name", partnerName);
                        partnerData.put("email", partnerEmail);
                        partnerData.put("points", partnerPoints);
                        partnerData.put("streak", partnerStreak);

                        createPartnerSection(partnerData);
                    } else {
                        createPartnerSection(null);
                    }
                })
                .addOnFailureListener(e -> {
                    createPartnerSection(null);
                });
    }

    private void createPartnerSection(Map<String, Object> partnerData) {
        LinearLayout partnerCard = new LinearLayout(this);
        partnerCard.setOrientation(LinearLayout.VERTICAL);
        partnerCard.setPadding(24, 20, 24, 20);
        partnerCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        partnerCard.setLayoutParams(params);

        TextView partnerTitle = new TextView(this);
        partnerTitle.setText("💕 Partner Connection");
        partnerTitle.setTextColor(0xFFFFFFFF);
        partnerTitle.setTextSize(18);
        partnerTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        partnerTitle.setGravity(Gravity.CENTER);
        partnerTitle.setPadding(0, 0, 0, 16);

        TextView partnerInfo = new TextView(this);
        if (partnerData != null) {
            String info = "Partner: " + partnerData.get("name") + "\n" +
                    "Email: " + partnerData.get("email") + "\n" +
                    "Total Points: " + (partnerData.get("points") != null ? partnerData.get("points") : 0) + "\n" +
                    "Their Completion Streak: " + (partnerData.get("streak") != null ? partnerData.get("streak") : 0) + " days\n" +
                    "Status: Connected";
            partnerInfo.setText(info);
            partnerInfo.setTextColor(0xFFE8BBE8);
        } else {
            partnerInfo.setText("💔 No partner linked\n\nConnect with your partner to compete and share dares!");
            partnerInfo.setTextColor(0xFF9575CD);
        }

        partnerInfo.setTextSize(14);
        partnerInfo.setGravity(Gravity.CENTER);

        // Action buttons
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);

        if (partnerData != null) {
            Button unlinkButton = new Button(this);
            unlinkButton.setText("💔 Unlink Partner");
            unlinkButton.setBackgroundResource(R.drawable.premium_button_gradient); // CHANGED
            unlinkButton.setTextColor(0xFFFFFFFF); // CHANGED
            unlinkButton.setTextSize(12);
            unlinkButton.setPadding(16, 8, 16, 8);
            unlinkButton.setOnClickListener(v -> showUnlinkConfirmation());
            buttonLayout.addView(unlinkButton);
        } else {
            Button addPartnerButton = new Button(this);
            addPartnerButton.setText("➕ Add Partner");
            addPartnerButton.setBackgroundResource(R.drawable.premium_button_gradient);
            addPartnerButton.setTextColor(0xFFFFFFFF);
            addPartnerButton.setTextSize(12);
            addPartnerButton.setPadding(16, 8, 16, 8);
            addPartnerButton.setOnClickListener(v -> showAddPartnerDialog());
            buttonLayout.addView(addPartnerButton);
        }

        LinearLayout.LayoutParams buttonContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonContainerParams.gravity = Gravity.CENTER;
        buttonContainerParams.setMargins(0, 16, 0, 0);
        buttonLayout.setLayoutParams(buttonContainerParams);

        partnerCard.addView(partnerTitle);
        partnerCard.addView(partnerInfo);
        partnerCard.addView(buttonLayout);
        contentLayout.addView(partnerCard);
    }

    private void updatePremiumSection() {
        // Remove existing premium card if it exists
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            if (contentLayout.getChildAt(i) instanceof LinearLayout) {
                LinearLayout child = (LinearLayout) contentLayout.getChildAt(i);
                if (child.getTag() != null && "premium_section".equals(child.getTag())) {
                    contentLayout.removeViewAt(i);
                    break;
                }
            }
        }

        LinearLayout premiumCard = new LinearLayout(this);
        premiumCard.setOrientation(LinearLayout.VERTICAL);
        premiumCard.setPadding(24, 20, 24, 20);
        premiumCard.setBackgroundResource(R.drawable.glass_card);
        premiumCard.setTag("premium_section");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        premiumCard.setLayoutParams(params);

        TextView premiumTitle = new TextView(this);
        premiumTitle.setText("💎 Premium Status");
        premiumTitle.setTextColor(0xFFFFFFFF);
        premiumTitle.setTextSize(18);
        premiumTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        premiumTitle.setGravity(Gravity.CENTER);
        premiumTitle.setPadding(0, 0, 0, 16);

        TextView premiumInfo = new TextView(this);
        String tierName = premiumManager != null ? premiumManager.getTierDisplayName() : "Free";
        String tierDesc = premiumManager != null ? premiumManager.getTierDescription() : "Limited features";

        String premiumText = "Current Plan: " + tierName + "\n" + tierDesc;

        // Add expiration info if premium
        if (premiumManager != null && premiumManager.isPremiumUser() && premiumExpiresAt > 0) {
            long timeLeft = premiumExpiresAt - System.currentTimeMillis();
            if (timeLeft > 0) {
                long daysLeft = timeLeft / (24 * 60 * 60 * 1000);
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String expiryDate = dateFormat.format(new Date(premiumExpiresAt));

                if (daysLeft <= 7) {
                    premiumText += "\n\n⚠️ Expires in " + daysLeft + " days (" + expiryDate + ")";
                } else {
                    premiumText += "\n\n📅 Expires: " + expiryDate;
                }
            } else {
                premiumText += "\n\n❌ EXPIRED - Please renew";
            }
        }

        premiumInfo.setText(premiumText);
        premiumInfo.setTextColor(0xFFE8BBE8);
        premiumInfo.setTextSize(14);
        premiumInfo.setGravity(Gravity.CENTER);

        Button viewPlansButton = new Button(this);
        viewPlansButton.setText("💎 View Premium Plans");
        viewPlansButton.setBackgroundResource(R.drawable.premium_button_gradient);
        viewPlansButton.setTextColor(0xFFFFFFFF);
        viewPlansButton.setTextSize(14);
        viewPlansButton.setPadding(24, 12, 24, 12);

        LinearLayout.LayoutParams plansButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        plansButtonParams.gravity = Gravity.CENTER;
        plansButtonParams.setMargins(0, 16, 0, 0);
        viewPlansButton.setLayoutParams(plansButtonParams);

        viewPlansButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PremiumUpgradeActivity.class);
            startActivity(intent);
        });

        premiumCard.addView(premiumTitle);
        premiumCard.addView(premiumInfo);
        premiumCard.addView(viewPlansButton);

        // Insert before account actions (which should be last)
        contentLayout.addView(premiumCard, contentLayout.getChildCount() - 1);
    }

    private void createAccountActionsSection() {
        LinearLayout actionsCard = new LinearLayout(this);
        actionsCard.setOrientation(LinearLayout.VERTICAL);
        actionsCard.setPadding(24, 20, 24, 20);
        actionsCard.setBackgroundResource(R.drawable.glass_card);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        actionsCard.setLayoutParams(params);

        TextView actionsTitle = new TextView(this);
        actionsTitle.setText("⚙️ Account Actions");
        actionsTitle.setTextColor(0xFFFFFFFF);
        actionsTitle.setTextSize(18);
        actionsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        actionsTitle.setGravity(Gravity.CENTER);
        actionsTitle.setPadding(0, 0, 0, 16);

        Button logoutButton = new Button(this);
        logoutButton.setText("🚪 Logout");
        logoutButton.setBackgroundResource(R.drawable.premium_button_gradient); // CHANGED
        logoutButton.setTextColor(0xFFFFFFFF); // CHANGED
        logoutButton.setTextSize(14);
        logoutButton.setPadding(24, 12, 24, 12);

        Button settingsButton = new Button(this);
        settingsButton.setText("⚙️ Settings & Preferences");
        settingsButton.setBackgroundResource(R.drawable.premium_button_gradient);
        settingsButton.setTextColor(0xFFFFFFFF);
        settingsButton.setTextSize(14);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        LinearLayout.LayoutParams logoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        logoutParams.gravity = Gravity.CENTER;
        logoutButton.setLayoutParams(logoutParams);

        logoutButton.setOnClickListener(v -> showLogoutConfirmation());

        actionsCard.addView(actionsTitle);
        actionsCard.addView(logoutButton);
        contentLayout.addView(actionsCard);
    }

    // ... (Keep all the existing methods for partner management, code regeneration, etc.)
    // I'll include the key ones here:

    private void showAddPartnerDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter 12-character partner code");
        input.setTextColor(0xFFFFFFFF);  // ✅ WHITE TEXT - VISIBLE ON DARK BACKGROUND!
        input.setHintTextColor(0xFF9575CD);  // ✅ PURPLE HINT TEXT
        input.setBackgroundResource(R.drawable.premium_input_field);  // ✅ CONSISTENT STYLING
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(12)});
        input.setPadding(12, 12, 12, 12);  // ✅ BETTER PADDING

        new AlertDialog.Builder(this)
                .setTitle("Add Partner")
                .setMessage("Enter your partner's 12-character invite code:")
                .setView(input)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String code = input.getText().toString().trim().toUpperCase();
                    if (code.length() == 12) {
                        connectToPartnerByCode(code);
                    } else {
                        showCustomToast("Code must be exactly 12 characters!");
                    }
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void connectToPartnerByCode(String partnerCode) {
        // Check if it's user's own code
        db.collection("dareus").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String myCode = doc.getString("inviteCode");
                    if (partnerCode.equals(myCode)) {
                        showCustomToast("You can't use your own code!");
                        return;
                    }

                    // Find partner by code
                    db.collection("dareus")
                            .whereEqualTo("inviteCode", partnerCode)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (!task.getResult().isEmpty()) {
                                        // Found partner
                                        String partnerUid = task.getResult().getDocuments().get(0).getId();
                                        String partnerName = task.getResult().getDocuments().get(0).getString("firstName");
                                        String partnerCurrentPartnerId = task.getResult().getDocuments().get(0).getString("partnerId");

                                        // Check if partner already has a partner
                                        if (partnerCurrentPartnerId != null && !partnerCurrentPartnerId.isEmpty()) {
                                            showCustomToast("This person is already partnered with someone else!");
                                            return;
                                        }

                                        // Link partners
                                        linkPartners(partnerUid, partnerName);
                                    } else {
                                        showCustomToast("Invalid code. Check with your partner!");
                                    }
                                } else {
                                    showCustomToast("Error searching for partner. Try again!");
                                }
                            });
                });
    }

    private void linkPartners(String partnerUid, String partnerName) {
        // Update current user's partnerId
        Map<String, Object> myUpdates = new HashMap<>();
        myUpdates.put("partnerId", partnerUid);

        // Update partner's partnerId
        Map<String, Object> partnerUpdates = new HashMap<>();
        partnerUpdates.put("partnerId", currentUser.getUid());

        // Update both documents
        db.collection("dareus").document(currentUser.getUid())
                .update(myUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Update partner's document
                    db.collection("dareus").document(partnerUid)
                            .update(partnerUpdates)
                            .addOnSuccessListener(aVoid2 -> {
                                // Success!
                                showCustomToast("Connected with " + partnerName + "! 🎉💕");

                                // FIXED: Clear existing UI before refreshing to prevent duplicates
                                if (contentLayout != null) {
                                    contentLayout.removeAllViews();
                                }

                                // Refresh the display
                                currentPartnerId = partnerUid;
                                loadUserData();
                            });
                });
    }

    private void regenerateMyCode() {
        new AlertDialog.Builder(this)
                .setTitle("Regenerate Code")
                .setMessage("This will create a new 12-character code for you.\n\nYour old code will stop working. Are you sure?")
                .setPositiveButton("Yes, Generate New Code", (dialog, which) -> {
                    generateNewInviteCode();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void generateNewInviteCode() {
        // Call secure Cloud Function to generate invite code
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();

        functions.getHttpsCallable("generateInviteCode")
                .call(data)
                .addOnSuccessListener(result -> {
                    Map<String, Object> response = (Map<String, Object>) result.getData();
                    boolean success = (boolean) response.get("success");

                    if (success) {
                        String inviteCode = (String) response.get("inviteCode");

                        // Track code regeneration for Second Chance badge
                        badgeTracker.onCodeRegenerated();

                        showCustomToast("New code generated: " + inviteCode);

                        // FIXED: Clear existing UI before refreshing to prevent duplicates
                        if (contentLayout != null) {
                            contentLayout.removeAllViews();
                        }

                        // Refresh the display
                        loadUserData();
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = "Error generating code";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("resource-exhausted")) {
                            errorMessage = "You can only generate one code per day. Try again tomorrow!";
                        } else if (e.getMessage().contains("Try again in")) {
                            errorMessage = e.getMessage();
                        } else {
                            errorMessage = "Error: " + e.getMessage();
                        }
                    }
                    showCustomToast(errorMessage);
                });
    }

    private void showUnlinkConfirmation() {
        String message = partnerName != null ?
                "Are you sure you want to unlink from " + partnerName + "?\n\nThis will remove the partnership for both of you." :
                "Are you sure you want to remove this partner link?\n\nThis will clear your partner connection.";

        new AlertDialog.Builder(this)
                .setTitle("Unlink Partner")
                .setMessage(message)
                .setPositiveButton("Yes, Unlink", (dialog, which) -> unlinkPartner())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void unlinkPartner() {
        if (currentPartnerId == null) return;

        // Remove partnerId from current user
        Map<String, Object> myUpdates = new HashMap<>();
        myUpdates.put("partnerId", null);

        // Remove partnerId from partner (if partner exists)
        Map<String, Object> partnerUpdates = new HashMap<>();
        partnerUpdates.put("partnerId", null);

        // Update current user first
        db.collection("dareus").document(currentUser.getUid())
                .update(myUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Try to update partner's document
                    db.collection("dareus").document(currentPartnerId)
                            .update(partnerUpdates)
                            .addOnSuccessListener(aVoid2 -> {
                                // Success
                                showCustomToast("Partner unlinked successfully! 💔");
                                currentPartnerId = null;
                                partnerName = null;

                                // Refresh the display
                                loadUserData();
                            });
                });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?\n\nYou'll need to sign in again next time.")
                .setPositiveButton("Yes, Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        showCustomToast("Logged out successfully! 👋");

        Intent intent = new Intent(ProfileManagementActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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