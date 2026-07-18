package com.tagaev.trrcrm.ui.settings

import com.tagaev.trrcrm.ui.i18n.tr

import com.tagaev.trrcrm.navigation.BottomNavItemId
import com.tagaev.trrcrm.navigation.BottomNavLayoutItem
import com.tagaev.trrcrm.navigation.BottomNavLayoutResolver
import com.tagaev.trrcrm.navigation.BottomNavLayoutState
import com.tagaev.trrcrm.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface BottomNavSaveResult {
    data object Success : BottomNavSaveResult
    data object SuccessNavigateHome : BottomNavSaveResult
    data class Error(val message: String) : BottomNavSaveResult
}

interface BottomNavLayoutEditorHost {
    val bottomNavDraft: StateFlow<List<BottomNavLayoutItem>>
    val bottomNavDirty: StateFlow<Boolean>
    val bottomNavSaveMessage: StateFlow<String?>
    val bottomNavSaveError: StateFlow<String?>

    fun moveBottomNavItem(fromIndex: Int, toIndex: Int)
    fun toggleBottomNavVisible(id: String)
    fun saveBottomNavLayout(activeTabId: BottomNavItemId?): BottomNavSaveResult
    fun resetBottomNavDraft()
    fun consumeBottomNavSaveMessage()
    fun consumeBottomNavSaveError()
}

class BottomNavLayoutEditorController(
    private val settings: AppSettings,
) : BottomNavLayoutEditorHost {
    private val _bottomNavDraft = MutableStateFlow(BottomNavLayoutResolver.mergeLayout(BottomNavLayoutState.savedLayout()))
    override val bottomNavDraft: StateFlow<List<BottomNavLayoutItem>> = _bottomNavDraft

    private val _bottomNavDirty = MutableStateFlow(false)
    override val bottomNavDirty: StateFlow<Boolean> = _bottomNavDirty

    private val _bottomNavSaveMessage = MutableStateFlow<String?>(null)
    override val bottomNavSaveMessage: StateFlow<String?> = _bottomNavSaveMessage

    private val _bottomNavSaveError = MutableStateFlow<String?>(null)
    override val bottomNavSaveError: StateFlow<String?> = _bottomNavSaveError

    override fun moveBottomNavItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val editorItems = BottomNavLayoutResolver.resolveEditorItems(_bottomNavDraft.value)
        if (fromIndex !in editorItems.indices || toIndex !in editorItems.indices) return

        val fromId = editorItems[fromIndex].id
        val toId = editorItems[toIndex].id
        val draft = _bottomNavDraft.value.toMutableList()
        val fromDraftIndex = draft.indexOfFirst { it.id == fromId }
        val toDraftIndex = draft.indexOfFirst { it.id == toId }
        if (fromDraftIndex < 0 || toDraftIndex < 0) return

        val item = draft.removeAt(fromDraftIndex)
        draft.add(toDraftIndex, item)
        _bottomNavDraft.value = draft
        updateDirty()
    }

    override fun toggleBottomNavVisible(id: String) {
        val tabId = BottomNavItemId.fromWire(id) ?: return
        if (!tabId.isHideable) return
        _bottomNavDraft.value = _bottomNavDraft.value.map { item ->
            if (item.id == id) item.copy(visible = !item.visible) else item
        }
        updateDirty()
    }

    override fun saveBottomNavLayout(activeTabId: BottomNavItemId?): BottomNavSaveResult {
        val draft = BottomNavLayoutResolver.enforceRequiredVisibility(_bottomNavDraft.value)
        _bottomNavDraft.value = draft
        if (draft.none { it.visible }) {
            _bottomNavSaveError.value = tr("settings_dolzhen_byt_viden_hotya_by_odin_punkt_menyu")
            return BottomNavSaveResult.Error(tr("settings_dolzhen_byt_viden_hotya_by_odin_punkt_menyu"))
        }

        settings.saveBottomNavLayout(draft)
        BottomNavLayoutState.applySaved(draft)
        updateDirty()
        _bottomNavSaveMessage.value = tr("settings_panel_navigatsii_sohranena")
        _bottomNavSaveError.value = null

        val activeHidden = activeTabId != null &&
            draft.any { it.id == activeTabId.wire && !it.visible }
        return if (activeHidden) {
            BottomNavSaveResult.SuccessNavigateHome
        } else {
            BottomNavSaveResult.Success
        }
    }

    override fun resetBottomNavDraft() {
        _bottomNavDraft.value = BottomNavLayoutResolver.mergeLayout(BottomNavLayoutState.savedLayout())
        updateDirty()
        _bottomNavSaveError.value = null
    }

    override fun consumeBottomNavSaveMessage() {
        _bottomNavSaveMessage.value = null
    }

    override fun consumeBottomNavSaveError() {
        _bottomNavSaveError.value = null
    }

    private fun updateDirty() {
        val saved = BottomNavLayoutResolver.mergeLayout(BottomNavLayoutState.savedLayout())
        _bottomNavDirty.value = !BottomNavLayoutResolver.layoutsEqual(_bottomNavDraft.value, saved)
    }
}
