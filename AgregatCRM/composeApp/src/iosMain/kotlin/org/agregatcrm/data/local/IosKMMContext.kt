package org.agregatcrm.data.local

import platform.Foundation.NSUserDefaults
import kotlin.takeIf

/** Implementation backed by NSUserDefaults */
class IosKMMContext : KMMContext {
    private val ud = NSUserDefaults.standardUserDefaults()

    override fun setInt(key: String, value: Int) =
        ud.setInteger(value.toLong(), key)

    override fun setString(key: String, value: String) =
        ud.setObject(value, key)

    override fun setBool(key: String, value: Boolean) =
        ud.setBool(value, key)

    override fun getInt(key: String, default: Int): Int =
        ud.integerForKey(key).toInt().takeIf { it != 0 } ?: default

    override fun getString(key: String, default: String): String? =
        ud.stringForKey(key).takeIf { it != "" } ?: default

    override fun getBool(key: String, default: Boolean): Boolean? =
        if (hasKey(key)) ud.boolForKey(key) else default

    fun hasKey(key: String): Boolean = ud.objectForKey(key) != null
}