# Aero Launcher — Play Store release checklist

Use this document when preparing a closed test or production upload.
Package / application id: value of `appPackage` in `gradle.properties` (default `com.yourstudio.aerolauncher`).

---

## 1. Privacy policy URL

- [ ] Host a public HTTPS privacy policy page (example placeholder):  
  `https://yourstudio.example/aero-launcher/privacy`
- [ ] Paste the final URL into Play Console → App content → Privacy policy.
- [ ] Policy must state clearly:
  - Aero Launcher is a device home screen; it does **not** sell personal data.
  - Optional AccessibilityService is used **only** to lock the screen on double-tap, after in-app opt-in; it does not read screen content.
  - Optional NotificationListenerService is used **only** for per-app badge counts after in-app opt-in; notification titles/bodies are **never** stored or sent.
  - Notes widget text, layout backup JSON, and preferences stay on-device unless the user exports a backup file they choose.
  - No account system; no analytics SDK in the current codebase.

---

## 2. Data safety form (Play Console answers)

Suggested answers for the current feature set (verify before submit):

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** (preferred), if you confirm no SDK collects data. If you later add crash reporting, update this form. |
| Is all user data encrypted in transit? | N/A if no collection; otherwise Yes (HTTPS). |
| Do you provide a way for users to request deletion? | N/A / Uninstall clears on-device data; exported backups are user-controlled files. |
| Data collected | None by the app network stack today. |
| Approximate location / precise location | Not collected. |
| Personal info / financial / health | Not collected. |
| Photos / videos | User may pick a wallpaper photo via the system picker; the URI is stored locally for display only — not uploaded. |
| App activity / installed apps | Used on-device to show the launcher grid (via `LauncherApps` / `<queries>`); not transmitted. |
| Device IDs | Not collected. |

If you answer “Yes” to any collection later, list: data type, purpose (App functionality), and “Ephemeral / on-device only” where accurate.

---

## 3. Content rating

- [ ] Complete the IARC questionnaire in Play Console.
- Expected category for a productivity / launcher utility with no UGC, no violence, no ads in-code: **Everyone** (confirm after questionnaire).
- [ ] Declare that the app is not a “news” or “dating” app, has no user-generated public content, and does not share location.

---

## 4. Store listing text

### Short description (≤ 80 characters)

```text
Original Android home screen with widgets, Focus modes, and privacy controls.
```

### Full description

```text
Aero Launcher is an original Android home screen built for clarity and control.

Home and dock
• Paged workspace with a customizable grid and glass-style dock
• Folders, drag-and-drop rearrange, and layout backup via the system file picker

Widgets
• Host standard Android widgets with bind and configure
• Widget stacks you can swipe, plus built-in Clock, Battery, and on-device Notes

Search and App Library
• Swipe down to search apps, settings shortcuts, and the web
• Swipe up for an App Library with categories and A–Z fast scroll

Quick panel
• Swipe down from the top edge for flashlight, brightness, and system Wi‑Fi / Bluetooth / sound panels
• Does not replace Android Quick Settings

Personalization
• Light, dark, or auto theme, accent colors, and icon masks (rounded, circle, squircle)
• Built-in wallpapers or your own photo, with optional blur

Privacy and Focus
• Hide apps and manage them in a Hidden apps screen
• Lock selected apps behind BiometricPrompt
• Focus modes (Work, Study, Sleep) that hide chosen apps from the home screen
• Optional notification badges only after an in-app explanation and Notification access you control — badge counts never store notification content

Aero Launcher does not use QUERY_ALL_PACKAGES. It discovers launchable apps with the standard MAIN/LAUNCHER query.
```

---

## 5. Graphics checklist

| Asset | Spec | Status |
| --- | --- | --- |
| High-res icon | 512 × 512 PNG, 32-bit, no alpha required by Play for the store icon upload | [ ] Create from Image Asset Studio / design tool; replace vector placeholders |
| Feature graphic | 1024 × 500 JPG or 24-bit PNG | [ ] Create branded banner (no Apple imagery) |
| Phone screenshots | 4–8 images, JPEG/PNG, min short side ≥ 320px | [ ] Capture on a real device or emulator |
| Suggested screenshot set | 1) Home + dock 2) Widgets / stack 3) App Library 4) Search 5) Quick panel 6) Settings / theme 7) Focus modes 8) Hidden apps / badges opt-in | [ ] |

