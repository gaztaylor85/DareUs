const {onDocumentCreated, onDocumentUpdated} = require('firebase-functions/v2/firestore');
const {onCall, HttpsError} = require('firebase-functions/v2/https');
const {setGlobalOptions} = require('firebase-functions/v2');
const admin = require('firebase-admin');
const {google} = require('googleapis');
const Filter = require('bad-words');

// Set global options for all v2 functions
setGlobalOptions({
  maxInstances: 10,
  region: 'us-central1'
});

admin.initializeApp();

const db = admin.firestore();
const profanityFilter = new Filter();

// ========================================
// AUDIT LOGGING
// ========================================

/**
 * Log suspicious activity for monitoring
 */
async function logAuditEvent(eventType, userId, details) {
  try {
    await db.collection('auditLog').add({
      eventType: eventType,
      userId: userId,
      details: details,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      timestampMs: Date.now()
    });

    console.log(`🔍 Audit log: ${eventType} by ${userId}`);
  } catch (error) {
    console.error('Error logging audit event:', error);
    // Don't throw - logging failures shouldn't break functionality
  }
}

// ========================================
// NOTIFICATION RATE LIMITING
// ========================================

/**
 * Check notification rate limits to prevent spam
 */
async function checkNotificationRateLimit(fromUserId, toToken) {
  const oneHourAgo = Date.now() - (60 * 60 * 1000);

  // Count notifications sent TO this token in the last hour
  const recentNotificationsQuery = await db.collection('notifications')
    .where('toToken', '==', toToken)
    .where('timestamp', '>=', oneHourAgo)
    .get();

  const notificationCount = recentNotificationsQuery.size;
  const hourlyLimit = 20; // Max 20 notifications per recipient per hour

  if (notificationCount >= hourlyLimit) {
    return {
      allowed: false,
      reason: `Notification limit reached (${hourlyLimit}/hour)`,
      count: notificationCount
    };
  }

  // Also check notifications FROM this user (prevent sender spam)
  if (fromUserId) {
    const senderQuery = await db.collection('notifications')
      .where('fromUserId', '==', fromUserId)
      .where('timestamp', '>=', oneHourAgo)
      .get();

    const senderCount = senderQuery.size;
    const senderLimit = 30; // Max 30 sent per user per hour

    if (senderCount >= senderLimit) {
      return {
        allowed: false,
        reason: `You've sent too many notifications (${senderLimit}/hour)`,
        count: senderCount
      };
    }
  }

  return {
    allowed: true,
    count: notificationCount
  };
}

