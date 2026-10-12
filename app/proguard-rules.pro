# Aero Launcher — R8 / ProGuard keep rules

# Keep Application entry and launcher Activity names referenced from the manifest.
-keep class com.yourstudio.aerolauncher.AeroLauncherApp { <init>(); }
-keep class com.yourstudio.aerolauncher.MainActivity { <init>(); }

# App widgets (instantiated by the system via the manifest).
-keep class com.yourstudio.aerolauncher.widgets.builtin.** { <init>(); }
-keep class * extends android.appwidget.AppWidgetProvider { <init>(); }

# Notification listener + accessibility (bound by the system).
-keep class com.yourstudio.aerolauncher.badges.AeroNotificationListener { <init>(); }
-keep class com.yourstudio.aerolauncher.LockAccessibilityService { <init>(); }

# Room entities / DAOs.
-keep class com.yourstudio.aerolauncher.data.** { *; }
-dontwarn androidx.room.paging.**

# Biometric / Material inflate.
-keep class androidx.biometric.** { *; }
-keep class com.google.android.material.** { *; }

# Keep native method names if any JNI appears later.
-keepclasseswithmembernames class * {
    native <methods>;
}
