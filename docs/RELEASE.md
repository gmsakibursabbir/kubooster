# Release

## Signing

A release keystore was generated locally for this build:

- Path: `app/keystore/kubooster-release.jks` (git-ignored)
- Alias: `kubooster` · RSA 4096 · validity 30 years
- Credentials: `keystore.properties` in the repo root (git-ignored). A copy of the password note is kept outside the repository at `J:\devtools\kubooster-keystore-note.txt`.

**For production distribution, replace this locally-generated key with your own and keep it backed up** — the key is the app's install identity; losing it means users cannot update.

## Build commands

```bash
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease      # app/build/outputs/apk/release/app-release.apk (signed when keystore.properties exists)
./gradlew :app:testDebugUnitTest
```

Release builds enable R8 minification + resource shrinking (`proguard-android-optimize.txt` + `proguard-rules.pro`, which keeps kotlinx-serialization and Shizuku binder classes).

## Checklist before distribution

- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts`
- [ ] Re-read `docs/TESTING.md` device checklist against the release build
- [ ] Confirm the compatibility manifest state is intentional (no unverified claims promoted)
- [ ] Verify the release APK installs and launches on a real device
- [ ] Keep `keystore.properties` and the JKS out of version control
