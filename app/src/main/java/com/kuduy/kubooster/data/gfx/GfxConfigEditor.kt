package com.kuduy.kubooster.data.gfx

/**
 * Pure line-level editing of a `UserCustom.ini` content string. No file or
 * device access — fully unit-testable.
 *
 * Two edit families are supported:
 *
 * 1. Frame rate — the keys the game's device profile uses for its frame-rate
 *    options (widely documented by the PUBG Mobile config community, e.g. the
 *    XDA "Explanation of settings USERCUSTOM.ini" thread):
 *
 *      r.PUBGDeviceFPSLow / r.PUBGDeviceFPSMid / r.PUBGDeviceFPSHigh /
 *      r.PUBGDeviceFPSHDR  / r.PUBGDeviceFPSUltraHDR
 *
 *    Setting all five to the same value is the classic GFX-tool FPS unlock.
 *
 * 2. Graphics quality — the standard UE4/PUBG quality CVars. The "balanced"
 *    preset values are taken from the published reference corpus (a real
 *    game-written profile, quality level 2, see UserCustomCodec docs); the
 *    other presets scale the same keys down (Smooth — fewer shadows/effects,
 *    less lag) or up (HD / Ultra HDR).
 *
 * Everything else in the file — sections, comments, unrelated keys — is left
 * byte-for-byte untouched.
 */
object GfxConfigEditor {

    val MANAGED_FPS_KEYS = listOf(
        "r.PUBGDeviceFPSLow",
        "r.PUBGDeviceFPSMid",
        "r.PUBGDeviceFPSHigh",
        "r.PUBGDeviceFPSHDR",
        "r.PUBGDeviceFPSUltraHDR",
        // Newer game versions cap the frame rate through this key as well;
        // older ones simply ignore it. Harmless to always set.
        "r.PUBGMobileFrameRate",
    )