Tips:
- Use a clean wallpaper from the built-in set (Dusk / Aurora).
- Do not show other brands’ trademarked UI beyond fair use of system panels.
- Crop status bar carrier names if needed for cleanliness.

---

## 6. Closed testing steps

1. [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts` if needed.
2. [ ] Build a release App Bundle:  
   `./gradlew :app:bundleRelease`  
   (R8 minify + resource shrinking are enabled for `release`.)
3. [ ] Sign with the upload keystore configured in Play App Signing.
4. [ ] Play Console → Testing → Closed testing → create track (e.g. `internal-qa`).
5. [ ] Upload the `.aab`, add release notes, roll out to the closed track.
6. [ ] Add testers by email list or Google Group.
7. [ ] Testers opt in via the track link, install from Play, set Aero Launcher as Home.
8. [ ] Smoke test on ≥ 2 API levels (e.g. 29 and 34+) and one low-RAM device:
   - Default Home role, back / Home button, rotation
   - Install / update / uninstall a third-party app; confirm grid refresh
   - Work profile app (if available) opens and shows “(Work)”
   - Return from Accessibility / Notification listener / Write settings screens
   - Widgets bind/configure, quick panel, Focus mode, app lock biometric
9. [ ] Collect feedback ≥ 14 days (or your org’s policy) before production.

---

## 7. Compliance audit report

### Assets (`ASSET_LICENSES.md`)

| Finding | Result |
| --- | --- |
| Inter font (OFL) logged | Pass |
| Original vectors / wallpapers / glass drawables logged | Pass |
| Third-party Apple assets | None |

### Branding text

| Finding | Result |
| --- | --- |
| App source / resources contain “iPhone”, “iOS”, or “Apple” product claims | **Pass** — not present in app code or user-facing strings (only an internal project rule that forbids Apple branding) |

### Package visibility

| Finding | Result |
| --- | --- |
| `QUERY_ALL_PACKAGES` | **Not used** |
| `<queries>` | `MAIN`+`LAUNCHER`, `WEB_SEARCH`, `APPWIDGET_UPDATE` only |

### Permissions (must match features)

| Permission | Feature | Required? |
| --- | --- | --- |
| `CAMERA` | Quick panel flashlight (`CameraManager.setTorchMode`) | Yes (optional feature; requested at use) |
| `FLASHLIGHT` | Legacy / OEM flashlight helpers | Keep for compatibility; feature-gated |
| `WRITE_SETTINGS` | Brightness slider | Yes (special settings screen; user must grant) |
| `USE_BIOMETRIC` | App lock | Yes |
| `USE_FINGERPRINT` | App lock on older APIs | Yes (compat) |
| Accessibility service | Double-tap lock | Opt-in only; not a normal permission |
| Notification listener | Badges | Opt-in only; not a normal permission |
| `QUERY_ALL_PACKAGES` | — | **Must remain absent** |

### Performance / release build

| Item | Status |
| --- | --- |
| R8 minify + shrinkResources on `release` | Enabled in `app/build.gradle.kts` |
| ProGuard keep rules for widgets / NLS / accessibility / Room | `app/proguard-rules.pro` |
| Shared RecyclerView pool + lazy icon cache + trim memory | Implemented |
| Debounced package reload (install/update/uninstall) | Implemented |
| Work profile apps via `LauncherApps` + all user profiles | Implemented |
| Home / back / rotation / `onNewIntent` reset | Implemented |

---

## 8. Pre-upload final gate

- [ ] Privacy policy URL live and linked
- [ ] Data safety form matches this build
- [ ] Content rating completed
- [ ] Short + full description pasted
- [ ] 512×512 icon + 1024×500 feature graphic + 4–8 screenshots uploaded
- [ ] Closed test smoke list passed
- [ ] `ASSET_LICENSES.md` still matches every bundled asset
- [ ] Release AAB built with R8; no debug signing on the Play upload
