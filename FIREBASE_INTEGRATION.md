# Firebase Analytics & Crashlytics Integration Guide

## Step 1: Update build.gradle (Already Done ✅)

Your `app/build.gradle` already has Firebase BOM configured:
```gradle
implementation platform('com.google.firebase:firebase-bom:33.6.0')
implementation 'com.google.firebase:firebase-auth'
implementation 'com.google.firebase:firebase-firestore'
implementation 'com.google.firebase:firebase-messaging'
implementation 'com.google.firebase:firebase-functions'
```

## Step 2: Add Analytics & Crashlytics Dependencies

Add these lines to `app/build.gradle` dependencies:

```gradle
dependencies {
    // ... existing dependencies ...

    // Analytics & Crashlytics
    implementation 'com.google.firebase:firebase-analytics'
    implementation 'com.google.firebase:firebase-crashlytics'
}
```

And add this plugin at the top:

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'  // Add this line
}
```

## Step 3: Add Crashlytics Plugin to Project build.gradle

In your root `build.gradle`:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.4.0'
        classpath 'com.google.firebase:firebase-crashlytics-gradle:2.9.9'  // Add this
    }
}
```

## Step 4: Enable Analytics & Crashlytics in Firebase Console

1. Go to Firebase Console: https://console.firebase.google.com/project/dareus-adcc4
2. Navigate to Analytics → Dashboard
3. Click "Enable Google Analytics"
4. Navigate to Crashlytics → Dashboard
5. Click "Enable Crashlytics"

## Step 5: Add Analytics Events to Your App

### Create Analytics Helper Class

Create `AnalyticsHelper.java`:

```java
package com.DareUs.app;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;

public class AnalyticsHelper {
    private static FirebaseAnalytics mFirebaseAnalytics;

    public static void initialize(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    // Dare Events
    public static void logDareSent(String category, int points) {
        Bundle bundle = new Bundle();
        bundle.putString("category", category);
        bundle.putInt("points", points);
        mFirebaseAnalytics.logEvent("dare_sent", bundle);
    }

    public static void logDareCompleted(String category, int points, int daysElapsed) {
        Bundle bundle = new Bundle();
        bundle.putString("category", category);
        bundle.putInt("points", points);
        bundle.putInt("days_elapsed", daysElapsed);
        mFirebaseAnalytics.logEvent("dare_completed", bundle);
    }

    public static void logDareRejected(String category) {
        Bundle bundle = new Bundle();
        bundle.putString("category", category);
        mFirebaseAnalytics.logEvent("dare_rejected", bundle);
    }

    // Badge Events
    public static void logBadgeUnlocked(String badgeId, int pointsAwarded) {
        Bundle bundle = new Bundle();
        bundle.putString("badge_id", badgeId);
        bundle.putInt("points_awarded", pointsAwarded);
        mFirebaseAnalytics.logEvent("badge_unlocked", bundle);
    }

    // Partner Events
    public static void logPartnerLinked() {
        mFirebaseAnalytics.logEvent("partner_linked", null);
    }

    public static void logPartnerUnlinked() {
        mFirebaseAnalytics.logEvent("partner_unlinked", null);
    }

    // Competition Events
    public static void logPrizeSet(String month) {
        Bundle bundle = new Bundle();
        bundle.putString("month", month);
        mFirebaseAnalytics.logEvent("prize_set", bundle);
    }

    public static void logPrizeRevealed(int cost) {
        Bundle bundle = new Bundle();
        bundle.putInt("cost", cost);
        mFirebaseAnalytics.logEvent("prize_revealed", bundle);
    }

    // Premium Events
    public static void logPremiumPurchased(String productId, double price) {
        Bundle bundle = new Bundle();
        bundle.putString("product_id", productId);
        bundle.putDouble("price", price);
        mFirebaseAnalytics.logEvent("premium_purchased", bundle);
    }

    public static void logPremiumCancelled() {
        mFirebaseAnalytics.logEvent("premium_cancelled", null);
    }

    // Screen Views
    public static void logScreenView(String screenName) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    // User Properties
    public static void setUserProperty(String name, String value) {
        mFirebaseAnalytics.setUserProperty(name, value);
    }

    public static void setPremiumStatus(boolean isPremium) {
        setUserProperty("premium_status", isPremium ? "premium" : "free");
    }

    public static void setPartnerLinked(boolean isLinked) {
        setUserProperty("partner_linked", isLinked ? "yes" : "no");
    }

    public static void setTotalDares(int total) {
        setUserProperty("total_dares", String.valueOf(total));
    }

    public static void setBadgesUnlocked(int count) {
        setUserProperty("badges_unlocked", String.valueOf(count));
    }
}
```

### Initialize in Application Class

Create `DareUsApplication.java`:

```java
package com.DareUs.app;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class DareUsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize Analytics
        AnalyticsHelper.initialize(this);

        // Enable Crashlytics collection
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }
}
```

Add to `AndroidManifest.xml`:

```xml
<application
    android:name=".DareUsApplication"
    android:allowBackup="true"
    ...>
```

## Step 6: Add Analytics Calls Throughout App

### In DareSelectionActivity.java

After sending a dare:
```java
AnalyticsHelper.logDareSent(category, points);
AnalyticsHelper.logScreenView("dare_selection");
```

### In DareInboxActivity.java

