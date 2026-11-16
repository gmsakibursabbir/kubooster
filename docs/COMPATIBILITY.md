# Compatibility Policy

## Manifest

`app/src/main/assets/compatibility_manifest.json` is the single source of verified game-side claims. Schema:

```jsonc
{
  "manifestVersion": 1,
  "entries": [
    {
      "gameVariant": "GLOBAL",              // GameVariant.name
      "packageName": "com.tencent.ig",      // verified package id
      "versionName": "4.7.0",               // exact target version
      "versionCode": null,                  // never assumed
      "supportedDeviceConditions": null,    // e.g. "arm64-v8a, Android 14, 8GB+"
      "verifiedFpsProfiles": [],            // e.g. ["60", "90"] — documented options only
      "graphicsRecommendations": null,
      "evidence": null,                     // provenance string; required for "verified"
      "verificationStatus": "unverified",   // "verified" | "unverified"
      "verifiedAt": null                    // ISO-8601 date evidence was recorded
    }
  ]
}
```

## Verification rules (enforced in code and tests)

1. An entry is **VERIFIED** only when `verificationStatus == "verified"` AND `evidence` is non-empty AND `verifiedFpsProfiles` is non-empty AND the installed `versionName` matches the entry exactly.
2. An FPS option enters `verifiedFpsOptions` only through that path. A 120 Hz display alone can never promote an option to Verified — the engine caps such options at `DISPLAY_COMPATIBLE_GAME_UNVERIFIED`.
3. `versionCode` is nullable: version codes are read from PackageManager at runtime and never invented in the manifest.
4. Display refresh rate (DisplayManager) and game-rendered FPS are different facts; the FPS screen states this explicitly.
5. No entry may claim verification without evidence — enforced by `ManifestParserTest`.

## Supported variants

| Variant | Package | Target |
|---|---|---|
| PUBG Mobile (Global) | `com.tencent.ig` | 4.7.0 |
| PUBG Mobile Flash | `com.tencent.igfit` | 4.7.0 |
| PUBG Mobile (Korea) | `com.pubg.krmobile` | 4.7.0 |
| PUBG Mobile (Vietnam) | `com.vng.pubgmobile` | 4.7.0 |
| Battlegrounds Mobile India | `com.pubg.imobile` | 4.7.0 |

Installed versions are compared numerically against the manifest target (`VersionComparison`): target / newer / older / unknown.

## Current verification state

**All entries are `unverified` and carry no FPS claims.** No fabricated device compatibility has been recorded. When real evidence exists (e.g., a documented device/game/FPV combination confirmed on hardware), fill the entry's `verifiedFpsProfiles`, `evidence`, `verificationStatus: "verified"`, and `verifiedAt`, and bump `manifestVersion` if the schema changes.

## Updating compatibility

The manifest ships in the APK; updates arrive with app updates. The parser ignores unknown keys, so newer app manifests stay readable by older parsers.
