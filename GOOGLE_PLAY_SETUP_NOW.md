# 🚀 Google Play Setup - DO THIS NOW

Since you have a Google Play Developer account, you can complete this in **30-60 minutes**!

---

## Step 1: Create New App (5 minutes)

1. Go to https://play.google.com/console
2. Click **"Create app"**
3. Fill in details:

### App Details
```
App name: DareUs
Default language: English (United States)
App or game: App
Free or paid: Free
```

### Declarations
- [x] I confirm this app complies with Google Play policies
- [x] I confirm this app complies with US export laws

4. Click **"Create app"**

---

## Step 2: Store Listing (15 minutes)

### App Details Tab

**Short description** (80 characters max):
```
Fun couples dare game! Send dares, earn points, compete monthly. Strengthen bonds! 💕
```

**Full description** (4000 characters - copy from APP_STORE_LISTING.md lines 9-100):
```
**Spice up your relationship with DareUs - the ultimate couples dare game!**

Transform ordinary moments into exciting adventures with your partner. Send dares, complete challenges, earn points, and compete in monthly competitions. Whether you're looking to break routine, create memories, or just have fun together, DareUs makes every day an adventure! 💕

### 🎯 HOW IT WORKS
[Copy full description from APP_STORE_LISTING.md]
```

**App icon** (512x512):
- Upload: `app/src/main/res/drawable/dareus_logo_app.png`
- Or temporarily use: Any 512x512 PNG (you can update later)

**Feature graphic** (1024x500):
- Create quick version: Purple/pink gradient with text "DareUs - Couples Dare Game"
- Or skip for now and add in next update

**Screenshots** (REQUIRED - minimum 2):
- Need 1080x1920 or 1080x2340
- Take from emulator or device
- Minimum 2, recommended 6-8

### Quick Screenshot Guide:
1. Open app in Android Studio emulator
2. Navigate to key screens
3. Press Cmd+S (Mac) or Ctrl+S (Win) to capture
4. Take these screens:
   - Welcome/login screen
   - Dare categories screen
   - Dare inbox
   - Badge collection
   - Leaderboard
   - Premium screen

**Phone screenshots** (REQUIRED):
- Upload at least 2 screenshots
- Max 8 screenshots

---

## Step 3: Store Settings (5 minutes)

### App Category
```
Category: Lifestyle
Tags: Relationships, Dating, Entertainment
```

### Contact Details
```
Email: [YOUR_SUPPORT_EMAIL]
Website: [YOUR_WEBSITE] (optional)
Phone: (optional)
```

### Privacy Policy (REQUIRED)
```
URL: [HOST_PRIVACY_POLICY_SOMEWHERE]
```

**Quick option**: Use GitHub or create a simple site
- GitHub Pages (free): https://pages.github.com/
- Or use Pastebin temporarily: https://pastebin.com/ (paste PRIVACY_POLICY.md)

---

## Step 4: Content Rating (10 minutes)

1. Go to **Content rating** section
2. Click **"Start questionnaire"**
3. Enter email address
4. Select **"Entertainment"** category

### Answer Questions:
```
Does your app contain violence?: NO
Does your app contain sexual content?: NO
Does your app contain profanity?: NO (it's filtered)
Does your app contain controlled substances?: NO
Does your app contain gambling?: NO
Does your app contain realistic weapons?: NO
```

5. Submit questionnaire
6. Rating will be: **Teen (13+)**

---

## Step 5: Target Audience (5 minutes)

### Age Groups
```
Target age: 18-24, 25-34, 35-44
```

### Store Presence
```
Available in: All countries (or select specific ones)
```

---

## Step 6: App Access (2 minutes)

```
All or some functionality restricted: NO
Special access required: NO
```

If you want to provide test account:
```
Test account: [test_email@example.com]
Password: [test_password]
```

---

## Step 7: Ads (1 minute)

```
Does your app contain ads?: NO
```

---

## Step 8: Data Safety (10 minutes)

This is CRITICAL and matches your PRIVACY_POLICY.md:

### Data Collection
**Does your app collect or share user data?**: YES

### Data Types Collected:

#### Personal Information
- [x] Name (First name only)
- [x] Email address

#### App Activity
- [x] App interactions
- [x] In-app search history (dare history)

#### App Info and Performance
- [x] Crash logs
- [x] Diagnostics

### Data Usage:

**Personal info (email, name)**:
- Purpose: App functionality, Account management
- Optional: NO
- Transferred off device: YES
- Encrypted in transit: YES
- Can user request deletion: YES

