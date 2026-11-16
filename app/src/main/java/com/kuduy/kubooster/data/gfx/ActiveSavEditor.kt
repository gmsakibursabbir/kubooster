package com.kuduy.kubooster.data.gfx

/**
 * Edits PUBG Mobile's `Saved/SaveGames/Active.sav`, the UE4 GVAS save that
 * holds the player's selected frame rate and graphics level. Current game
 * versions take the frame rate from this file, not from `UserCustom.ini`, so
 * an FPS unlock that only edits the .ini does nothing.
 *
 * Only existing `IntProperty` values are overwritten in place (same 4 bytes),
 * so the file size and layout never change. No property is ever added.
 *
 * GVAS IntProperty layout (little-endian):
 *   int32 nameLen | name\0 | int32 12 | "IntProperty\0" | int64 4 | byte 0 (no GUID) | int32 value
 *
 * Frame-rate levels (as the game names them):
 *   2 Power Saving · 3 Medium · 4 High (30) · 5 Ultra (40) ·
 *   6 Extreme (60) · 7 Extreme+ (90) · 8 Ultra Extreme (120)
 * Property names and levels match the open-source MK-PUBG-Mobile-Tool.
 */
object ActiveSavEditor {

    /** Properties known to hold the frame-rate level. */
    val KNOWN_FPS_PROPERTIES = setOf("FPSLevel", "BattleFPS", "LobbyFPS")

    private val TYPE_TAG = "IntProperty\u0000".toByteArray(Charsets.ISO_8859_1)

    data class IntProp(val name: String, val valueOffset: Int, val value: Int)

    fun fpsLevel(fps: Int): Int = when {
        fps >= 120 -> 8
        fps >= 90 -> 7
        fps >= 60 -> 6
        fps >= 40 -> 5
        else -> 4
    }

    fun levelLabel(level: Int): String = when (level) {
        2 -> "Power Saving"
        3 -> "Medium"
        4 -> "High (30)"
        5 -> "Ultra (40)"
        6 -> "Extreme (60)"
        7 -> "Extreme+ (90)"
        8 -> "Ultra Extreme (120)"
        else -> "level $level"
    }

    /**
     * Frame-rate properties: the known names, plus any other int property
     * whose name ends in "FPS" or "FPSLevel" (newer versions split combat,
     * lobby, hub and home settings).
     */
    fun isFpsProperty(name: String): Boolean =
        name in KNOWN_FPS_PROPERTIES || name.endsWith("FPS") || name.endsWith("FPSLevel")

    /** Every IntProperty found in [bytes]. Unparseable regions are skipped. */
    fun readIntProperties(bytes: ByteArray): List<IntProp> {
        val out = mutableListOf<IntProp>()
        var i = indexOf(bytes, TYPE_TAG, 0)
        while (i >= 0) {
            parseAt(bytes, i)?.let { out += it }
            i = indexOf(bytes, TYPE_TAG, i + TYPE_TAG.size)
        }
        return out
    }

    fun readFpsProperties(bytes: ByteArray): List<IntProp> =
        readIntProperties(bytes).filter { isFpsProperty(it.name) }

    /**
     * Returns a copy of [bytes] with every frame-rate property set to [level],
     * or null if the file has no recognisable frame-rate property (nothing to
     * edit; the caller must not write).
     */
    fun applyFpsLevel(bytes: ByteArray, level: Int): ByteArray? {
        require(level in 2..8) { "fps level out of range: $level" }
        return applyLevel(bytes, level, ::isFpsProperty)
    }

    /**
     * Graphics-quality properties: `ArtQuality`, `LobbyRenderQuality`,
     * `BattleRenderQuality` (and any other `*RenderQuality`).
     */
    fun isQualityProperty(name: String): Boolean =
        name == "ArtQuality" || name.endsWith("RenderQuality")

    /**
     * Game quality level per KuBooster preset, verified on PUBG Mobile and
     * PUBG Flash v4.7.0 (a device with Super Smooth selected in game had
     * BattleRenderQuality=1):
     *   1 Super Smooth · 2 Smooth · 3 Balanced · 4 HD · 5 HDR ·
     *   6 Ultra HDR · 7 Extreme HDR
     * The game clamps a level the device doesn't support down to its highest
     * allowed one, and higher levels also lower the allowed frame rate (e.g.
     * Ultra HDR caps at Ultra/40) — so a wrong level silently kills the FPS
     * unlock.
     */
    fun qualityLevel(presetKey: String): Int? = when (presetKey) {
        "supersmooth" -> 1
        "smooth" -> 2
        "balanced" -> 3
        "hd" -> 4
        "ultrahdr" -> 6
        else -> null
    }

