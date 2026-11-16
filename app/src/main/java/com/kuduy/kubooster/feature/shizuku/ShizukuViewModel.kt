package com.kuduy.kubooster.feature.shizuku

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.domain.model.OptimizationOperationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ShizukuState(
    val installed: Boolean = false,
    val running: Boolean = false,
    val authorized: Boolean = false,
    val writeSecureGranted: Boolean = false,
    /** Result line of the last privileged operation, or null. */
    val lastOperationMessage: String? = null,
    val working: Boolean = false,
)

class ShizukuViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state

    init {
        refresh()
    }

    fun refresh() {
        val repo = container.shizukuRepository
        _state.value = ShizukuState(
            installed = repo.isInstalled(),
            running = repo.isServiceAvailable(),
            authorized = repo.hasPermission(),
            writeSecureGranted = container.settingsManager.canWriteSecureSettings(),
            lastOperationMessage = _state.value.lastOperationMessage,
        )
    }

    /** Opens the Shizuku permission dialog for this app. */
    fun requestPermission() {
        container.shizukuRepository.requestPermission()
    }

    /**
     * The single allowlisted privileged operation: `pm grant <self>
     * WRITE_SECURE_SETTINGS`. Verified by reading the resulting capability.
     */
    fun grantWriteSecureSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(working = true)
            try {
                val exit = container.shizukuRepository.grantSelfWriteSecureSettings()
                val granted = container.settingsManager.canWriteSecureSettings()
                val message = when {
                    granted -> "WRITE_SECURE_SETTINGS granted. Animation speed and battery saver operations are now available."
                    exit == null -> "Operation failed — Shizuku did not execute the command."
                    else -> "Operation ran (exit $exit) but the permission is not active yet. Try again after restarting Shizuku."
                }
                if (granted) {
                    // The optimization screen probes WRITE_SECURE_SETTINGS live,
                    // so no cached flag needs refreshing here.
                }
                _state.value = _state.value.copy(
                    working = false,
                    lastOperationMessage = message,
                    writeSecureGranted = granted,
                    authorized = container.shizukuRepository.hasPermission(),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    working = false,
                    lastOperationMessage = "Operation failed (${e.javaClass.simpleName}).",
                )
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShizukuViewModel(container) as T
        }
    }
}
