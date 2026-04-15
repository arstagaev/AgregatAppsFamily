package com.tagaev.trrcrm.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agregat.db.Database

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:crm.db")
        runCatching { Database.Schema.create(driver) }
        return driver
    }
}

actual val isSqlDriverAvailable: Boolean = true
