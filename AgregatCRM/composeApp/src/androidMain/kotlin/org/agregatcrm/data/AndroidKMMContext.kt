package org.agregatcrm.data

import android.content.SharedPreferences
import org.agregatcrm.data.local.KMMContext

/** Implementation backed by Android SharedPreferences */
class AndroidKMMContext(private val prefs: SharedPreferences) : KMMContext {
    override fun setInt(key: String, value: Int)     = prefs.edit().putInt(key, value).apply()
    override fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun setBool(key: String, value: Boolean)  = prefs.edit().putBoolean(key, value).apply()

    override fun getInt(key: String, default: Int): Int       = prefs.getInt(key, default)
    override fun getString(key: String, default: String): String?              = prefs.getString(key, default)
    override fun getBool(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
}
