package com.kuduy.kubooster.feature.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiagnosticsState(
    val loading: Boolean = true,
    val error: String? = null,
    val device: DeviceCapabilities? = null,
)

class DiagnosticsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                _state.value = DiagnosticsState(
                    loading = false,
                    device = container.deviceRepository.getDeviceCapabilities(),
                )
            } catch (e: Exception) {
                _state.value = DiagnosticsState(
                    loading = false,
                    error = "Diagnostics could not be read (${e.javaClass.simpleName}).",
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DiagnosticsViewModel(container) as T
        }
    }
}
