package com.tagaev.mobileagregatcrm.ui.mainscreen

import com.arkivanov.decompose.ComponentContext
import com.tagaev.mobileagregatcrm.data.AppSettings
import com.tagaev.mobileagregatcrm.data.EventsRepository
import com.tagaev.mobileagregatcrm.data.FilterState
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.utils.DefaultConfig
import org.agregatcrm.models.EventItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock


interface ListComponent {
    val resource: StateFlow<Resource<List<EventItemDto>>>
    suspend fun fullRefresh()
    /**
     * Loads the next page and appends to the current list. Call from "Загрузить ещё (+10)".
     */
    suspend fun loadMore(increment: Int = 10)
    fun setFiltersAndRefresh(new: FilterState)
    fun sendMessage(number: String, date: String, message: String)
    fun openDetails(number: String, snapshot: EventItemDto?)
    fun openFavorites()
}

class DefaultListComponent(
    componentContext: ComponentContext,
    private val appSettings: AppSettings, // <- inject via Koin
    private val onOpenDetails: (String, EventItemDto?) -> Unit,
    private val onOpenFavorites: () -> Unit
) : ListComponent, ComponentContext by componentContext, KoinComponent {

    private val appScope: CoroutineScope by inject()
    private val api: EventsApi by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo by lazy { EventsRepository(api, apiConfig) }

    // Pagination state
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private val currentItems = mutableListOf<EventItemDto>()
    private var nextOffset: Int = 0
    private var pageSize: Int = 10

    // Dedup by stable key (prefer guid, fallback to number|date)
    private val seenKeys = LinkedHashSet<String>()
    private fun keyOf(e: EventItemDto): String =
        e.guid?.takeIf { it.isNotBlank() } ?: "${e.number}|${e.date}"

    private val _resource = MutableStateFlow<Resource<List<EventItemDto>>>(Resource.Loading)
    val resourceInternal: StateFlow<Resource<List<EventItemDto>>> = _resource
    override val resource: StateFlow<Resource<List<EventItemDto>>> get() = resourceInternal

    init {
        // 1) Show cached list immediately if present
        val cached = appSettings.loadEventsCache()
        if (cached.isNotEmpty()) {
            _resource.value = Resource.Success(cached)
            currentItems.clear()
            currentItems.addAll(cached)
            seenKeys.clear()
            seenKeys.addAll(cached.map(::keyOf))
        }
        // 2) Kick a fresh load with saved filters
        appScope.launch { fullRefresh() }
    }

    override suspend fun fullRefresh() {
        println("fullRefresh> 0")
        mutex.withLock {
            val filters: FilterState = appSettings.loadFilters()
            // Reset pagination to the first page on a full refresh
            pageSize = filters.count
            nextOffset = 0

            _resource.value = Resource.Loading

            val res: Resource<List<EventItemDto>> = repo.loadEvents(
                count = pageSize,
                ncount = nextOffset,
                orderBy = filters.orderBy ?: "Дата",
                orderDir = filters.orderDir ?: "desc",
                filterBy = filters.filterBy ?: DefaultConfig.FILTER_BY,
                filterVal = filters.filterVal ?: DefaultConfig.FILTER_VAL
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
                    appSettings.saveEventsCache(currentItems)
                    // Persist new offset so subsequent app launches can continue paging
                    appSettings.saveFilters(filters.copy(ncount = nextOffset))

                    println("fullRefresh> Success")
                }
                is Resource.Error -> {
                    // Fall back to cache if available, otherwise show the error
                    val cached = appSettings.loadEventsCache()
                    if (cached.isNotEmpty()) {
                        currentItems.clear()
                        currentItems.addAll(cached)
                        seenKeys.clear()
                        seenKeys.addAll(cached.map(::keyOf))
                        _resource.value = Resource.Success(currentItems.toList())
                    } else {
                        _resource.value = res
                    }
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
            // Keep showing the current list while we page
            val filters: FilterState = appSettings.loadFilters()

            val res: Resource<List<EventItemDto>> = repo.loadEvents(
                count = increment,
                ncount = nextOffset,
                orderBy = filters.orderBy ?: "Дата",
                orderDir = filters.orderDir ?: "asc",
                filterBy = filters.filterBy ?: "ПодразделениеКомпании",
                filterVal = filters.filterVal ?: "Сочи"
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
                        appSettings.saveEventsCache(currentItems)
                    }
                    // persist new offset
                    appSettings.saveFilters(filters.copy(ncount = nextOffset))
                }
                is Resource.Error -> {
                    // Do NOT drop current list on paging error: keep showing it.
                    // You may surface this via a toast/snackbar at the UI layer if desired.
                    // Here we just re-emit the current list to be explicit.
                    _resource.value = Resource.Success(currentItems.toList())
                }
                is Resource.Loading -> {
                    // no-op; keep current content
                }
            }
        }
    }

    // Call this from your TopControls changes / dialogs
    fun updateFilters(filters: FilterState, refreshNow: Boolean = true) {
        appSettings.saveFilters(filters)
    }

    // Optional helper to change filters + persist + refresh (keeps interface unchanged)
    override fun setFiltersAndRefresh(new: FilterState) {
        println("setFiltersAndRefresh> ${new.toString()}")
        appSettings.saveFilters(new.copy(ncount = 0))
        pageSize = new.count
        nextOffset = 0
        appScope.launch { fullRefresh() }
    }

    override fun sendMessage(number: String, date: String, message: String) {
        appScope.launch {
            // You can react to the result if needed
            repo.sendMessage(number, date, message)
        }
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
}
