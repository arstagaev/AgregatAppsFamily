package com.tagaev.trrcrm.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.agregat.db.Database
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val DB_FILE_NAME = "crm.db"

/** Display / bundle-aligned folder name (matches iOS CFBundleDisplayName). */
private const val APP_DATA_DIR_WIN_MAC = "TRR CRM"

private fun desktopAppDataRoot(): Path {
    val home = Paths.get(System.getProperty("user.home") ?: ".")
    val os = (System.getProperty("os.name") ?: "").lowercase()
    return when {
        os.contains("win") -> {
            val local = System.getenv("LOCALAPPDATA")
            if (!local.isNullOrBlank()) {
                Paths.get(local, APP_DATA_DIR_WIN_MAC)
            } else {
                home.resolve("AppData").resolve("Local").resolve(APP_DATA_DIR_WIN_MAC)
            }
        }
        os.contains("mac") -> {
            home.resolve("Library").resolve("Application Support").resolve(APP_DATA_DIR_WIN_MAC)
        }
        else -> {
            val xdg = System.getenv("XDG_DATA_HOME")
            if (!xdg.isNullOrBlank()) {
                Paths.get(xdg, "trrcrm")
            } else {
                home.resolve(".local").resolve("share").resolve("trrcrm")
            }
        }
    }
}

private fun resolveDesktopDatabaseFile(): Path {
    val dir = desktopAppDataRoot()
    Files.createDirectories(dir)
    return dir.resolve(DB_FILE_NAME)
}

/**
 * Dev / legacy: [crm.db] next to the process working directory (often not writable when installed).
 * If the new user-data file does not exist yet, copy once so cache is preserved.
 */
private fun migrateLegacyCwdDatabaseIfNeeded(target: Path) {
    if (Files.exists(target)) return
    val legacy = Paths.get(System.getProperty("user.dir"), DB_FILE_NAME)
    if (Files.isRegularFile(legacy)) {
        runCatching {
            Files.copy(legacy, target)
        }
    }
}

private fun jdbcSqliteUrlForPath(dbFile: Path): String {
    val normalized = dbFile.toAbsolutePath().normalize().toString().replace('\\', '/')
    return "jdbc:sqlite:$normalized"
}

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = resolveDesktopDatabaseFile()
        migrateLegacyCwdDatabaseIfNeeded(dbFile)
        val driver = JdbcSqliteDriver(jdbcSqliteUrlForPath(dbFile))
        runCatching { Database.Schema.create(driver) }
        return driver
    }
}

actual val isSqlDriverAvailable: Boolean = true
