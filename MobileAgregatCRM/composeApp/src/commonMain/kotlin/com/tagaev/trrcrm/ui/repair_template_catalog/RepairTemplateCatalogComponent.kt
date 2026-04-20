package com.tagaev.trrcrm.ui.repair_template_catalog

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.models.RepairTemplateCatalogItemDto
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IRepairTemplateCatalogComponent : IListMaster {
    val items: StateFlow<Resource<List<RepairTemplateCatalogItemDto>>>
}

class RepairTemplateCatalogComponent(
    componentContext: ComponentContext,
    @Suppress("UNUSED_PARAMETER") private val onBack: () -> Unit,
) : IRepairTemplateCatalogComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 30
        private val DEFAULT_REFINE = RefineState(
            orderBy = Refiner.OrderBy.OFF,
            orderDir = Refiner.Dir.ASC,
            searchQueryType = Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL,
        )

        private val ALLOWED_SEARCH = Refiner.SearchQueryType.repairTemplateCatalogSearchTypes
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _items =
        MutableStateFlow<Resource<List<RepairTemplateCatalogItemDto>>>(Resource.Loading)
    override val items: StateFlow<Resource<List<RepairTemplateCatalogItemDto>>> = _items

    private val _refineState = MutableStateFlow(DEFAULT_REFINE)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedGuid

    private val loadedItems = mutableListOf<RepairTemplateCatalogItemDto>()
    private val loadedKeys = mutableSetOf<String>()

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun RepairTemplateCatalogItemDto.key(): String =
        guid.trim().ifBlank { link?.trim().orEmpty() }
            .ifBlank { code?.trim().orEmpty() }
            .ifBlank { name?.trim().orEmpty() }

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun selectItemFromList(guid: String?) {
        _selectedGuid.value = guid
    }

    override fun fullRefresh() {
        appScope.launch {
            _items.value = Resource.Success(data = loadedItems, additionalLoading = true)
            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadRepairTemplateCatalog(0, refineState.value)
            if (result is Resource.Success) {
                loadedItems.clear()
                loadedKeys.clear()
                for (item in result.data) {
                    val key = item.key()
                    if (key.isNotBlank() && loadedKeys.add(key)) loadedItems.add(item)
                }
                _items.value =
                    Resource.Success(loadedItems.toList(), additionalLoading = false)
            } else {
                _items.value = result
            }
        }
    }

    override fun loadMore() {
        appScope.launch {
            _items.value = Resource.Success(data = loadedItems, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadRepairTemplateCatalog(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                for (item in result.data) {
                    val key = item.key()
                    if (key.isNotBlank() && loadedKeys.add(key)) loadedItems.add(item)
                }
                _items.value =
                    Resource.Success(loadedItems.toList(), additionalLoading = false)
            } else {
                _items.value = result
            }
        }
    }

    override fun changePanel(masterDetailPanel: MasterPanel) {
        _masterScreenPanel.value = masterDetailPanel
    }

    override fun setRefineState(newState: RefineState) {
        _refineState.value = newState.copy(searchQuery = newState.searchQuery.trim())
        saveRefineState(_refineState.value)
        _ncount.value = 0
        fullRefresh()
    }

    override suspend fun sendMessage(
        itemNumber: String,
        itemDate: String,
        message: String,
    ): String? = "Справочник «Шаблоны ремонта» только для просмотра"

    override fun addLocalMessage(orderGuid: String?, message: MessageModel) {
        // read-only
    }

    private fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.REPAIR_TEMPLATE_CATALOG_REFINE_STATE)
        if (raw.isNullOrBlank()) return DEFAULT_REFINE
        val decoded = runCatching { json.decodeFromString<RefineState>(raw) }.getOrDefault(DEFAULT_REFINE)
        val searchType = decoded.searchQueryType.takeIf { it in ALLOWED_SEARCH }
            ?: Refiner.SearchQueryType.REPAIR_TEMPLATE_MODEL
        return decoded.copy(
            orderBy = Refiner.OrderBy.OFF,
            searchQueryType = searchType,
        )
    }

    private fun saveRefineState(state: RefineState) {
        val encoded = runCatching { json.encodeToString(state) }.getOrNull() ?: return
        appSettings.setString(AppSettingsKeys.REPAIR_TEMPLATE_CATALOG_REFINE_STATE, encoded)
    }
}
