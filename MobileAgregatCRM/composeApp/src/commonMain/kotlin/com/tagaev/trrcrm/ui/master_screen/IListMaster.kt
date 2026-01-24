package com.tagaev.trrcrm.ui.master_screen

import com.tagaev.trrcrm.domain.RefineState
import com.tagaev.trrcrm.ui.master_screen.models.MessageModel
import kotlinx.coroutines.flow.StateFlow

interface IListMaster {
    val refineState: StateFlow<RefineState>
    val ncount: StateFlow<Int>

    val masterScreenPanel: StateFlow<MasterPanel>
    val selectedItemGuid: StateFlow<String?>


    fun setRefineState(newState: RefineState)
    fun fullRefresh()
    fun loadMore()
    suspend fun sendMessage(itemNumber: String, itemDate: String, message: String): Boolean

    fun addLocalMessage(
        orderGuid: String?,
        message: MessageModel
    )

    fun selectItemFromList(guid: String?)
    fun changePanel(masterDetailPanel: MasterPanel)
}