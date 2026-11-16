## GFX correctness update

- **Root-cause fix for FPS unlock not applying** - new `+CVars=` keys are now written inside the `[UserCustom DeviceProfile]` section, the device profile the game actually applies. Previously they were appended at the end of the file, where the game's parser can ignore them and the unlock silently did nothing. If the file has no profile section, one is created.
- **`r.PUBGMobileFrameRate` added to the FPS unlock** - newer game versions cap frame rate through this key in addition to the five classic `r.PUBGDeviceFPS*` keys; older versions ignore it harmlessly.
- **Super Smooth preset** - Flash-exclusive tier (shown for PUBG Flash): lightest tuning, everything optional off, lowest readable resolution scale.
- **FPS chips renamed to match the game**: 60 Extreme, 90 Extreme+, 120 Ultra Extreme, with a hint to pick the option in the game's Frame Rate settings after applying.
- **Remove GFX fixed** - the red Remove GFX settings card now disappears after a successful restore (backup is cleared; a fresh one is created on the next apply).
- **Play Protect guidance** - the in-app update banner and README explain the standard "App scan recommended" dialog shown for all sideloaded APKs.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config file, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. Settings-mode features remain fully reversible and ban-safe.
