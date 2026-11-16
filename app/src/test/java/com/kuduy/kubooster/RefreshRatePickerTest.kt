package com.kuduy.kubooster

import com.kuduy.kubooster.data.gfx.RefreshRatePicker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshRatePickerTest {

    private val oneplus8t = RefreshRatePicker.normalize(listOf(60.00001f, 120.00001f))

    @Test
    fun `normalizes reported rates`() {
        assertEquals(listOf(60, 120), oneplus8t)
    }

    @Test
    fun `120 fps locks 120 Hz, 60 fps needs no lock`() {
        assertEquals(120, RefreshRatePicker.pick(120, oneplus8t))
        assertNull(RefreshRatePicker.pick(60, oneplus8t))
    }

    @Test
    fun `90 fps on a 60-120 panel is uneven, falls back to max`() {
        assertFalse(RefreshRatePicker.isSmooth(90, oneplus8t))
        assertEquals(120, RefreshRatePicker.pick(90, oneplus8t))
    }

    @Test
    fun `90 fps uses an exact 90 Hz mode when the panel has one`() {
        val panel = listOf(60, 90, 120)
        assertTrue(RefreshRatePicker.isSmooth(90, panel))
        assertEquals(90, RefreshRatePicker.pick(90, panel))
    }

    @Test
    fun `60 Hz-only panel never locks`() {
        assertNull(RefreshRatePicker.pick(90, listOf(60)))
    }
}
