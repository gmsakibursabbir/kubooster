## Fixed: PUBG Flash not applying or opening + Restore always available

- **Apply works when the game is still open** - a game left in Recent apps stays running (PUBG Flash especially), and KuBooster refused to edit its files, so the apply silently did nothing and the game never opened. KuBooster now closes the game itself through Shizuku before applying or restoring, checks it is gone, then continues and reopens it.
- **Game opens reliably after apply** - the game is now started through Shizuku (`am start`, the same as ADB), which OEM "app launching other apps" restrictions on ColorOS/OxygenOS and MIUI cannot block. The normal launch is kept as a fallback; the diagnostic log shows which one ran.
- **Restore / Reset is always shown** - with a backup it restores your original settings; without one (e.g. after reinstalling) it becomes "Reset to default", which removes KuBooster's settings from the game config and unlocks the refresh rate. For frame rate and graphics, use the game's own Reset Graphics.
- **Backups survive reinstalls** - a copy of the original files is kept next to the game's own files and loaded back automatically.
- **Diagnostic log** - now scans the game's config files for frame-rate limits and records what the game saved before a restore.

Note: installing over a test build signed with a different key requires uninstalling KuBooster first; your game files are not affected.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome.
