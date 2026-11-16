# KuBooster

**KuBooster by KUDUY** — *Play Smooth. Stay Cool.*

A lightweight, honest Android gaming performance companion focused on PUBG Mobile compatibility. KuBooster reads your device's real capabilities through public Android APIs, checks PUBG Mobile installation and version against a bundled compatibility manifest, recommends graphics presets and FPS targets that fit your hardware, and applies a small set of reversible, user-confirmed system settings.

KuBooster **never** modifies game files, game memory, or anti-cheat systems, and never reports a change it did not actually verify.

## Features

- **Device Intelligence** — manufacturer, model, CPU (cores/ABIs), GPU (renderer, vendor, OpenGL ES, Vulkan), RAM tier, Android version, all from public APIs. Unknown values are shown as "Unknown", never guessed.
- **Display Capability** — current refresh rate and all advertised Display.Mode rates, kept strictly separate from game FPS.
- **PUBG Detection** — supports PUBG Mobile Global (`com.tencent.ig`), PUBG Mobile Flash (`com.tencent.igfit`), KR (`com.pubg.krmobile`), VN (`com.vng.pubgmobile`), and BGMI (`com.pubg.imobile`) via a narrow `<queries>` declaration. Installed version is read from PackageManager, never assumed.
- **Compatibility Engine** — combines device + display + bundled manifest into a confidence-scored report. FPS options are labeled Verified / Unverified / Not supported; display Hz is never reported as game FPS.
- **Graphics presets** — Smooth Performance, Balanced, High Quality, plus a persisted custom profile (DataStore).
- **Boost performance** — a confirmable, transactional flow that may change peak/min refresh-rate preference, brightness, battery saver, and animation scale. Every applied change is verified by read-back and recorded for restoration.
- **Restoration Manager** — transaction journal in DataStore; restore one change or all of them, always to the recorded *original* value.
- **Optional Shizuku** — used only to grant `WRITE_SECURE_SETTINGS` to this app via the documented binder API with a compile-time-fixed command. Core features work fully without Shizuku.

## Build

Requirements: JDK 17, Android SDK (compileSdk 35, build-tools 35.0.0).

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # signed release APK (needs keystore.properties)
./gradlew :app:testDebugUnitTest  # JVM unit tests
```

The release signing key is configured through `keystore.properties` (git-ignored). See `docs/RELEASE.md`.

Output: `app/build/outputs/apk/{debug,release}/`

## Permissions

- `WRITE_SETTINGS` — only used after the user explicitly grants it through the system "Modify system settings" screen, for reversible brightness/refresh-rate changes.
- Narrow package visibility for the five PUBG variants only.
- No INTERNET, location, camera, microphone, contacts, SMS, accessibility, VPN, or device-administrator permissions. No analytics, no ads, works offline.

## Documentation

- `docs/ARCHITECTURE.md` — module layout, engines, design decisions
- `docs/SECURITY.md` — security posture, operation allowlist, prohibited behaviors
- `docs/COMPATIBILITY.md` — manifest schema and verification policy
- `docs/TESTING.md` — test coverage and device-validation checklist
- `docs/PROJECT_STATUS.md` — current status and known limitations
- `docs/RELEASE.md` — signing and distribution
