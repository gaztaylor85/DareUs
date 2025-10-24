# Build & Test Instructions

## Quick Reference

**Version**: 1.1.0 (Build 2)
**Min SDK**: 24 (Android 7.0)
**Target SDK**: 34 (Android 14)
**Build Type**: Release (ProGuard enabled)

---

## Building Release APK/AAB

### Method 1: Android Studio (Recommended)

1. **Clean Project**
   ```
   Build → Clean Project
   ```

2. **Generate Signed Bundle/APK**
   ```
   Build → Generate Signed Bundle / APK
   → Select "Android App Bundle" (for Google Play)
   → Create new keystore or select existing
   ```

3. **Keystore Info** (Save this securely!)
   - Keystore Path: `[YOUR_PATH]/dareus-release.jks`
   - Keystore Password: `[SAVE_THIS]`
   - Key Alias: `dareus-key`
   - Key Password: `[SAVE_THIS]`

4. **Build Variants**
   - Select: `release`
   - Sign with keystore
   - Output: `app/release/app-release.aab`

### Method 2: Gradle Command Line

```bash
# Build release AAB
./gradlew bundleRelease

# Build release APK
./gradlew assembleRelease

# Output locations:
# AAB: app/build/outputs/bundle/release/app-release.aab
# APK: app/build/outputs/apk/release/app-release.apk
```

---

## Pre-Build Checklist

### ✅ Configuration
- [x] Version code updated to 2
- [x] Version name updated to 1.1.0
- [x] ProGuard enabled
- [x] Resource shrinking enabled
- [x] All dependencies up to date
- [x] google-services.json present
- [ ] Release signing configured

### ✅ Code Quality
- [x] All security fixes deployed
- [x] Deprecated code removed
- [x] No debug logs in release
- [x] All Cloud Functions integrated
- [x] Error handling added

### ⚠️ Firebase Setup (Required)
- [ ] Download latest google-services.json
- [ ] Add production SHA-1 to Firebase
- [ ] Enable Analytics
- [ ] Enable Crashlytics
- [ ] Test Cloud Functions

---

## Testing Checklist

### 1. Authentication & Onboarding

#### Registration
- [ ] Email validation works
- [ ] Password requirements enforced
- [ ] Account creation successful
- [ ] Welcome email sent (if configured)
- [ ] Profile setup required

#### Login
- [ ] Login with valid credentials works
- [ ] Login with invalid credentials fails properly
- [ ] Error messages clear
- [ ] Forgot password works (if configured)
- [ ] Session persists on app restart

### 2. Partner Linking

#### Invite Code Generation
- [ ] Generate invite code button works
- [ ] Code displayed correctly
- [ ] Code is 12 characters (new secure format)
- [ ] Share functionality works
- [ ] Rate limit (1 per day) enforced
- [ ] Error messages for rate limit clear

#### Partner Connection
- [ ] Enter partner code screen loads
- [ ] Valid code connects successfully
- [ ] Invalid code shows error
- [ ] Cannot use own code
- [ ] Partner already linked shows error
- [ ] Request sent notification works
- [ ] Both users see connection

### 3. Dare System

#### Sending Dares
- [ ] All 4 free categories work
- [ ] Premium category locked for free users
- [ ] Point values display correctly
- [ ] Dare text appears in inbox
- [ ] Partner receives notification
- [ ] Rate limit enforced (10/week free)
- [ ] Rate limit message clear
- [ ] Content moderation blocks profanity

#### Receiving Dares
- [ ] Inbox loads all dares
- [ ] Pending dares show correctly
- [ ] Complete button works
- [ ] Reject button works
- [ ] Points awarded after completion
- [ ] Notification sent to sender
- [ ] UI updates after completion

#### Custom Dares (Premium)
- [ ] Create custom dare works
- [ ] Custom dare sent successfully
- [ ] Custom dare received correctly
- [ ] Content validation works
- [ ] Premium check enforced

### 4. Points & Badges

#### Point System
- [ ] Points awarded for sending dares
- [ ] Points awarded for completing dares
- [ ] Speed bonus calculated correctly
- [ ] Points display updates
- [ ] Cannot manipulate points directly
- [ ] Backend validation working

