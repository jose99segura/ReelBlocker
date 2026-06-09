# AGENTS.md

## Build (Windows PowerShell)

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
.\gradlew.bat assembleDebug      # APK only
.\gradlew.bat installDebug       # build + push to device
.\gradlew.bat bundleRelease      # Play Store AAB
.\gradlew.bat clean
.\gradlew.bat test               # JVM unit tests (no device needed)
```

Live logs: `adb logcat -s ReelBlocker.Service ReelBlocker.Streak ReelBlocker.Collection`

## Architecture

Single-module Android app (`app/`). Jetpack Compose + Material 3. **No XML layouts** — all UI is `@Composable` functions. Navigation is hand-rolled (`sealed class Screen` in `Navigation.kt`) — **no Navigation-Compose dependency**.

Source: `app/src/main/java/app/reelblocker/` — 28 files, flat package, no subdirectories.

- **`BlockerService.kt`** — `AccessibilityService`, core detection. Runs in separate process. Reads prefs directly.
- **`MainActivity.kt`** — single Activity, Compose host. Contains `AppRoot` + `BottomNavBar`.
- **`Stats.kt`**, **`Streak.kt`**, **`MascotCollection.kt`**, **`Premium.kt`**, **`Breaks.kt`** — all state is singletons backed by **one SharedPreferences file**: `reelblocker_prefs`.
- **`HintConfig.kt`** — detection hints with baked-in defaults + remote JSON fetch. Remote URL contains `CHANGEME` (not active yet).
- **`MascotEvolution.kt`** + **`MascotSpecies.kt`** — Canvas-drawn mascots (primitive shapes, no PNG assets).

## Gotchas

- **`Screen.bottomTabs` is a lazy getter** — eager companion init would read null data objects and break the `when` exhaustiveness in `BottomNavBar`. Keep it lazy.
- **Facebook is paused** — `HintConfig.DEFAULT_FACEBOOK_REELS` is empty, TikTok is the third active app (`BLOCKABLE_APPS` includes it).
- **`isAccessibilityTool="true"`** in `res/xml/accessibility_config.xml` is load-bearing for Play Store compliance. Never remove or set to false.
- **`versionCode` convention**: `versionName` string with dots removed × 10. E.g. `"1.8"` → `180`. Leaves gap for hotfix (1.8.1 → 181).
- **English strings live in `values/strings.xml`** (unqualified — fallback for unsupported locales), Spanish in `values-es/strings.xml`.
- **Pro is one-time IAP**, product ID `"basta_pro"`, not a subscription. Premium state is cached in `reelblocker_prefs` so the accessibility service can read it cross-process.
- **No lint/format config** beyond Android defaults. No CI configured.
- **JVM unit tests** exist under `app/src/test/` (4 files: HintConfig, HealthCheck, Collection, MascotSpecies). Use `org.json:json` test dependency for JSON parsing (android.jar stub throws "not mocked").
