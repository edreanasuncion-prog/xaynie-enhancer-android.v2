# X Studio Day 3 → repaired: change report

## Defects found beyond the brief (would have blocked the build)
- Raw line breaks inside Kotlin string literals (XStudioNavHost, ProfileAndHistoryScreens, XSocialScreen) → compile errors. Fixed with `\n`.
- `SupabaseModule.publicStorageUrl("social-media", …)` called a 1-argument function → compile error. Added a bucket overload.
- Icons `Clapperboard`, `Eraser`, `Flame` do not exist in Compose material icons → replaced with `Theaters`, `AutoFixOff`, `LocalFireDepartment`.
- `gradle-wrapper.jar` was a hand-built class, not Gradle's wrapper → removed; official wrapper is generated (see README / codemagic).
- Alight XML import called `setFeature` with flags Android's parser rejects, which would fail every import → now best-effort (the DOCTYPE/ENTITY text check remains the guard).
- Photo and Video preview containers had no height (Canvas-based before/after slider collapsed) → explicit aspect ratio / max height.

## By requested area
1. **Auth/Supabase** – `SupabaseModule.initialize()` (build credentials → `/api/config` fallback) returns Ready/Failed; no placeholder client; `client` throws if not initialized. `AuthViewModel` initializes first, then starts the session observer; failure shows a "Configuration error" card with Retry. `XStudioRoot` no longer skips `AuthScreen` when unconfigured. Login, sign-up (username metadata), 6-digit OTP, recovery, persistence, logout untouched.
2. **Video Enhancer** – source resolution/fps probed (frame count ÷ duration, else container rate). New `SOURCE FPS` option; 30/60/90/120 enabled only when ≤ source rate. `FrameDropEffect` only for a real reduction. Failed/cancelled exports delete the partial file and never reach history. Output size/fps is re-measured from the exported file. Encoder/decoder failures get specific messages. 150 MB limit now enforced.
3. **MediaStore** – one `MediaStoreExporter`: null stream = failure, byte-count verified, entry deleted on any failure, MIME from extension (PNG/JPEG/WebP → Pictures/XStudio, MP4 → Movies/XStudio). All existing buttons use it; history/Quick Tools now report real results.
4. **X Motion** – original XML kept in memory (session only) and every non-imported element listed with counts; false "preserved in XML metadata" text removed; "Saved" label changed to "Session only" (projects are not persisted). PNG export refuses frames with no visible layer or no drawn pixels, checks encode/copy, sanitises file name, uses uniform shape scale. MP4 remains disabled.
5. **Photo Enhancer** – `OutputPlan` computes and reports real output size and the limiting factor; history label never claims a tier that was not reached; JPEG write is checked. Software pipeline, batch, before/after, subsampling and memory caps unchanged. "AI Detail" renamed "Detail boost".
6. **ONNX** – dependency, version-catalog entry, ProGuard rule, `assets/models/`, engine remnants removed. Engine renamed `PhotoEnhanceEngine`.
7. **History** – `HistoryFiles` (72 h TTL, strict path guard limited to app storage), worker rewritten, expired photos hidden in queries, extra sweep at launch. Videos are never selected by photo cleanup.
8. **Build/docs** – codemagic.yaml, README, build report corrected; junit added for unit tests.

## Files
Modified: app/build.gradle.kts, app/proguard-rules.pro, gradle/libs.versions.toml, codemagic.yaml, gradlew, gradlew.bat, README.md, BUILD_AND_TEST_REPORT.md, XStudioApp.kt, AuthScreen.kt, XStudioDatabase.kt, Models.kt, HistoryCleanupWorker.kt, XStudioNavHost.kt, SupabaseModule.kt, PhotoEnhancerScreen.kt, ProfileAndHistoryScreens.kt, StudioToolsScreens.kt, VideoEnhancerScreen.kt, XMotionScreen.kt, XSocialScreen.kt.
Added: core/media/MediaStoreExporter.kt, history/HistoryFiles.kt, photoenhancer/OutputPlan.kt, videoenhancer/VideoFpsPolicy.kt, app/src/test/kotlin/com/xstudio/app/{OutputPlanTest,VideoFpsPolicyTest,HistoryAndExportTest,XMotionParserTest}.kt, REPAIR_REPORT.md.
Removed: gradle/wrapper/gradle-wrapper.jar, app/src/main/assets/models/README_MODELS.txt.

## Remaining limitations
- Not compiled or run (see BUILD_AND_TEST_REPORT.md).
- Favorites are NOT exempt from the 72-hour photo expiry (original behaviour kept).
- Android 8–9: gallery saves may need the legacy storage permission, which the app does not request; failures are reported, not hidden.
- X Social trending thumbnails build a *public* URL for the private `social-media` bucket and will likely not load (signed URLs not implemented).
- X Motion: projects live in memory only; XML is not re-exportable; no effects/audio/keyframes; no MP4.
- TikTok Exporter applies resolution only (now stated in the UI); ZIP to Video only inspects (text corrected). Both were not otherwise audited.
- `/api/config` field names are accepted as `supabaseUrl|supabase_url` and `supabaseAnonKey|supabase_anon_key|supabaseKey`; the backend was not available to confirm.
- Password-recovery email link has no in-app deep-link handler (unchanged).

## Device testing needed
Android 8, 10, 13/14 devices: sign-in/OTP; Photo export (JPEG in Pictures/XStudio); Video 30 and 60 fps sources with every enabled FPS chip, cancel, and a forced encoder failure (4K on a weak device); PNG frame export; history expiry (set clock +73 h).