    /** Graphics presets, in the order offered in the UI. */
    val GRAPHICS_PRESETS: Map<String, Map<String, String>> = mapOf(
        // Lightest tuning, styled after PUBG Flash's exclusive "Super Smooth"
        // tier: everything optional off, minimal streaming/shadows, lowest
        // readable resolution scale. For the weakest devices and highest FPS.
        "supersmooth" to mapOf(
            "r.UserQualitySetting" to "0",
            "r.PUBGQualityLevel" to "0",
            "r.PUBGMaxSupportQualityLevel" to "0",
            "r.UserHDRSetting" to "1",
            "r.PUBGLDR" to "0",
            "r.ShadowQuality" to "0",
            "r.Shadow.MaxCSMResolution" to "512",
            "r.Shadow.CSM.MaxMobileCascades" to "1",
            "r.Shadow.DistanceScale" to "0.1",
            "r.MobileContentScaleFactor" to "0.6",
            "r.MobileSimpleShader" to "1",
            "r.BloomQuality" to "0",
            "r.DepthOfFieldQuality" to "0",
            "r.LightShaftQuality" to "0",
            "r.RefractionQuality" to "0",
            "r.DetailMode" to "0",
            "r.EmitterSpawnRateScale" to "0",
            "r.ParticleLODBias" to "2",
            "r.MobileNumDynamicPointLights" to "0",
            "r.MaxAnisotropy" to "1",
        ),
        // Fastest standard: minimal shadows/effects/particles — tuned to avoid
        // lag on weak devices. Resolution scale stays at 0.7 (not lower) so the
        // game stays readable.
        "smooth" to mapOf(
            "r.UserQualitySetting" to "0",
            "r.PUBGQualityLevel" to "0",
            "r.PUBGMaxSupportQualityLevel" to "0",
            "r.UserHDRSetting" to "1",
            "r.PUBGLDR" to "0",
            "r.ShadowQuality" to "0",
            "r.Shadow.MaxCSMResolution" to "512",
            "r.Shadow.CSM.MaxMobileCascades" to "1",
            "r.Shadow.DistanceScale" to "0.2",
            "r.MobileContentScaleFactor" to "0.7",
            "r.MobileSimpleShader" to "1",
            "r.BloomQuality" to "0",
            "r.DepthOfFieldQuality" to "0",
            "r.LightShaftQuality" to "0",
            "r.RefractionQuality" to "0",
            "r.DetailMode" to "0",
            "r.EmitterSpawnRateScale" to "0",
            "r.ParticleLODBias" to "2",
            "r.MobileNumDynamicPointLights" to "0",
            "r.MaxAnisotropy" to "4",
        ),
        // Middle ground — scaled-down version of the reference profile.
        "balanced" to mapOf(
            "r.UserQualitySetting" to "1",
            "r.PUBGQualityLevel" to "1",
            "r.PUBGMaxSupportQualityLevel" to "1",
            "r.UserHDRSetting" to "2",
            "r.PUBGLDR" to "1",
            "r.ShadowQuality" to "1",
            "r.Shadow.MaxCSMResolution" to "1080",
            "r.Shadow.CSM.MaxMobileCascades" to "2",
            "r.Shadow.DistanceScale" to "0.3",
            "r.MobileContentScaleFactor" to "0.8",
            "r.MobileSimpleShader" to "0",
            "r.BloomQuality" to "0.8",
            "r.DepthOfFieldQuality" to "0",
            "r.LightShaftQuality" to "0.8",
            "r.RefractionQuality" to "0",
            "r.DetailMode" to "1",
            "r.EmitterSpawnRateScale" to "0.8",
            "r.ParticleLODBias" to "2",
            "r.MobileNumDynamicPointLights" to "0.8",
            "r.MaxAnisotropy" to "8",
        ),
        // Matches the real game-written profile from the reference corpus.
        "hd" to mapOf(
            "r.UserQualitySetting" to "2",
            "r.PUBGQualityLevel" to "2",
            "r.PUBGMaxSupportQualityLevel" to "2",
            "r.UserHDRSetting" to "2",
            "r.PUBGLDR" to "1",
            "r.ShadowQuality" to "2",
            "r.Shadow.MaxCSMResolution" to "1080",
            "r.Shadow.CSM.MaxMobileCascades" to "2",
            "r.Shadow.DistanceScale" to "0.5",
            "r.MobileContentScaleFactor" to "1.0",
            "r.MobileSimpleShader" to "0",
            "r.BloomQuality" to "0.8",
            "r.DepthOfFieldQuality" to "0",
            "r.LightShaftQuality" to "0.8",
            "r.RefractionQuality" to "0",
            "r.DetailMode" to "2",
            "r.EmitterSpawnRateScale" to "1.0",
            "r.ParticleLODBias" to "2",
            "r.MobileNumDynamicPointLights" to "0.8",
            "r.MaxAnisotropy" to "8",
        ),
        // HDR pipeline with full lighting — for strong devices.
        "ultrahdr" to mapOf(
            "r.UserQualitySetting" to "2",
            "r.PUBGQualityLevel" to "2",
            "r.PUBGMaxSupportQualityLevel" to "2",
            "r.UserHDRSetting" to "3",
            "r.PUBGLDR" to "1",
            "r.MaterialQualityLevel" to "1",
            "r.ShadowQuality" to "2",
            "r.Shadow.MaxCSMResolution" to "1080",
            "r.Shadow.CSM.MaxMobileCascades" to "2",
            "r.Shadow.DistanceScale" to "1.0",
            "r.MobileContentScaleFactor" to "1.0",
            "r.MobileSimpleShader" to "0",
            "r.BloomQuality" to "1.0",
            "r.DepthOfFieldQuality" to "1",
            "r.LightShaftQuality" to "1.0",
            "r.RefractionQuality" to "1",
            "r.DetailMode" to "2",
            "r.EmitterSpawnRateScale" to "1.0",
            "r.ParticleLODBias" to "0",
            "r.MobileNumDynamicPointLights" to "1.0",
            "r.MaxAnisotropy" to "8",
            "r.Mobile.TonemapperFilm" to "1",
            "r.Mobile.AlwaysResolveDepth" to "1",
        ),
    )

    private val CVALS_LINE = Regex("^([ \\t]*)\\+CVars=(.*)$")

    /** The five FPS entries, all set to the same value. */
    fun fpsEntries(fps: Int): Map<String, String> =
        MANAGED_FPS_KEYS.associateWith { fps.toString() }

    /** Resolution tiers offered in the UI; "default" applies nothing. */
    val RESOLUTION_KEYS = listOf("default", "720p", "1080p")

    fun resolutionEntries(key: String): Map<String, String> = when (key) {
        "default" -> emptyMap()
        "720p" -> mapOf("r.MobileContentScaleFactor" to "0.7")
        "1080p" -> mapOf("r.MobileContentScaleFactor" to "1.0")
        else -> error("unknown resolution key: $key")
    }

