package org.agregatcrm.data.local

interface FavoritesStore {
    fun getFavorites(): Set<String>
    fun saveFavorites(set: Set<String>)
}