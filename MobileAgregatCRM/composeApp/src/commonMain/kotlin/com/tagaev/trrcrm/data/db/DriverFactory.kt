package com.tagaev.trrcrm.data.db

import app.cash.sqldelight.db.SqlDriver
import com.agregat.db.Database

//expect class DriverFactory {
//    fun createDriver(): SqlDriver
//}

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return Database(driver)
}
