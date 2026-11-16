# Project Status

Last updated: 2026-11-16 · Build: **v1.0.2 (new launcher icon + v1.0.1 bug fixes)**

## v1.0.1 bug fixes

1. **Theme toggle had no effect** — the "Follow system dark mode" preference was stored but never read by MainActivity; now wired.
2. **Brightness could report a false "Applied & verified"** — when adaptive (auto) brightness is on, Android ignores manual brightness writes. The proposal is now marked unsupported with that exact reason instead of claiming success.
3. **Boost operations wrongly unavailable after app restart** — WRITE_SECURE_SETTINGS availability was cached in a volatile flag that reset on process restart, so already-granted operations (battery saver, animation scale) appeared unavailable after closing and reopening the app. Replaced with a live probe write.
4. **Stale "pending restorable changes" count** in Settings — now re-read every time the screen is shown.

## Completed

### Phase 0 — Environment
- Full Android toolchain provisioned from scratch (no SDK existed): Temurin JDK 17.0.21 + Gradle 8.11.1 + Android SDK (platform-tools, android-35, build-tools 35.0.0) under `J:\devtools\`.
- No emulator/physical device available — see limitations.

### Phase 1 — Foundation
- Gradle Kotlin DSL, version catalog with pinned dependencies (AGP 8.9.1, Kotlin 2.0.21, Compose BOM 2024.12.01, Shizuku API 13.1.5).
- Compose + Material 3, brand design system (dark-first, lime accent, Inter variable), edge-to-edge, bottom navigation, navigation graph, DataStore preferences.

### Phase 2 — Device Intelligence
- `DeviceInfoRepositoryImpl`: identity, CPU (cores/ABIs/64-bit), GPU (EGL renderer/vendor, GLES version via pbuffer probe, Vulkan feature + version), RAM tier, security patch.
- Thermal via `PowerManager` status API; skin temperature via `HardwarePropertiesManager` only (null when unavailable). Battery level/state/battery-saver; health intentionally not read (constant is not public API).

### Phase 3 — Display Intelligence
- `DisplayCapabilitiesProvider` / `DisplayInfoRepositoryImpl`: current + advertised refresh rates, resolution, density, VRR flag. Refresh-rate preference change is attempted only where the documented `peak_refresh_rate` key exists, with read-back verification.

### Phase 4 — Game Detection
- 5 variants incl. **PUBG Mobile Flash (`com.tencent.igfit`)**; narrow `<queries>`; real versionName/longVersionCode/installer; no assumptions from labels.

### Phase 5 — Compatibility Engine
- Bundled versioned manifest (5 entries, target 4.7.0, all `unverified` — no invented claims). Report covers device support, version check (target/newer/older/unknown), verified FPS set, unsupported features, limitations, confidence, summary.

### Phase 6 — FPS & Graphics Profiles
- 30/40/60/90/120 with Verified / Display-compatible-unverified / Not supported states.
- 3 engine presets with RAM-tier and thermal-aware advice + persisted custom profile.

### Phase 7 — UI
- Onboarding (5 pages incl. real detection run), Dashboard, FPS, Graphics, Diagnostics, Shizuku, Optimization (review → confirm → apply → verify → restore), Settings. All values from repositories; unknowns explicit.

### Phase 8 — Shizuku
- Optional; documented binder API (vendored AIDL); two compile-time-fixed commands (grant/revoke WRITE_SECURE_SETTINGS); verified after execution.

### Phase 9 — Optimization & Restoration
- Proposal → user confirmation → apply → read-back verify → journal → restore-one/all. Originals recorded pre-change; idempotent restore.

### Phase 10/11 — Testing & Release
- 20 JVM unit tests, all passing (`:app:testDebugUnitTest`).
- Release keystore verified (`keytool -list`); R8 signed release APK built (1.65 MB) and signature verified with `apksigner`.
- Unslopovich detector scan run; 3 false positives verified and suppressed with justification.

## Build artifacts

| Artifact | Status |
|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | Built — v1.0.1 |
| `app/build/outputs/apk/release/app-release.apk` | Built, R8-minified, signed — **1.65 MB**; `apksigner verify` passes |

Signing: keystore password mismatch in `keystore.properties` was the only release-build blocker; corrected against the keystore note copy kept outside the repository (`J:\devtools\kubooster-keystore-note.txt`).

## Test results
- JVM unit tests: 20/20 pass.
- Instrumented/UI tests: configured, not executed (no device).

## Known limitations
1. **No physical-device validation** — no emulator/device in this environment. Layout, refresh-rate behavior, Shizuku grant flow, and PUBG detection remain hardware-checked items (`docs/TESTING.md`).
2. **All manifest entries unverified by design** — game-side FPS claims require real evidence; the app reports "unverified" until then.
3. **Refresh-rate ops are device-dependent** — only devices exposing `peak_refresh_rate` support them; others show them as unsupported with a reason.
4. **Battery health** not read (public API absent); temperature only via skin-temperature sensor where exposed.
5. Gradle wrapper not yet committed at the time of this writing (added via `gradle wrapper` task; see repo).

## Next steps
1. Hardware validation pass (`docs/TESTING.md` checklist) on a real Android 14 device.
2. Gather real evidence to populate manifest entries as they are verified.
3. Measure APK size/startup against targets (<10 MB release expected after R8; debug is naturally larger).
