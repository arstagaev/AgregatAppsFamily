package com.tagaev.mobileagregatcrm.ui.mainscreen

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.AppSettingsKeys
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.FilterState
import com.tagaev.mobileagregatcrm.data.db.EventsCacheStore
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.utils.DefaultValuesConst
import com.tagaev.mobileagregatcrm.models.EventItemDto
import com.tagaev.mobileagregatcrm.utils.getTimestamp
import com.tagaev.mobileagregatcrm.utils.getTimestampWithFormat
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock


interface ListComponent {
    val resource: StateFlow<Resource<List<EventItemDto>>>
    var dept: String


    suspend fun fullRefresh()
    /**
     * Loads the next page and appends to the current list. Call from "Загрузить ещё (+10)".
     */
    suspend fun loadMore(increment: Int = 30)
    fun setFiltersAndRefresh(new: FilterState)
    fun sendMessage(number: String, date: String, message: String)
    fun openDetails(number: String, snapshot: EventItemDto?)
    fun openFavorites()
}

class MainListComponent(
    componentContext: ComponentContext,
    private val appSettings: AppSettings, // <- inject via Koin
    private val onOpenDetails: (String, EventItemDto?) -> Unit,
    private val onOpenFavorites: () -> Unit
) : ListComponent, ComponentContext by componentContext, KoinComponent {

    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()
    private val apiConfig: ApiConfig by inject()
    private val eventsCacheStore: EventsCacheStore by inject()
    private val repo by lazy { EventsRepository(api, apiConfig) }

    private val backCallback = BackCallback { /* NO HANDLE */ }

    // Pagination state
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val currentItems = mutableListOf<EventItemDto>()
    private var nextOffset: Int = 0
//    private var pageSize: Int = DefaultValuesConst.COUNT

    private var requestsCounter = 0

    // ---- Rate limiting: at most one API request per 5 seconds ----
    private val RATE_LIMIT_MS = 500L
    private var lastApiCallAt: Long = 0L

    private suspend fun awaitRateLimit() {
        val now = getTimestamp
        val since = now - lastApiCallAt
        if (since in 0 until RATE_LIMIT_MS) {
            delay(RATE_LIMIT_MS - since)
        }
        // mark start of a new window
        lastApiCallAt = getTimestamp
    }

    /**
     * Normalizes a city label/value into the request format:
     * - trims
     * - replaces one or more spaces with a single underscore
     * Hyphens and other characters are preserved.
     */
    private fun normalizeFilterForRequest(raw: String?): String {
        val fallback = DefaultValuesConst.FILTER_VAL
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return fallback
        return s.replace(Regex("\\s+"), "_")
    }

    // Dedup by stable key (prefer guid, fallback to number|date)
    private val seenKeys = LinkedHashSet<String>()
    private fun keyOf(e: EventItemDto): String =
        e.guid?.takeIf { it.isNotBlank() } ?: "${e.number}|${e.date}"

    private val _resource = MutableStateFlow<Resource<List<EventItemDto>>>(Resource.Loading)
    val resourceInternal: StateFlow<Resource<List<EventItemDto>>> = _resource
    override val resource: StateFlow<Resource<List<EventItemDto>>> get() = resourceInternal
    override var dept: String = appSettings.getString(AppSettingsKeys.DEPARTMENT,"NO DEFINED")

    init {
        backHandler.register(backCallback)
        println("INIT")
        // 1) Show cached list immediately if present
        val cached = eventsCacheStore.load() //appSettings.loadEventsCache()
        println("items size ${cached.size}")
        if (cached.isNotEmpty()) {
            _resource.value = Resource.Success(cached)
            currentItems.clear()
            currentItems.addAll(cached)
            seenKeys.clear()
            seenKeys.addAll(cached.map(::keyOf))
        }
        // 2) Kick a fresh load with saved filters
        appScope.launch { fullRefresh() }
        requestsCounter = 0
    }

    override suspend fun fullRefresh() {
        println("fullRefresh> 0")
        dept = appSettings.getString(AppSettingsKeys.DEPARTMENT,"NO DEFINED")
        requestsCounter++
        mutex.withLock {
            val filters: FilterState = appSettings.loadFilters()
            val filterReqVal = normalizeFilterForRequest(filters.filterVal)
            // Reset pagination to the first page on a full refresh
//            pageSize = filters.count

            nextOffset = 0

            // Enforce max 1 request / 3s across all entry points
            if (requestsCounter > 10) {
                awaitRateLimit()
            }

            _resource.value = Resource.Loading

            val res: Resource<List<EventItemDto>> = repo.loadEvents(
                count = DefaultValuesConst.COUNT,
                ncount = nextOffset,
                orderBy = filters.orderBy,
                orderDir = filters.orderDir,
                filterBy = filters.filterBy,
                filterVal = filterReqVal
            )
            println("fullRefresh> 1")
            when (res) {
                is Resource.Success -> {
                    // Replace items & advance offset
                    currentItems.clear()
                    seenKeys.clear()
                    val fresh = res.data
                    currentItems.addAll(fresh)
                    seenKeys.addAll(fresh.map(::keyOf))
                    nextOffset += fresh.size
                    println("fullRefresh> ${currentItems.getOrNull(0)?.companyDepartment}  currentItems size:${currentItems.size}")
                    _resource.value = Resource.Success(currentItems.toList())
                    eventsCacheStore.save(currentItems)
//                    appSettings.saveEventsCache(currentItems)
                    // Persist new offset so subsequent app launches can continue paging
                    appSettings.saveFilters(filters.copy(ncount = nextOffset))
                    appSettings.setString(AppSettingsKeys.LAST_UPDATE,"${getTimestampWithFormat(getTimestamp)}")
                    println("fullRefresh> Success")
                }
                is Resource.Error -> {
                    // Fall back to cache if available, otherwise show the error
                    val cached = eventsCacheStore.load()//appSettings.loadEventsCache()
                    if (cached.isNotEmpty()) {
                        currentItems.clear()
                        currentItems.addAll(cached)
                        seenKeys.clear()
                        seenKeys.addAll(cached.map(::keyOf))
                        _resource.value = Resource.Success(currentItems.toList())
                    } else {
                        _resource.value = res
                    }
                    delay(500)
                    _resource.value = Resource.Error(causes = res.causes)
                    println("fullRefresh> Error ${res.causes}  ${res.exception?.message}")
                }
                is Resource.Loading -> {
                    _resource.value = Resource.Loading
                    println("fullRefresh> Loading")
                }
            }
        }
    }

    /** Loads the next page and appends to the current list. Call from "Загрузить ещё (+10)". */
    override suspend fun loadMore(increment: Int) {
        mutex.withLock {
            // Enforce max 1 request / 5s across all entry points
            awaitRateLimit()

            // _resource.value = Resource.Loading  // removed eager loading state

            // Keep showing the current list while we page
            val filters: FilterState = appSettings.loadFilters()
            val filterValNormalized = normalizeFilterForRequest(filters.filterVal)

            val res: Resource<List<EventItemDto>> = repo.loadEvents(
                count = increment,
                ncount = nextOffset,
                orderBy = filters.orderBy,
                orderDir = filters.orderDir,
                filterBy = filters.filterBy,
                filterVal = filterValNormalized
            )

            when (res) {
                is Resource.Success -> {
                    val incoming = res.data
                    var added = 0
                    for (e in incoming) {
                        val k = keyOf(e)
                        if (seenKeys.add(k)) {
                            currentItems.add(e)
                            added++
                        }
                    }
                    nextOffset += incoming.size // advance by what server returned
                    if (added > 0) {
                        _resource.value = Resource.Success(currentItems.toList())
                        eventsCacheStore.save(currentItems)
                    }
                    // persist new offset
                    appSettings.saveFilters(filters.copy(ncount = nextOffset))
                }
                is Resource.Error -> {

                    // Immediately restore the current list so the UI doesn't blank out
                    _resource.value = Resource.Success(currentItems.toList())

                    // Emit a transient error so UI can show an alert dialog
                    _resource.value = Resource.Error(res.exception, res.causes)
                }
                is Resource.Loading -> {
                    _resource.value = Resource.Loading
                    // no-op; keep current content
                }
            }
        }
    }

    // Optional helper to change filters + persist + refresh (keeps interface unchanged)
    override fun setFiltersAndRefresh(new: FilterState) {
        val normalized = new.copy(
            filterVal = normalizeFilterForRequest(new.filterVal),
            ncount = 0
        )
        appSettings.saveFilters(normalized)
//        pageSize = normalized.count
        nextOffset = 0
        appScope.launch { fullRefresh() }
    }

    override fun sendMessage(number: String, date: String, message: String) {
//        appScope.launch {
//            // You can react to the result if needed
//            repo.sendMessage(number, date, message)
//        }
    }

    // Navigation

    override fun openDetails(number: String, snapshot: EventItemDto?) {
        // remember last opened event (if you use DetailsUiState)
        val ui = appSettings.loadDetailsUi().copy(lastEventNumber = number)
        appSettings.saveDetailsUi(ui)
        onOpenDetails(number, snapshot)
    }
//        onOpenDetails(number, snapshot)

    override fun openFavorites() = onOpenFavorites()

    private fun updateBackCallback() {
        // Set isEnabled to true if you want to override the back button
        backCallback.isEnabled = true // or false
    }
}