#### Badge System
- [ ] Badge grid displays correctly
- [ ] Locked badges show as locked
- [ ] Unlocked badges show unlocked
- [ ] Badge unlock animation plays
- [ ] 50 bonus points awarded
- [ ] Cannot unlock fake badges
- [ ] Badge validation working

#### Badge List to Test:
- [ ] First Dare (complete 1 dare)
- [ ] Dare Master (complete 10 dares)
- [ ] Speed Demon (complete in 1 hour)
- [ ] Social Butterfly (share invite code)
- [ ] Negotiator (negotiate 3 dares)
- [ ] High Roller (100+ points in 1 dare)

### 5. Monthly Competition

#### Setup
- [ ] Competition creates automatically
- [ ] Prize selection dialog shows
- [ ] Prize saves correctly
- [ ] Partner sees own prize entry
- [ ] Partner's prize hidden initially
- [ ] Countdown displays correctly

#### During Month
- [ ] Points update in real-time
- [ ] Leaderboard accurate
- [ ] Both users see same scores
- [ ] Winner indicator shows
- [ ] Days remaining countdown works

#### Prize Reveal
- [ ] Reveal button shows (if >7 days left)
- [ ] Costs 25 points
- [ ] Insufficient points blocks reveal
- [ ] Prize shown after payment
- [ ] Cannot reveal again
- [ ] Backend validates payment

### 6. Premium Features

#### Subscription Flow
- [ ] Premium screen loads
- [ ] Pricing displayed correctly
- [ ] Purchase dialog works
- [ ] Google Play billing works
- [ ] Premium status activates
- [ ] Features unlock immediately
- [ ] Subscription shows in Google Play

#### Premium Benefits
- [ ] Unlimited dares (999/week)
- [ ] 5th category (Chores) unlocked
- [ ] Custom dares available
- [ ] Premium badge accessible
- [ ] Premium indicator shows

#### Subscription Management
- [ ] Cancel works in Google Play
- [ ] Premium expires at end of period
- [ ] Features lock after expiration
- [ ] Renewal works automatically

### 7. Security & Edge Cases

#### Security Tests
- [ ] Cannot modify points directly
- [ ] Cannot complete dare twice
- [ ] Cannot send dare to self
- [ ] Cannot bypass rate limits
- [ ] Cannot unlock fake badges
- [ ] Cannot reveal prize for free
- [ ] Cannot manipulate scores
- [ ] Content moderation working

#### Edge Cases
- [ ] No internet connection handled
- [ ] Server timeout handled
- [ ] Invalid data handled
- [ ] Empty states show properly
- [ ] Loading states show
- [ ] Error messages clear

#### Race Conditions
- [ ] Two dares sent simultaneously
- [ ] Badge unlocked twice simultaneously
- [ ] Partner links at same time
- [ ] Prize revealed twice

### 8. UI/UX Testing

#### Visual
- [ ] All screens load properly
- [ ] Colors/gradients correct
- [ ] Text readable
- [ ] Icons display correctly
- [ ] Animations smooth
- [ ] No layout issues

#### Navigation
- [ ] Back button works
- [ ] Home button works
- [ ] Navigation between screens smooth
- [ ] Deep links work (if configured)
- [ ] State persists correctly

