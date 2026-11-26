# Lodestill (working name)

A focus app: places you care about grow layers from unbroken focus sessions.
Interrupted sessions leave scaffolding, never a broken streak. Progress never decays.

- One unbroken session of the set length builds one layer at a place.
- Leaving the app starts a grace period (default 90 s). Past grace, the session
  becomes scaffolding; the time already focused is kept.
- Finish scaffolding within the window (default 24 h) and the layer still counts.
- Nothing resets. No streaks, no punishment.

## Tiers

- **0 - Off**: sessions run in-app only.
- **1 - DND**: Do Not Disturb silences interruptions during sessions.
- **2 - Guard**: usage access + overlay; a return banner floats over other apps.
- **3 - Block**: accessibility service returns you to your session when another
  app opens. Enabled only after an in-app disclosure; never reads content.

Under battery saver, blocks or alerts can be late or missed.

## Build

Gradle 8.9, AGP 8.5.2, Kotlin 2.0.20, minSdk 26, target 34.

    gradle :app:testDebugUnitTest   # engine unit tests
    gradle :app:assembleDebug       # sideloadable debug APK

CI: `.github/workflows/build.yml` runs tests, then builds and uploads the
debug APK artifact.

## Status

Working name and package (`dev.joyal.lodestill`) are provisional pending owner
decision. Design: approved Meta-minimal v3 mockups (flat surfaces, blue #0064E0
on actions only, pill buttons, 16dp cards, Roboto).
