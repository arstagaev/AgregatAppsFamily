package org.agregatcrm.di

import org.agregatcrm.data.local.IosKMMContext
import org.agregatcrm.data.local.KMMContext
import org.koin.dsl.module

/** Binds KMMContext to IosKMMContext */
val iosModule = module {
    single<KMMContext> { IosKMMContext() }
}