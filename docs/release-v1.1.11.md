## Smoother frame pacing + honest FPS warnings

- **Screen refresh rate now matches your FPS** - frames are only shown evenly when the refresh rate is a whole multiple of the frame rate. KuBooster now locks the lowest refresh rate your FPS divides into evenly (120 FPS -> 120 Hz, 90 FPS -> 90 Hz on screens that have it), instead of always forcing the maximum. At 60 FPS or lower nothing is locked, and an earlier lock is removed.
- **Stutter warning** - on screens without a matching mode (e.g. 90 FPS on a 60/120 Hz panel) the FPS card warns that it can't be shown evenly and suggests 60 or the panel's maximum; the diagnostic log notes it too.
- **PUBG Flash starts at 60 FPS** - Flash limits the frame rate itself on many phones, and forcing more only adds lag. Selecting Flash now defaults to 60 with a clear warning if you pick higher (you still can).

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome.
