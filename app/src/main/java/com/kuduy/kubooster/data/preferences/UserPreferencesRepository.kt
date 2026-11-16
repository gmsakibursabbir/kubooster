package com.kuduy.kubooster.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuduy.kubooster.domain.model.CustomGraphicsProfile
import com.kuduy.kubooster.domain.model.FpsOption
import com.kuduy.kubooster.domain.model.GraphicsPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * User settings and the custom graphics profile, persisted with DataStore.
 * DataStore here is the only persistence besides the restoration journal.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val onboarded = booleanPreferencesKey("onboarding_complete")
        val fpsOption = stringPreferencesKey("custom_fps_option")
        val prioritizeBattery = booleanPreferencesKey("custom_battery_priority")
        val prioritizeThermal = booleanPreferencesKey("custom_thermal_priority")
        val selectedPreset = stringPreferencesKey("selected_preset")
        val themeFollowSystem = booleanPreferencesKey("theme_follow_system")
    }

    val onboardingComplete: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.onboarded] ?: false }

    suspend fun setOnboardingComplete() {
        context.userPreferencesDataStore.edit { it[Keys.onboarded] = true }
    }

    val customProfile: Flow<CustomGraphicsProfile> = context.userPreferencesDataStore.data.map { prefs ->
        CustomGraphicsProfile(
            preferredFpsOption = prefs[Keys.fpsOption]?.let { name ->
                FpsOption.entries.firstOrNull { it.name == name }
            },
            prioritizeBattery = prefs[Keys.prioritizeBattery] ?: false,
            prioritizeThermal = prefs[Keys.prioritizeThermal] ?: true,
            selectedPreset = prefs[Keys.selectedPreset]?.let { name ->
                GraphicsPreset.entries.firstOrNull { it.name == name }
            } ?: GraphicsPreset.BALANCED,
        )
    }

    suspend fun saveCustomProfile(profile: CustomGraphicsProfile) {
        context.userPreferencesDataStore.edit { prefs ->
            profile.preferredFpsOption?.let { prefs[Keys.fpsOption] = it.name } ?: prefs.remove(Keys.fpsOption)
            prefs[Keys.prioritizeBattery] = profile.prioritizeBattery
            prefs[Keys.prioritizeThermal] = profile.prioritizeThermal
            prefs[Keys.selectedPreset] = profile.selectedPreset.name
        }
    }

    val themeFollowSystem: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[Keys.themeFollowSystem] ?: true }

    suspend fun setThemeFollowSystem(value: Boolean) {
        context.userPreferencesDataStore.edit { it[Keys.themeFollowSystem] = value }
    }

    /** Clears every preference this app wrote. Called from Settings. */
    suspend fun clearAll() {
        context.userPreferencesDataStore.edit { it.clear() }
    }
}