    fun qualityLabel(level: Int): String = when (level) {
        1 -> "Super Smooth"
        2 -> "Smooth"
        3 -> "Balanced"
        4 -> "HD"
        5 -> "HDR"
        6 -> "Ultra HDR"
        7 -> "Extreme HDR"
        else -> "level $level"
    }

    /** Like [applyFpsLevel] for the graphics-quality properties. */
    fun applyQualityLevel(bytes: ByteArray, level: Int): ByteArray? {
        require(level in 1..7) { "quality level out of range: $level" }
        return applyLevel(bytes, level, ::isQualityProperty)
    }

    /** The frame-rate and graphics values GFX manages (what Remove restores). */
    fun managedValues(bytes: ByteArray): Map<String, Int> = readIntProperties(bytes)
        .filter { isFpsProperty(it.name) || isQualityProperty(it.name) }
        .associate { it.name to it.value }

    /**
     * Sets each named int property that exists in [bytes] to its value in
     * [values]; everything else (other game settings) is left untouched.
     */
    fun setValues(bytes: ByteArray, values: Map<String, Int>): ByteArray {
        val copy = bytes.copyOf()
        readIntProperties(bytes).forEach { p -> values[p.name]?.let { writeInt32(copy, p.valueOffset, it) } }
        return copy
    }

    private fun applyLevel(bytes: ByteArray, level: Int, match: (String) -> Boolean): ByteArray? {
        val props = readIntProperties(bytes).filter { match(it.name) }
        if (props.isEmpty()) return null
        val copy = bytes.copyOf()
        props.forEach { writeInt32(copy, it.valueOffset, level) }
        return copy
    }

    /**
     * Diagnostic: every string in the save that looks like a frame-rate or
     * quality setting name, with the type string that follows it (e.g.
     * "BattleFPS : IntProperty"). Catches settings stored with types this
     * editor does not write, so the log shows why an unlock could not apply.
     */
    fun describeRelevantStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        var start = -1
        for (i in bytes.indices) {
            val c = bytes[i].toInt() and 0xFF
            if (c in 0x20..0x7E) {
                if (start < 0) start = i
            } else {
                if (start >= 0 && c == 0 && i - start >= 3) {
                    strings += String(bytes, start, i - start, Charsets.ISO_8859_1)
                }
                start = -1
            }
        }
        val interesting = Regex("FPS|Frame|Quality|Render", RegexOption.IGNORE_CASE)
        return strings.mapIndexedNotNull { i, s ->
            if (!interesting.containsMatchIn(s) || s.endsWith("Property")) null
            else "$s : ${strings.getOrNull(i + 1) ?: "?"}"
        }.distinct().take(60)
    }

    /** Parses one IntProperty whose type tag starts at [tagAt]. */
    private fun parseAt(b: ByteArray, tagAt: Int): IntProp? {
        // Type-name length (12) sits right before the tag.
        if (tagAt < 4 || readInt32(b, tagAt - 4) != TYPE_TAG.size) return null
        // Walk back over the property name: "...nameLen | name\0 | 12 | tag".
        val nameEnd = tagAt - 4 // index just past the name's NUL
        if (nameEnd < 1 || b[nameEnd - 1].toInt() != 0) return null
        var nameStart = nameEnd - 1
        while (nameStart > 0 && b[nameStart - 1] in 0x20..0x7E) nameStart--
        val nameLen = nameEnd - nameStart
        if (nameLen < 2 || nameStart < 4 || readInt32(b, nameStart - 4) != nameLen) return null
        val name = String(b, nameStart, nameLen - 1, Charsets.ISO_8859_1)
        // After the tag: int64 size == 4, one GUID flag byte == 0, then the value.
        val sizeAt = tagAt + TYPE_TAG.size
        val valueAt = sizeAt + 8 + 1
        if (valueAt + 4 > b.size) return null
        if (readInt32(b, sizeAt) != 4 || readInt32(b, sizeAt + 4) != 0) return null
        if (b[sizeAt + 8].toInt() != 0) return null
        return IntProp(name, valueAt, readInt32(b, valueAt))
    }

    private fun readInt32(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xFF) or
            ((b[at + 1].toInt() and 0xFF) shl 8) or
            ((b[at + 2].toInt() and 0xFF) shl 16) or
            ((b[at + 3].toInt() and 0xFF) shl 24)

    private fun writeInt32(b: ByteArray, at: Int, v: Int) {
        b[at] = v.toByte()
        b[at + 1] = (v shr 8).toByte()
        b[at + 2] = (v shr 16).toByte()
        b[at + 3] = (v shr 24).toByte()
    }

    private fun indexOf(hay: ByteArray, needle: ByteArray, from: Int): Int {
        outer@ for (i in from..hay.size - needle.size) {
            for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}
