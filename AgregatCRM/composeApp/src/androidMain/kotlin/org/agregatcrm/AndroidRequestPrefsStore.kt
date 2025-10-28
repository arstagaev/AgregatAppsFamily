package org.agregatcrm

import android.content.Context
import android.content.SharedPreferences
import org.agregatcrm.data.local.RequestPrefsStore
import org.agregatcrm.models.RequestPrefs

class AndroidRequestPrefsStore(context: Context) : RequestPrefsStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("request_prefs", Context.MODE_PRIVATE)

    override fun save(prefsObj: RequestPrefs) {
        prefs.edit()
            .putInt("count", prefsObj.count)
            .putInt("ncount", prefsObj.ncount)
            .putString("filterBy", prefsObj.filterBy)
            .putString("filterVal", prefsObj.filterVal)
            .apply()
    }

    override fun load(): RequestPrefs {
        return RequestPrefs(
            count = prefs.getInt("count", 20),
            ncount = prefs.getInt("ncount", 0),
            filterBy = prefs.getString("filterBy", "") ?: "",
            filterVal = prefs.getString("filterVal", "") ?: ""
        )
    }
}