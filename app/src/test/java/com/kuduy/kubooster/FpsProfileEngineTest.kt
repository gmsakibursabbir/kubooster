package com.kuduy.kubooster

import com.kuduy.kubooster.data.compatibility.FpsProfileEngine
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.FpsProfileState
import com.kuduy.kubooster.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FpsProfileEngineTest {

    private val engine = FpsProfileEngine()

    @Test
    fun `60hz display - 90 and 120 are not supported`() {
        val device = Fixtures.device(display = Fixtures.display(rates = listOf(60f), current = 60f))
        val profiles = engine.evaluate(device, device.display, verifiedFps = emptyList())
        val byHz = profiles.associateBy { it.option.hz }

        assertEquals(FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED, byHz.getValue(60).state)
        assertEquals(FpsProfileState.NOT_SUPPORTED, byHz.getValue(90).state)
        assertEquals(FpsProfileState.NOT_SUPPORTED, byHz.getValue(120).state)
    }

    @Test
    fun `120hz display - 120 is display compatible but unverified without manifest evidence`() {
        val device = Fixtures.device(display = Fixtures.display(rates = listOf(60f, 90f, 120f), current = 120f))
        val profiles = engine.evaluate(device, device.display, verifiedFps = emptyList())
        val byHz = profiles.associateBy { it.option.hz }

        assertEquals(FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED, byHz.getValue(120).state)
        assertTrue(
            "explanation must not claim the game renders at this rate",
            !byHz.getValue(120).explanation.contains("will render", ignoreCase = true),
        )
    }

    @Test
    fun `manifest verification promotes the state`() {
        val device = Fixtures.device(display = Fixtures.display(rates = listOf(60f, 90f, 120f)))
        val profiles = engine.evaluate(device, device.display, verifiedFps = listOf(FpsOption.FPS_90))
        val byHz = profiles.associateBy { it.option.hz }

        assertEquals(FpsProfileState.VERIFIED_AVAILABLE, byHz.getValue(90).state)
        assertEquals(FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED, byHz.getValue(120).state)
    }

    @Test
    fun `display with no advertised rates - everything unconfirmed except verified`() {
        val device = Fixtures.device(display = Fixtures.display(rates = emptyList(), current = 0f))
        val profiles = engine.evaluate(device, device.display, verifiedFps = emptyList())
        assertTrue(profiles.all { it.state == FpsProfileState.NOT_SUPPORTED })
    }

    @Test
    fun `recommended profile is the highest non-unsupported`() {
        val device = Fixtures.device(display = Fixtures.display(rates = listOf(60f, 90f, 120f)))
        val recommended = engine.recommendedProfile(device, device.display, emptyList())
        assertEquals(FpsOption.FPS_120, recommended?.option)
        assertEquals(FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED, recommended?.state)
    }
}