#### Responsiveness
- [ ] Works on small screens (5")
- [ ] Works on large screens (6.5"+)
- [ ] Works in portrait mode
- [ ] Landscape mode disabled/works

### 9. Notifications

#### Push Notifications
- [ ] New dare notification received
- [ ] Dare completed notification received
- [ ] Badge unlocked notification received
- [ ] Partner linked notification received
- [ ] Notification opens correct screen
- [ ] Notification permission requested

#### In-App Notifications
- [ ] Toast messages display
- [ ] Success messages show
- [ ] Error messages show
- [ ] Loading indicators work

### 10. Performance

#### Load Times
- [ ] App starts in <3 seconds
- [ ] Screens load in <2 seconds
- [ ] Images load quickly
- [ ] No lag during interactions

#### Memory
- [ ] No memory leaks
- [ ] App doesn't crash on low memory
- [ ] Background tasks work
- [ ] Long sessions stable

#### Battery
- [ ] Reasonable battery usage
- [ ] Background sync efficient
- [ ] Location not used unnecessarily

---

## Device Testing Matrix

### Minimum Testing Devices

| Device | Android | Screen | Status |
|--------|---------|--------|--------|
| Pixel 7 | 14 | 6.3" | [ ] |
| Samsung Galaxy S21 | 13 | 6.2" | [ ] |
| OnePlus 9 | 12 | 6.5" | [ ] |
| Budget Phone | 10 | 5.5" | [ ] |

### Test on Various Android Versions
- [ ] Android 14 (API 34)
- [ ] Android 13 (API 33)
- [ ] Android 12 (API 31)
- [ ] Android 10 (API 29)
- [ ] Android 7 (API 24) - Minimum

---

## Integration Testing

### Cloud Functions
```bash
# Test each function manually
1. Send a dare → Check onDareSent logs
2. Complete a dare → Check onDareCompleted logs
3. Generate invite code → Check generateInviteCode logs
4. Link partner → Check sendPartnerLinkRequest logs
5. Unlock badge → Check awardBadgeBonus logs
6. Reveal prize → Check revealPartnerPrize logs
```

### Firebase Console Checks
- [ ] Firestore rules deployed
- [ ] All 14 functions active
- [ ] No function errors
- [ ] Audit logs working
- [ ] Rate limiting working

---

## Debugging Commands

### View Logs
```bash
# Filter by app
adb logcat | grep DareUs

# Filter by tag
adb logcat -s TAG_NAME

# Clear logs
adb logcat -c
```

### Install APK
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Uninstall App
```bash
adb uninstall com.DareUs.app
```

### Check App Info
```bash
adb shell dumpsys package com.DareUs.app
```

---

## Performance Profiling

### Using Android Studio Profiler
1. Run app in debug mode
2. Open Android Profiler
3. Check:
   - CPU usage
   - Memory usage
   - Network activity
   - Energy consumption

### Benchmark Targets
- App startup: <3 seconds
- Screen transition: <300ms
- Network request: <2 seconds
- Database query: <500ms

---

## Crash Testing

### Force Crashes
```java
// Test crash reporting
throw new RuntimeException("Test crash");

// Test non-fatal error
try {
    // something
} catch (Exception e) {
    FirebaseCrashlytics.getInstance().recordException(e);
}
```

### Verify in Firebase Console
- Wait 5-10 minutes
- Check Crashlytics dashboard
- Verify stack traces
- Check user counts

---

## Beta Testing

### Internal Testing (10-100 users)
1. Upload AAB to Google Play Console
2. Create internal testing track
3. Add testers by email
4. Share opt-in link
5. Collect feedback

### Closed Testing (100-1000 users)
1. Create closed testing track
2. Set up feedback channel
3. Monitor for 1-2 weeks
4. Fix critical issues
5. Iterate based on feedback

---

## Pre-Launch Final Checks

### 24 Hours Before
- [ ] All tests passed
- [ ] No critical bugs
- [ ] All features working
- [ ] Performance acceptable
- [ ] Notifications working
- [ ] Cloud Functions active
- [ ] Rate limits working
- [ ] Security verified

### Launch Day
- [ ] Build signed AAB
- [ ] Upload to Google Play
- [ ] Submit for review
- [ ] Prepare for rollout
- [ ] Monitor console
- [ ] Be ready for hotfix

---

## Common Issues & Solutions

### Build Errors
**Issue**: ProGuard errors
**Solution**: Check proguard-rules.pro, add keep rules

**Issue**: google-services.json not found
**Solution**: Download from Firebase Console

**Issue**: Dependency conflicts
**Solution**: Use Firebase BOM for version management

### Runtime Errors
**Issue**: Cloud Function timeout
**Solution**: Check function logs, increase timeout

**Issue**: Points not awarded
**Solution**: Check backend triggers, verify Firestore rules

**Issue**: Badge not unlocking
**Solution**: Verify badge ID in whitelist

---

## Getting Help

**Firebase Issues**: Check Firebase Console → Functions → Logs
**Build Issues**: Check Android Studio → Build → Build Output
**Runtime Issues**: Check Logcat for stack traces
**Security Issues**: Check FINAL_SECURITY_AUDIT.md

---

## Success Criteria

✅ All tests passed
✅ No crashes in 100+ test sessions
✅ <1% error rate
✅ All Cloud Functions working
✅ Security audit passed
✅ Performance acceptable
✅ User feedback positive

---

**You're ready to launch when all checklist items are complete!** 🚀
