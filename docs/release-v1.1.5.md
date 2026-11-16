## Graphics quality in the save + diagnostic log

- **Graphics presets now also write Active.sav** - along with the frame-rate properties, the save's quality properties (`ArtQuality`, `LobbyRenderQuality`, `BattleRenderQuality`, any `*RenderQuality`) are set to the preset's game level: Smooth 1, Balanced 2, HD 3, Ultra HD 5, Super Smooth 6 (per MK-PUBG-Mobile-Tool). The game's own Graphics setting then reflects what KuBooster applied.
- **Super Smooth for every variant** - the chip is no longer limited to PUBG Flash; it maps to the save's Super Smooth level regardless of which PUBG you play.
- **Diagnostic log** - the GFX screen now has a Diagnostic log card (Show / Copy / Share / Clear). Every read, write, verify and Active.sav finding is recorded - file sizes, setting names and values, and the SaveGames folder listing when the file is missing. If FPS doesn't unlock on your device, tap Share and send the log. It contains no personal data.
- **Verified apply** - after writing, the save is read back and the log shows exactly which levels the game will see.
- Tests cover the quality mapping and quality-property writes (only `ArtQuality`/`RenderQuality` names change, file size preserved).

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. Settings-mode features remain fully reversible and ban-safe.
