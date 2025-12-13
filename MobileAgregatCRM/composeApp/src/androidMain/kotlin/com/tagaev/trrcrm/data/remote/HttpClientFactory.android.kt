package com.tagaev.trrcrm.data.remote

import com.tagaev.secrets.Secrets

actual val isPublish: Boolean = Secrets.IS_PUBLISH.toBoolean()