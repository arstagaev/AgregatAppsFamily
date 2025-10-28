package org.agregatcrm

import android.app.Application
import org.agregatcrm.data.local.createShared
import org.agregatcrm.di.androidModule
import org.agregatcrm.di.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(applicationContext)           // makes the Android Context available to get()
            modules(commonModule, androidModule) // load your shared + Android modules
        }
        val store = AndroidRequestPrefsStore(this)
        createShared(store)
    }
}