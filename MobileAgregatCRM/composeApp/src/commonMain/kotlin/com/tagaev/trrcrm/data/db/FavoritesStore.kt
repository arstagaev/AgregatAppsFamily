package com.tagaev.trrcrm.data.db

import com.agregatcrm.db.Favorites
import com.agregatcrm.db.FavoritesQueries
import kotlin.time.ExperimentalTime

class FavoritesStore(
    private val queries: FavoritesQueries? = null
) {
    private val memoryFavorites = LinkedHashSet<String>()

    fun list(): List<Favorites> = queries?.selectAll()?.executeAsList() ?: emptyList()

    fun isFavorite(number: String): Boolean = queries?.selectByNumber(number)?.executeAsOneOrNull() != null || number in memoryFavorites

    @OptIn(ExperimentalTime::class)
    fun add(number: String) {
        if (queries != null) {
            queries.insertOrReplace(number, kotlin.time.Clock.System.now().toEpochMilliseconds())
        } else {
            memoryFavorites.add(number)
        }
    }

    fun remove(number: String) {
        if (queries != null) {
            queries.deleteByNumber(number)
        } else {
            memoryFavorites.remove(number)
        }
    }

    fun toggle(number: String) {
        if (isFavorite(number)) remove(number) else add(number)
    }
}
