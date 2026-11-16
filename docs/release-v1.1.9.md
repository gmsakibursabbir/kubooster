## UI fixes + game opens again after apply

- **The game opens again after swiping** - the launch now runs from the Activity context. Starting activities from an app-context background call is blocked on some OEM skins (MIUI/HyperOS, ColorOS), which silently killed the launch; there is now also a fallback launcher intent, diagnostic-log entries for the launch, and a visible message if it still fails.
- **Proper game name shown** - the Game Version card shows the selected game's full name and version, e.g. "PUBG Mobile Flash - v3.7.0".
- **Restore is never hidden** - the Restore/Discard backup card moved above Anti-Aliasing so it is immediately visible, not buried below the sticky apply bar.
- **Resolution slider fixed** - the slider rendered its thumb detached from the track; Default / 720p / 1080p now use the clean segmented control used by the other pickers.
- **"Super Smooth" shows in full** - graphics tiles wrap to two lines instead of truncating.
- **Sticky apply bar no longer covers content** - Anti-Aliasing and the Diagnostic log scroll fully into view above the bar.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome.
