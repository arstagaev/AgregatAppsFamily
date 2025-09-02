package org.agregatcrm.data.local

import platform.Foundation.NSUserDefaults

class IOSFavoritesStore : FavoritesStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getFavorites(): Set<String> {
        val arr = defaults.stringArrayForKey("favorites") ?: emptyList<String>()
        return arr.toSet() as Set<String>
    }

    override fun saveFavorites(set: Set<String>) {
        defaults.setObject(set.toList(), forKey = "favorites")
    }
}