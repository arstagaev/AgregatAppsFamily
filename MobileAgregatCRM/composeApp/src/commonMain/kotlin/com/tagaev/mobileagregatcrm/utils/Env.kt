package com.tagaev.mobileagregatcrm.utils

import com.tagaev.secrets.Secrets

object Env {
    val appEnv: String get() = Secrets.APP_ENV       // "debug" or "prod"
    val isDebug: Boolean get() = appEnv == "debug"
    val isProd: Boolean get() = appEnv == "prod"
}
