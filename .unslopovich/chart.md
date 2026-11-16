# KuBooster — unslopovich chart

- **Product**: KuBooster by KUDUY — Android gaming performance utility (PUBG Mobile compatibility companion).
- **Platforms**: Android 8.0+ (API 26), primary target Android 14.
- **Stack**: Kotlin, Jetpack Compose, Material 3, single `:app` module, DataStore, optional Shizuku.
- **Top tasks**: 1) See device compatibility with PUBG Mobile (honest, no invented FPS claims). 2) Run the confirmable "Boost performance" flow (small set of reversible Android settings). 3) Look up FPS/graphics guidance for their hardware.
- **Brand decisions**: Spec-defined identity is the brand: near-black navy surfaces (#090B10/#121722/#192130), single neon-lime accent #B7FF38 (primary/onPrimary), electric blue #3B82F6 (secondary), Inter variable typeface. Dark-first; a mapped light scheme exists for OS-forced light mode. Lime-on-dark is a deliberate brand pairing, not a slop gradient.
- **Accepted exceptions**:
  - `unvirtualized-list` suppressed in FpsScreen/GraphicsScreen/OptimizationScreen — lists are 3–5 fixed cards by design; LazyColumn would add nested-scroll complexity for zero gain.
  - KubeCard uses a slightly rounded (18dp) elevated surface with a 1px brand border — the border is part of the brand spec, not a "white card with gray border" template tell.
- **States**: every async surface has loading / error-with-next-step / explicit "Unknown (not exposed)" states. No fake numbers anywhere — metrics come from DeviceInfoRepository etc.
- **Motion**: default Material transitions only; haptics on confirm/toggle moments (compose 1.7 types: LongPress/TextHandleMove); no pulses, no glows, no hover-scale.
- **Not yet checked on running app**: no emulator/device in this environment; all findings are code-based. Layout verification at font_scale 2.0 and light/dark on hardware remains open — see docs/TESTING.md.
