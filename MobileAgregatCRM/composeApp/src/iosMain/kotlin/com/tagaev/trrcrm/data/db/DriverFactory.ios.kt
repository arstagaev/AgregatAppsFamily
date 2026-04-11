package com.tagaev.trrcrm.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.agregat.db.Database

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, "crm.db")
    }
}

actual val isSqlDriverAvailable: Boolean = true
