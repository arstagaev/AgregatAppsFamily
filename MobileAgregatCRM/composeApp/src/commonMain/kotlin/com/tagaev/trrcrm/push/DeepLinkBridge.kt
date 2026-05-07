package com.tagaev.trrcrm.push

import com.tagaev.trrcrm.ui.root.IRootComponent

object DeepLinkBridge {
    private var root: IRootComponent? = null

    fun setRoot(component: IRootComponent) {
        root = component
    }

    fun handle(screen: String, docId: String?, messageText: String?, title: String?) {
        root?.onDeepLink(screen, docId, messageText, title)
    }
}
