package org.agregatcrm.utils

import androidx.compose.runtime.mutableStateOf
import org.agregatcrm.feature.OrderByOption
import org.agregatcrm.feature.OrderDirOption
import org.agregatcrm.models.EventItemDto


data class RequestConfig(
    var count: Int = 20,
    var ncount: Int = 0,

    var filterBy: String = "ПодразделениеКомпании",
    var filterVal: String = "Казань",

    var orderBy: OrderByOption = OrderByOption.DATE,
    var orderDir: OrderDirOption = OrderDirOption.ASC,
    var lastSuccessfulUpdate: String = ""
)

object CONST {
    const val BASE_URL = "https://agrapp.agregatka.ru/"
}

var requestEventsList = mutableStateOf(RequestConfig())

var requestEventDetails = mutableStateOf(RequestConfig())

enum class CurrentScreen { List, Details, Favorites }
var CURRENT_SCREEN = mutableStateOf(CurrentScreen.List)

var TARGET_EVENT = mutableStateOf(EventItemDto())