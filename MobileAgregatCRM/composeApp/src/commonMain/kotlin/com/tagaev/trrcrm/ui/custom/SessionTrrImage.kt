package com.tagaev.trrcrm.ui.custom

import mobileagregatcrm.composeapp.generated.resources.Res
import mobileagregatcrm.composeapp.generated.resources.trr_nfs1
import mobileagregatcrm.composeapp.generated.resources.trr_nfs1999
import mobileagregatcrm.composeapp.generated.resources.trr_vicecity
import mobileagregatcrm.composeapp.generated.resources.trr_vicecity2
import org.jetbrains.compose.resources.DrawableResource

object SessionTrrImage {
    private val pool = listOf(
        Res.drawable.trr_vicecity2,
        Res.drawable.trr_vicecity,
        Res.drawable.trr_nfs1999,
        Res.drawable.trr_nfs1
    )

    private var selected: DrawableResource? = null

    fun get(): DrawableResource {
        return selected ?: pool.random().also { selected = it }
    }
}

