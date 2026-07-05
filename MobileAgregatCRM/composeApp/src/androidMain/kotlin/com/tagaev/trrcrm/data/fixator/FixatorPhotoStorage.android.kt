package com.tagaev.trrcrm.data.fixator

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import okio.Path.Companion.toPath

actual fun fixatorAppStorageFolderName(): String = "TRR APP"

private class AndroidPublicGallerySaver(
    private val context: Context,
) : PublicGallerySaver {
    override suspend fun save(normalizedBytes: ByteArray, displayName: String): GallerySaveResult =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PermissionChecker.PERMISSION_GRANTED
                if (!granted) return@withContext GallerySaveResult.PermissionDenied
            }
            runCatching {
                val folderName = fixatorAppStorageFolderName()
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/$folderName",
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext GallerySaveResult.Failed
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(normalizedBytes)
                } ?: return@withContext GallerySaveResult.Failed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                GallerySaveResult.Success
            }.getOrElse { error ->
                CameraFixatorLog.d("gallery_save_failed message=${error.message}")
                GallerySaveResult.Failed
            }
        }
}

actual fun createFixatorPhotoStorage(): FixatorPhotoStorage {
    val context = GlobalContext.get().get<Context>()
    val rootDir = context.getExternalFilesDir(null)
        ?: context.filesDir
    val rootPath = rootDir.resolve(fixatorAppStorageFolderName()).absolutePath.toPath()
    return FixatorPhotoStorage(
        storageRoot = rootPath,
        publicGallerySaver = AndroidPublicGallerySaver(context),
    )
}
