package com.kuduy.kubooster.feature.optimization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.data.optimization.OptimizationRepositoryImpl
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.DeviceCapabilities
import com.kuduy.kubooster.domain.model.OptimizationOperationId
import com.kuduy.kubooster.domain.model.OptimizationProposal
import com.kuduy.kubooster.domain.model.OptimizationReport
import com.kuduy.kubooster.domain.model.RestorationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface OptimizationStep {
    data object Loading : OptimizationStep
    data class Review(val proposals: List<OptimizationProposal>) : OptimizationStep
    data object Applying : OptimizationStep
    data class Result(val report: OptimizationReport) : OptimizationStep
    data class Restored(val results: List<RestorationResult>) : OptimizationStep
    data class Error(val message: String) : OptimizationStep
}

data class OptimizationUiState(
    val step: OptimizationStep = OptimizationStep.Loading,
    val selected: Set<OptimizationOperationId> = emptySet(),
)

class OptimizationViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(OptimizationUiState())
    val state: StateFlow<OptimizationUiState> = _state

    init {
        loadProposals()
    }

    fun loadProposals() {
        viewModelScope.launch {
            _state.value = OptimizationUiState(step = OptimizationStep.Loading)
            try {
                val device: DeviceCapabilities = container.deviceRepository.getDeviceCapabilities()
                val proposals = (container.optimizationRepository as OptimizationRepositoryImpl)
                    .buildProposals(device, device.display)
                val supported = proposals.filter { it.supported }
                _state.value = OptimizationUiState(
                    step = OptimizationStep.Review(proposals),
                    selected = supported.map { it.operationId }.toSet(),
                )
            } catch (e: Exception) {
                _state.value = OptimizationUiState(
                    step = OptimizationStep.Error(
                        "Recommendations could not be built (${e.javaClass.simpleName}).",
                    ),
                )
            }
        }
    }

    fun toggle(operationId: OptimizationOperationId) {
        val current = _state.value
        if (current.step is OptimizationStep.Review) {
            _state.value = current.copy(
                selected = if (operationId in current.selected) {
                    current.selected - operationId
                } else {
                    current.selected + operationId
                },
            )
        }
    }

    /** Applies only the user-confirmed, supported proposals. */
    fun apply() {
        val current = _state.value
        val step = current.step as? OptimizationStep.Review ?: return
        viewModelScope.launch {
            _state.value = current.copy(step = OptimizationStep.Applying)
            try {
                val chosen = step.proposals.filter {
                    it.supported && it.operationId in current.selected
                }
                val report = (container.optimizationRepository as OptimizationRepositoryImpl).apply(chosen)
                _state.value = OptimizationUiState(step = OptimizationStep.Result(report))
            } catch (e: Exception) {
                _state.value = OptimizationUiState(
                    step = OptimizationStep.Error("Optimization failed (${e.javaClass.simpleName}). Nothing was changed unless a result said otherwise."),
                )
            }
        }
    }

    fun restoreAll() {
        viewModelScope.launch {
            try {
                val results = container.restorationRepository.restoreAll()
                _state.value = _state.value.copy(step = OptimizationStep.Restored(results))
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    step = OptimizationStep.Error("Restoration failed (${e.javaClass.simpleName})."),
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OptimizationViewModel(container) as T
        }
    }
}
