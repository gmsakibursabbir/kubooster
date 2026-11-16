package com.kuduy.kubooster.data.compatibility

import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.DisplayCapabilities
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.FpsProfile
import com.kuduy.kubooster.domain.model.FpsProfileState
import kotlin.math.roundToInt

/**
 * FPS Profile Engine. Evaluates each FPS option against:
 *  1. the display's advertised refresh rates, and
 *  2. the compatibility manifest's verified game options.
 *
 * The engine never claims VERIFIED_AVAILABLE unless the manifest documents it,
 * and never conflates display Hz with rendered game FPS.
 */
class FpsProfileEngine {

    fun evaluate(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
        verifiedFps: List<FpsOption>,
    ): List<FpsProfile> = FpsOption.entries.map { option -> evaluateOne(option, display, verifiedFps) }

    private fun evaluateOne(
        option: FpsOption,
        display: DisplayCapabilities,
        verifiedFps: List<FpsOption>,
    ): FpsProfile {
        val displaySupports = display.supportedRefreshRatesHz.any {
            (it * 100).roundToInt() >= option.hz * 100
        }
        return when {
            verifiedFps.contains(option) -> FpsProfile(
                option = option,
                state = FpsProfileState.VERIFIED_AVAILABLE,
                explanation = "Documented as verified for this game version in the bundled compatibility manifest.",
            )
            displaySupports -> FpsProfile(
                option = option,
                state = FpsProfileState.DISPLAY_COMPATIBLE_GAME_UNVERIFIED,
                explanation = "Your display advertises a ${display.maxRefreshRateHz.roundToInt()} Hz mode, so this option is " +
                    "technically displayable, but game support for it is unverified. The game renders FPS " +
                    "independently of the display refresh rate.",
            )
            else -> FpsProfile(
                option = option,
                state = FpsProfileState.NOT_SUPPORTED,
                explanation = when {
                    display.supportedRefreshRatesHz.isEmpty() ->
                        "This display does not advertise its supported refresh rates, so this option cannot be confirmed."
                    else ->
                        "Your display does not advertise a refresh rate at or above ${option.hz} Hz " +
                            "(highest: ${display.maxRefreshRateHz.roundToInt()} Hz)."
                },
            )
        }
    }

    /**
     * Highest option whose state is better than NOT_SUPPORTED — used by the
     * dashboard for the "recommended FPS profile" tile.
     */
    fun recommendedProfile(
        device: DeviceCapabilities,
        display: DisplayCapabilities,
        verifiedFps: List<FpsOption>,
    ): FpsProfile? = evaluate(device, display, verifiedFps)
        .filter { it.state != FpsProfileState.NOT_SUPPORTED }
        .maxByOrNull { it.option.hz }
}
