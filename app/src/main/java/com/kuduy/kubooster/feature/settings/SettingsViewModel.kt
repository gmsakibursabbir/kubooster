package com.kuduy.kubooster.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kuduy.kubooster.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val themeFollowSystem: Boolean = true,
    val cleared: Boolean = false,
)

class SettingsViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    /** Count of restorable changes, read live from the restoration journal. */
    val pendingRestoreCount: StateFlow<Int> = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            container.userPreferences.themeFollowSystem.collect { follow ->
                _state.value = _state.value.copy(themeFollowSystem = follow)
            }
        }
        viewModelScope.launch {
            val count = container.restorationRepository.pendingCount()
            (pendingRestoreCount as MutableStateFlow<Int>).value = count
        }
    }

    /** Re-reads the journal so the count is current whenever the screen is shown. */
    fun refreshCount() {
        viewModelScope.launch {
            val count = container.restorationRepository.pendingCount()
            (pendingRestoreCount as MutableStateFlow<Int>).value = count
        }
    }

    fun setThemeFollowSystem(value: Boolean) {
        viewModelScope.launch {
            container.userPreferences.setThemeFollowSystem(value)
        }
    }

    /** Clears local preferences (custom profile, flags). Restoration journal is kept — it exists to protect the user's system state. */
    fun clearPreferences() {
        viewModelScope.launch {
            container.userPreferences.clearAll()
            _state.value = _state.value.copy(cleared = true)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container) as T
        }
    }
}
