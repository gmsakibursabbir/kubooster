## Frame-rate check after playing

- **"Frame rate kept" / "Game lowered the frame rate" card** - after you apply and play, KuBooster compares the frame rate it wrote with what the game saved. If the game lowered it (PUBG Flash does this on some phones, e.g. 90 back to 60), a red card tells you and suggests the frame rate to pick; if it kept it, a green card confirms it.
- The check only runs once the game has rewritten its save since the apply, so there is no verdict right after applying. It clears on a new apply, Restore or Reset.
- The diagnostic log records every check.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome.
