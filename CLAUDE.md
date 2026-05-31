# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

ReelBlocker (in-app brand: **Basta!**) is a single-module Android app that detects when the user is inside Instagram Reels or YouTube Shorts and immediately fires the system Back action to leave that surface. It does not block the host apps themselves — only the short-video surface inside them.

On top of the core detection, the app has a gamification layer: a daily streak with an evolving mascot, a 5-species **Pokédex-style collection** unlocked by reaching day 21 of an active streak (where the habit consolidates) (mascot graduates → archived in inventory → new egg of a different species emerges), and full stats.

## Build / run

- Open in Android Studio and let Gradle sync, then Run on a connected device, **or**:
- `./gradlew assembleDebug` (use `gradlew.bat` on Windows) to produce an APK under `app/build/outputs/apk/`.
- Windows toolchain needs `JAVA_HOME` set; the JDK shipped with Android Studio works (`C:\Program Files\Android\Android Studio1\jbr`).
- No unit/instrumentation tests configured. No lint/format config beyond Android defaults.
- After install: user must manually enable the service at Settings → Accessibility → Basta!. The in-app Onboarding deep-links to that screen.
- Live logs: `adb logcat -s ReelBlocker.Service ReelBlocker.Streak ReelBlocker.Collection` (tags defined per file).

Toolchain: `compileSdk`/`targetSdk` 35, `minSdk` 24, Java/Kotlin target 17. Compose BOM + Material 3.

## Architecture

The codebase is Jetpack Compose + Material 3 + Material You. All UI is built programmatically with Composables — there is no XML layout file. Navigation is a hand-rolled state machine (no Navigation-Compose dependency) with a bottom NavigationBar.

Code lives under `app/src/main/java/app/reelblocker/`:

### Detection layer (always-on)

- **`BlockerService.kt`** — the `AccessibilityService` registered in `AndroidManifest.xml` with config `res/xml/accessibility_config.xml`. Receives events for Instagram, YouTube and (recently) Facebook. BFS-walks `rootInActiveWindow` (capped at 800 nodes) looking for any `viewIdResourceName` that contains a substring from `INSTAGRAM_REEL_HINTS` / `YOUTUBE_SHORTS_HINTS`. A match triggers `performGlobalAction(GLOBAL_ACTION_BACK)`, gated by `MIN_INTERVAL_MS` (600 ms) anti-bounce. Facebook is wired as a first-class blockable app (toggle, stats, strict-streak) but **paused**: `HintConfig.DEFAULT_FACEBOOK_REELS` ships empty and the FB branch returns early while it stays empty (so the tree is never walked). Pausing was a deliberate decision after an on-device investigation (May 2026) found FB hostile to the resource-id strategy — see [[reference-facebook-internals]]: (1) resource-ids are obfuscated to `(name removed)`; (2) everything is hosted in a single `FbMainTabActivity`; (3) the immersive Reels viewer's accessibility tree stays contaminated with the home-feed chrome, so home vs. Reels can't be told apart by node presence without risking closing the whole app. The debug-only `dumpFacebookTree` (now logs content-descriptions + `FB STATE` Activity className, since ids are useless) stays for future discovery. To revisit: build a multi-signal heuristic gated on `isVisibleToUser` + fullscreen bounds.

### State / persistence

All persistent state lives in one `SharedPreferences` file (`reelblocker_prefs`):

- **`Stats.kt`** — per-day block counts (Instagram + YouTube split), 30-day rolling history, per-app enable toggles, Pro feature flags, onboarding-done flag.
- **`Streak.kt`** — daily streak engine. `tick()` increments by 1 if called the day after the last valid date, resets to 1 if a day was skipped, no-ops if same-day. `breakStreak()` zeros the count but preserves the record. On the transition into `MascotLevel.ADULT` (day 21), `tick()` writes a `pending_graduation_from` flag. `shouldBeProtecting(ctx)` enforces the **strict per-app model**: returns true only if accessibility is on AND *all* installed `BLOCKABLE_APPS` are enabled — toggling any app off counts as "stopped protecting" and breaks the streak.
- **`MascotCollection.kt`** (`object Collection`) — collected-mascot inventory. Reads the pending-graduation flag, archives the mascot, breaks the streak with reason `"graduation"`, picks a next species (random from uncollected; falls back to random-of-all when complete). Exposes `currentSpecies(ctx)`, `read(ctx)`, `pendingGraduation(ctx)`, `consumePendingGraduation(ctx, daysReached)`, `uniqueCount(ctx)`.
- **`Premium.kt`** — Play Billing wrapper for the Pro one-time purchase. Caches `is_pro_purchased` in the same prefs so `BlockerService` (separate process) can read it without re-querying Billing.
- **`Breaks.kt`** — Pro feature. Time-boxed pause of the blocker (`break_end_ms`) with one-per-day quota (`break_consumed_date`). The home subtext switches to a countdown while `millisRemaining(ctx)` is non-null.

**Backup**: `AndroidManifest.xml` declares `allowBackup="true"` plus explicit rules at `res/xml/backup_rules.xml` (Android ≤ 11) and `res/xml/data_extraction_rules.xml` (Android 12+). Both include only `sharedpref/reelblocker_prefs.xml`. Auto Backup ships the user's progress to Google Drive and restores it on reinstall / device migration. No other stores to back up.

