package com.DareUs.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BadgeCelebrationDialog extends Dialog {

    private static final String TAG = "BadgeCelebration";
    private BadgeSystem.Badge badge;
    private Handler animationHandler = new Handler();

    public BadgeCelebrationDialog(Context context, BadgeSystem.Badge badge) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.badge = badge;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            createMegaCelebrationUI();

            // Make dialog truly fullscreen
            Window window = getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
                window.setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                );
            }

            setCancelable(false);
            startEpicAnimations();

        } catch (Exception e) {
            Log.e(TAG, "Error creating celebration dialog", e);
            dismiss();
        }
    }

    private void createMegaCelebrationUI() {
        // Clean main container with glass effect
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(40, 40, 40, 40);
        mainLayout.setBackgroundColor(0x80000000); // Semi-transparent overlay

        // Premium glass card container
        LinearLayout cardLayout = new LinearLayout(getContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setGravity(Gravity.CENTER);
        cardLayout.setPadding(32, 24, 32, 24);
        cardLayout.setBackgroundResource(R.drawable.glass_card);
        cardLayout.setElevation(20);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(32, 100, 32, 100);
        cardLayout.setLayoutParams(cardParams);

        // Simple achievement text
        TextView achievementText = new TextView(getContext());
        achievementText.setText("Achievement Unlocked");
        achievementText.setTextColor(0xFFBB86FC);
        achievementText.setTextSize(16);
        achievementText.setTypeface(null, android.graphics.Typeface.BOLD);
        achievementText.setGravity(Gravity.CENTER);
        achievementText.setPadding(0, 0, 0, 16);

        // Badge name (no emoji)
        TextView badgeName = new TextView(getContext());
        badgeName.setText(badge.name);
        badgeName.setTextColor(0xFFFFFFFF);
        badgeName.setTextSize(20);
        badgeName.setTypeface(null, android.graphics.Typeface.BOLD);
        badgeName.setGravity(Gravity.CENTER);
        badgeName.setPadding(0, 0, 0, 12);

        // Badge description
        TextView badgeDesc = new TextView(getContext());
        badgeDesc.setText(badge.description);
        badgeDesc.setTextColor(0xFFE8BBE8);
        badgeDesc.setTextSize(14);
        badgeDesc.setGravity(Gravity.CENTER);
        badgeDesc.setPadding(0, 0, 0, 24);

        // Simple continue button
        Button continueButton = new Button(getContext());
        continueButton.setText("Continue");
        continueButton.setTextColor(0xFFFFFFFF);
        continueButton.setTextSize(16);
        continueButton.setTypeface(null, android.graphics.Typeface.BOLD);
        continueButton.setBackgroundColor(0xFFBB86FC);
        continueButton.setOnClickListener(v -> dismiss());

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        continueButton.setLayoutParams(buttonParams);

        cardLayout.addView(achievementText);
        cardLayout.addView(badgeName);
        cardLayout.addView(badgeDesc);
        cardLayout.addView(continueButton);

        mainLayout.addView(cardLayout);
        setContentView(mainLayout);
    }

    private void startEpicAnimations() {
        try {
            // Find all the animated elements
            LinearLayout mainLayout = (LinearLayout) findViewById(android.R.id.content);
            if (mainLayout == null) return;

            // Entrance animations with delays
            animationHandler.postDelayed(() -> {
                // Title 1 slides in from top
                TextView title1 = (TextView) mainLayout.getChildAt(0);
                title1.setTranslationY(-200f);
                title1.animate()
                        .translationY(0f)
                        .setDuration(800)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }, 100);

            animationHandler.postDelayed(() -> {
                // Title 2 slides in from top
                TextView title2 = (TextView) mainLayout.getChildAt(1);
                title2.setTranslationY(-200f);
                title2.animate()
                        .translationY(0f)
                        .setDuration(800)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }, 300);

            animationHandler.postDelayed(() -> {
                // Emoji bounces in
                LinearLayout emojiContainer = (LinearLayout) mainLayout.getChildAt(2);
                emojiContainer.setScaleX(0f);
                emojiContainer.setScaleY(0f);
                emojiContainer.animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(600)
                        .withEndAction(() -> {
                            emojiContainer.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(300)
                                    .start();
                        })
                        .start();

                // Start continuous emoji pulsing
                startEmojiPulsing(emojiContainer);
            }, 600);

            animationHandler.postDelayed(() -> {
                // Badge card slides in from bottom
                LinearLayout badgeCard = (LinearLayout) mainLayout.getChildAt(3);
                badgeCard.setTranslationY(300f);
                badgeCard.animate()
                        .translationY(0f)
                        .setDuration(800)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }, 900);

            animationHandler.postDelayed(() -> {
                // Button fades in
                Button button = (Button) mainLayout.getChildAt(4);
                button.setAlpha(0f);
                button.animate()
                        .alpha(1f)
                        .setDuration(600)
                        .start();

                // Start button pulsing
                startButtonPulsing(button);
            }, 1400);

        } catch (Exception e) {
            Log.e(TAG, "Error in animations", e);
        }
    }

    private void startEmojiPulsing(LinearLayout emojiContainer) {
        Runnable pulseRunnable = new Runnable() {
            @Override
            public void run() {
                emojiContainer.animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(1000)
                        .withEndAction(() -> {
                            emojiContainer.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(1000)
                                    .withEndAction(() -> {
                                        animationHandler.postDelayed(this, 500);
                                    })
                                    .start();
                        })
                        .start();
            }
        };
        animationHandler.postDelayed(pulseRunnable, 1000);
    }

    private void startButtonPulsing(Button button) {
        Runnable buttonPulse = new Runnable() {
            @Override
            public void run() {
                button.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(1500)
                        .withEndAction(() -> {
                            button.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(1500)
                                    .withEndAction(() -> {
                                        animationHandler.postDelayed(this, 1000);
                                    })
                                    .start();
                        })
                        .start();
            }
        };
        animationHandler.postDelayed(buttonPulse, 500);
    }

    @Override
    public void dismiss() {
        animationHandler.removeCallbacksAndMessages(null);
        super.dismiss();
    }
}