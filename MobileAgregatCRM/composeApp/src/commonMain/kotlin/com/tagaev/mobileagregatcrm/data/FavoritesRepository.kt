package com.tagaev.mobileagregatcrm.data

import com.agregatcrm.db.Favorites
import com.agregatcrm.db.FavoritesQueries
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class FavoritesRepository(private val q: FavoritesQueries) {
    fun all(): Flow<List<Favorites>> = q.selectAll().asFlow().mapToList(Dispatchers.Default)
    fun exists(number: String): Boolean = q.selectByNumber(number).executeAsOneOrNull() != null
    fun upsert(number: String, ts: Long) = q.insertOrReplace(number, ts)
    fun delete(number: String) = q.deleteByNumber(number)
}