### Mascot rendering (Canvas)

- **`MascotEvolution.kt`** — the `MascotLevel` enum (Egg=day 0, Cracking=3, Hatchling=8, Adult=21 — graduation point) with palette + `@StringRes displayNameRes`, plus the `MascotCanvas` Composable that draws the mascot on a `Canvas` using primitive shapes (ovals, paths, gradients). Drawing dispatches by species via the species-specific functions in MascotSpecies.kt.
- **`MascotSpecies.kt`** — the `MascotSpecies` enum (5 entries: CLASICA, DRAGON, TORTUGA, LOBO, BUHO) with `accentTint` + `@StringRes displayNameRes` + per-species drawing functions (`drawDragonBody`, `drawTortugaBody`, `drawLoboBody`, `drawBuhoBody`). The CLASICA species uses the original `drawCreature` flags from MascotEvolution.kt. Egg / Cracking phases share the same primitive across species, tinted by `species.accentTint`.

### UI (Compose)

- **`MainActivity.kt`** — `AppRoot` wraps everything in a `Scaffold` with a bottom `NavigationBar` (`BottomNavBar` composable inside the file). Four tabs map to `Screen.Home / Stats / Inventory / Settings` (sealed class in `Navigation.kt`). Includes the `HowItWorksDialog` (help icon) and `GraduationDialog` (fires on `ON_RESUME` if `Collection.pendingGraduation` is non-null).
- **`StreakCard.kt`** — the home hero. Mascot inside a progress ring, huge "X días" counter, single subtext line that counts down to graduation in the species' accent color.
- **`StatsScreen.kt`** — Hoy hero + IG/YT distribution strip + 7-day chart + time recovered + record + Pro upsell.
- **`InventoryScreen.kt`** — "Species X / 5" hero, "Active now" row (current species + level + days), 2×3 grid of slots (filled mascot + name + date, or locked `?`), repeat-hint when applicable.
- **`SettingsScreen.kt`** — Apps to block, Protection (accessibility, battery, disable), Pro, About. The "Disable protection" row triggers `ConfirmDisableDialog`.
- **`OnboardingScreen.kt`** — 4-page pager: wordmark, mascot intro (with timeline 0/3/8/21), enable service, exclude from battery.
- **`ConfirmDisableDialog.kt`** — sad mascot + warning before breaking the streak.
- **`TipQuote.kt`** + **`Tips.kt`** — rotating motivational quote read from `string-array tips_quotes` in resources (so it localizes automatically).

### Navigation

`Navigation.kt` declares `sealed class Screen` and exposes `Screen.bottomTabs` as a getter (lazy on access — eager-init bites with `data object` static order). `ScreenContainer` cross-fades between tabs (no spatial direction).

### Internationalization

The app supports **English (default fallback) and Spanish** via `res/values/strings.xml` (English) and `res/values-es/strings.xml` (Spanish). English lives in the unqualified `values/` so any locale Android can't match (French, German, Portuguese…) falls back to English instead of Spanish. Every user-facing string in code uses `stringResource(R.string.xxx)` / `pluralStringResource(R.plurals.xxx)` / `ctx.getString(...)`. Mascot level names and species names are `@StringRes` references on the enum. Rotating tips are a `string-array`. Date formatting respects `Locale.getDefault()` (system locale).

## Common tasks

### Refreshing detection hints when IG/YT push updates

1. Reproduce the unblocked Reel/Short on device.
2. `adb shell uiautomator dump` → pull `/sdcard/window_dump.xml` (UTF-16).
3. Find a `resource-id` unique to the short-video surface (not present in the normal feed).
4. Add to `INSTAGRAM_REEL_HINTS` or `YOUTUBE_SHORTS_HINTS` in `BlockerService.kt`. Don't broaden to substrings that also match the normal feed (false positives close the host app).

### Adding another blocked app (TikTok, etc.)

1. Add the package name to `<queries>` in `AndroidManifest.xml` and to `Stats.BLOCKABLE_APPS`.
2. New hint list in `BlockerService.kt` + new branch inside `onAccessibilityEvent`'s `when (pkg)`.

### Adding a sixth mascot species

1. New enum entry in `MascotSpecies` with `id`, `displayNameRes`, `accentTint`.
2. New drawing function (e.g. `drawFoxBody`) using the helpers in `MascotSpecies.kt`.
3. New dispatch branch in `drawMascot` inside `MascotEvolution.kt`.
4. New `mascot_species_*` key in `values/strings.xml` and `values-en/strings.xml`.
5. Update the inventory hero subtitle copy if you mention the count.

### Known limits (architectural — do not "fix")

- Android sandboxing means the Reels/Shorts entry button cannot be hidden without root + Xposed. The app reacts *after* the user enters the surface, so a brief flash is expected.
- Only works inside native apps configured in `accessibility_config.xml`. Browsers are out of reach.
- The mascot system uses runtime-drawn primitives — no PNG assets. Adding species costs ~50–100 LOC of Canvas drawing each.
