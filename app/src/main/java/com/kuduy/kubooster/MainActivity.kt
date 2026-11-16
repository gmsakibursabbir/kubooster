package com.kuduy.kubooster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kuduy.kubooster.core.designsystem.KuBoosterTheme
import com.kuduy.kubooster.ui.KuBoosterRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as KuBoosterApp).container
        setContent {
            KuBoosterTheme {
                KuBoosterRoot(container)
            }
        }
    }
}
