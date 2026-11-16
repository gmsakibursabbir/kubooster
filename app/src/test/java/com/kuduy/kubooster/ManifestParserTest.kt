package com.kuduy.kubooster

import com.kuduy.kubooster.data.compatibility.ManifestParser
import com.kuduy.kubooster.domain.model.GameVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the real bundled manifest file, so a broken asset fails CI before it
 * ships.
 */
class ManifestParserTest {

    private val parser = ManifestParser()

    private fun loadManifestRaw(): String {
        val candidates = listOf(
            File("../app/src/main/assets/compatibility_manifest.json"),
            File("app/src/main/assets/compatibility_manifest.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("compatibility_manifest.json not found from test working dir")
        return file.readText()
    }

    @Test
    fun `bundled manifest parses`() {
        val manifest = parser.parse(loadManifestRaw())
        assertTrue(manifest.manifestVersion >= 1)
        assertEquals(5, manifest.entries.size)
    }

    @Test
    fun `every entry targets the maintained version`() {
        val manifest = parser.parse(loadManifestRaw())
        manifest.entries.forEach { entry ->
            assertEquals("4.7.0", entry.versionName)
            assertNotNull(entry.packageName)
        }
    }

    @Test
    fun `variant package names match the registry`() {
        val manifest = parser.parse(loadManifestRaw())
        GameVariant.entries.forEach { variant ->
            val entry = manifest.entries.firstOrNull { it.gameVariant == variant.name }
            assertNotNull("manifest missing variant ${variant.name}", entry)
            assertEquals(variant.packageName, entry!!.packageName)
        }
    }

    @Test
    fun `no entry claims verification without evidence`() {
        val manifest = parser.parse(loadManifestRaw())
        manifest.entries.forEach { entry ->
            val claimsVerified = entry.verificationStatus.equals("verified", ignoreCase = true)
            if (claimsVerified) {
                assertTrue("verified entry must carry evidence", !entry.evidence.isNullOrBlank())
                assertTrue("verified entry must list verified FPS profiles", entry.verifiedFpsProfiles.isNotEmpty())
            }
        }
    }

    @Test
    fun `targetEntry lookup works for each variant`() {
        val manifest = parser.parse(loadManifestRaw())
        GameVariant.entries.forEach { variant ->
            assertNotNull(parser.targetEntry(manifest, variant))
        }
    }
}
