# Security

## What KuBooster will never do

The following are prohibited by the product security policy and are not implemented anywhere in the codebase:

- Aimbots, auto-aim, aim-lock, automated recoil control, or any gameplay automation
- Game memory modification, process injection, or PUBG executable/config modification
- Anti-cheat bypass, ban-evasion mechanisms, signature manipulation, or "anti-ban" claims
- Arbitrary or user-supplied shell commands through any privilege channel
- Killing background processes or "RAM cleaning" theater
- Claiming a device can render 90/120 FPS merely because its display supports those rates
- Promising zero bans or any anti-cheat outcome

## Privilege model

| Capability | Permission | Grant path | Used for |
|---|---|---|---|
| Reversible Settings.System writes | `WRITE_SETTINGS` | User action in system "Modify system settings" screen | Peak/min refresh-rate preference, brightness |
| Reversible Settings.Global writes | `WRITE_SECURE_SETTINGS` | Optional Shizuku grant (`pm grant` on our own package) | Battery saver, animation scales |

### Shizuku operation allowlist

Exactly two commands, fixed at compile time in `ShizukuRepositoryImpl`, executed through the documented `IShizukuService.newProcess` binder API:

1. `pm grant com.kuduy.kubooster android.permission.WRITE_SECURE_SETTINGS`
2. `pm revoke com.kuduy.kubooster android.permission.WRITE_SECURE_SETTINGS`

There is no code path that forwards user input to a shell command. Each execution requires: Shizuku installed + running + permission granted + explicit user tap, and the result is verified by reading the resulting capability (`SystemSettingsManager.canWriteSecureSettings()`).

### Settings write allowlist

`SystemSettingsManager` accepts writes only for these keys (compile-time set, no dynamic keys):

- `Settings.System`: `peak_refresh_rate`, `min_refresh_rate`, `screen_brightness`
- `Settings.Global`: `low_power`, `window_animation_scale`, `transition_animation_scale`, `animator_duration_scale`

## Data and network

- No `INTERNET` permission. Core features are fully offline.
- No analytics, ads, or telemetry SDKs. Nothing is uploaded.
- Package visibility is restricted to five PUBG package names via `<queries>`.
- No PUBG account data or player IDs are read or stored.
- Local storage: DataStore preferences (custom profile, onboarding flag) and the restoration journal (operation records only, no secrets).

## Honest reporting

Every optimization result carries `applied` and `verified` booleans. A write that was accepted but fails read-back is reported as "Applied, unverified" — never as success. Restoration records always keep the value that existed *before* our change, so restoring twice is idempotent.

## Dependencies

Pinned versions in `gradle/libs.versions.toml` (AGP 8.9.1, Kotlin 2.0.21, Shizuku API 13.1.5, etc.). Shizuku AIDL stubs are vendored from RikkaApps/Shizuku-API (Apache 2.0) and kept minimal.
