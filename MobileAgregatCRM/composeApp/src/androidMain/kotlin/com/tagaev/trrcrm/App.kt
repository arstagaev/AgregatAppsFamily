package com.tagaev.trrcrm

import android.app.Application
import com.tagaev.trrcrm.di.androidModule
import com.tagaev.trrcrm.di.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(commonModule, androidModule)
        }
    }
}
