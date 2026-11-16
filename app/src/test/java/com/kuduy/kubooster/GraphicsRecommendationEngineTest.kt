package com.kuduy.kubooster

import com.kuduy.kubooster.data.compatibility.GraphicsRecommendationEngine
import com.kuduy.kubooster.domain.model.GraphicsPreset
import com.kuduy.kubooster.domain.model.RamTier
import com.kuduy.kubooster.domain.model.ThermalStatus
import com.kuduy.kubooster.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphicsRecommendationEngineTest {

    private val engine = GraphicsRecommendationEngine()

    @Test
    fun `returns three presets always`() {
        val recs = engine.recommend(Fixtures.device(), Fixtures.display())
        assertEquals(
            listOf(
                GraphicsPreset.SMOOTH_PERFORMANCE,
                GraphicsPreset.BALANCED,
                GraphicsPreset.HIGH_QUALITY,
            ),
            recs.map { it.preset },
        )
    }

    @Test
    fun `entry tier device - smooth preset recommends lower demand`() {
        val device = Fixtures.device(ramBytes = 4L * RamTier.GB)
        val smooth = engine.recommend(device, device.display).first { it.preset == GraphicsPreset.SMOOTH_PERFORMANCE }
        val graphicsAdvice = smooth.settings.first { it.setting == "Graphics" }
        assertEquals("Smooth", graphicsAdvice.value)
        assertTrue(smooth.rationale.isNotEmpty())
    }

    @Test
    fun `flagship device - quality preset allows HDR`() {
        val device = Fixtures.device(ramBytes = 12L * RamTier.GB)
        val quality = engine.recommend(device, device.display).first { it.preset == GraphicsPreset.HIGH_QUALITY }
        val graphicsAdvice = quality.settings.first { it.setting == "Graphics" }
        assertEquals("HDR", graphicsAdvice.value)
    }

    @Test
    fun `mid tier device - quality preset does not recommend HDR`() {
        val device = Fixtures.device(ramBytes = 6L * RamTier.GB)
        val quality = engine.recommend(device, device.display).first { it.preset == GraphicsPreset.HIGH_QUALITY }
        val graphicsAdvice = quality.settings.first { it.setting == "Graphics" }
        assertEquals("HD", graphicsAdvice.value)
    }

    @Test
    fun `hot device adds thermal rationale`() {
        val device = Fixtures.device(thermal = ThermalStatus.SEVERE)
        val smooth = engine.recommend(device, device.display).first { it.preset == GraphicsPreset.SMOOTH_PERFORMANCE }
        assertTrue(
            "thermal guidance must mention heat",
            smooth.rationale.any { it.lowercase().contains("thermal") || it.lowercase().contains("heat") },
        )
    }
}
