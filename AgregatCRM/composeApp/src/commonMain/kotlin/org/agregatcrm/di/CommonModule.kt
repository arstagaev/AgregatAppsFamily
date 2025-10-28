package org.agregatcrm.di

import org.agregatcrm.data.local.CMPPrefs
import org.koin.dsl.module

val commonModule = module {
    single { CMPPrefs(get()) }
}