**App activity**:
- Purpose: App functionality, Analytics
- Optional: NO
- Transferred off device: YES
- Encrypted in transit: YES
- Can user request deletion: YES

### Privacy Policy
```
URL: [YOUR_PRIVACY_POLICY_URL]
```

---

## Step 9: Government Apps (1 minute)

```
Is this a government app?: NO
```

---

## Step 10: Financial Features (2 minutes)

### In-app Purchases
```
Does your app offer in-app purchases?: YES
```

List products:
```
- Premium Monthly Subscription: $4.99/month
- Premium Yearly Subscription: $39.99/year
```

**Note**: You'll set up actual products later in "Monetize" section

---

## Step 11: Production Track Setup (5 minutes)

1. Go to **"Release"** → **"Production"**
2. Click **"Create new release"**
3. Upload release type: **Android App Bundle**

### First Build Required:
```
You need to build and upload an AAB file
See QUICK_BUILD_GUIDE.md for instructions
```

**For now**, you can save draft and come back when AAB is ready.

---

## Step 12: App Signing (Already Done)

Google Play manages signing automatically for new apps.

If asked:
```
[x] Let Google manage and protect my app signing key
```

---

## Quick Temporary Solutions

### Don't Have Assets Yet?

**App Icon**:
- Use existing: `app/src/main/res/drawable/dareus_logo_app.png`
- Or create 512x512 purple square with heart ❤️

**Screenshots**:
- Use Android Studio emulator
- Pixel 5 or Pixel 6 device
- Take 2 minimum, 6-8 ideal

**Privacy Policy**:
- Use GitHub Gist: https://gist.github.com/
- Create new gist, paste PRIVACY_POLICY.md
- Use raw URL

**Feature Graphic**:
- Optional - can add later
- Or create simple 1024x500 with text

---

## What You Can Skip for Initial Submission

These can be added later:
- [ ] Feature graphic (optional)
- [ ] Promo video (optional)
- [ ] Additional screenshots beyond 2
- [ ] Store listing translations
- [ ] Device catalogs

---

## After Completing Store Setup

You'll be at this checklist:

### Ready to Build ✅
- [x] Store listing created
- [x] Content rating set
- [x] Data safety filled
- [x] App details complete

### Need to Build 🔨
- [ ] Generate signing key (5 min)
- [ ] Build release AAB (10 min)
- [ ] Upload to production track (5 min)
- [ ] Submit for review (2 min)

**Then**: Wait 1-3 days for Google review

---

## Store Listing Checklist

Before submitting, verify:

- [x] App name: DareUs
- [x] Short description (80 chars)
- [x] Full description (4000 chars)
- [x] App icon (512x512)
- [x] 2+ screenshots
- [x] Category: Lifestyle
- [x] Content rating: Teen
- [x] Data safety form complete
- [x] Privacy policy URL
- [x] Contact email
- [x] Pricing: Free

---

## Common Issues

### "Privacy Policy Required"
**Solution**: Host PRIVACY_POLICY.md somewhere public:
- GitHub Gist (free): https://gist.github.com/
- GitHub Pages (free): https://pages.github.com/
- Pastebin: https://pastebin.com/
- Your own website

### "Screenshots Required"
**Solution**: Need at least 2 screenshots:
1. Open Android Studio
2. Run app in emulator
3. Take 2 screenshots (Cmd+S or Ctrl+S)
4. Upload to Google Play

### "Content Rating Incomplete"
**Solution**: Complete questionnaire in Content Rating section

---

## Timeline

**Store Setup Only**: 30-60 minutes
**Store Setup + Build**: 1-2 hours
**Store Setup + Build + Submit**: 2-3 hours
**Google Review**: 1-3 days

---

## Next Document to Read

After completing store setup:
→ **QUICK_BUILD_GUIDE.md** (creating this next!)

This will show you how to:
1. Generate signing key (5 min)
2. Build release AAB (10 min)
3. Upload and submit (5 min)

---

## 🎯 Your Status

✅ Google Play account - **DONE**
⏳ Store listing - **START HERE** (30-60 min)
⏳ Build AAB - **NEXT** (15 min)
⏳ Submit - **FINAL STEP** (5 min)

**You're one hour away from submitting to Google Play!** 🚀

---

## Need Help?

- Google Play Console: https://play.google.com/console
- Google Play Help: https://support.google.com/googleplay/android-developer
- Store Listing Guide: https://support.google.com/googleplay/android-developer/answer/9859152

---

**Let's get your app in the store!** 💪
