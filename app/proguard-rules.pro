# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }

# Firebase Functions
-keep class com.google.firebase.functions.** { *; }
-keepclassmembers class com.google.firebase.functions.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keepclassmembers class com.google.firebase.messaging.** { *; }

# Keep all model classes (for Firestore serialization)
-keep class com.DareUs.app.** { *; }

# Google Play Billing Library v6
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.billingclient.api.ProductDetails { *; }
-keep class com.android.billingclient.api.ProductDetails$* { *; }
-keep class com.android.billingclient.api.Purchase { *; }
-keep class com.android.billingclient.api.Purchase$* { *; }
-keepclassmembers class com.android.billingclient.api.** { *; }

# Guava (required for ImmutableList)
-dontwarn com.google.common.**
-keep class com.google.common.collect.ImmutableList { *; }