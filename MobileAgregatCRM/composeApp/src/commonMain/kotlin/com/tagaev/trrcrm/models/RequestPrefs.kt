package org.agregatcrm.models

data class RequestPrefs(
    val count: Int = 20,
    val ncount: Int = 0,
    val filterBy: String = "",
    val filterVal: String = ""
)