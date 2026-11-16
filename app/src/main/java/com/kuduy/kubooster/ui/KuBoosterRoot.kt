package com.kuduy.kubooster.ui

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.kuduy.kubooster.feature.dashboard.UpdateBanner
import com.kuduy.kubooster.feature.update.UpdateViewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kuduy.kubooster.di.AppContainer
import com.kuduy.kubooster.feature.gfx.GfxScreen
import com.kuduy.kubooster.feature.gfx.GfxViewModel
import kotlinx.coroutines.launch

/**
 * Root: the app is a single GFX screen — no onboarding, no bottom bar, no
 * other destinations.
 */
@Composable
fun KuBoosterRoot(container: AppContainer) {
    Scaffold { padding ->
        val vm: GfxViewModel = viewModel(factory = GfxViewModel.factory(container))
        LaunchedEffect(Unit) { vm.refresh() }

        // In-app updates (GitHub releases): checked on launch, banner on top of GFX.
        val updateVm: UpdateViewModel = viewModel(factory = UpdateViewModel.factory(container, autoCheckOnLaunch = true))
        val updateState by updateVm.state.collectAsState()
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    updateVm.onDownloadComplete(
                        intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1,
                    )
                }
            }
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            onDispose { context.unregisterReceiver(receiver) }
        }
        val gfxState by vm.state.collectAsState()
        val scope = rememberCoroutineScope()
        val themeDarkOverride by container.userPreferences.themeDark.collectAsState(initial = null)
        val systemDark = isSystemInDarkTheme()
        val isDark = when (themeDarkOverride) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            GfxScreen(
                state = gfxState,
                isDark = isDark,
                onToggleTheme = {
                    scope.launch {
                        container.userPreferences.setThemeDark(if (isDark) "light" else "dark")
                    }
                },
                onRefresh = { vm.refresh() },
                onSelectTarget = { vm.selectTarget(it) },
                onSelectFps = { vm.selectFps(it) },
                onSelectPreset = { vm.selectPreset(it) },
                onSelectResolution = { vm.selectResolution(it) },
                onSetAntiAliasing = { vm.setAntiAliasing(it) },
                onRequestShizuku = { container.shizukuRepository.requestPermission() },
                onApply = { vm.apply() },
                onRemoveGfx = { vm.removeGfx() },
                onDiscardBackup = { vm.discardSaveBackup() },
                onDismissOverlay = { vm.dismissApplyPhase() },
                onLaunchGame = { vm.launchGameFrom(context) },
                topContent = {
                    UpdateBanner(
                        update = updateState,
                        onDownload = { updateVm.download() },
                        onInstall = { updateVm.install() },
                        onOpenRelease = { updateVm.openReleasePage() },
                    )
                },
            )
        }
    }
}
