package com.kuduy.kubooster.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.preferences.UserPreferencesRepository
import com.kuduy.kubooster.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Onboarding state machine: welcome → what it does → Shizuku optional →
 * safety → device detection (runs the real engines) → done.
 */
class OnboardingViewModel(
    private val container: AppContainer,
    preferences: UserPreferencesRepository,
) : ViewModel() {

    /** null until the first preference read resolves. */
    val onboardingComplete: StateFlow<Boolean?> = preferences.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _detectionRunning = MutableStateFlow(false)
    val detectionRunning: StateFlow<Boolean> = _detectionRunning

    private val _detectionDone = MutableStateFlow(false)
    val detectionDone: StateFlow<Boolean> = _detectionDone

    /** Runs the Device Intelligence + Game Detection engines for real. */
    fun runDeviceDetection() {
        if (_detectionRunning.value) return
        viewModelScope.launch {
            _detectionRunning.value = true
            try {
                container.warmGpuInfo()
                container.deviceRepository.getDeviceCapabilities()
                container.gameDetectionRepository.detectAllVariants()
                _detectionDone.value = true
            } catch (_: Exception) {
                // Detection failure surfaces on the dashboard as explicit
                // "unknown" values; onboarding never blocks on it.
                _detectionDone.value = false
            } finally {
                _detectionRunning.value = false
            }
        }
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            container.userPreferences.setOnboardingComplete()
            onFinished()
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OnboardingViewModel(container, container.userPreferences) as T
        }
    }
}
