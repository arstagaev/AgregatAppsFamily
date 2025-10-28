package org.agregatcrm.di

import android.content.Context
import org.agregatcrm.data.AndroidKMMContext
import org.agregatcrm.data.local.KMMContext
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Binds KMMContext to AndroidKMMContext(get()) */
val androidModule = module {
    single<KMMContext> {
        // get() here returns the Android Context because we called androidContext() below
        AndroidKMMContext(
            prefs = androidContext().getSharedPreferences(
                "cmp_prefs",
                Context.MODE_PRIVATE
            )
        )
    }
}
