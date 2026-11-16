# Architecture

## Module layout

KuBooster is a single `:app` Gradle module with strict package-layering. Multi-module Gradle was evaluated and rejected: the dependency graph is small and stable, and 15+ Gradle modules would multiply build time and APK size for no testability gain. The layering rules are the same ones a multi-module setup would enforce:

```text
com.kuduy.kubooster
├── core/
│   ├── common/          AppDispatchers (injectable dispatchers)
│   └── designsystem/    Theme, Type, KubeColors, shared components
├── domain/
│   ├── model/           DeviceCapabilities, CompatibilityReport, FpsProfile, OptimizationProposal, ...
│   └── repository/      Interfaces consumed by feature ViewModels
├── data/
│   ├── device/          DeviceInfoRepositoryImpl, EglInfo, HardwareTemperature
│   ├── display/         DisplayInfoRepositoryImpl
│   ├── game/            GameDetectionRepositoryImpl
│   ├── compatibility/   CompatibilityEngine, FpsProfileEngine, GraphicsRecommendationEngine,
│   │                    ManifestParser, CompatibilityRepositoryImpl
│   ├── graphics/        GraphicsProfileRepositoryImpl
│   ├── optimization/    OptimizationRepositoryImpl
│   ├── restoration/     RestorationRepositoryImpl + journal (DataStore)
│   ├── preferences/     UserPreferencesRepository (DataStore)
│   └── shizuku/         ShizukuRepositoryImpl (+ Shizuku AIDL stubs)
├── system/              SystemSettingsManager, DisplayCapabilitiesProvider, SettingsRestorerImpl
├── feature/             onboarding, dashboard, fps, graphics, diagnostics, shizuku, optimization, settings
├── di/                  AppContainer (explicit hand-written graph)
└── ui/                  KuBoosterRoot (navigation), Routes
```

**Rule**: `feature/` depends on `domain/` interfaces; `data/` implements them; nothing in `domain/` imports Android framework classes except pure-value models.

## Development order (per product spec)

The engines were built before the UI, in this order: device intelligence → display capability → game detection → compatibility engine → FPS profiles → graphics recommendations → UI → Shizuku → optimization → restoration. Every dashboard value originates in one of these engines; nothing is hardcoded in UI.

## Key design decisions

| Decision | Rationale |
|---|---|
| Single Gradle module, package-layered | APK size and build speed; the "avoid overengineering" rule |
| Hand-written `AppContainer` (no Hilt) | Small graph, explicit construction, fewer codegen deps |
| Manifest parsed from bundled asset with kotlinx-serialization | JVM-testable; works offline; versioned for future updates |
| Verification states as an enum, not a boolean | "Verified / display-compatible-but-unverified / not supported" is a 3-state fact |
| Transaction journal in DataStore (JSON list) | Restoration must survive process death; always records the *pre-change* value |
| AIDL stubs vendored from RikkaApps/Shizuku-API (Apache-2.0) | The documented way to invoke `IShizukuService.newProcess` |
| Inter variable font (single TTF, variation settings) | Brand typography from the product spec at ~0.9 MB cost |

## Data flow example — dashboard

```
DashboardViewModel.refresh()
  → DeviceInfoRepositoryImpl.getDeviceCapabilities()     (Build/ActivityManager/EGL/DisplayManager/PowerManager/BatteryManager)
  → GameDetectionRepositoryImpl.detectInstalledGame()    (PackageManager, narrow <queries>)
  → CompatibilityRepositoryImpl.buildReport()            (CompatibilityEngine + bundled manifest)
  → DashboardState                                       (StateFlow → Compose)
```

## Error model

Every repository operation returns real values or null. Engines translate nulls into explicit states (`ThermalStatus.UNAVAILABLE`, `RamTier.UNKNOWN`, `VerificationStatus.UNVERIFIED`). ViewModels catch exceptions into user-readable messages with a retry action. No result is ever silently reported as success.