After completing a dare:
```java
AnalyticsHelper.logDareCompleted(category, totalPoints, daysElapsed);
```

After rejecting a dare:
```java
AnalyticsHelper.logDareRejected(category);
```

### In BadgeTracker.java

After unlocking a badge:
```java
AnalyticsHelper.logBadgeUnlocked(badgeId, 50);
```

### In PartnerLinkingActivity.java

After linking:
```java
AnalyticsHelper.logPartnerLinked();
AnalyticsHelper.setPartnerLinked(true);
```

### In LeaderboardActivity.java

After setting prize:
```java
AnalyticsHelper.logPrizeSet(currentMonth);
```

After revealing prize:
```java
AnalyticsHelper.logPrizeRevealed(25);
```

### In PremiumUpgradeActivity.java

After purchase:
```java
AnalyticsHelper.logPremiumPurchased(productId, price);
AnalyticsHelper.setPremiumStatus(true);
```

## Step 7: Add Crashlytics Error Logging

### Manual Crash Reporting

Add to catch blocks:
```java
try {
    // ... code ...
} catch (Exception e) {
    FirebaseCrashlytics.getInstance().recordException(e);
    Log.e(TAG, "Error message", e);
}
```

### Custom Keys for Context

```java
FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
crashlytics.setCustomKey("user_id", userId);
crashlytics.setCustomKey("dare_id", dareId);
crashlytics.setCustomKey("screen", "DareInbox");
```

### User Identifier

```java
FirebaseCrashlytics.getInstance().setUserId(userId);
```

## Step 8: Test Crashlytics

Add a test crash button in debug builds:

```java
if (BuildConfig.DEBUG) {
    Button crashButton = findViewById(R.id.crashButton);
    crashButton.setOnClickListener(view -> {
        throw new RuntimeException("Test Crash");
    });
}
```

Or use:
```java
FirebaseCrashlytics.getInstance().log("Test log message");
```

## Step 9: Download Updated google-services.json

1. Go to Firebase Console Project Settings
2. Download the latest `google-services.json`
3. Replace the file in `app/google-services.json`
4. This ensures Analytics & Crashlytics are properly configured

## Step 10: Verify Installation

### Check Analytics Debug View
```bash
adb shell setprop debug.firebase.analytics.app com.DareUs.app
```

Then use Firebase Console → Analytics → DebugView to see events in real-time.

### Check Crashlytics
Force a crash, then wait 5-10 minutes. Check Firebase Console → Crashlytics.

## Important Analytics Events to Track

### Core Events (Already Listed Above)
- ✅ dare_sent
- ✅ dare_completed
- ✅ dare_rejected
- ✅ badge_unlocked
- ✅ partner_linked
- ✅ prize_revealed
- ✅ premium_purchased

### Additional Events to Consider
- user_registered
- tutorial_completed
- invite_code_generated
- invite_code_shared
- dare_negotiation_started
- monthly_competition_viewed
- settings_changed
- notification_received
- notification_clicked
- app_update_prompt

### User Properties to Track
- premium_status (free/premium)
- partner_linked (yes/no)
- total_dares (count)
- badges_unlocked (count)
- user_since (date)
- last_active (date)

## Privacy Considerations

### Disable Analytics for Users Who Opt Out

```java
FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false);
```

### GDPR Compliance

Add consent dialog for EU users:
```java
if (isEUUser()) {
    showConsentDialog(() -> {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(true);
    });
}
```

## Performance Monitoring (Optional)

Add to build.gradle:
```gradle
implementation 'com.google.firebase:firebase-perf'
```

Automatically tracks:
- App start time
- Screen rendering
- Network requests

## Cost Monitoring

Firebase Analytics is **FREE** with generous limits:
- Unlimited events
- Unlimited user properties
- 500 distinct events
- 25 user properties

Firebase Crashlytics is also **FREE**:
- Unlimited crash reports
- Custom logging
- User tracking

## Useful Queries in Firebase Console

### Top Dares Categories
Analytics → Events → dare_sent → category parameter

### Completion Rate
Compare dare_sent vs dare_completed events

### Premium Conversion
Users with premium_purchased event / Total users

### Badge Engagement
Events → badge_unlocked → Count by badge_id

### Retention
Analytics → Retention → Cohorts

## Debugging

### Enable Verbose Logging
```bash
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb logcat -v time -s FA FA-SVC
```

### Test Events
```bash
adb shell setprop debug.firebase.analytics.app com.DareUs.app
```

## Quick Integration Checklist

- [ ] Add dependencies to build.gradle
- [ ] Add Crashlytics plugin
- [ ] Create AnalyticsHelper class
- [ ] Create Application class
- [ ] Update AndroidManifest
- [ ] Add analytics calls to activities
- [ ] Add error logging to catch blocks
- [ ] Download new google-services.json
- [ ] Test in DebugView
- [ ] Verify Crashlytics
- [ ] Enable in Firebase Console

## Resources

- Firebase Analytics Docs: https://firebase.google.com/docs/analytics
- Firebase Crashlytics Docs: https://firebase.google.com/docs/crashlytics
- Analytics Events Reference: https://firebase.google.com/docs/analytics/events

---

This integration will give you:
✅ User behavior insights
✅ Crash reporting & debugging
✅ Feature usage analytics
✅ Conversion tracking
✅ User retention metrics
✅ Performance monitoring