// ========================================
// NOTIFICATION HANDLER WITH RATE LIMITING
// ========================================
exports.sendQueuedNotifications = onDocumentCreated('notifications/{notificationId}', async (event) => {
  const snap = event.data;
  if (!snap) return null;

  const data = snap.data();
  const notificationId = event.params.notificationId;
  console.log('New notification:', notificationId);

  // Skip if already sent
  if (data.sent) {
    return null;
  }

  // Validate required fields
  if (!data.toToken || !data.title || !data.body) {
    console.error('Missing required notification fields');
    await snap.ref.update({
      sent: false,
      error: 'Missing required fields',
      failedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return null;
  }

  try {
    // Rate limit check
    const rateLimitCheck = await checkNotificationRateLimit(
      data.fromUserId || null,
      data.toToken
    );

    if (!rateLimitCheck.allowed) {
      console.warn(`⚠️ Notification rate limit exceeded: ${rateLimitCheck.reason}`);

      await snap.ref.update({
        sent: false,
        blocked: true,
        blockReason: rateLimitCheck.reason,
        blockedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return null;
    }

    // Validate notification content
    if (data.body.length > 500) {
      console.warn('Notification body too long');
      await snap.ref.update({
        sent: false,
        error: 'Body too long (max 500 chars)',
        failedAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return null;
    }

    // Send the notification
    const message = {
      token: data.toToken,
      notification: {
        title: data.title.substring(0, 100), // Limit title length
        body: data.body.substring(0, 500)    // Limit body length
      },
      android: {
        priority: 'high',
        notification: {
          sound: 'default',
          clickAction: 'FLUTTER_NOTIFICATION_CLICK'
        }
      }
    };

    const response = await admin.messaging().send(message);
    console.log(`✅ Notification sent successfully:`, response);

    await snap.ref.update({
      sent: true,
      sentAt: admin.firestore.FieldValue.serverTimestamp(),
      messageId: response
    });

    return response;
  } catch (error) {
    console.error('Error sending notification:', error);

    // Handle specific FCM errors
    let errorMessage = error.message;
    if (error.code === 'messaging/invalid-registration-token' ||
        error.code === 'messaging/registration-token-not-registered') {
      errorMessage = 'Invalid or expired FCM token';
    }

    await snap.ref.update({
      sent: false,
      error: errorMessage,
      errorCode: error.code || 'unknown',
      failedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return null;
  }
});

// ========================================
// HELPER FUNCTIONS
// ========================================

/**
 * Calculate bonus points based on completion speed
 */
function calculateBonusPoints(basePoints, sentAt, completedAt) {
  const timeElapsed = completedAt - sentAt;
  const daysElapsed = Math.floor(timeElapsed / (24 * 60 * 60 * 1000)) + 1;

  let bonusMultiplier = 0;
  switch (daysElapsed) {
    case 1: bonusMultiplier = 1.0; break;    // Double points day 1
    case 2: bonusMultiplier = 0.75; break;   // 1.75x points day 2
    case 3: bonusMultiplier = 0.5; break;    // 1.5x points day 3
    case 4: bonusMultiplier = 0.25; break;   // 1.25x points day 4
    default: bonusMultiplier = 0; break;     // Base points after day 4
  }

  const bonusPoints = Math.floor(basePoints * bonusMultiplier);
  return {
    bonusPoints,
    totalPoints: basePoints + bonusPoints,
    daysElapsed
  };
}

/**
 * Award points to a user with validation
 */
async function awardPoints(userId, points, reason) {
  try {
    const userRef = db.collection('dareus').doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      console.error(`User ${userId} not found`);
      return false;
    }

    const currentPoints = userDoc.data().points || 0;
    const newPoints = currentPoints + points;

    await userRef.update({
      points: newPoints,
      lastPointsUpdate: admin.firestore.FieldValue.serverTimestamp(),
      lastPointsReason: reason
    });

    console.log(`Awarded ${points} points to ${userId} for: ${reason}. New total: ${newPoints}`);
    return true;
  } catch (error) {
    console.error(`Error awarding points to ${userId}:`, error);
    return false;
  }
}

// ========================================
// CONTENT MODERATION
// ========================================

/**
 * Enhanced profanity filter with leet speak detection
 */
function containsProfanity(text) {
  if (!text) return false;

  // Normalize text for better detection
  const normalized = text.toLowerCase()
    // Leet speak conversions
    .replace(/1/g, 'i')
    .replace(/3/g, 'e')
    .replace(/4/g, 'a')
    .replace(/5/g, 's')
    .replace(/7/g, 't')
    .replace(/0/g, 'o')
    .replace(/\@/g, 'a')
    .replace(/\$/g, 's')
    // Remove spaces between letters (k i l l -> kill)
    .replace(/\s+/g, '')
    // Remove special characters
    .replace(/[^a-z]/g, '');

  // Check with profanity filter
  if (profanityFilter.isProfane(text) || profanityFilter.isProfane(normalized)) {
    return true;
  }

  // Additional custom checks for explicit content
  const explicitPatterns = [
    /n\s*u\s*d\s*e/i,
    /s\s*e\s*x/i,
    /k\s*i\s*l\s*l/i,
    /d\s*i\s*e/i,
    /h\s*u\s*r\s*t/i,
    /\bxxx\b/i,
    /p\s*o\s*r\s*n/i,
  ];

  for (const pattern of explicitPatterns) {
    if (pattern.test(text)) {
      return true;
    }
  }

  return false;
}

/**
 * Validate dare content for safety and appropriateness
 */
function validateDareContent(dareText, isCustom = false) {
  const errors = [];

  // Length validation
  if (!dareText || dareText.trim().length === 0) {
    errors.push('Dare text cannot be empty');
  }

  if (dareText.length < 5) {
    errors.push('Dare text too short (minimum 5 characters)');
  }

  if (dareText.length > 500) {
    errors.push('Dare text too long (maximum 500 characters)');
  }

  // Profanity check
  if (containsProfanity(dareText)) {
    errors.push('Dare contains inappropriate language');
  }

  // Check for URLs (prevent phishing/spam)
  const urlPattern = /(https?:\/\/|www\.|\.com|\.org|\.net)/gi;
  if (urlPattern.test(dareText)) {
    errors.push('Dares cannot contain URLs or links');
  }

  // Check for phone numbers
  const phonePattern = /\d{3}[-.\s]?\d{3}[-.\s]?\d{4}/g;
  if (phonePattern.test(dareText)) {
    errors.push('Dares cannot contain phone numbers');
  }

  // Check for email addresses
  const emailPattern = /[a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\.[a-zA-Z0-9_-]+/gi;
  if (emailPattern.test(dareText)) {
    errors.push('Dares cannot contain email addresses');
  }

  // Check for excessive special characters (spam indicator)
  const specialCharCount = (dareText.match(/[^a-zA-Z0-9\s]/g) || []).length;
  if (specialCharCount > dareText.length * 0.3) {
    errors.push('Dare contains too many special characters');
  }

  // Check for excessive caps (spam indicator)
  const capsCount = (dareText.match(/[A-Z]/g) || []).length;
  const letterCount = (dareText.match(/[a-zA-Z]/g) || []).length;
  if (letterCount > 0 && capsCount / letterCount > 0.7 && dareText.length > 10) {
    errors.push('Please don\'t use excessive capital letters');
  }

  // Custom dare specific checks
  if (isCustom) {
    // Check for self-harm or dangerous activities
    const dangerousPatterns = [
      /harm.*self/i,
      /cut.*self/i,
      /suicide/i,
      /jump.*off/i,
      /crash/i,
      /drunk.*driv/i,
    ];

    for (const pattern of dangerousPatterns) {
      if (pattern.test(dareText)) {
        errors.push('Dare may involve dangerous or harmful activities');
        break;
      }
    }
  }

  return {
    valid: errors.length === 0,
    errors: errors,
    cleaned: errors.length === 0 ? dareText.trim() : null
  };
}

/**
 * Validate dare before creation (callable function)
 */
exports.validateDare = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const data = request.data;

  const {dareText, isCustom} = data;

  if (!dareText) {
    throw new HttpsError('invalid-argument', 'Dare text required');
  }

  try {
    const validation = validateDareContent(dareText, isCustom || false);

    if (!validation.valid) {
      return {
        valid: false,
        errors: validation.errors,
        message: validation.errors[0] // Return first error
      };
    }

    return {
      valid: true,
      cleaned: validation.cleaned,
      message: 'Dare content is acceptable'
    };

  } catch (error) {
    console.error('Error validating dare:', error);
    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// DARE COMPLETION - SERVER-SIDE POINT VALIDATION
// ========================================
exports.onDareCompleted = onDocumentUpdated('dares/{dareId}', async (event) => {
    const change = event.data;
    if (!change) return null;

    const before = change.before.data();
    const after = change.after.data();
    const dareId = event.params.dareId;

    // Only process when status changes TO completed
    if (before.status !== 'completed' && after.status === 'completed') {
      console.log(`Dare ${dareId} completed by ${after.toUserId}`);

      try {
        const basePoints = after.points || 0;
        const sentAt = after.sentAt || Date.now();
        const completedAt = after.completedAt || Date.now();

        // Calculate actual points with time bonus
        const pointsCalc = calculateBonusPoints(basePoints, sentAt, completedAt);

        // Store calculated points back to dare document
        await change.after.ref.update({
          earnedPoints: pointsCalc.totalPoints,
          bonusPoints: pointsCalc.bonusPoints,
          basePoints: basePoints,
          completionDay: pointsCalc.daysElapsed
        });

        // Award points to the completer
        await awardPoints(
          after.toUserId,
          pointsCalc.totalPoints,
          `Completed ${after.category} dare in ${pointsCalc.daysElapsed} days`
        );

        // Increment total dares completed counter
        await db.collection('dareus').doc(after.toUserId).update({
          totalDares: admin.firestore.FieldValue.increment(1)
        });

        console.log(`✅ Awarded ${pointsCalc.totalPoints} points (${basePoints} base + ${pointsCalc.bonusPoints} bonus)`);

        return null;
      } catch (error) {
        console.error(`Error processing dare completion:`, error);
        return null;
      }
    }

    return null;
  });

// ========================================
// RATE LIMITING HELPER
// ========================================

/**
 * Check if user has exceeded rate limits for dare sending
 */
async function checkRateLimit(userId) {
  const now = Date.now();
  const oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000);

  // Get user's premium status
  const userDoc = await db.collection('dareus').doc(userId).get();

  if (!userDoc.exists) {
    return {allowed: false, reason: 'User not found'};
  }

  const userData = userDoc.data();
  const premiumTier = userData.premiumTier || 'FREE';
  const premiumExpiresAt = userData.premiumExpiresAt || 0;

  // Check if premium is active
  const isPremium = premiumTier !== 'FREE' && premiumExpiresAt > now;

  // Premium users have no limits
  if (isPremium) {
    return {allowed: true, isPremium: true, remaining: 999};
  }

  // Count dares sent in the last 7 days for FREE users
  const recentDaresQuery = await db.collection('dares')
    .where('fromUserId', '==', userId)
    .where('sentAt', '>=', oneWeekAgo)
    .get();

  const weeklyDareCount = recentDaresQuery.size;
  const weeklyLimit = 5; // Free users: 5 dares per week

  if (weeklyDareCount >= weeklyLimit) {
    return {
      allowed: false,
      reason: `Weekly limit reached (${weeklyLimit} dares)`,
      limit: weeklyLimit,
      used: weeklyDareCount,
      remaining: 0,
      isPremium: false
    };
  }

  return {
    allowed: true,
    limit: weeklyLimit,
    used: weeklyDareCount,
    remaining: weeklyLimit - weeklyDareCount,
    isPremium: false
  };
}

// ========================================
// DARE SENT - AWARD SENDER POINTS + RATE LIMIT CHECK
// ========================================
exports.onDareSent = onDocumentCreated('dares/{dareId}', async (event) => {
    const snap = event.data;
    if (!snap) return null;

    const dare = snap.data();
    const dareId = event.params.dareId;

    console.log(`New dare ${dareId} sent from ${dare.fromUserId} to ${dare.toUserId}`);

    try {
      // STEP 1: Validate content
      const contentValidation = validateDareContent(dare.dareText, dare.isCustom || false);

      if (!contentValidation.valid) {
        console.warn(`⚠️ Content validation failed for dare ${dareId}: ${contentValidation.errors.join(', ')}`);

        // Audit log: inappropriate content attempt
        await logAuditEvent('inappropriate_content_detected', dare.fromUserId, {
          dareId: dareId,
          dareText: dare.dareText,
          errors: contentValidation.errors,
          category: dare.category,
          isCustom: dare.isCustom
        });

        // Mark dare as rejected due to inappropriate content
        await snap.ref.update({
          status: 'rejected',
          rejectedAt: Date.now(),
          rejectionReason: 'inappropriate_content',
          rejectionMessage: contentValidation.errors[0]
        });

        return null;
      }

      // STEP 2: Check rate limits
      const rateLimitCheck = await checkRateLimit(dare.fromUserId);

      if (!rateLimitCheck.allowed) {
        console.warn(`⚠️ Rate limit exceeded for user ${dare.fromUserId}: ${rateLimitCheck.reason}`);

        // Audit log: dare spam attempt
        await logAuditEvent('dare_rate_limit_exceeded', dare.fromUserId, {
          dareId: dareId,
          reason: rateLimitCheck.reason,
          limit: rateLimitCheck.limit,
          used: rateLimitCheck.used
        });

        // Mark dare as rejected due to rate limit
        await snap.ref.update({
          status: 'rejected',
          rejectedAt: Date.now(),
          rejectionReason: 'rate_limit_exceeded',
          rejectionMessage: rateLimitCheck.reason
        });

        return null;
      }

      const senderPoints = dare.points || 1;

      // Award points to sender
      await awardPoints(
        dare.fromUserId,
        senderPoints,
        `Sent ${dare.category} dare`
      );

      // Increment dares sent counter
      await db.collection('dareus').doc(dare.fromUserId).update({
        daresSent: admin.firestore.FieldValue.increment(1),
        lastDareSentAt: Date.now()
      });

      console.log(`✅ Awarded ${senderPoints} points to sender (${rateLimitCheck.remaining} remaining this week)`);
      return null;
    } catch (error) {
      console.error(`Error processing dare send:`, error);
      return null;
    }
  });

// ========================================
// INVITE CODE MANAGEMENT
// ========================================

/**
 * Generate a cryptographically secure 12-character invite code
 */
function generateSecureInviteCode() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Removed confusing chars (0, O, I, 1)
  const crypto = require('crypto');
  let code = '';

  for (let i = 0; i < 12; i++) {
    const randomByte = crypto.randomBytes(1)[0];
    code += chars[randomByte % chars.length];
  }

  return code;
}

/**
 * Check invite code generation rate limit (1 per day)
 */
async function checkInviteCodeRateLimit(userId) {
  const userDoc = await db.collection('dareus').doc(userId).get();

  if (!userDoc.exists) {
    return {allowed: false, reason: 'User not found'};
  }

  const lastCodeGeneratedAt = userDoc.data().lastCodeGeneratedAt || 0;
  const now = Date.now();
  const oneDayAgo = now - (24 * 60 * 60 * 1000);

  if (lastCodeGeneratedAt > oneDayAgo) {
    const hoursRemaining = Math.ceil((lastCodeGeneratedAt + 24 * 60 * 60 * 1000 - now) / (60 * 60 * 1000));
    return {
      allowed: false,
      reason: `You can only generate one invite code per day. Try again in ${hoursRemaining} hours.`
    };
  }

  return {allowed: true};
}

/**
 * Generate a new invite code with rate limiting
 */
exports.generateInviteCode = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;

  try {
    // Check rate limit
    const rateLimitCheck = await checkInviteCodeRateLimit(userId);

    if (!rateLimitCheck.allowed) {
      throw new HttpsError('resource-exhausted', rateLimitCheck.reason);
    }

    // Generate code and check uniqueness
    let inviteCode;
    let isUnique = false;
    let attempts = 0;
    const maxAttempts = 10;

    while (!isUnique && attempts < maxAttempts) {
      inviteCode = generateSecureInviteCode();

      // Check if code already exists
      const existingCode = await db.collection('dareus')
        .where('inviteCode', '==', inviteCode)
        .limit(1)
        .get();

      if (existingCode.empty) {
        isUnique = true;
      }

      attempts++;
    }

    if (!isUnique) {
      throw new HttpsError('internal', 'Failed to generate unique code');
    }

    // Save the code to user's document
    await db.collection('dareus').doc(userId).update({
      inviteCode: inviteCode,
      inviteCodeGeneratedAt: admin.firestore.FieldValue.serverTimestamp(),
      lastCodeGeneratedAt: Date.now()
    });

    console.log(`✅ Generated invite code ${inviteCode} for user ${userId}`);

    return {
      success: true,
      inviteCode: inviteCode,
      message: 'Invite code generated successfully!'
    };

  } catch (error) {
    console.error('Error generating invite code:', error);
    throw new HttpsError('internal', error.message);
  }
});

/**
 * Verify invite code with rate limiting (prevent brute force)
 */
exports.verifyInviteCode = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const {inviteCode} = request.data;

  if (!inviteCode || inviteCode.length !== 12) {
    throw new HttpsError('invalid-argument', 'Invalid invite code format');
  }

  try {
    // Rate limit: Check recent failed attempts
    const userDoc = await db.collection('dareus').doc(userId).get();
    const failedAttempts = userDoc.data().failedInviteAttempts || [];

    // Filter attempts from last hour
    const oneHourAgo = Date.now() - (60 * 60 * 1000);
    const recentFailedAttempts = failedAttempts.filter(timestamp => timestamp > oneHourAgo);

    // Allow max 5 failed attempts per hour
    if (recentFailedAttempts.length >= 5) {
      // Audit log: potential brute force attack
      await logAuditEvent('invite_code_brute_force_attempt', userId, {
        attemptedCode: inviteCode,
        failedAttempts: recentFailedAttempts.length
      });

      throw new HttpsError(
        'resource-exhausted',
        'Too many failed attempts. Please wait 1 hour before trying again.'
      );
    }

    // Search for user with this invite code
    const partnerQuery = await db.collection('dareus')
      .where('inviteCode', '==', inviteCode.toUpperCase())
      .limit(1)
      .get();

    if (partnerQuery.empty) {
      // Code not found - record failed attempt
      await db.collection('dareus').doc(userId).update({
        failedInviteAttempts: admin.firestore.FieldValue.arrayUnion(Date.now())
      });

      // Audit log: failed invite code attempt
      await logAuditEvent('invite_code_not_found', userId, {
        attemptedCode: inviteCode
      });

      throw new HttpsError(
        'not-found',
        'Invite code not found. Please check the code and try again.'
      );
    }

    const partnerDoc = partnerQuery.docs[0];
    const partnerId = partnerDoc.id;
    const partnerName = partnerDoc.data().firstName || 'Partner';

    // Prevent self-linking
    if (partnerId === userId) {
      throw new HttpsError(
        'invalid-argument',
        'You cannot link with yourself!'
      );
    }

    // Clear failed attempts on success
    await db.collection('dareus').doc(userId).update({
      failedInviteAttempts: []
    });

    console.log(`✅ Invite code ${inviteCode} verified for user ${userId} -> partner ${partnerId}`);

    return {
      success: true,
      partnerId: partnerId,
      partnerName: partnerName,
      message: `Found ${partnerName}! Ready to link.`
    };

  } catch (error) {
    console.error('Error verifying invite code:', error);

    if (error instanceof HttpsError) {
      throw error;
    }

    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// PARTNER LINKING WITH CONSENT
// ========================================

/**
 * Send partner link request (requires approval)
 */
exports.sendPartnerLinkRequest = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const {partnerId} = request.data;

  if (!partnerId) {
    throw new HttpsError('invalid-argument', 'Partner ID required');
  }

  if (userId === partnerId) {
    throw new HttpsError('invalid-argument', 'Cannot link with yourself');
  }

  try {
    // Check if users already linked
    const userDoc = await db.collection('dareus').doc(userId).get();
    const partnerDoc = await db.collection('dareus').doc(partnerId).get();

    if (!userDoc.exists || !partnerDoc.exists) {
      throw new HttpsError('not-found', 'User not found');
    }

    const currentPartner = userDoc.data().partnerId;
    if (currentPartner) {
      throw new HttpsError(
        'already-exists',
        'You are already linked with a partner. Unlink first.'
      );
    }

    const partnerLinked = partnerDoc.data().partnerId;
    if (partnerLinked) {
      throw new HttpsError(
        'already-exists',
        'This person is already linked with someone else'
      );
    }

    // Check for existing pending request
    const existingRequest = await db.collection('partnerLinkRequests')
      .where('fromUserId', '==', userId)
      .where('toUserId', '==', partnerId)
      .where('status', '==', 'pending')
      .limit(1)
      .get();

    if (!existingRequest.empty) {
      throw new HttpsError(
        'already-exists',
        'You already sent a link request to this person'
      );
    }

    // Create partner link request
    const requestData = {
      fromUserId: userId,
      toUserId: partnerId,
      fromUserName: userDoc.data().firstName || 'Someone',
      toUserName: partnerDoc.data().firstName || 'Partner',
      status: 'pending',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expiresAt: Date.now() + (7 * 24 * 60 * 60 * 1000) // Expires in 7 days
    };

    const requestRef = await db.collection('partnerLinkRequests').add(requestData);

    // Send notification to partner
    const partnerToken = partnerDoc.data().fcmToken;
    if (partnerToken) {
      await db.collection('notifications').add({
        toToken: partnerToken,
        fromUserId: userId,
        title: '💕 Partner Link Request',
        body: `${userDoc.data().firstName || 'Someone'} wants to link with you!`,
        timestamp: Date.now(),
        sent: false,
        type: 'partner_link_request',
        requestId: requestRef.id
      });
    }

    console.log(`✅ Partner link request sent from ${userId} to ${partnerId}`);

    return {
      success: true,
      requestId: requestRef.id,
      message: 'Partner link request sent! Waiting for approval.'
    };

  } catch (error) {
    console.error('Error sending partner link request:', error);

    if (error instanceof HttpsError) {
      throw error;
    }

    throw new HttpsError('internal', error.message);
  }
});

/**
 * Accept partner link request
 */
exports.acceptPartnerLinkRequest = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const {requestId} = request.data;

  if (!requestId) {
    throw new HttpsError('invalid-argument', 'Request ID required');
  }

  try {
    const requestDoc = await db.collection('partnerLinkRequests').doc(requestId).get();

    if (!requestDoc.exists) {
      throw new HttpsError('not-found', 'Link request not found');
    }

    const requestData = requestDoc.data();

    // Verify this request is for the current user
    if (requestData.toUserId !== userId) {
      throw new HttpsError(
        'permission-denied',
        'This request is not for you'
      );
    }

    // Check if request is still pending
    if (requestData.status !== 'pending') {
      throw new HttpsError(
        'failed-precondition',
        `Request already ${requestData.status}`
      );
    }

    // Check if request expired
    if (requestData.expiresAt < Date.now()) {
      await db.collection('partnerLinkRequests').doc(requestId).update({
        status: 'expired',
        expiresAt: admin.firestore.FieldValue.serverTimestamp()
      });

      throw new HttpsError(
        'failed-precondition',
        'Link request expired'
      );
    }

    const fromUserId = requestData.fromUserId;

    // Link the partners
    await db.collection('dareus').doc(userId).update({
      partnerId: fromUserId,
      partnerLinkedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    await db.collection('dareus').doc(fromUserId).update({
      partnerId: userId,
      partnerLinkedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Update request status
    await db.collection('partnerLinkRequests').doc(requestId).update({
      status: 'accepted',
      acceptedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Send notification to requester
    const requesterDoc = await db.collection('dareus').doc(fromUserId).get();
    const requesterToken = requesterDoc.data().fcmToken;

    if (requesterToken) {
      await db.collection('notifications').add({
        toToken: requesterToken,
        fromUserId: userId,
        title: '💕 Partner Linked!',
        body: `${requestData.toUserName} accepted your partner link request!`,
        timestamp: Date.now(),
        sent: false,
        type: 'partner_link_accepted'
      });
    }

    console.log(`✅ Partner link accepted: ${userId} <-> ${fromUserId}`);

    return {
      success: true,
      partnerId: fromUserId,
      partnerName: requestData.fromUserName,
      message: 'Partner linked successfully!'
    };

  } catch (error) {
    console.error('Error accepting partner link request:', error);

    if (error instanceof HttpsError) {
      throw error;
    }

    throw new HttpsError('internal', error.message);
  }
});

/**
 * Reject partner link request
 */
exports.rejectPartnerLinkRequest = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const {requestId} = request.data;

  if (!requestId) {
    throw new HttpsError('invalid-argument', 'Request ID required');
  }

  try {
    const requestDoc = await db.collection('partnerLinkRequests').doc(requestId).get();

    if (!requestDoc.exists) {
      throw new HttpsError('not-found', 'Link request not found');
    }

    const requestData = requestDoc.data();

    if (requestData.toUserId !== userId) {
      throw new HttpsError(
        'permission-denied',
        'This request is not for you'
      );
    }

    if (requestData.status !== 'pending') {
      throw new HttpsError(
        'failed-precondition',
        `Request already ${requestData.status}`
      );
    }

    // Update request status
    await db.collection('partnerLinkRequests').doc(requestId).update({
      status: 'rejected',
      rejectedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    console.log(`✅ Partner link rejected by ${userId}`);

    return {
      success: true,
      message: 'Partner link request rejected'
    };

  } catch (error) {
    console.error('Error rejecting partner link request:', error);

    if (error instanceof HttpsError) {
      throw error;
    }

    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// CHECK RATE LIMIT (callable function for client)
// ========================================
exports.checkDareRateLimit = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;

  try {
    const rateLimitCheck = await checkRateLimit(userId);

    return {
      allowed: rateLimitCheck.allowed,
      isPremium: rateLimitCheck.isPremium || false,
      limit: rateLimitCheck.limit || 999,
      used: rateLimitCheck.used || 0,
      remaining: rateLimitCheck.remaining || 999,
      reason: rateLimitCheck.reason || null
    };
  } catch (error) {
    console.error('Error checking rate limit:', error);
    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// BADGE BONUS POINTS
// ========================================
exports.awardBadgeBonus = onCall(async (request) => {
  // Verify user is authenticated
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'User must be authenticated');
  }

  const userId = request.auth.uid;
  const badgeId = request.data.badgeId;

  if (!badgeId) {
    throw new HttpsError('invalid-argument', 'Badge ID required');
  }

  try {
    // SECURITY: Validate that this is a real badge ID
    const validBadgeIds = [
      'first_dare', 'dare_master', 'speed_demon', 'consistent_player',
      'social_butterfly', 'negotiator', 'high_roller', 'streak_master',
      'category_expert', 'perfect_month', 'comeback_kid', 'night_owl',
      'early_bird', 'weekend_warrior', 'power_couple', 'code_breaker',
      'mystery_master', 'dare_legend', 'completionist', 'team_player'
    ];

    if (!validBadgeIds.includes(badgeId)) {
      console.log(`⚠️ SECURITY: Invalid badge ID attempted: ${badgeId} by user ${userId}`);
      await logAuditEvent('badge_fraud_attempt', userId, { badgeId });
      throw new HttpsError('invalid-argument', 'Invalid badge ID');
    }

    // Verify badge hasn't already been awarded
    const userRef = db.collection('dareus').doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      throw new HttpsError('not-found', 'User not found');
    }

    const unlockedBadges = userDoc.data().unlockedBadges || [];

    if (unlockedBadges.includes(badgeId)) {
      console.log(`Badge ${badgeId} already awarded to ${userId}`);
      return { success: false, message: 'Badge already unlocked' };
    }

    // Award 50 bonus points for badge unlock
    const bonusPoints = 50;
    await awardPoints(userId, bonusPoints, `Unlocked badge: ${badgeId}`);

    // Mark badge as unlocked
    await userRef.update({
      unlockedBadges: admin.firestore.FieldValue.arrayUnion(badgeId)
    });

    console.log(`✅ Badge ${badgeId} unlocked for ${userId}, awarded ${bonusPoints} points`);

    return {
      success: true,
      pointsAwarded: bonusPoints,
      message: `Badge unlocked! +${bonusPoints} points`
    };
  } catch (error) {
    console.error(`Error awarding badge bonus:`, error);
    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// VALIDATE AND AWARD PRIZE REVEAL COST
// ========================================
exports.revealPartnerPrize = onCall(async (request) => {
  // Verify authentication
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const competitionId = request.data.competitionId;
  const cost = 25; // Prize reveal costs 25 points

  if (!competitionId) {
    throw new HttpsError('invalid-argument', 'Competition ID required');
  }

  try {
    // Get user's current points
    const userRef = db.collection('dareus').doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      throw new HttpsError('not-found', 'User not found');
    }

    const currentPoints = userDoc.data().points || 0;

    if (currentPoints < cost) {
      throw new HttpsError('failed-precondition', `Insufficient points. Need ${cost}, have ${currentPoints}`);
    }

    // Deduct points
    await userRef.update({
      points: currentPoints - cost,
      lastPointsUpdate: admin.firestore.FieldValue.serverTimestamp(),
      lastPointsReason: 'Revealed partner prize'
    });

    // Update competition reveal status
    const competitionRef = db.collection('monthlyCompetitions').doc(competitionId);
    const competitionDoc = await competitionRef.get();

    if (!competitionDoc.exists) {
      throw new HttpsError('not-found', 'Competition not found');
    }

    const isUser1 = competitionDoc.data().user1Id === userId;
    const revealField = isUser1 ? 'user2Revealed' : 'user1Revealed';

    await competitionRef.update({
      [revealField]: true
    });

    console.log(`✅ User ${userId} revealed partner prize for ${cost} points`);

    return {
      success: true,
      pointsDeducted: cost,
      newBalance: currentPoints - cost
    };
  } catch (error) {
    console.error(`Error revealing prize:`, error);
    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// GOOGLE PLAY PURCHASE VERIFICATION
// ========================================
exports.verifyPurchase = onCall(async (request) => {
  // Verify authentication
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;
  const {purchaseToken, productId, packageName} = request.data;

  // Validate required fields
  if (!purchaseToken || !productId || !packageName) {
    throw new HttpsError(
      'invalid-argument',
      'Purchase token, product ID, and package name required'
    );
  }

  console.log(`Verifying purchase for user ${userId}, product: ${productId}`);

  try {
    // Initialize Google Play Developer API
    const auth = new google.auth.GoogleAuth({
      scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });

    const authClient = await auth.getClient();
    const androidPublisher = google.androidpublisher({
      version: 'v3',
      auth: authClient,
    });

    // Verify the purchase with Google Play API
    let purchase;

    // Check if it's a subscription or one-time purchase
    if (productId.includes('subscription') || productId.includes('month')) {
      // It's a subscription
      purchase = await androidPublisher.purchases.subscriptions.get({
        packageName: packageName,
        subscriptionId: productId,
        token: purchaseToken,
      });
    } else {
      // It's a one-time product purchase
      purchase = await androidPublisher.purchases.products.get({
        packageName: packageName,
        productId: productId,
        token: purchaseToken,
      });
    }

    const purchaseData = purchase.data;
    console.log('Purchase verification successful:', purchaseData);

    // Check purchase state (0 = purchased, 1 = canceled)
    const purchaseState = purchaseData.purchaseState || purchaseData.paymentState;

    if (purchaseState !== 0 && purchaseState !== 1) {
      throw new HttpsError(
        'failed-precondition',
        'Purchase not in valid state'
      );
    }

    if (purchaseState === 1) {
      throw new HttpsError(
        'failed-precondition',
        'Purchase was cancelled'
      );
    }

    // Determine premium tier based on product ID
    let premiumTier = 'PREMIUM_COUPLE';
    let durationDays = 30; // Default to monthly

    if (productId.includes('plus')) {
      premiumTier = 'PREMIUM_COUPLE_PLUS';
    }

    if (productId.includes('yearly') || productId.includes('year')) {
      durationDays = 365;
    } else if (productId.includes('month') || productId.includes('monthly')) {
      durationDays = 30;
    }

    // Calculate expiration date
    const now = Date.now();
    const expiresAt = now + (durationDays * 24 * 60 * 60 * 1000);

    // Update user's premium status in Firestore
    await db.collection('dareus').doc(userId).update({
      premiumTier: premiumTier,
      premiumPurchasedAt: now,
      premiumExpiresAt: expiresAt,
      lastPurchaseToken: purchaseToken,
      lastPurchaseProductId: productId,
      lastPurchaseVerifiedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    // Store purchase record for audit trail
    await db.collection('purchases').add({
      userId: userId,
      productId: productId,
      purchaseToken: purchaseToken,
      premiumTier: premiumTier,
      purchasedAt: now,
      expiresAt: expiresAt,
      verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
      purchaseState: purchaseState,
      packageName: packageName
    });

    console.log(`✅ Premium ${premiumTier} granted to user ${userId}, expires: ${new Date(expiresAt)}`);

    // Audit log: successful purchase
    await logAuditEvent('premium_purchase_verified', userId, {
      productId: productId,
      premiumTier: premiumTier,
      expiresAt: expiresAt,
      purchaseToken: purchaseToken.substring(0, 20) + '...' // Partial token for security
    });

    return {
      success: true,
      premiumTier: premiumTier,
      expiresAt: expiresAt,
      message: `Premium activated! Expires ${new Date(expiresAt).toLocaleDateString()}`
    };

  } catch (error) {
    console.error('Error verifying purchase:', error);

    if (error.code === 401 || error.code === 403) {
      throw new HttpsError(
        'permission-denied',
        'Google Play API authentication failed. Check service account credentials.'
      );
    }

    throw new HttpsError(
      'internal',
      `Purchase verification failed: ${error.message}`
    );
  }
});

// ========================================
// CHECK PREMIUM STATUS (with expiration check)
// ========================================
exports.checkPremiumStatus = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError('unauthenticated', 'Must be authenticated');
  }

  const userId = request.auth.uid;

  try {
    const userDoc = await db.collection('dareus').doc(userId).get();

    if (!userDoc.exists) {
      throw new HttpsError('not-found', 'User not found');
    }

    const userData = userDoc.data();
    const premiumTier = userData.premiumTier || 'FREE';
    const expiresAt = userData.premiumExpiresAt || 0;
    const now = Date.now();

    // Check if premium has expired
    if (premiumTier !== 'FREE' && expiresAt < now) {
      // Premium expired, downgrade to FREE
      await db.collection('dareus').doc(userId).update({
        premiumTier: 'FREE',
        premiumExpiredAt: now
      });

      return {
        isPremium: false,
        tier: 'FREE',
        expired: true,
        message: 'Premium subscription expired'
      };
    }

    return {
      isPremium: premiumTier !== 'FREE',
      tier: premiumTier,
      expiresAt: expiresAt,
      daysRemaining: Math.floor((expiresAt - now) / (24 * 60 * 60 * 1000)),
      expired: false
    };

  } catch (error) {
    console.error('Error checking premium status:', error);
    throw new HttpsError('internal', error.message);
  }
});

// ========================================
// DAILY COMPETITION SNAPSHOTS (for badge tracking)
// ========================================
/**
 * Scheduled function to create daily snapshots of competition scores
 * Runs at 11:59 PM every day
 * Enables final_hour, early_bird_winner, and comeback_kid badges
 */
exports.createDailyCompetitionSnapshots = require('firebase-functions/v2/scheduler').onSchedule(
  {
    schedule: '59 23 * * *', // Every day at 11:59 PM
    timeZone: 'America/New_York',
    region: 'us-central1'
  },
  async (event) => {
    console.log('📸 Creating daily competition snapshots...');

    try {
      // Get current month code
      const now = new Date();
      const monthCode = `${now.getFullYear()}-${now.getMonth() + 1}`;

      // Get all active competitions for this month
      const competitions = await db.collection('monthlyCompetitions')
        .where('monthCode', '==', monthCode)
        .get();

      let snapshotCount = 0;

      for (const competitionDoc of competitions.docs) {
        const data = competitionDoc.data();

        // Create daily snapshot
        await competitionDoc.ref.collection('dailySnapshots').add({
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          timestampMs: Date.now(),
          user1Points: data.user1Points || 0,
          user2Points: data.user2Points || 0,
          user1Id: data.user1Id,
          user2Id: data.user2Id,
          dayOfMonth: now.getDate()
        });

        snapshotCount++;
      }

      console.log(`✅ Created ${snapshotCount} daily snapshots`);
      return {success: true, snapshots: snapshotCount};

    } catch (error) {
      console.error('Error creating daily snapshots:', error);
      throw error;
    }
  }
);