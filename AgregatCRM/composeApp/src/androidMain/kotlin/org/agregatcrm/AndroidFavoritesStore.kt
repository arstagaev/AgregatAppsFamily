package org.agregatcrm

import android.content.Context
import android.content.SharedPreferences
import org.agregatcrm.data.local.FavoritesStore

class AndroidFavoritesStore(context: Context) : FavoritesStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)

    override fun getFavorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet()) ?: emptySet()

    override fun saveFavorites(set: Set<String>) {
        prefs.edit().putStringSet("favorites", set).apply()
    }
}