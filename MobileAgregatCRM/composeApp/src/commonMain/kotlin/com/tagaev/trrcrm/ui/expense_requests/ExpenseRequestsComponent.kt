package com.tagaev.trrcrm.ui.expense_requests

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.EventsApi.Companion.json
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.withOrderByMigratedFromDateLastModificationIfNeeded
import com.tagaev.trrcrm.models.ExpenseRequestDto
import com.tagaev.trrcrm.ui.master_screen.IListMaster
import com.tagaev.trrcrm.ui.master_screen.MasterPanel
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface IExpenseRequestsComponent : IListMaster {
    val expenseRequests: StateFlow<Resource<List<ExpenseRequestDto>>>
}

class ExpenseRequestsComponent(
    componentContext: ComponentContext,
    @Suppress("UNUSED_PARAMETER") private val onBack: () -> Unit,
) : IExpenseRequestsComponent,
    ComponentContext by componentContext,
    KoinComponent {

    companion object {
        private const val PAGE_SIZE = 30
        private val DEFAULT_REFINE = RefineState(
            orderBy = Refiner.OrderBy.DATE,
            orderDir = Refiner.Dir.DESC,
            searchQueryType = Refiner.SearchQueryType.CODE,
        )
    }

    private val appScope: CoroutineScope by inject()
    private val repository: MainRepository by inject()
    private val appSettings: AppSettings by inject()

    private val _expenseRequests =
        MutableStateFlow<Resource<List<ExpenseRequestDto>>>(Resource.Loading)
    override val expenseRequests: StateFlow<Resource<List<ExpenseRequestDto>>> = _expenseRequests

    private val _refineState = MutableStateFlow(DEFAULT_REFINE)
    override val refineState: StateFlow<RefineState> = _refineState

    val _masterScreenPanel = MutableStateFlow(MasterPanel.List)
    override val masterScreenPanel: StateFlow<MasterPanel> = _masterScreenPanel

    private val _ncount = MutableStateFlow(0)
    override val ncount: StateFlow<Int> = _ncount

    private val _selectedGuid = MutableStateFlow<String?>(null)
    override val selectedItemGuid: StateFlow<String?> = _selectedGuid

    private val loadedItems = mutableListOf<ExpenseRequestDto>()
    private val loadedKeys = mutableSetOf<String>()

    val backCallback = BackCallback { _masterScreenPanel.value = MasterPanel.List }

    private fun ExpenseRequestDto.key(): String = guid.trim()

    init {
        fullRefresh()
        backHandler.register(backCallback)
    }

    override fun selectItemFromList(guid: String?) {
        _selectedGuid.value = guid
    }

    override fun fullRefresh() {
        appScope.launch {
            _expenseRequests.value = Resource.Success(data = loadedItems, additionalLoading = true)
            _refineState.value = loadRefineState()
            _ncount.value = 0

            val result = repository.loadExpenseRequests(0, refineState.value)
            if (result is Resource.Success) {
                loadedItems.clear()
                loadedKeys.clear()
                for (item in result.data) {
                    val key = item.key()
                    if (loadedKeys.add(key)) loadedItems.add(item)
                }
                _expenseRequests.value =
                    Resource.Success(loadedItems.toList(), additionalLoading = false)
            } else {
                _expenseRequests.value = result
            }
        }
    }

    override fun loadMore() {
        appScope.launch {
            _expenseRequests.value = Resource.Success(data = loadedItems, additionalLoading = true)
            val nextOffset = _ncount.value + PAGE_SIZE
            val result = repository.loadExpenseRequests(nextOffset, refineState.value)
            if (result is Resource.Success) {
                _ncount.value = nextOffset
                for (item in result.data) {
                    val key = item.key()
                    if (loadedKeys.add(key)) loadedItems.add(item)
                }
                _expenseRequests.value =
                    Resource.Success(loadedItems.toList(), additionalLoading = false)
            } else {
                _expenseRequests.value = result
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
    ): String? = "Отправка сообщений для заявок на расход ДС не поддерживается"

    override fun addLocalMessage(orderGuid: String?, message: MessageModel) {
        // read-only screen
    }

    override suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> =
        repository.resolveTreeRootDocument(rawBaseDocument)

    private fun loadRefineState(): RefineState {
        val raw = appSettings.getStringOrNull(AppSettingsKeys.EXPENSE_REQUESTS_REFINE_STATE)
        if (raw.isNullOrBlank()) return DEFAULT_REFINE
        val decoded = runCatching { json.decodeFromString<RefineState>(raw) }.getOrDefault(DEFAULT_REFINE)
        val migrated = decoded.withOrderByMigratedFromDateLastModificationIfNeeded(false)
        if (migrated != decoded) saveRefineState(migrated)
        return migrated.copy(
            orderBy = Refiner.OrderBy.DATE,
            orderDir = migrated.orderDir,
        )
    }

    private fun saveRefineState(state: RefineState) {
        val encoded = runCatching { json.encodeToString(state) }.getOrNull() ?: return
        appSettings.setString(AppSettingsKeys.EXPENSE_REQUESTS_REFINE_STATE, encoded)
    }
}