    /** Decoded key -> value for every `+CVars=` line; later occurrences win. */
    fun readCvars(content: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        content.lineSequence().forEach { raw ->
            val match = CVALS_LINE.find(raw) ?: return@forEach
            val rawValue = match.groupValues[2].trim()
            if (rawValue.isEmpty()) return@forEach
            val decoded = if (UserCustomCodec.isEncoded(rawValue)) {
                UserCustomCodec.decode(rawValue) ?: rawValue
            } else {
                rawValue // tolerate plain (unencoded) entries defensively
            }
            val eq = decoded.indexOf('=')
            if (eq > 0) {
                map[decoded.substring(0, eq).trim()] = decoded.substring(eq + 1).trim()
            }
        }
        return map
    }

    fun readFpsValues(content: String): Map<String, String> =
        readCvars(content).filterKeys { it in MANAGED_FPS_KEYS }

    /**
     * Sets every key in [values] to its given value (replacing existing lines
     * in place). Missing keys are inserted inside the `[UserCustom
     * DeviceProfile]` section — the device profile the game actually applies —
     * never blindly at the end of the file where a different section's keys
     * live (there the game would ignore them, and the unlock would silently
     * do nothing). If the file has no such section, it is created at the end.
     * Line endings of the original file are preserved.
     */
    fun applyCvars(content: String, values: Map<String, String>): String {
        require(values.isNotEmpty()) { "no values to apply" }
        require(values.keys.all { it.isNotBlank() }) { "blank cvar key" }
        require(values.values.all { it.isNotBlank() }) { "blank cvar value" }
        val newline = if (content.contains("\r\n")) "\r\n" else "\n"
        val lines = content.split("\r\n", "\n")
        val managed = values.keys.toSet()
        val replaced = mutableSetOf<String>()

        // Locate the [UserCustom DeviceProfile] section: its header index and
        // the index of the first line after it that starts another section.
        var sectionHeader = -1
        var sectionEnd = lines.size
        val headerRe = Regex("^\\s*\\[\\s*UserCustom\\s+DeviceProfile\\s*\\]\\s*$", RegexOption.IGNORE_CASE)
        val anyHeaderRe = Regex("^\\s*\\[.*\\]\\s*$")
        lines.forEachIndexed { index, raw ->
            if (headerRe.matches(raw)) sectionHeader = index
            else if (sectionHeader >= 0 && index > sectionHeader && anyHeaderRe.matches(raw) && sectionEnd == lines.size) {
                sectionEnd = index
            }
        }

        val out = lines.mapIndexed { _, raw ->
            val match = CVALS_LINE.find(raw)
            if (match == null) {
                raw
            } else {
                val prefix = match.groupValues[1]
                val rawValue = match.groupValues[2].trim()
                val decoded = if (UserCustomCodec.isEncoded(rawValue)) {
                    UserCustomCodec.decode(rawValue) ?: rawValue
                } else {
                    rawValue
                }
                val eq = decoded.indexOf('=')
                val key = if (eq > 0) decoded.substring(0, eq).trim() else ""
                if (key in managed) {
                    replaced.add(key)
                    "$prefix+CVars=${UserCustomCodec.encode("$key=${values[key]}")}"
                } else {
                    raw
                }
            }
        }.toMutableList()

        val missing = values.keys.filterNot { it in replaced }
        if (missing.isNotEmpty()) {
            val newLines = missing.map { "+CVars=${UserCustomCodec.encode("$it=${values[it]}")}" }
            when {
                // Append after the last existing cvar line of the target
                // section (keeps the block together and inside the section).
                sectionHeader >= 0 -> {
                    var insertAt = sectionHeader + 1
                    for (i in sectionHeader + 1 until sectionEnd) {
                        if (CVALS_LINE.matches(out[i])) insertAt = i + 1
                    }
                    out.addAll(insertAt, newLines)
                }
                // No profile section at all — create one; a bare +CVars= line
                // outside any section is ignored by the game's parser.
                else -> {
                    if (out.lastOrNull()?.isNotBlank() == true) out.add("")
                    out.add("[UserCustom DeviceProfile]")
                    out.addAll(newLines)
                }
            }
        }
        return out.joinToString(newline)
    }

    fun applyFps(content: String, fps: Int): String {
        require(fps in 30..240) { "unrealistic fps value: $fps" }
        return applyCvars(content, fpsEntries(fps))
    }
}
