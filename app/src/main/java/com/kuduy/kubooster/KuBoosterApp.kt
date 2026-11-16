package com.kuduy.kubooster

import android.app.Application
import com.kuduy.kubooster.di.AppContainer

/** Holds the app-wide dependency container. No background services, no SDKs. */
class KuBoosterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
