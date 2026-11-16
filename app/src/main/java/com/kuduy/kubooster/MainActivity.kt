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
            // Theme: explicit in-app toggle ("dark"/"light") wins; otherwise
            // the stored follow-system preference decides.
            val followSystem by container.userPreferences.themeFollowSystem
                .collectAsState(initial = true)
            val themeDark by container.userPreferences.themeDark
                .collectAsState(initial = null)
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeDark) {
                "dark" -> true
                "light" -> false
                else -> if (followSystem) systemDark else false
            }
            KuBoosterTheme(darkTheme = dark) {
                KuBoosterRoot(container)
            }
        }
    }
}
