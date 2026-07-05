package com.tagaev.trrcrm.navigation

import kotlinx.serialization.Serializable

@Serializable
data class BottomNavLayoutItem(
    val id: String,
    val visible: Boolean = true,
)
