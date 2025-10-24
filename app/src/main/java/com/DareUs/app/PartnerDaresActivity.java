package com.DareUs.app;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.concurrent.TimeUnit;

public class PartnerDaresActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private String partnerName = "Your Partner";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        setupUI();
        loadPartnerCompletedDares();
    }

    private void setupUI() {
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

        // Back button
        Button backButton = new Button(this);
        backButton.setText("← Back to Dares");
        backButton.setTextColor(0xFFBB86FC);
        backButton.setTextSize(14);
        backButton.setBackgroundColor(0x00000000);
        backButton.setPadding(0, 0, 0, 16);
        backButton.setOnClickListener(v -> finish());

        // Header
        TextView headerText = new TextView(this);
        headerText.setText("💕 Partner's Completed Dares");
        headerText.setTextColor(0xFFFFFFFF);
        headerText.setTextSize(28);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setGravity(Gravity.CENTER);
        headerText.setPadding(0, 0, 0, 8);
        headerText.setShadowLayer(15f, 0f, 0f, 0xFFFF6B9D);

        TextView subHeader = new TextView(this);
        subHeader.setText("See what dares your partner has completed");
        subHeader.setTextColor(0xFFE8BBE8);
        subHeader.setTextSize(14);
        subHeader.setGravity(Gravity.CENTER);
        subHeader.setPadding(0, 0, 0, 24);

        contentLayout.addView(backButton);
        contentLayout.addView(headerText);
        contentLayout.addView(subHeader);

        scrollView.addView(contentLayout);
        setContentView(scrollView);
    }

    private void loadPartnerCompletedDares() {
        Log.d("PartnerDares", "Loading partner completed dares for user: " + currentUser.getUid());

        db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid()) // Dares I sent to partner
                .whereEqualTo("status", "completed")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("PartnerDares", "Query successful. Found " + queryDocumentSnapshots.size() + " completed dares");

                    if (queryDocumentSnapshots.isEmpty()) {
                        showNoDares();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Log.d("PartnerDares", "Processing dare: " + document.getId());
                            createPartnerDareCard(document);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PartnerDares", "Error loading partner dares", e);
                    showError();
                });
    }

    private void createPartnerDareCard(QueryDocumentSnapshot document) {
        try {
            String dareText = document.getString("dareText");
            String category = document.getString("category");
            Long completedAt = document.getLong("completedAt");
            Long earnedPoints = document.getLong("earnedPoints");

            Log.d("PartnerDares", "Creating card for dare: " + dareText + ", category: " + category);

            LinearLayout dareCard = new LinearLayout(this);
            dareCard.setOrientation(LinearLayout.VERTICAL);
            dareCard.setPadding(20, 16, 20, 16);
            dareCard.setBackgroundResource(R.drawable.glass_card);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 12);
            dareCard.setLayoutParams(cardParams);

            // Category badge
            TextView categoryText = new TextView(this);
            categoryText.setText(category != null ? category.toUpperCase() : "UNKNOWN");
            categoryText.setBackgroundColor(0xFF4CAF50);
            categoryText.setTextColor(0xFFFFFFFF);
            categoryText.setPadding(8, 4, 8, 4);
            categoryText.setTextSize(11);
            categoryText.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryText.setGravity(Gravity.CENTER);

            // Dare text
            TextView dareTextView = new TextView(this);
            dareTextView.setText(dareText);
            dareTextView.setTextColor(0xFFBBBBBB); // Grayed out since completed
            dareTextView.setTextSize(16);
            dareTextView.setGravity(Gravity.CENTER);
            dareTextView.setPadding(0, 8, 0, 8);

            // Completion info
            TextView completionInfo = new TextView(this);
            if (completedAt != null) {
                long daysAgo = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - completedAt);
                String timeText;
                if (daysAgo == 0) {
                    timeText = "✅ Completed today";
                } else if (daysAgo == 1) {
                    timeText = "✅ Completed yesterday";
                } else {
                    timeText = "✅ Completed " + daysAgo + " days ago";
                }

                if (earnedPoints != null) {
                    timeText += " • Earned " + earnedPoints + " points";
                }

                completionInfo.setText(timeText);
            } else {
                completionInfo.setText("✅ COMPLETED");
            }
            completionInfo.setTextColor(0xFF4CAF50);
            completionInfo.setTextSize(12);
            completionInfo.setGravity(Gravity.CENTER);

            dareCard.addView(categoryText);
            dareCard.addView(dareTextView);
            dareCard.addView(completionInfo);

            contentLayout.addView(dareCard);

            Log.d("PartnerDares", "Successfully added dare card to layout");

        } catch (Exception e) {
            Log.e("PartnerDares", "Error creating partner dare card", e);
        }
    }

    private void showNoDares() {
        TextView noDataText = new TextView(this);
        noDataText.setText("📭 No Completed Dares Yet\n\nYour partner hasn't completed any dares you've sent them yet.\n\nSend them some challenges to get started!");
        noDataText.setTextColor(0xFFFFFFFF);
        noDataText.setTextSize(16);
        noDataText.setGravity(Gravity.CENTER);
        noDataText.setPadding(24, 50, 24, 50);
        contentLayout.addView(noDataText);
    }

    private void showError() {
        TextView errorText = new TextView(this);
        errorText.setText("❌ Error Loading Data\n\nSomething went wrong. Please try again later.");
        errorText.setTextColor(0xFFFF6B6B);
        errorText.setTextSize(16);
        errorText.setGravity(Gravity.CENTER);
        errorText.setPadding(24, 50, 24, 50);
        contentLayout.addView(errorText);
    }
}