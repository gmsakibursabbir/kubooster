package com.kuduy.kubooster

import com.kuduy.kubooster.data.gfx.GfxConfigEditor
import com.kuduy.kubooster.data.gfx.UserCustomCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codec is verified against the published reference corpus
 * (vsec7/PUBGM-UserCustom-En-Decoder). If these pairs break, the engine
 * writes garbage — they are the most important tests in the suite.
 */
class UserCustomCodecTest {

    @Test
    fun `decode known reference pairs`() {
        assertEquals(
            "r.PUBGQualityLevel=2",
            UserCustomCodec.decode("0B57292C3B3E280C1815100D00351C0F1C15444B"),
        )
        assertEquals(
            "r.UserQualitySetting=2",
            UserCustomCodec.decode("0B572C0A1C0B280C1815100D002A1C0D0D10171E444B"),
        )
        assertEquals(
            "r.PUBGLDR=1",
            UserCustomCodec.decode("0B57292C3B3E353D2B4448"),
        )
        assertEquals(
            "r.ShadowQuality=2",
            UserCustomCodec.decode("0B572A11181D160E280C1815100D00444B"),
        )
        assertEquals(
            "r.MobileContentScaleFactor=0.8",
            UserCustomCodec.decode("0B5734161B10151C3A16170D1C170D2A1A18151C3F181A0D160B44495741"),
        )
        assertEquals(
            "r.StaticMeshLODDistanceScale=1.0",
            UserCustomCodec.decode("0B572A0D180D101A341C0A1135363D3D100A0D18171A1C2A1A18151C44485749"),
        )
    }

    @Test
    fun `encode matches reference corpus`() {
        assertEquals(
            "0B57292C3B3E280C1815100D00351C0F1C15444B",
            UserCustomCodec.encode("r.PUBGQualityLevel=2"),
        )
        assertEquals(
            "0B572A11181D160E280C1815100D00444B",
            UserCustomCodec.encode("r.ShadowQuality=2"),
        )
    }

    @Test
    fun `roundtrip is byte exact`() {
        val sample = "r.PUBGDeviceFPSLow=90"
        assertEquals(sample, UserCustomCodec.decode(UserCustomCodec.encode(sample)))
    }

    @Test
    fun `fps value encoding roundtrip`() {
        val entry = "r.PUBGDeviceFPSHDR=120"
        assertEquals(entry, UserCustomCodec.decode(UserCustomCodec.encode(entry)))
    }

    @Test
    fun `malformed input is rejected`() {
        assertNull(UserCustomCodec.decode(""))
        assertNull(UserCustomCodec.decode("0B5"))
        assertNull(UserCustomCodec.decode("not-hex!"))
    }

    @Test
    fun `isEncoded distinguishes hex from plain text`() {
        assertTrue(UserCustomCodec.isEncoded("0B574448"))
        assertFalse(UserCustomCodec.isEncoded("r.PUBGDeviceFPSLow=90"))
        assertFalse(UserCustomCodec.isEncoded(""))
    }
}

class GfxConfigEditorTest {

    private fun encoded(entry: String) = "+CVars=" + UserCustomCodec.encode(entry)

    /** Synthetic file shaped like a real one: sections + encoded cvars. */
    private fun sampleContent(): String = listOf(
        "[UserCustom DeviceProfile]",
        encoded("r.UserQualitySetting=2"),
        encoded("r.PUBGMaxSupportQualityLevel=2"),
        encoded("r.PUBGDeviceFPSLow=60"),
        encoded("r.PUBGDeviceFPSMid=60"),
        encoded("r.PUBGDeviceFPSHigh=60"),
        encoded("r.PUBGDeviceFPSHDR=60"),
        "",
        "[FansSwitcher]",
        encoded("r.PUBGDeviceFPSUltraHDR=60"),
    ).joinToString("\n")

