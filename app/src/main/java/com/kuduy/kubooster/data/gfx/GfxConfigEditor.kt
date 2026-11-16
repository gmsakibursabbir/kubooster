package com.kuduy.kubooster.data.gfx

/**
 * Pure line-level editing of a `UserCustom.ini` content string. No file or
 * device access — fully unit-testable.
 *
 * The managed keys are the ones the game's device profile uses for its
 * frame-rate options (widely documented by the PUBG Mobile config community,
 * e.g. the XDA "Explanation of settings USERCUSTOM.ini" thread):
 *
 *   r.PUBGDeviceFPSLow / r.PUBGDeviceFPSMid / r.PUBGDeviceFPSHigh /
 *   r.PUBGDeviceFPSHDR  / r.PUBGDeviceFPSUltraHDR
 *
 * Setting all five to the same value is the classic GFX-tool FPS unlock:
 * the game then offers the corresponding frame-rate options in its own
 * settings menu, regardless of the device whitelist.
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
    )

    private val CVALS_LINE = Regex("^([ \\t]*)\\+CVars=(.*)$")

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
     * Sets every managed FPS key to [fps]. Existing lines are rewritten in
     * place, preserving their original indentation. Keys absent from the file
     * are appended after the last `+CVars=` line (or at the end of the file).
     * Line endings of the original file are preserved.
     */
    fun applyFps(content: String, fps: Int): String {
        require(fps in 30..240) { "unrealistic fps value: $fps" }
        val newline = if (content.contains("\r\n")) "\r\n" else "\n"
        val lines = content.split("\r\n", "\n")
        val managed = MANAGED_FPS_KEYS.toSet()
        val replaced = mutableSetOf<String>()
        var lastCvarsIndex = -1

        val out = lines.mapIndexed { index, raw ->
            val match = CVALS_LINE.find(raw)
            if (match == null) {
                raw
            } else {
                lastCvarsIndex = index
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
                    "$prefix+CVars=${UserCustomCodec.encode("$key=$fps")}"
                } else {
                    raw
                }
            }
        }.toMutableList()

        val missing = MANAGED_FPS_KEYS.filterNot { it in replaced }
        if (missing.isNotEmpty()) {
            val insertAt = if (lastCvarsIndex >= 0) lastCvarsIndex + 1 else out.size
            out.addAll(
                insertAt,
                missing.map { "+CVars=${UserCustomCodec.encode("$it=$fps")}" },
            )
        }
        return out.joinToString(newline)
    }
}
