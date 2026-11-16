package com.kuduy.kubooster.data.device

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20

/**
 * Reads the GPU renderer/vendor and the highest client-supported OpenGL ES
 * version through EGL + GLES20 — public, documented APIs only.
 *
 * Technique: create an off-screen pbuffer surface, make it current on a GLES2
 * context, and query glGetString. Every failure degrades to null, which the UI
 * renders as "Unknown" — we never substitute a guessed GPU name.
 */
object EglInfo {

    data class GlesVersion(val major: Int, val minor: Int)

    data class Result(
        val renderer: String?,
        val vendor: String?,
        val glesVersion: GlesVersion?,
    )

    private val lock = Any()

    @Volatile
    private var cached: Result? = null

    fun renderer(): String? = query()?.renderer
    fun vendor(): String? = query()?.vendor
    fun glesVersion(): GlesVersion? = query()?.glesVersion

    private fun query(): Result? {
        cached?.let { return it }
        synchronized(lock) {
            cached?.let { return it }
            cached = runCatching { performQuery() }.getOrNull()
            return cached
        }
    }

    private fun performQuery(): Result {
        val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display != EGL14.EGL_NO_DISPLAY) { "no EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize failed" }
        return try {
            val configAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(
                EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) &&
                    numConfigs[0] > 0,
            ) { "no EGL config" }
            val config: EGLConfig = configs[0] ?: error("null EGL config")

            val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
            val surface: EGLSurface =
                EGL14.eglCreatePbufferSurface(display, config, pbufferAttribs, 0)
            check(surface != EGL14.EGL_NO_SURFACE) { "no pbuffer surface" }

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE,
            )
            val context = EGL14.eglCreateContext(
                display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0,
            )
            check(context != EGL14.EGL_NO_CONTEXT) { "no EGL context" }

            try {
                check(EGL14.eglMakeCurrent(display, surface, surface, context)) { "makeCurrent failed" }
                try {
                    val renderer = GLES20.glGetString(GLES20.GL_RENDERER)?.takeIf { it.isNotBlank() }
                    val vendor = GLES20.glGetString(GLES20.GL_VENDOR)?.takeIf { it.isNotBlank() }
                    // The client API version string, e.g. "OpenGL ES 3.2 V@0615.28…".
                    val versionString = GLES20.glGetString(GLES20.GL_VERSION).orEmpty()
                    val glesVersion = parseGlesVersion(versionString)
                    // Try a 3.x context to learn the highest supported client version.
                    val highest = if ((glesVersion?.major ?: 2) >= 3) {
                        tryProbeHigherContext(display, config) ?: glesVersion
                    } else {
                        glesVersion
                    }
                    Result(renderer = renderer, vendor = vendor, glesVersion = highest)
                } finally {
                    EGL14.eglMakeCurrent(
                        display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT,
                    )
                }
            } finally {
                EGL14.eglDestroyContext(display, context)
                EGL14.eglDestroySurface(display, surface)
            }
        } finally {
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(display)
        }
    }

    /** Attempts a 3.0 context to report the highest available GLES version. */
    private fun tryProbeHigherContext(display: EGLDisplay, config: EGLConfig): GlesVersion? {
        val context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(0x30CA /* EGL_CONTEXT_CLIENT_VERSION */, 3, EGL14.EGL_NONE), 0,
        )
        if (context == EGL14.EGL_NO_CONTEXT) return null
        return try {
            val surface = EGL14.eglCreatePbufferSurface(
                display, config, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0,
            )
            if (surface == EGL14.EGL_NO_SURFACE) return null
            try {
                if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return null
                parseGlesVersion(GLES20.glGetString(GLES20.GL_VERSION).orEmpty())
            } finally {
                EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT,
                )
                EGL14.eglDestroySurface(display, surface)
            }
        } finally {
            EGL14.eglDestroyContext(display, context)
        }
    }

    private fun parseGlesVersion(query: String): GlesVersion? {
        val regex = Regex("""OpenGL[_ ]ES[ _]?(\d+)\.(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(query) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        return GlesVersion(major, minor)
    }
}
