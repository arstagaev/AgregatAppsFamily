package com.tagaev.trrcrm.data.fixator

import okio.Path.Companion.toPath
import java.nio.file.Files
import java.nio.file.Paths

actual fun fixatorAppStorageFolderName(): String = "TRR CRM"

private class NoOpPublicGallerySaver : PublicGallerySaver {
    override suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult =
        GallerySaveResult.Unavailable
}

private fun desktopAppDataRoot(): String {
    val home = Paths.get(System.getProperty("user.home") ?: ".")
    val os = (System.getProperty("os.name") ?: "").lowercase()
    return when {
        os.contains("win") -> {
            val local = System.getenv("LOCALAPPDATA")
            if (!local.isNullOrBlank()) {
                Paths.get(local, fixatorAppStorageFolderName()).toString()
            } else {
                home.resolve("AppData").resolve("Local").resolve(fixatorAppStorageFolderName()).toString()
            }
        }
        os.contains("mac") -> {
            home.resolve("Library").resolve("Application Support").resolve(fixatorAppStorageFolderName()).toString()
        }
        else -> {
            val xdg = System.getenv("XDG_DATA_HOME")
            if (!xdg.isNullOrBlank()) {
                Paths.get(xdg, "trrcrm").toString()
            } else {
                home.resolve(".local").resolve("share").resolve("trrcrm").toString()
            }
        }
    }
}

actual fun createFixatorPhotoStorage(): FixatorPhotoStorage {
    val root = desktopAppDataRoot()
    Files.createDirectories(Paths.get(root))
    return FixatorPhotoStorage(
        storageRoot = root.toPath(),
        publicGallerySaver = NoOpPublicGallerySaver(),
    )
}
