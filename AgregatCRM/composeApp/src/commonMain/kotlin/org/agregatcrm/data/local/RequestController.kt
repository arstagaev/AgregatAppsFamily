package org.agregatcrm.data.local

import org.agregatcrm.models.RequestPrefs

//class RequestController(private val store: RequestPrefsStore) {
//    private var current = store.load()
//
//    fun get(): RequestPrefs = current
//
//    fun update(new: RequestPrefs) {
//        current = new
//        store.save(current)
//    }
//}

// commonMain
class RequestController(private val store: RequestPrefsStore) {
    private var current: RequestPrefs = store.load()

    fun get(): RequestPrefs = current

    fun setCount(value: Int) {
        current = current.copy(count = value)
        store.save(current)
    }

    fun setNcount(value: Int) {
        current = current.copy(ncount = value)
        store.save(current)
    }

    fun setFilter(by: String, `val`: String) {
        current = current.copy(filterBy = by, filterVal = `val`)
        store.save(current)
    }
}


// commonMain
class Shared(private val requestPrefsStore: RequestPrefsStore) {
    val requestController = RequestController(requestPrefsStore)
}

// Common entry you’ll call from Android/iOS
fun createShared(requestPrefsStore: RequestPrefsStore): Shared =
    Shared(requestPrefsStore)