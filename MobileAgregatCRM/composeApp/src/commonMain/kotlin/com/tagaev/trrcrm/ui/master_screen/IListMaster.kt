package com.tagaev.trrcrm.ui.master_screen

import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.domain.TreeRootResolvedDocument
import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.domain.Refiner
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.flow.StateFlow

sealed interface DeepLinkOpenResult {
    data class OpenedDetails(val guid: String) : DeepLinkOpenResult
    data class OpenedResultsList(val count: Int) : DeepLinkOpenResult
    data object NotFound : DeepLinkOpenResult
    data class Failed(val reason: String) : DeepLinkOpenResult
}

interface IListMaster {
    val refineState: StateFlow<RefineState>
    val ncount: StateFlow<Int>

    val masterScreenPanel: StateFlow<MasterPanel>
    val selectedItemGuid: StateFlow<String?>


    fun setRefineState(newState: RefineState)
    fun fullRefresh()
    fun loadMore()
    /** Returns null on success, or a human-readable error string on failure. */
    suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): String?

    fun addLocalMessage(
        orderGuid: String?,
        message: MessageModel
    )

    suspend fun resolveBaseDocument(rawBaseDocument: String): Resource<TreeRootResolvedDocument> =
        Resource.Error(causes = "Переход по документу-основанию недоступен на этом экране")

    suspend fun resolveNotificationTarget(identifier: String, messageHint: String?): String? = null

    fun findAndSelectByNotification(identifier: String, messageHint: String?): Boolean = false

    fun enterDeepLinkMode() {}

    fun restoreAfterDeepLinkIfNeeded() {}

    suspend fun resolveAndOpenDeepLink(
        identifier: String,
        messageHint: String?,
        preferredSearchType: Refiner.SearchQueryType?,
    ): DeepLinkOpenResult {
        val resolvedGuid = resolveNotificationTarget(identifier, messageHint)
            ?: return DeepLinkOpenResult.NotFound
        selectItemFromList(resolvedGuid)
        changePanel(MasterPanel.Details)
        return DeepLinkOpenResult.OpenedDetails(resolvedGuid)
    }

    fun selectItemFromList(guid: String?)
    fun changePanel(masterDetailPanel: MasterPanel)
}
