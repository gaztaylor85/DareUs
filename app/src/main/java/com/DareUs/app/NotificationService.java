package com.DareUs.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class NotificationService extends Service {

    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "dareus_notifications";
    private static final String CHANNEL_NAME = "DareUs Notifications";

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration daresListener;
    private ListenerRegistration customDaresListener;
    private ListenerRegistration completionsListener;

    private ListenerRegistration haggleListener;
    private ListenerRegistration acceptListener;
    private ListenerRegistration rejectListener;

    // 🎯 KEY FIX: Track when service started to avoid old notifications
    private long serviceStartTime;

    @Override
    public void onCreate() {
        super.onCreate();

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 🎯 Record when service started
        serviceStartTime = System.currentTimeMillis();

        Log.d(TAG, "NotificationService created at: " + serviceStartTime);

        createNotificationChannel();

        if (currentUser != null) {
            // 🎯 Wait 2 seconds before setting up listeners to avoid initial load notifications
            new android.os.Handler().postDelayed(() -> {
                setupFirebaseListeners();
            }, 2000);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );

            // Make sure it's visible and makes sound
            channel.setDescription("Get notified about new dares and achievements");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.setShowBadge(true);
            channel.enableLights(true);
            channel.setLightColor(0xFFB794F6);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created successfully");
        }
    }

    private void setupFirebaseListeners() {
        Log.d(TAG, "Setting up Firebase listeners for user: " + currentUser.getUid());

        // Listen for new regular dares received
        daresListener = db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Regular dares listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // 🎯 ONLY notify about dares sent AFTER service started
                                Long sentAt = dc.getDocument().getLong("sentAt");
                                if (sentAt != null && sentAt > (System.currentTimeMillis() - 2000)) {
                                    String category = dc.getDocument().getString("category");
                                    Log.d(TAG, "NEW regular dare received: " + category);
                                    sendNotification(
                                            "💕 New Dare Received!",
                                            "You got a " + category + " dare from your partner!",
                                            DareInboxActivity.class
                                    );
                                } else {
                                    Log.d(TAG, "Skipping old dare notification (sent before service started)");
                                }
                            }
                        }
                    }
                });

        // Listen for new custom dares received
        customDaresListener = db.collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending_negotiation")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Custom dares listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // 🎯 ONLY notify about custom dares sent AFTER service started
                                Long sentAt = dc.getDocument().getLong("sentAt");
                                if (sentAt != null && sentAt > serviceStartTime) {
                                    Log.d(TAG, "NEW custom dare received for negotiation");
                                    sendNotification(
                                            "✨ Custom Dare for Review!",
                                            "Your partner sent you a custom dare to negotiate!",
                                            CustomDareNegotiationActivity.class
                                    );
                                } else {
                                    Log.d(TAG, "Skipping old custom dare notification");
                                }
                            }
                        }
                    }
                });

        // Listen for dare completions by partner
        completionsListener = db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Completions listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String status = dc.getDocument().getString("status");
                                if ("completed".equals(status)) {
                                    // 🎯 ONLY notify about completions AFTER service started
                                    Long completedAt = dc.getDocument().getLong("completedAt");
                                    if (completedAt != null && completedAt > serviceStartTime) {
                                        String category = dc.getDocument().getString("category");
                                        Long points = dc.getDocument().getLong("earnedPoints");
                                        Log.d(TAG, "Partner completed NEW dare: " + category);
                                        sendNotification(
                                                "🎉 Dare Completed!",
                                                "Your partner completed a " + category + " dare and earned " + points + " points!",
                                                LeaderboardActivity.class
                                        );
                                    } else {
                                        Log.d(TAG, "Skipping old completion notification");
                                    }
                                } else if ("rejected".equals(status)) {
                                    // 🎯 Check rejection timestamp too
                                    Long rejectedAt = dc.getDocument().getLong("rejectedAt");
                                    if (rejectedAt != null && rejectedAt > serviceStartTime) {
                                        String category = dc.getDocument().getString("category");
                                        sendNotification(
                                                "💔 Dare Rejected",
                                                "Your partner rejected a " + category + " dare",
                                                LeaderboardActivity.class
                                        );
                                    }
                                }
                            }
                        }
                    }
                });
        haggleListener = db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid()) // I sent it, partner is haggling
                .whereEqualTo("status", "pending_negotiation")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Haggle listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Long lastCounterOfferAt = dc.getDocument().getLong("lastCounterOfferAt");
                                if (lastCounterOfferAt != null && lastCounterOfferAt > serviceStartTime) {
                                    Long negotiatedPoints = dc.getDocument().getLong("negotiatedPoints");
                                    sendNotification(
                                            "💰 Custom Dare Counter-Offer!",
                                            "Your partner wants " + negotiatedPoints + " points for your custom dare!",
                                            CustomDareNegotiationActivity.class
                                    );
                                }
                            }
                        }
                    }
                });

// Listen for custom dare acceptances
        acceptListener = db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .whereEqualTo("isCustom", true) // ✅ Explicitly check for custom dares
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Accept listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String status = dc.getDocument().getString("status");

                                if ("pending".equals(status)) { // Custom dare was accepted
                                    Long acceptedAt = dc.getDocument().getLong("acceptedAt");
                                    // ✅ Reduce timing restriction to 1 second instead of service start time
                                    if (acceptedAt != null && acceptedAt > (System.currentTimeMillis() - 1000)) {
                                        Long points = dc.getDocument().getLong("points");
                                        sendNotification(
                                                "✅ Custom Dare Accepted!",
                                                "Your partner accepted your custom dare for " + points + " points!",
                                                DareInboxActivity.class
                                        );
                                    }
                                }
                            }
                        }
                    }
                });

// Listen for custom dare rejections
        rejectListener = db.collection("dares")
                .whereEqualTo("fromUserId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Reject listener error", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String status = dc.getDocument().getString("status");
                                Boolean wasCustom = dc.getDocument().getBoolean("isCustom");

                                if ("rejected".equals(status) && Boolean.TRUE.equals(wasCustom)) {
                                    Long rejectedAt = dc.getDocument().getLong("rejectedAt");
                                    if (rejectedAt != null && rejectedAt > serviceStartTime) {
                                        sendNotification(
                                                "❌ Custom Dare Rejected",
                                                "Your partner rejected your custom dare",
                                                CustomDareNegotiationActivity.class
                                        );
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void sendNotification(String title, String message, Class<?> targetActivity) {
        try {
            Intent intent = new Intent(this, targetActivity);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, (int) System.currentTimeMillis(), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.dareus_logo_app)  // 🎯 Use your logo
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setColor(0xFFB794F6)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(new long[]{0, 250, 250, 250})
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d(TAG, "Notification sent: " + title);
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "NotificationService destroyed");
        if (daresListener != null) daresListener.remove();
        if (customDaresListener != null) customDaresListener.remove();
        if (completionsListener != null) completionsListener.remove();
        if (haggleListener != null) haggleListener.remove();
        if (acceptListener != null) acceptListener.remove();
        if (rejectListener != null) rejectListener.remove();
    }
    }
