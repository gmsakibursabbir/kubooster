# Project Status

Last updated: 2026-11-16 · Build: **v1.1.13 (frame-rate check after playing)**

## v1.1.13 changes

1. **Applied record** — `PubgGfxEngine.recordApplied` stores the applied FPS level + a hash of the written Active.sav (`<pkg>.applied`); cleared on backup delete / reset.
2. **`GfxViewModel.checkKept`** — on load, once the save hash differs from the applied one, compares `BattleFPS` to the applied level and sets `fpsCheck`; reset on apply.
3. **`FpsCheckCard`** on the GFX screen — "Frame rate kept" or "Game lowered the frame rate" with a suggested FPS; logged.
4. Unverified on device.

## v1.1.12 changes

1. **GVAS BoolProperty support** in `ActiveSavEditor` (read + in-place set; value byte lives in the tag). Unit-tested.
2. **"Flash Exclusive" preset (PUBG Flash only)** — Super Smooth CVars + quality level 1 + `bFlashOnlyQuality=true`; other presets set it false. Preselected for Flash; the 60 FPS default for Flash is removed.
3. Restore puts `bFlashOnlyQuality` back; the log shows its value on load/apply/restore.
4. Unverified: that `bFlashOnlyQuality` is exactly what Flash's own "FLASH Exclusive" option writes — confirm from a log taken after selecting it in-game.

## v1.1.11 changes

1. **`RefreshRatePicker`** — locks the lowest panel rate that is a whole multiple of the target FPS (max as fallback); no lock at ≤ 60 FPS. Unit-tested (`RefreshRatePickerTest`).
2. **Stutter warning** on the FPS card when the panel has no even mode for the chosen FPS (e.g. 90 on 60/120 Hz).
3. **PUBG Flash defaults to 60 FPS** with a warning above 60 — Flash's own cap can't be lifted from its files (diagnostic scan found no FPS limit in any Flash config).

## v1.1.10 changes

1. **Apply/Restore close a running game first** — via Shizuku `am force-stop`, verified with `pidof`. A game left in Recent apps (PUBG Flash especially) used to make every apply abort silently, so nothing was written and the game never opened.
2. **Game launch via Shizuku `am start`** (resolved launcher activity, `monkey` fallback), then the Activity-context launch as a fallback — OEM launch restrictions can't block the shell user.
3. **Restore / Reset to default always visible** — without a backup, removes every KuBooster-managed `+CVars` key from UserCustom.ini and the refresh-rate lock.
4. **Backups mirrored beside the game files** (`*.kubooster-original`) and re-imported on load, so they survive a KuBooster reinstall.
5. Diagnostics: `.ini` frame-rate scan under `Saved/`; Active.sav values logged before restore.

## v1.1.9 changes

1. **Resolution slider replaced with the segmented control** — the custom slider rendered its thumb detached from the track end (broken visuals); Default / 720p / 1080p now use the same segmented control as Game Version and FPS.
2. **"Super Smooth" tile no longer truncated** — tiles allow two lines, so the graphics grid shows the full preset name.
3. **Sticky "Swipe to Apply & Boost" bar no longer covers content** — bottom scroll room increased so the Anti-Aliasing card and Diagnostic log can be scrolled fully into view.
4. **The game opens again after a successful apply** — the launch now runs from the Activity context (starting activities from an app-context background launch is blocked on some OEM skins — MIUI/HyperOS, ColorOS — which silently killed the launch), with a fallback launcher intent, diagnostic-log entries, and a visible message if launching fails.
5. **Proper game name shown** — the Game Version card captions the selected variant's full name and installed version (e.g. "PUBG Mobile Flash · v3.7.0").
6. **Restore card moved above Anti-Aliasing** — the backup/restore controls are never buried below the fold.
7. Unused slider code and imports removed.

## v1.1.8 changes

