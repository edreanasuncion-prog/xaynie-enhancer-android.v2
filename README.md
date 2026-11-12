# X Studio — Native Android (Kotlin + Jetpack Compose)

Kotlin 2.1.0 · AGP 8.7.3 · Gradle 8.9 · Java 17 · Compose BOM 2024.12.01 · Media3 1.5.0 · Room · WorkManager · Supabase Kotlin SDK 3.0.3.

**Status: source repaired, not yet build-verified.** No Gradle build has been run on this code. See `BUILD_AND_TEST_REPORT.md` for exactly what was and was not checked.

## What the app does (and does not)
- **Photo Enhancer** – on-device *software* enhancement (subsampled decode, progressive scaling, 5-tap sharpen, luma-preserving colour pass). No neural network, no ONNX model. Output is capped at 8192 px per side and ≈24 MP for memory safety, so 8K is only reached for near-square images; the real output size is shown after each photo.
- **Video Enhancer** – Media3 Transformer (H.264): optional resolution scaling and contrast. Frame rate can be kept or **lowered** (frames are dropped, never created); only rates the clip's detected frame rate allows are offered.
- **X Motion** – shape layers, timeline, Alight Motion XML import (shapes only; everything else is reported as not imported), PNG frame export. **MP4 export is not available in v1.0.**
- **ZIP to Video** only inspects ZIPs; it does not render video. **TikTok Exporter** applies the resolution choice only.
- Photo history expires after exactly 72 hours; video history does not expire.

## Build
1. Provide credentials (never commit them): copy `local.properties.example` to `local.properties` and set `SUPABASE_URL` and `SUPABASE_ANON_KEY`, **or** set `XSTUDIO_API_BASE_URL` and the app will read `GET /api/config`. Without either, the app shows a configuration error and does not let anyone in.
2. This archive intentionally has **no `gradle-wrapper.jar`** (the previous one was a hand-made stand-in, not Gradle's). Generate the official wrapper once, with Gradle 8.9+ installed:
   `gradle wrapper --gradle-version 8.9 --distribution-type bin`
   then `chmod +x gradlew` (and in git: `git update-index --chmod=+x gradlew`).
3. `./gradlew :app:assembleDebug --stacktrace` and `./gradlew :app:testDebugUnitTest`.

## Codemagic
Repository root must contain `settings.gradle.kts`. Create the environment variable group **`xstudio_config`** with `SUPABASE_URL` + `SUPABASE_ANON_KEY` (or `XSTUDIO_API_BASE_URL`). The workflow `android-debug-apk` generates the official wrapper, builds the debug APK and runs the unit tests; it fails early if no backend configuration is present. Never put `service_role` or Gemini keys in the app or in this group.
