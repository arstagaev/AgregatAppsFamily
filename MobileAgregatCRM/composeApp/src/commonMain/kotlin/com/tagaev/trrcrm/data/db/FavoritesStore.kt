package com.tagaev.trrcrm.data.db

import com.agregatcrm.db.Favorites
import com.agregatcrm.db.FavoritesQueries
import kotlin.time.ExperimentalTime

class FavoritesStore(
    private val queries: FavoritesQueries
) {
    fun list(): List<Favorites> = queries.selectAll().executeAsList()

    fun isFavorite(number: String): Boolean =
        queries.selectByNumber(number).executeAsOneOrNull() != null

    @OptIn(ExperimentalTime::class)
    fun add(number: String) {
        queries.insertOrReplace(number, kotlin.time.Clock.System.now().toEpochMilliseconds())
    }

    fun remove(number: String) {
        queries.deleteByNumber(number)
    }

    fun toggle(number: String) {
        if (isFavorite(number)) remove(number) else add(number)
    }
}
