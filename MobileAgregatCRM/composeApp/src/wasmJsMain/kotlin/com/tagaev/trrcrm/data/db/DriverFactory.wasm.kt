package com.tagaev.trrcrm.data.db

import app.cash.sqldelight.db.SqlDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        error("SQLDelight driver is not configured for web target in simplified mode.")
    }
}

actual val isSqlDriverAvailable: Boolean = false
