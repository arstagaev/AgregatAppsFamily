package org.agregatcrm.data.local


import org.agregatcrm.models.RequestPrefs
import platform.Foundation.NSUserDefaults

class IOSRequestPrefsStore : RequestPrefsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun save(prefsObj: RequestPrefs) {
        defaults.setInteger(prefsObj.count.toLong(), forKey = "count")
        defaults.setInteger(prefsObj.ncount.toLong(), forKey = "ncount")
        defaults.setObject(prefsObj.filterBy, forKey = "filterBy")
        defaults.setObject(prefsObj.filterVal, forKey = "filterVal")
    }

    override fun load(): RequestPrefs {
        val count = defaults.integerForKey("count").toInt()
        val ncount = defaults.integerForKey("ncount").toInt()
        val filterBy = defaults.stringForKey("filterBy") ?: ""
        val filterVal = defaults.stringForKey("filterVal") ?: ""
        return RequestPrefs(count, ncount, filterBy, filterVal)
    }
}