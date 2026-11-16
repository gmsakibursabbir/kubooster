package com.kuduy.kubooster.feature.graphics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.GraphicsPreset
import com.kuduy.kubooster.domain.model.GraphicsRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GraphicsScreenState(
    val loading: Boolean = true,
    val error: String? = null,
    val device: DeviceCapabilities? = null,
    val recommendations: List<GraphicsRecommendation> = emptyList(),
    val selectedPreset: GraphicsPreset = GraphicsPreset.BALANCED,
    val saved: Boolean = false,
)

class GraphicsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(GraphicsScreenState())
    val state: StateFlow<GraphicsScreenState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, saved = false)
            try {
                val device = container.deviceRepository.getDeviceCapabilities()
                val recommendations = container.graphicsProfileRepository.getRecommendations(device)
                val profile = container.graphicsProfileRepository.getCustomProfile()
                _state.value = GraphicsScreenState(
                    loading = false,
                    device = device,
                    recommendations = recommendations,
                    selectedPreset = profile.selectedPreset,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Graphics recommendations could not be built (${e.javaClass.simpleName}).",
                )
            }
        }
    }

    /** Persists the chosen preset and re-runs the engine for live advice. */
    fun selectPreset(preset: GraphicsPreset) {
        viewModelScope.launch {
            val profile = container.graphicsProfileRepository.getCustomProfile()
            container.graphicsProfileRepository.saveCustomProfile(
                profile.copy(selectedPreset = preset),
            )
            _state.value = _state.value.copy(selectedPreset = preset, saved = true)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GraphicsViewModel(container) as T
        }
    }
}
