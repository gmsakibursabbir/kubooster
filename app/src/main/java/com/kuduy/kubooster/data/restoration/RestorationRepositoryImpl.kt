package com.kuduy.kubooster.data.restoration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kuduy.kubooster.domain.model.OptimizationOperationId
import com.kuduy.kubooster.domain.model.RestorationResult
import com.kuduy.kubooster.domain.model.RestorationTransaction
import com.kuduy.kubooster.domain.repository.RestorationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.restorationDataStore: DataStore<Preferences> by preferencesDataStore(name = "restoration")

/**
 * Transaction-based Restoration Manager.
 *
 * Journal entries are stored as JSON in DataStore. Crucially the journal always
 * records the value that existed BEFORE our change — never our own applied
 * value — so restoring repeatedly is idempotent.
 */
class RestorationRepositoryImpl(
    private val context: Context,
    private val restorer: SettingsRestorer,
) : RestorationRepository {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class JournalEntry(
        val operationId: OptimizationOperationId,
        val settingKey: String,
        val originalValue: String?,
        val appliedValue: String,
        val appliedAtEpochMs: Long,
        val verified: Boolean,
        val restorable: Boolean,
    )

    private val journalKey = stringPreferencesKey("restoration_journal")
    private val lastAppliedKey = longPreferencesKey("restoration_last_applied")

    override suspend fun history(): List<RestorationTransaction> =
        readJournal().map { it.toDomain() }

    override suspend fun pendingCount(): Int = readJournal().filter { it.restorable }.size

    override suspend fun restoreOne(operationId: OptimizationOperationId): RestorationResult {
        val journal = readJournal()
        val entry = journal.filter { it.operationId == operationId && it.restorable }
            .maxByOrNull { it.appliedAtEpochMs }
            ?: return RestorationResult(operationId, restored = false, verified = false, message = "No restorable change recorded for this operation")

        val ok = restorer.restore(entry.settingKey, entry.originalValue)
        if (!ok) {
            return RestorationResult(
                operationId,
                restored = false,
                verified = false,
                message = "Restoration failed — the system rejected the write. Try granting settings permission again.",
            )
        }
        // Verify by comparing the live value with the recorded original.
        val live = restorer.readCurrent(entry.settingKey)
        val verified = if (entry.originalValue == null) {
            live == null
        } else {
            live == entry.originalValue
        }
        val updated = journal.filterNot {
            it.operationId == operationId && it.appliedAtEpochMs == entry.appliedAtEpochMs
        }
        writeJournal(updated)
        return RestorationResult(
            operationId,
            restored = true,
            verified = verified,
            message = if (verified) "Restored to ${entry.originalValue ?: "default (unset)"}" else "Restoration applied, but verification read-back did not match",
        )
    }

    override suspend fun restoreAll(): List<RestorationResult> =
        OptimizationOperationId.entries.map { restoreOne(it) }.filter { it.restored || it.message.contains("No restorable") == false }

    /** Records a new transaction, dropping any previous entry for the same key. */
    suspend fun record(
        operationId: OptimizationOperationId,
        settingKey: String,
        originalValue: String?,
        appliedValue: String,
        verified: Boolean,
        restorable: Boolean,
    ) {
        val journal = readJournal()
            .filterNot { it.settingKey == settingKey }
            .plus(
                JournalEntry(
                    operationId, settingKey, originalValue, appliedValue,
                    System.currentTimeMillis(), verified, restorable,
                ),
            )
        writeJournal(journal)
    }

    private suspend fun readJournal(): List<JournalEntry> {
        val prefs = context.restorationDataStore.data.first()
        val raw = prefs[journalKey] ?: return emptyList()
        return runCatching { json.decodeFromString<List<JournalEntry>>(raw) }.getOrDefault(emptyList())
    }

    private suspend fun writeJournal(entries: List<JournalEntry>) {
        context.restorationDataStore.edit { prefs ->
            prefs[journalKey] = json.encodeToString(entries)
            prefs[lastAppliedKey] = System.currentTimeMillis()
        }
    }

    private fun JournalEntry.toDomain() = RestorationTransaction(
        operationId = operationId,
        settingKey = settingKey,
        originalValue = originalValue,
        appliedValue = appliedValue,
        appliedAtEpochMs = appliedAtEpochMs,
        verified = verified,
        restorable = restorable,
    )
}

/**
 * Applies and reverts the concrete settings. Implemented as an interface so the
 * restoration logic is unit-testable without Android.
 */
interface SettingsRestorer {
    fun readCurrent(key: String): String?
    fun restore(key: String, originalValue: String?): Boolean
}
