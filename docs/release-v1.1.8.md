## KuBooster is now a GFX-only app

- **GFX is the whole app** - onboarding, bottom bar and the other tabs are gone. Open it and you're on the GFX screen; the in-app update banner sits under the header.
- **Redesigned GFX screen** - hero card showing your device, Shizuku status, and the frame rate + graphics the game will use; segmented game and FPS pickers; resolution slider; a 3-column graphics grid; a backup card; and a sticky swipe-to-apply bar.
- **HDR preset** - HD with the HDR lighting pipeline (game level 5), between HD and Ultra HDR.
- **Anti-aliasing toggle** - 4x MSAA on or off (`r.MobileMSAA`), off by default because MSAA costs frame rate.
- **Better diagnostics** - the config scan now covers every .ini under `Saved/`, and the game's Active.sav values are logged before Restore.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. GFX changes remain fully reversible and ban-safe settings features are untouched.
