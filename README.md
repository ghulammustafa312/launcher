# Aero Launcher

Original Android launcher app (Java, XML views, AndroidX). Placeholder name and package — change before publishing.

Package: `com.yourstudio.aerolauncher` (set in one place: `appPackage` in `gradle.properties`).

## Requirements

- JDK 17 (Android Studio’s bundled JBR is fine)
- Android SDK Platform 36
- Android Studio or command-line tools

This repo is source-only; open it in Android Studio on your machine to sync Gradle and build. No SDK was installed from this project setup.

## Build

```bash
# Debug APK
./gradlew assembleDebug

# Install on a connected / wirelessly paired device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or from Android Studio: Run on your device after wireless debugging / adb pair
```

Release AAB (signing configured later):

```bash
./gradlew bundleRelease
```

Toolchain pins:

- compileSdk / targetSdk 36, minSdk 24
- AGP 8.13.2, Gradle 8.13, JDK 17

## Change the package name

1. Edit `appPackage` in `gradle.properties`.
2. Move `app/src/main/java/com/yourstudio/aerolauncher` so the folders match the new package.
3. Update the `package` line in every Java file.

A published package name cannot be changed later.

## What’s included so far

- Design system, splash, settings (theme + adjustable 4×6 grid)
- Real launcher: `HOME` / `MAIN` / `DEFAULT`, `singleTask`
- `<queries>` for `MAIN`/`LAUNCHER` (no `QUERY_ALL_PACKAGES`)
- Installed apps via `LauncherApps` on a background thread, icon cache, package callbacks
- ViewPager2 pages, page dots, glass dock (4 apps)
- Scale-up open animation; “Set as default” via `RoleManager` (API 29+) or Home settings
- Edit mode (wiggle, remove, drag across pages/dock), folders, App info / Uninstall / Hide
- Layout saved in Room; backup/restore via the system file picker (no storage permission)
- App Library page (categories, recently added, A–Z fast scroller)
- Swipe-down search (apps, settings shortcuts, web fallback; no contacts)
- Gestures: swipe up library, pinch settings, opt-in AccessibilityService double-tap lock

## Wireless debugging (your device)

On the phone: Developer options → Wireless debugging → Pair device with pairing code.

```bash
adb pair HOST:PORT
adb connect HOST:PORT
adb devices
```

Then Run from Android Studio or `adb install -r` the debug APK.
