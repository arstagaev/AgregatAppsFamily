package org.agregatcrm.data.local

import org.agregatcrm.models.RequestPrefs


interface RequestPrefsStore {
    fun save(prefs: RequestPrefs)
    fun load(): RequestPrefs
}