    @Test
    fun `reads existing fps values`() {
        val values = GfxConfigEditor.readFpsValues(sampleContent())
        assertEquals("60", values["r.PUBGDeviceFPSLow"])
        assertEquals("60", values["r.PUBGDeviceFPSUltraHDR"])
        assertEquals(5, values.size)
    }

    @Test
    fun `replaces existing keys in place and leaves others untouched`() {
        val edited = GfxConfigEditor.applyFps(sampleContent(), 90)
        val values = GfxConfigEditor.readFpsValues(edited)
        GfxConfigEditor.MANAGED_FPS_KEYS.forEach { key ->
            assertEquals("90", values[key])
        }
        // Non-managed keys survive untouched:
        assertEquals("2", GfxConfigEditor.readCvars(edited)["r.UserQualitySetting"])
        assertEquals("2", GfxConfigEditor.readCvars(edited)["r.PUBGMaxSupportQualityLevel"])
        // Section headers untouched:
        assertTrue(edited.contains("[UserCustom DeviceProfile]"))
        assertTrue(edited.contains("[FansSwitcher]"))
    }

    @Test
    fun `appends missing keys`() {
        val content = "[UserCustom DeviceProfile]\n" + encoded("r.UserQualitySetting=1")
        val edited = GfxConfigEditor.applyFps(content, 120)
        val values = GfxConfigEditor.readFpsValues(edited)
        GfxConfigEditor.MANAGED_FPS_KEYS.forEach { key ->
            assertEquals("120", values[key])
        }
    }

    @Test
    fun `empty file gets a device profile section with all fps keys`() {
        val edited = GfxConfigEditor.applyFps("", 60)
        // The section must be created — bare +CVars= lines outside any
        // section are ignored by the game's parser.
        assertTrue(edited.contains("[UserCustom DeviceProfile]"))
        assertEquals(
            GfxConfigEditor.MANAGED_FPS_KEYS.size,
            GfxConfigEditor.readFpsValues(edited).size,
        )
    }

    @Test
    fun `missing keys are inserted into the device profile section`() {
        val content = listOf(
            "[UserCustom DeviceProfile]",
            encoded("r.UserQualitySetting=1"),
            "",
            "[FansSwitcher]",
            encoded("r.PUBGDeviceFPSUltraHDR=60"),
        ).joinToString("\n")
        val edited = GfxConfigEditor.applyFps(content, 120)
        val lines = edited.lines()
        val profileIndex = lines.indexOfFirst { it == "[UserCustom DeviceProfile]" }
        val fansIndex = lines.indexOfFirst { it == "[FansSwitcher]" }
        // Every new FPS key must land between the two section headers,
        // not at the end of the file where [FansSwitcher] lives.
        GfxConfigEditor.MANAGED_FPS_KEYS.filter { it != "r.PUBGDeviceFPSUltraHDR" }
            .forEach { key ->
                val lineIndex = lines.indexOfFirst { it.endsWith(UserCustomCodec.encode("$key=120")) }
                assertTrue("missing line for $key", lineIndex >= 0)
                assertTrue(
                    "$key must be inside [UserCustom DeviceProfile]",
                    lineIndex in (profileIndex + 1) until fansIndex,
                )
            }
    }

    @Test
    fun `file without a profile section gets one created`() {
        val content = listOf(
            "[SomeOtherSection]",
            encoded("r.UserQualitySetting=1"),
        ).joinToString("\n")
        val edited = GfxConfigEditor.applyFps(content, 90)
        assertTrue(edited.contains("[UserCustom DeviceProfile]"))
        assertEquals(
            GfxConfigEditor.MANAGED_FPS_KEYS.size,
            GfxConfigEditor.readFpsValues(edited).size,
        )
        // The original section survives untouched.
        assertTrue(edited.contains("[SomeOtherSection]"))
    }

