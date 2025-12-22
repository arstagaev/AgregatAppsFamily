package com.tagaev.trrcrm

import android.app.Application
import com.google.firebase.FirebaseApp
import com.tagaev.trrcrm.di.androidModule
import com.tagaev.trrcrm.di.commonModule
import com.tagaev.trrcrm.push.NotificationHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        startKoin {
            androidContext(this@App)
            modules(commonModule, androidModule)
        }
//        NotificationHelper.ensureNotificationChannel(this)
    }
}
