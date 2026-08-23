package com.tagaev.trrcrm.ui.permissions

expect fun openExternalDocument(fileName: String, mimeType: String, bytes: ByteArray): Boolean
