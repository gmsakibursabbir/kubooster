package com.kuduy.kubooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kuduy.kubooster.core.designsystem.KuBoosterTheme
import com.kuduy.kubooster.ui.KuBoosterRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KuBoosterApp).container
        setContent {
            // Bug fix: the Settings toggle previously had no effect because the
            // stored preference was never consulted here.
            val followSystem by container.userPreferences.themeFollowSystem
                .collectAsState(initial = true)
            KuBoosterTheme(
                darkTheme = if (followSystem) isSystemInDarkTheme() else false,
            ) {
                KuBoosterRoot(container)
            }
        }
    }
}
