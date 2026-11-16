package com.kuduy.kubooster.data.gfx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic log for GFX mode: every read, write, verify and Active.sav
 * finding is recorded so the user can copy it and report what went wrong.
 * Kept in memory and mirrored to app-private storage, so it survives the app
 * being killed while the game runs. Contains no personal data — only file
 * paths, sizes, setting names and values.
 */
object GfxLog {

    private const val MAX_LINES = 400
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)
    private var file: File? = null

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    @Synchronized
    fun init(dir: File) {
        if (file != null) return
        file = File(dir, "gfx_log.txt")
        val saved = try { file?.takeIf { it.exists() }?.readLines().orEmpty() } catch (_: Exception) { emptyList() }
        _lines.value = saved.takeLast(MAX_LINES)
    }

    @Synchronized
    fun log(message: String) {
        val line = "${time.format(Date())}  $message"
        _lines.update { (it + line).takeLast(MAX_LINES) }
        persist()
    }

    @Synchronized
    fun clear() {
        _lines.value = emptyList()
        persist()
    }

    fun text(): String = _lines.value.joinToString("\n")

    private fun persist() {
        try { file?.writeText(text()) } catch (_: Exception) { /* logging must never crash */ }
    }
}
