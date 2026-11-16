## Refresh-rate lock + safe restore

- **No more 90 FPS stutter**: when applying above 60 FPS, KuBooster locks the panel's peak and minimum refresh rate to the maximum through Shizuku, so the game can't hold the screen at 60 Hz while rendering 90 (typical on PUBG Flash). Original display values are saved and restored on Remove GFX, or automatically when applying 60 FPS or less.
- **Safe restore**: restoring writes only the frame-rate and graphics values back into the *current* Active.sav instead of overwriting the whole file - other game settings changed since the backup are kept. The card shows exactly what restore will put back, and a bad backup can be discarded.
- **Cleaner config rebuild**: every apply builds UserCustom.ini from the pristine backup first, so keys from earlier applies never linger.
- **Hitching fix**: the small texture streaming pool (`r.Streaming.PoolSize`) is no longer set - it caused stutters.
- **Simpler screen**: resolution row, 30 FPS and Default graphics options removed; the sensible defaults are Super Smooth + 90 FPS.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. Settings-mode features remain fully reversible and ban-safe.
