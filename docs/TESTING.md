# Testing

## Automated tests (JVM, `:app:testDebugUnitTest`)

| Suite | Covers |
|---|---|
| `VersionComparisonTest` | numeric (not lexicographic) version comparison, suffix tolerance, major-segment dominance |
| `FpsProfileEngineTest` | FPS state logic: display-supported vs not-supported vs manifest-verified; no false "will render" claims; recommendation = highest non-unsupported |
| `ManifestParserTest` | the real bundled asset parses; 5 variants present; package names match the registry; no entry claims verification without evidence; target version is 4.7.0 |
| `GraphicsRecommendationEngineTest` | 3 presets always returned; RAM-tier-driven advice; HDR only for flagship; thermal rationale on hot devices |

All 20 tests pass as of this build. Engines are pure Kotlin where possible so they run on the JVM without an emulator.

## Instrumented tests

`src/androidTest` scaffolding (Compose UI test runner) is configured. No device/emulator exists in this build environment, so instrumented and UI tests were **not executed** — they require hardware. The Compose screens are structured (state-driven, no direct repository calls) to make those tests straightforward on a machine with a device.

## Device validation checklist (open — requires physical devices)

- [ ] Android 8.0 (API 26) — minSdk floor: onboarding, dashboard, boost flow
- [ ] Android 14 — primary target
- [ ] 60 Hz device — 90/120 options must show Not supported
- [ ] 90 Hz device — 120 option must show Not supported
- [ ] 120 Hz device — 120 option must show display-compatible but **unverified**
- [ ] Multiple OEM skins (Pixel exposes peak_refresh_rate; many OEMs do not — the refresh-rate operations must show as unsupported with a reason, not fail silently)
- [ ] Shizuku: not installed / service stopped / permission denied / granted (grant + revoke round-trip)
- [ ] PUBG v4.7.0 installed (target match), older and newer versions (older/newer states)
- [ ] Each of the 5 variants (especially `com.tencent.igfit` Flash and `com.pubg.imobile` BGMI)
- [ ] Low battery + battery saver active (Disable battery saver proposal appears only with WRITE_SECURE_SETTINGS)
- [ ] Thermal warnings (proposal copy + dashboard warning)
- [ ] Interrupted operations: kill app between apply and verify; journal must restore originals on next run
- [ ] Restoration: restore-one, restore-all, journal cleared after restore, idempotent double-restore
- [ ] font_scale 2.0 and screen-reader pass on dashboard + optimization screens
- [ ] APK size and startup time measured against targets (<10 MB release, <1.5 s cold start)

**Rule honored**: no physical-device claim is made until the run actually happens.
