package com.kuduy.kubooster.data.gfx

/**
 * PUBG Mobile stores the `+CVars=` values in `UserCustom.ini` as a hex string
 * of each byte XOR-ed with 0x79.
 *
 * The scheme is verified against the published reference corpus
 * (vsec7/PUBGM-UserCustom-En-Decoder):
 *   encode("r.PUBGQualityLevel=2") == "0B57292C3B3E280C1815100D00351C0F1C15444B"
 *   decode("0B57292C3B3E280C1815100D00351C0F1C15444B") == "r.PUBGQualityLevel=2"
 *
 * Text is treated as ISO-8859-1 so every byte round-trips 1:1; the config is
 * ASCII in practice, and non-ASCII bytes (e.g. UTF-8 sequences elsewhere in
 * the file) pass through unchanged.
 */
object UserCustomCodec {

    private const val XOR_KEY = 0x79
    private val HEX = "0123456789ABCDEF"

    fun encode(text: String): String = buildString(text.length * 2) {
        for (b in text.toByteArray(Charsets.ISO_8859_1)) {
            val v = (b.toInt() xor XOR_KEY) and 0xFF
            append(HEX[(v shr 4) and 0xF])
            append(HEX[v and 0xF])
        }
    }

    fun decode(hex: String): String? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val byte = hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: return null
            out[i] = (byte xor XOR_KEY).toByte()
        }
        return String(out, Charsets.ISO_8859_1)
    }

    /** True when [hex] is a plausible encoded value (even-length hex string). */
    fun isEncoded(hex: String): Boolean =
        hex.length % 2 == 0 && hex.isNotEmpty() && hex.all {
            it.isDigit() || it in 'A'..'F' || it in 'a'..'f'
        }
}
