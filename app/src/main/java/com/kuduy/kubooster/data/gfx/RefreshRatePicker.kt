package com.kuduy.kubooster.data.gfx

import kotlin.math.roundToInt

/**
 * Chooses the screen refresh rate to lock for a target frame rate.
 *
 * Frames are only shown evenly when the refresh rate is a whole multiple of
 * the frame rate (60 FPS on 60/120 Hz, 120 FPS on 120 Hz). 90 FPS on a
 * 60/120 Hz panel can't be paced evenly — every other frame is held longer,
 * which reads as stutter even at a high FPS counter.
 */
object RefreshRatePicker {

    /** Panel rates rounded to whole Hz, ascending (e.g. 60.00001 -> 60). */
    fun normalize(rates: List<Float>): List<Int> = rates.map { it.roundToInt() }.filter { it > 0 }.distinct().sorted()

    /** True when [fps] can be shown evenly on some rate of the panel. */
    fun isSmooth(fps: Int, panelHz: List<Int>): Boolean = panelHz.any { it >= fps && it % fps == 0 }

    /**
     * The rate to lock for [fps]: the lowest panel rate that is a whole
     * multiple of it; if none exists, the panel maximum (best available).
     * Null when no lock is needed (fps <= 60 or the panel can't go above 60).
     */
    fun pick(fps: Int, panelHz: List<Int>): Int? {
        if (fps <= 60 || panelHz.isEmpty() || panelHz.max() <= 60) return null
        return panelHz.filter { it >= fps && it % fps == 0 }.minOrNull() ?: panelHz.max()
    }
}
