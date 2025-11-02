package com.tagaev.mobileagregatcrm

import android.app.Application
import com.tagaev.mobileagregatcrm.di.androidModule
import com.tagaev.mobileagregatcrm.di.commonModule
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