    @Test
    fun `crlf line endings are preserved`() {
        val crlf = sampleContent().replace("\n", "\r\n")
        val edited = GfxConfigEditor.applyFps(crlf, 90)
        // Every LF must be part of a CRLF pair — no bare LF may remain.
        // (A blank CRLF line is "\r\n\r\n", which legitimately contains the
        // substring "\n\r\n"; hence the lookbehind check, not plain contains.)
        assertTrue(edited.contains("\r\n"))
        assertFalse(Regex("(^|[^\r])\n").containsMatchIn(edited))
        assertEquals(GfxConfigEditor.MANAGED_FPS_KEYS.size, GfxConfigEditor.readFpsValues(edited).size)
    }

    @Test
    fun `lines without cvars are preserved byte for byte`() {
        val content = sampleContent()
        val nonCvarLines = content.lineSequence()
            .filter { !it.trimStart().startsWith("+CVars=") }
            .toList()
        val edited = GfxConfigEditor.applyFps(content, 90)
        val editedNonCvar = edited.lineSequence()
            .filter { !it.trimStart().startsWith("+CVars=") }
            .toList()
        assertEquals(nonCvarLines, editedNonCvar)
    }

    @Test
    fun `unrealistic fps is rejected`() {
        try {
            GfxConfigEditor.applyFps("x", 999)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // pass
        }
    }

    @Test
    fun `applyCvars handles fps and graphics keys together`() {
        val combined = GfxConfigEditor.fpsEntries(90) +
            GfxConfigEditor.GRAPHICS_PRESETS.getValue("smooth")
        val edited = GfxConfigEditor.applyCvars(sampleContent(), combined)
        val cvars = GfxConfigEditor.readCvars(edited)
        GfxConfigEditor.MANAGED_FPS_KEYS.forEach { key ->
            assertEquals("90", cvars[key])
        }
        assertEquals("0", cvars["r.UserQualitySetting"])
        assertEquals("1", cvars["r.MobileSimpleShader"])
        assertEquals("0.7", cvars["r.MobileContentScaleFactor"])
        assertEquals("0", cvars["r.PUBGMaxSupportQualityLevel"])
    }

    @Test
    fun `every graphics preset round-trips through encode and decode`() {
        GfxConfigEditor.GRAPHICS_PRESETS.forEach { (name, values) ->
            val edited = GfxConfigEditor.applyCvars("", values)
            val read = GfxConfigEditor.readCvars(edited)
            values.forEach { (key, value) ->
                assertEquals("$name/$key", value, read[key])
            }
        }
    }

    @Test
    fun `super smooth is the lightest preset`() {
        val superSmooth = GfxConfigEditor.GRAPHICS_PRESETS.getValue("supersmooth")
        val smooth = GfxConfigEditor.GRAPHICS_PRESETS.getValue("smooth")
        assertTrue(
            "Super Smooth must use a lower resolution scale than Smooth",
            superSmooth.getValue("r.MobileContentScaleFactor").toDouble() <
                smooth.getValue("r.MobileContentScaleFactor").toDouble(),
        )
        assertEquals("0", superSmooth.getValue("r.ShadowQuality"))
        assertEquals("1", superSmooth.getValue("r.MobileSimpleShader"))
    }

    @Test
    fun `resolution entries map to scale factors`() {
        assertTrue(GfxConfigEditor.resolutionEntries("default").isEmpty())
        assertEquals(
            "0.7",
            GfxConfigEditor.resolutionEntries("720p")["r.MobileContentScaleFactor"],
        )
        assertEquals(
            "1.0",
            GfxConfigEditor.resolutionEntries("1080p")["r.MobileContentScaleFactor"],
        )
        // Resolution overrides the preset's own scale value when merged last.
        val combined = GfxConfigEditor.GRAPHICS_PRESETS.getValue("hd") +
            GfxConfigEditor.resolutionEntries("720p")
        val edited = GfxConfigEditor.applyCvars(sampleContent(), combined)
        assertEquals("0.7", GfxConfigEditor.readCvars(edited)["r.MobileContentScaleFactor"])
    }

    @Test
    fun `blank keys or values are rejected`() {
        try {
            GfxConfigEditor.applyCvars("x", mapOf(" " to "1"))
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // pass
        }
    }
}
