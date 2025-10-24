package com.DareUs.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationScheduler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("DARE_REMINDER".equals(action)) {
            checkAndSendDareReminder(context);
        } else if ("COMPLETION_REMINDER".equals(action)) {
            checkAndSendCompletionReminder(context);
        }
    }

    private void checkAndSendDareReminder(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Check if user has unsent dares available
        PremiumManager premiumManager = new PremiumManager(currentUser.getUid(), null);

        // Send reminder notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "dareus_notifications")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("💕 Send Your Partner a Dare!")
                .setContentText("You still have dares left to send today!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(1001, builder.build());
    }

    private void checkAndSendCompletionReminder(Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Check if user has pending dares to complete
        FirebaseFirestore.getInstance()
                .collection("dares")
                .whereEqualTo("toUserId", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "dareus_notifications")
                                .setSmallIcon(R.drawable.ic_launcher_foreground)
                                .setContentTitle("🎯 Complete Your Dares!")
                                .setContentText("You have " + querySnapshot.size() + " dares waiting for you!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true);

                        NotificationManagerCompat.from(context).notify(1002, builder.build());
                    }
                });
    }

    public static void scheduleReminders(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Daily dare reminder at 7 PM
        Intent dareReminderIntent = new Intent(context, NotificationScheduler.class);
        dareReminderIntent.setAction("DARE_REMINDER");
        PendingIntent dareReminderPendingIntent = PendingIntent.getBroadcast(
                context, 1001, dareReminderIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for 7 PM daily
        long triggerTime = System.currentTimeMillis() + (19 * 60 * 60 * 1000); // 7 PM
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime,
                AlarmManager.INTERVAL_DAY, dareReminderPendingIntent);
    }
}