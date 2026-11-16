## Fixed: FPS clamped to 40-60 after applying

- **Root cause found and verified on game v4.7.0**: a device with Super Smooth selected in game writes `BattleRenderQuality=1` - proving the real quality levels are 1 Super Smooth, 2 Smooth, 3 Balanced, 4 HD, 5 HDR, 6 Ultra HDR, 7 Extreme HDR. The previous mapping had Super Smooth as 6, which the game read as **Ultra HDR - and higher quality levels clamp the allowed frame rate** (Ultra HDR caps at Ultra/40). A wrong graphics level was silently killing the FPS unlock.
- KuBooster's graphics presets now write the correct level, so the frame rate you pick is no longer clamped.
- **"In game now" card** - the config card is replaced by a simpler one showing the frame rate and graphics the game will actually use, in the game's own names (Extreme / Extreme+ / Ultra Extreme; Super Smooth ... Extreme HDR).
- The diagnostic log warns when FPS is unlocked without a graphics preset (a leftover high graphics level in the save can still clamp the frame rate).

Shizuku (ADB-level access, no root) required for GFX mode. GFX mode edits PUBG's config and save files, which is detectable by anti-cheat - KuBooster warns explicitly and never guarantees any outcome. Settings-mode features remain fully reversible and ban-safe.
