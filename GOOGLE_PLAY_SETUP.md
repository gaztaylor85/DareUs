# Google Play API Setup for Purchase Verification

This guide explains how to set up Google Play Developer API credentials for server-side purchase verification.

## 📋 Prerequisites

- Google Play Console account with your app published (at least in internal testing)
- Google Cloud Platform project linked to your Play Console
- Firebase project with Functions enabled

## 🔧 Step 1: Enable Google Play Developer API

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your Firebase project
3. Navigate to **APIs & Services** > **Library**
4. Search for **"Google Play Developer API"**
5. Click **Enable**

## 🔑 Step 2: Create Service Account

1. In Google Cloud Console, go to **IAM & Admin** > **Service Accounts**
2. Click **+ CREATE SERVICE ACCOUNT**
3. Fill in details:
   - **Name**: `firebase-functions-billing`
   - **Description**: `Service account for Firebase Functions to verify Google Play purchases`
4. Click **CREATE AND CONTINUE**
5. Skip granting roles (not needed for this use case)
6. Click **DONE**

## 🔐 Step 3: Generate Service Account Key

1. Click on the newly created service account
2. Go to **KEYS** tab
3. Click **ADD KEY** > **Create new key**
4. Choose **JSON** format
5. Click **CREATE**
6. Save the downloaded JSON file securely (⚠️ DO NOT commit to git!)

## 🔗 Step 4: Link Service Account to Play Console

1. Open the downloaded JSON key file
2. Copy the **email** field (looks like `firebase-functions-billing@your-project.iam.gserviceaccount.com`)
3. Go to [Google Play Console](https://play.google.com/console/)
4. Select your app
5. Go to **Setup** > **API access**
6. Scroll to **Service accounts** section
7. Click **Grant access** for your service account
8. Assign the following permissions:
   - ✅ **View financial data, orders, and cancellation survey responses**
   - ✅ **Manage orders and subscriptions**
9. Click **Invite user** > **Send invite**

## 📦 Step 5: Upload Service Account Credentials to Firebase

```bash
# Navigate to your functions directory
cd functions

# Create a service-account.json file (add to .gitignore!)
# Paste the contents of your downloaded JSON key

# Update .gitignore
echo "service-account.json" >> .gitignore

# Verify the file exists
ls -la service-account.json
```

## 🚀 Step 6: Update Functions Code

Your `functions/index.js` already has the verification function. To use the service account:

**Option A: Environment Variable (Recommended for Production)**

```bash
# Set the credentials as an environment variable
firebase functions:config:set google.credentials="$(cat service-account.json)"

# Deploy
firebase deploy --only functions
```

**Option B: Direct File Reference (Easier for Development)**

In `functions/index.js`, update the `verifyPurchase` function:

```javascript
const auth = new google.auth.GoogleAuth({
  keyFile: './service-account.json', // Path to your key file
  scopes: ['https://www.googleapis.com/auth/androidpublisher'],
});
```

## 🧪 Step 7: Test the Integration

### From Android App:

```java
// After successful purchase from Google Play Billing
FirebaseFunctions functions = FirebaseFunctions.getInstance();

Map<String, Object> data = new HashMap<>();
data.put("purchaseToken", purchase.getPurchaseToken());
data.put("productId", purchase.getProducts().get(0)); // e.g., "premium_couple_monthly"
data.put("packageName", getPackageName()); // e.g., "com.DareUs.app"

functions.getHttpsCallable("verifyPurchase")
    .call(data)
    .addOnSuccessListener(result -> {
        Map<String, Object> response = (Map<String, Object>) result.getData();
        String premiumTier = (String) response.get("premiumTier");
        Long expiresAt = (Long) response.get("expiresAt");

        Log.d("Billing", "Premium activated: " + premiumTier);
        // Update UI to show premium features
    })
    .addOnFailureListener(e -> {
        Log.e("Billing", "Purchase verification failed", e);
        // Show error to user
    });
```

### Test Purchase Flow:

1. Make a test purchase in your app (use test account)
2. Check Firebase Functions logs: `firebase functions:log --only verifyPurchase`
3. Verify user's premium status updated in Firestore
4. Check `/purchases` collection for audit record

## ⚠️ Security Checklist

- [ ] Service account JSON key is in `.gitignore`
- [ ] Service account has minimal permissions (only Play Console access)
- [ ] Purchase tokens are validated server-side before granting premium
- [ ] Firestore rules prevent clients from setting `premiumTier` directly
- [ ] Purchase audit trail stored in `/purchases` collection

## 🔍 Troubleshooting

### Error: "403 Forbidden" from Google Play API

**Cause**: Service account not properly linked to Play Console

**Fix**:
- Verify service account email is added in Play Console > API Access
- Check permissions include "View financial data"
- Wait 24 hours after adding (can take time to propagate)

### Error: "401 Unauthorized"

**Cause**: Missing or invalid credentials

**Fix**:
- Verify `service-account.json` exists in `functions/` directory
- Check JSON file is valid (not corrupted)
- Ensure Google Play Developer API is enabled in Cloud Console

### Error: "Invalid purchase token"

**Cause**: Token already consumed or invalid

**Fix**:
- Purchase tokens can only be verified once with some API versions
- Use real test purchases (not mock data)
- Ensure purchase was actually completed

## 📊 Monitoring

Monitor purchase verification success rate:

```bash
# View logs
firebase functions:log --only verifyPurchase

# Count successful verifications (last 7 days)
firebase functions:log --only verifyPurchase | grep "Premium.*granted"
```

## 🔄 Subscription Renewal Handling

For automatic renewal detection, set up a Cloud Pub/Sub topic:

1. Play Console > **Monetization setup** > **Real-time developer notifications**
2. Create a Cloud Pub/Sub topic: `projects/YOUR_PROJECT/topics/play-billing`
3. Grant Pub/Sub permissions to Google Play
4. Create a Firebase Function to listen to subscription events:

```javascript
exports.handleSubscriptionEvent = functions.pubsub
  .topic('play-billing')
  .onPublish(async (message) => {
    const data = message.json;
    // Handle subscription renewed, expired, cancelled events
  });
```

## 📚 Additional Resources

- [Google Play Billing Documentation](https://developer.android.com/google/play/billing)
- [Google Play Developer API Reference](https://developers.google.com/android-publisher)
- [Firebase Functions Documentation](https://firebase.google.com/docs/functions)