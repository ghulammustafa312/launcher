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

## What’s in this step (Prompt 1)

- Design system: midnight indigo / violet / aqua / accent orange, light / dark / auto
- Inter font (SIL OFL 1.1 — see `ASSET_LICENSES.md`)
- Glass card / panel styles, shared 24dp corners
- SplashScreen API splash with placeholder icon
- Settings activity shell (theme mode + palette swatches)
- Edge-to-edge via `WindowInsets`

Home-launcher intent filters and app grid come in later prompts.

## Wireless debugging (your device)

On the phone: Developer options → Wireless debugging → Pair device with pairing code.

```bash
adb pair HOST:PORT
adb connect HOST:PORT
adb devices
```

Then Run from Android Studio or `adb install -r` the debug APK.