1. **GFX is now the app's only screen** — onboarding, bottom bar and the other tabs are gone from the UI (their code is kept but unreferenced); the in-app GitHub update banner sits under the GFX header.
2. **Redesigned GFX screen** — hero card with device/Shizuku status and the frame rate + graphics the game will use, segmented game and FPS pickers, resolution slider, a 3-column graphics grid (Super Smooth available for every variant), anti-aliasing toggle, backup card, and a sticky swipe-to-apply bar.
3. **HDR preset added** (game level 5 — HD with the HDR lighting pipeline) and **anti-aliasing toggle** (`r.MobileMSAA` 4x on / 1x off, off by default since MSAA costs frame rate).
4. **Diagnostics extended** — the scan now covers every .ini under `Saved/` for frame-rate settings, and the game's Active.sav values are logged before Restore.

## v1.1.7 changes

1. **Refresh-rate lock above 60 FPS** — when applying 90/120 FPS, KuBooster locks the panel's peak and minimum refresh rate to the maximum via Shizuku, so the game can't hold the screen at 60 Hz while rendering 90 (stutter on PUBG Flash). Original values are saved and restored on Remove GFX, or automatically when applying 60 FPS or less.
2. **Safe restore** — restoring now writes only the frame-rate and graphics values back into the *current* Active.sav instead of overwriting the whole file, so game settings changed since the backup are kept. The card shows exactly what restore will put back, with a "Discard backup" option for a bad backup.
3. **UserCustom.ini rebuilt from the pristine backup** — applies build on top of the untouched original, so keys from earlier applies can't linger.
4. **`r.Streaming.PoolSize` dropped from all presets** — the small texture pool caused hitching.
5. **Simpler GFX screen** — resolution row, 30 FPS, Default graphics option, preset descriptions and extra hints removed; status and restore merged into one card; defaults are Super Smooth + 90.
6. Test added for the value-only restore.

## v1.1.6 changes

