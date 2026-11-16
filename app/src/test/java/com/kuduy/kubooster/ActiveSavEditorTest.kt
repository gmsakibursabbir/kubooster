package com.kuduy.kubooster

import com.kuduy.kubooster.data.gfx.ActiveSavEditor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class ActiveSavEditorTest {

    private fun ByteArrayOutputStream.int32(v: Int) {
        write(v); write(v shr 8); write(v shr 16); write(v shr 24)
    }

    private fun ByteArrayOutputStream.fstring(s: String) {
        int32(s.length + 1); write(s.toByteArray(Charsets.ISO_8859_1)); write(0)
    }

    /** GVAS IntProperty bytes, same layout MK-PUBG-Mobile-Tool matches on. */
    private fun ByteArrayOutputStream.intProp(name: String, value: Int) {
        fstring(name); fstring("IntProperty"); int32(4); int32(0); write(0); int32(value)
    }

    private fun sav(vararg props: Pair<String, Int>): ByteArray = ByteArrayOutputStream().apply {
        write("GVAS".toByteArray()); int32(2); int32(517)
        fstring("Active")
        props.forEach { (n, v) -> intProp(n, v) }
        fstring("None"); int32(0)
    }.toByteArray()

    @Test
    fun `reads int properties and fps ones`() {
        val b = sav("ArtQuality" to 2, "FPSLevel" to 6, "BattleFPS" to 6, "LobbyFPS" to 5)
        val all = ActiveSavEditor.readIntProperties(b).associate { it.name to it.value }
        assertEquals(mapOf("ArtQuality" to 2, "FPSLevel" to 6, "BattleFPS" to 6, "LobbyFPS" to 5), all)
        assertEquals(
            setOf("FPSLevel", "BattleFPS", "LobbyFPS"),
            ActiveSavEditor.readFpsProperties(b).map { it.name }.toSet(),
        )
    }

    @Test
    fun `apply sets every fps property and nothing else`() {
        val b = sav("ArtQuality" to 2, "FPSLevel" to 6, "BattleFPS" to 6, "LobbyFPS" to 5, "HubFPS" to 4)
        val out = ActiveSavEditor.applyFpsLevel(b, 8)!!
        assertEquals(b.size, out.size)
        val after = ActiveSavEditor.readIntProperties(out).associate { it.name to it.value }
        assertEquals(mapOf("ArtQuality" to 2, "FPSLevel" to 8, "BattleFPS" to 8, "LobbyFPS" to 8, "HubFPS" to 8), after)
        // Only the value bytes change: equal to a file written with the new levels.
        val expected = sav("ArtQuality" to 2, "FPSLevel" to 8, "BattleFPS" to 8, "LobbyFPS" to 8, "HubFPS" to 8)
        assertArrayEquals(expected, out)
    }

    @Test
    fun `no fps property means no write`() {
        assertNull(ActiveSavEditor.applyFpsLevel(sav("ArtQuality" to 2), 7))
    }

    @Test
    fun `fps to game level`() {
        assertEquals(4, ActiveSavEditor.fpsLevel(30))
        assertEquals(6, ActiveSavEditor.fpsLevel(60))
        assertEquals(7, ActiveSavEditor.fpsLevel(90))
        assertEquals(8, ActiveSavEditor.fpsLevel(120))
    }
}
