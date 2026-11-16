## Active.sav FPS unlock - the real fix

- **Why the FPS unlock "did nothing" before**: modern game versions take the selected frame rate from `Saved/SaveGames/Active.sav` (the UE4 GVAS save), not from `UserCustom.ini`. Applying GFX now writes both files.
- **How it works**: every frame-rate property in the save (`FPSLevel`, `BattleFPS`, `LobbyFPS`, plus any other `*FPS*` int property) is overwritten in place with the matching level: Power Saving (2), Medium (3), High 30 (4), Ultra 40 (5), Extreme 60 (6), Extreme+ 90 (7), Ultra Extreme 120 (8). Property layout matches the open-source MK-PUBG-Mobile-Tool. Only existing values change - the file size and layout never change.
- **Honest results**: Success is shown only when the save write is verified. If Active.sav is missing or has no frame-rate field yet, the app tells you exactly what to do: open the game once, change Frame Rate in its settings, fully close it, then apply again.
- **Backup & restore**: Active.sav is backed up separately (before the first write) and Remove GFX restores both files byte-exact.
- **Current config card** now shows the save's actual frame-rate level, and after applying you are warned not to change Frame Rate in the game again - that overwrites the unlock.

Also from v1.1.3: section-aware `[UserCustom DeviceProfile]` writes, `r.PUBGMobileFrameRate` key, Super Smooth preset (Flash), game-accurate FPS labels (60 Extreme / 90 Extreme+ / 120 Ultra Extreme), red Remove button that disappears after restore, Play Protect guidance in the update banner.

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. Settings-mode features remain fully reversible and ban-safe.