1. **Graphics level mapping corrected and verified on v4.7.0** — a device with Super Smooth selected in game writes `BattleRenderQuality=1`, proving the real mapping: 1 Super Smooth · 2 Smooth · 3 Balanced · 4 HD · 5 HDR · 6 Ultra HDR · 7 Extreme HDR. The previous mapping (Smooth=1 … Super Smooth=6) made the game read level 6 as Ultra HDR — and **higher quality levels clamp the allowed frame rate** (Ultra HDR caps at Ultra/40), so a wrong level silently killed the FPS unlock. This was the cause of "applied but FPS stuck at 40-60".
2. **"In game now" card** — the Current config card is replaced by a simpler card showing the frame rate and graphics the game will actually use, in the game's own names (Extreme / Extreme+ / Ultra Extreme; Super Smooth … Extreme HDR). Raw CVar rows and the hero card are removed.
3. Warning is logged when FPS is unlocked without a graphics preset (the save's existing graphics level may clamp the frame rate).
4. Tests updated for the corrected mapping.

## v1.1.5 changes

1. **Graphics quality now written into Active.sav too** — besides the frame-rate properties, the save's quality properties (`ArtQuality`, `LobbyRenderQuality`, `BattleRenderQuality`, any `*RenderQuality`) are set to the preset's game level (1 Smooth · 2 Balanced · 3 HD · 5 Ultra HD · 6 Super Smooth, per MK-PUBG-Mobile-Tool). The game's own Graphics setting then matches what KuBooster applied.
2. **Super Smooth offered for every game variant** (no longer Flash-only) — it maps to the save's level 6 regardless of variant.
3. **GFX diagnostic log** — every read, write, verify and Active.sav finding (file sizes, setting names/values, SaveGames folder listing when missing) is recorded in memory + app-private file. The GFX screen has a Diagnostic log card with Show / Copy / Share / Clear, so "FPS doesn't unlock" reports become actionable. Contains no personal data.
4. Engine write failures now log the shell exit code and copy output; remove/apply/load steps are all logged.
5. Tests cover the quality level mapping and quality-property write (ArtQuality/RenderQuality only, byte-size preserved).

## v1.1.4 changes

1. **Active.sav FPS unlock (the real fix for "FPS not working")** — modern game versions take the selected frame rate from `Saved/SaveGames/Active.sav` (UE4 GVAS save), not from `UserCustom.ini`. Applying GFX now edits both: the .ini cvars as before, plus every frame-rate property in the save (`FPSLevel`, `BattleFPS`, `LobbyFPS`, and any other `*FPS*` int property) is set in place to the matching level (2 Power Saving · 3 Medium · 4 High(30) · 5 Ultra(40) · 6 Extreme(60) · 7 Extreme+(90) · 8 Ultra Extreme(120)). Property layout matches the open-source MK-PUBG-Mobile-Tool.
2. **Honest apply result** — Success is reported only when the save write is verified too; specific failure messages guide the user (Active.sav missing / no FPS field yet → open the game once, change Frame Rate, fully close it, apply again).
3. **Active.sav is backed up separately** (restored by Remove GFX together with the .ini); the Current config card shows the save's actual frame-rate level; after applying, users are warned not to change Frame Rate in the game again (it overwrites the unlock).
4. Unit tests cover the GVAS editor: read/apply/round-trip byte-exactness, non-FPS properties untouched, no-FPS-field no-write, level mapping.

## v1.1.3 changes

1. **GFX writes are now section-aware (root-cause fix for "FPS unlock not working")** — missing `+CVars=` keys are inserted inside the `[UserCustom DeviceProfile]` section (the device profile the game actually applies). Previously they were appended at the end of the file, landing in whatever section was last or outside any section — there the game's parser ignores them and the unlock silently did nothing. If the file has no profile section, one is created.
2. **`r.PUBGMobileFrameRate` added to the FPS unlock** — newer game versions cap the frame rate through this key in addition to the five classic `r.PUBGDeviceFPS*` keys; older versions ignore it harmlessly.
3. **Super Smooth preset added** (Flash exclusive, shown only for PUBG Flash) — lightest tuning: everything optional off, lowest readable resolution scale (0.6), shadows off, simple shader on.
4. **Remove GFX flow fixed** — after a successful restore the backup is deleted and the "Remove GFX settings" card disappears (it previously stayed visible forever). The button is now solid red.
5. Graphics presets round-trip tests cover all presets including Super Smooth; new tests assert section-aware insertion and section creation.

## v1.1.2 changes

1. **GFX screen redesigned to the product mockups** — two-tone "kuBooster GFX" title, theme toggle button (moon/sun) in the header, "PRO OPTIMIZER" hero card, plain section rows (**Game Version / Resolution / Graphics / FPS Limit**) with brand-styled chips, and a full-width **Apply & Run Game** button that launches PUBG after a verified write.
2. **Resolution setting added** — Default / 720p / 1080p (`r.MobileContentScaleFactor` 1.0 / 0.7; overrides the preset's own scale value when merged).
3. **Apply overlays per mockup** — full-screen "Optimizing… / Injecting kuBooster configuration" spinner during the write and "Success! / Launching game…" with a backup reminder afterwards; failures surface as message cards.
4. **Backup made explicit** — the confirm dialog shows the backup state ("will be backed up before writing" / "already backed up — restorable any time"), the backup is verified to exist before any write, and the success overlay repeats that removal/restore is one tap.
5. **In-app theme toggle** — moon/sun button stores an explicit dark/light override ("dark"/"light" preference, absent = follow system); MainActivity honors it. Light-mode accent switched to blue per the light mockup.
6. **README download button** — big "Download Latest APK" badge pointing at releases/latest; release badge bumped; "never modifies the game" wording updated to describe the two modes honestly.

## v1.1.1 changes

1. **Graphics presets in GFX mode** — alongside the frame-rate unlock, the GFX tab now offers the same tiers as the game's own menu: **Smooth / Balanced / HD / Ultra HDR**, plus "Game's own" (frame rate only). Preset values are standard UE4/PUBG quality CVars (`r.UserQualitySetting`, `r.ShadowQuality`, `r.MobileContentScaleFactor`, …); "Balanced"/"HD" values come from the published reference corpus (a real game-written profile), Smooth is tuned to cut lag on weak devices, Ultra HDR enables the full HDR pipeline for strong devices. FPS + graphics are applied in one verified write.
2. **Remove GFX settings (reset to default)** — a dedicated card writes the pristine backed-up `UserCustom.ini` back byte-exact, returning the game to its own default config.
3. **"Current config" card fixed and extended** — shows the decoded frame-rate summary (big number), each FPS key, and the current graphics quality level read from the file. When PUBG has never written a key it says "not set / Game default" — that is the honest state, not a read failure (the game uses its built-in defaults for absent keys).
4. **Light mode fixed** — the light color scheme was previously built on `darkColorScheme`, so card surfaces kept dark defaults in light mode. Rebuilt with `lightColorScheme` and full light container roles; status pills and warnings in GFX mode now use theme-aware colors instead of hardcoded dark-palette colors.

## v1.1.0 changes

1. **GFX mode (the FPS unlock)** — a new tab that edits PUBG's `UserCustom.ini` directly:
   - Decodes the file's hex-encoded XOR-0x79 `+CVars` values natively (`UserCustomCodec`), unit-tested against the published reference decoder corpus.
   - Sets `r.PUBGDeviceFPSLow/Mid/High/HDR/UltraHDR` to 30/60/90/120 (`GfxConfigEditor`, pure + fully tested) so the game itself offers higher frame rates — works on PUBG Mobile and PUBG Mobile Flash.
   - All file access via Shizuku (`sh -c` under ADB privileges, no root): existence check, read, `pidof` game-running check, staged write via `/data/local/tmp` + `cp`, read-back byte verification.
   - Automatic byte-exact backup in app-private storage before the first edit; one-tap restore.
   - Explicit acknowledge-checkbox warning that modified configs are detectable and can be banned; no outcome guaranteed.
   - Requires the game to be closed; reports honest diagnostics (config folder listing) when the file is not found.
2. **FPS screen now funnels into GFX mode** — labels stay honest; a card explains the facts and offers "Open GFX mode".
3. Bottom navigation is now 5 tabs (Home / FPS / GFX / Graphics / Settings).

Design stance change: previous versions refused game-file modification entirely. The user (product owner) requested the GFX-tool capability; it is now implemented as a clearly separated, opt-in, warned, backed-up mode rather than silently absent. The safe-mode features remain untouched and unchanged.

## v1.0.3 changes

1. **Game variant selector** — the dashboard now offers Auto-detect plus explicit pins for PUBG Mobile Global / Flash / KR / VN / BGMI. The pinned choice (stored in `selected_game_variant` preference) drives FPS and graphics guidance; when the pinned game is not installed the app falls back to auto-detection rather than inventing data.
2. **In-app updates from GitHub** — `GithubUpdateChecker` queries `api.github.com` for the latest release of `gmsakibursabbir/kubooster`; when newer than the installed version a dashboard banner offers to download the signed APK via the system DownloadManager (from `github.com` only) and hand it to the Android package installer. Auto-check on launch is toggleable in Settings → Updates. New permissions: `INTERNET` and `REQUEST_INSTALL_PACKAGES` (both commented honestly in the manifest). FileProvider added for installer hand-off.
3. **Network honesty updated** — `docs/SECURITY.md` and README now document the single update-check use of the network; no analytics or other endpoints exist.

## v1.0.2 changes

- New launcher icon (adaptive + all densities) built from the KUDUY helmet logo.

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
| `app/build/outputs/apk/release/app-release.apk` | Built, R8-minified, signed — **v1.1.1**; `apksigner verify` passes |

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
