# 🔨 Quick Build Guide - Release AAB

**Time Required**: 15-20 minutes
**Prerequisite**: Android Studio installed

---

## Step 1: Generate Signing Key (5 minutes)

### In Android Studio:

1. **Menu**: `Build` → `Generate Signed Bundle / APK`

2. **Select**: `Android App Bundle`

3. **Click**: `Create new...` (for keystore)

4. **Fill in details**:

```
Key store path: C:\Users\[YOUR_USERNAME]\dareus-release-key.jks
Password: [CREATE_STRONG_PASSWORD - SAVE THIS!]

Key:
Alias: dareus-release
Password: [SAME_PASSWORD_OR_DIFFERENT - SAVE THIS!]
Validity: 25 years (default is fine)

Certificate:
First and Last Name: [Your Name]
Organizational Unit: [Your Company/Personal]
Organization: [Your Company Name]
City or Locality: [Your City]
State or Province: [Your State]
Country Code: [Your Country Code, e.g., US]
```

5. **Click**: `OK`

### ⚠️ CRITICAL: Save These Credentials!

Create a file `keystore-credentials.txt` (KEEP SECURE, DON'T COMMIT TO GIT):
```
Keystore Path: C:\Users\[USERNAME]\dareus-release-key.jks
Keystore Password: [YOUR_KEYSTORE_PASSWORD]
Key Alias: dareus-release
Key Password: [YOUR_KEY_PASSWORD]

BACKUP THIS FILE AND THE .jks FILE SOMEWHERE SAFE!
```

**Important**:
- If you lose these, you can NEVER update your app on Google Play
- Make multiple backups (USB drive, cloud, password manager)
- Add `*.jks` to .gitignore

---

## Step 2: Build Release Bundle (5 minutes)

1. **Select build variant**: `release`

2. **Enter your keystore credentials** (from Step 1)

3. **Destination folder**: Use default or choose custom location

4. **Click**: `Finish`

### Build Process:
```
✓ Gradle build starting...
✓ Compiling resources...
✓ Running ProGuard...
✓ Shrinking resources...
✓ Packaging app bundle...
✓ Signing bundle...
✓ Build successful!
```

### Output Location:
```
app/build/outputs/bundle/release/app-release.aab
```

---

## Step 3: Verify Build (2 minutes)

### Check File Size:
```
Expected size: 5-15 MB (with ProGuard)
```

### Optional: Test Locally
```bash
# Convert AAB to APK for testing
# Download bundletool: https://github.com/google/bundletool/releases

java -jar bundletool.jar build-apks --bundle=app-release.aab --output=app.apks --mode=universal

# Install
java -jar bundletool.jar install-apks --apks=app.apks
```

---

## Alternative: Command Line Build

If you prefer terminal:

```bash
# Navigate to project directory
cd C:\Users\gazta\AndroidStudioProjects\DareUs

# Clean build
./gradlew clean

# Build release bundle
./gradlew bundleRelease

# Output at: app/build/outputs/bundle/release/app-release.aab
```

---

## Step 4: Upload to Google Play (5 minutes)

### In Google Play Console:

1. Go to your app → **Release** → **Production**

2. Click **Create new release**

3. **Upload**: `app-release.aab`

4. **Release name**: `1.1.0 (Build 2) - Security Update`

5. **Release notes** (copy from APP_STORE_LISTING.md):
```
🔒 Security & Stability Update

- Comprehensive security audit completed
- All vulnerabilities patched
- Enhanced data protection
- Improved fraud prevention
- Faster performance
- Better error handling
- Various stability improvements

Thank you for using DareUs! 💕
```

6. **Review release** → Check everything looks good

7. **Start rollout to Production**

---

## Step 5: Submit for Review (2 minutes)

1. Verify all sections have green checkmarks:
   - [x] Store listing
   - [x] Content rating
   - [x] Data safety
   - [x] Pricing & distribution
   - [x] App content
   - [x] Release

2. Click **Submit for review**

3. **Wait**: 1-3 days for Google review

---

## Troubleshooting

### Error: "Keystore not found"
**Solution**: Make sure you saved the .jks file in the correct location

### Error: "Incorrect keystore password"
**Solution**: Check your saved credentials, retype carefully

### Error: "ProGuard errors"
**Solution**: Check `app/proguard-rules.pro` - should already be configured

### Error: "Build failed"
**Solution**:
1. Clean project: `Build` → `Clean Project`
2. Rebuild: `Build` → `Rebuild Project`
3. Check error message in Build Output

### Error: "Resource not found"
**Solution**: Make sure all drawables/resources exist

---

## Build Checklist

Before building:
- [x] Version code: 2
- [x] Version name: 1.1.0
- [x] ProGuard enabled
- [x] google-services.json present
- [x] All Cloud Functions deployed
- [x] Firestore rules deployed

After building:
- [ ] AAB file created successfully
- [ ] File size reasonable (5-15 MB)
- [ ] Keystore backed up safely
- [ ] Credentials saved securely

---

## Quick Reference

### Build Commands
```bash
# Clean
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release AAB
./gradlew bundleRelease

# Build release APK (for testing)
./gradlew assembleRelease
```

### File Locations
```
Release AAB: app/build/outputs/bundle/release/app-release.aab
Release APK: app/build/outputs/apk/release/app-release.apk
Debug APK:   app/build/outputs/apk/debug/app-debug.apk
```

---

## Testing Before Upload

### Install on Device
```bash
# Install APK
adb install app/build/outputs/apk/release/app-release.apk

# Uninstall
adb uninstall com.DareUs.app
```

### Quick Test Checklist
- [ ] App launches
- [ ] Login works
- [ ] Partner linking works
- [ ] Send dare works
- [ ] Complete dare works
- [ ] Points awarded correctly
- [ ] No crashes

---

## After Submission

### What Happens Next:

1. **Automated checks** (5 minutes)
   - APK scan
   - Policy compliance
   - Security scan

2. **Manual review** (1-3 days)
   - Content review
   - Functionality check
   - Policy compliance

3. **Approval or Rejection**
   - ✅ Approved: Live in 24-48 hours
   - ❌ Rejected: Fix issues and resubmit

### Email Notifications:
- Submission received
- In review
- Approved/Rejected

---

## Important Notes

### Version Management
```
Current: 1.1.0 (Build 2)
Next update: 1.2.0 (Build 3)

Always increment versionCode for each upload!
```

### Rollout Options
```
- Immediate: 100% of users
- Staged: Start at 20%, increase gradually
- Closed testing: Limited users first
```

### Update Frequency
```
Bug fixes: As needed
Features: Monthly recommended
Security: Immediately if critical
```

---

## Success!

When you see this in Google Play Console:
```
✓ App bundle uploaded successfully
✓ Release created: 1.1.0 (2)
✓ Status: Pending review
```

**You're done!** 🎉

Now wait for Google's review (1-3 days).

---

## Pro Tips

### First Upload
- Start with internal testing track
- Test with 10-50 people first
- Move to production after verification

### Updates
- Keep old .jks file forever
- Never lose signing credentials
- Same signing key for all updates

### Testing
- Use internal testing for betas
- Create closed testing for larger group
- Production for public release

---

## Need Help?

**Build errors**: Check Android Studio Build Output
**Upload errors**: Check Google Play Console error messages
**Signing errors**: Verify keystore credentials

---

## Next Steps

After uploading:
1. ✅ Monitor Google Play Console for review status
2. ✅ Check email for notifications
3. ✅ Be ready to fix any policy violations
4. ✅ Plan marketing for launch day
5. ✅ Prepare support channels

---

**Time to build: 15-20 minutes**
**Time to submit: 5 minutes**
**Review time: 1-3 days**

**Let's ship it!** 🚀
