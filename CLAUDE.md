# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ReelBlocker is a tiny Android app (Kotlin, single-module Gradle project) whose only job is to detect when the user is inside Instagram Reels or YouTube Shorts and immediately fire the system Back action to leave that screen. It does not block the host apps themselves — only the short-video surface inside them.

## Build / run

- Open in Android Studio and let Gradle sync, then Run on a connected device, **or**:
- `./gradlew assembleDebug` (use `gradlew.bat` on Windows) to produce an APK under `app/build/outputs/apk/`.
- There are no unit or instrumentation tests in the project, and no lint/format config beyond Android defaults.
- After install: user must manually enable the service at Settings → Accessibility → ReelBlocker. `MainActivity` only opens that settings screen; it cannot toggle the service.
- Live logs while developing: `adb logcat -s ReelBlocker` (tag defined in `BlockerService`).

Toolchain: `compileSdk`/`targetSdk` 34, `minSdk` 24, Java/Kotlin target 17.

## Architecture

Two source files, both in `app/src/main/java/com/example/reelblocker/`:

- **`BlockerService.kt`** — an `AccessibilityService` registered in `AndroidManifest.xml` with config `res/xml/accessibility_config.xml`. It only receives events for the packages listed in that XML (`android:packageNames`). On each event from Instagram or YouTube it BFS-walks `rootInActiveWindow` (capped at 800 nodes) looking for any `viewIdResourceName` that contains a substring from `INSTAGRAM_REEL_HINTS` / `YOUTUBE_SHORTS_HINTS`. A match triggers `performGlobalAction(GLOBAL_ACTION_BACK)`, gated by a `MIN_INTERVAL_MS` (600 ms) anti-bounce.
- **`MainActivity.kt`** — UI built programmatically (no XML layout). Shows whether the service is enabled (by reading `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`) and a button to open accessibility settings.

The whole detection strategy depends on internal resource-ID substrings of the target apps. **Those IDs change with Instagram/YouTube updates** — when the blocker stops working, the fix is almost always to add new hints to the two lists in `BlockerService.kt`. `shorts_dump.xml` / `shorts_dump2.xml` at the repo root are captured UI dumps (e.g. from `uiautomator dump`) used to discover those IDs; keep them when refreshing hints.

### Adding another app (e.g. TikTok, Facebook)

1. Add the package name to `android:packageNames` in `res/xml/accessibility_config.xml`.
2. Add a new hint list constant in `BlockerService.kt` and a new branch in the `when (pkg)` inside `onAccessibilityEvent`.

### Known limits (don't try to "fix" these — they're architectural)

- Android sandboxing means the Reels/Shorts button cannot actually be hidden without root + Xposed; this app only reacts after the user enters the surface, so a short flash is expected.
- Only works inside the native apps configured in `accessibility_config.xml`, not in browsers.
