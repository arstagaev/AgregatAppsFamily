package com.tagaev.mobileagregatcrm.utils

import androidx.compose.runtime.mutableStateOf
import com.tagaev.mobileagregatcrm.feature.OrderByOption
import com.tagaev.mobileagregatcrm.feature.OrderDirOption
import org.agregatcrm.models.EventItemDto


data class RequestConfig(
    var count: Int = 20,
    var ncount: Int = 0,

    var filterBy: String = DefaultConfig.FILTER_BY,
    var filterVal: String = DefaultConfig.FILTER_VAL,

    var orderBy: OrderByOption = OrderByOption.DATE,
    var orderDir: OrderDirOption = OrderDirOption.ASC,
    var lastSuccessfulUpdate: String = ""
)

object DefaultConfig {
    const val FILTER_BY = "ПодразделениеКомпании"
    const val FILTER_VAL = "Сочи"
}

object CONST {
    const val BASE_URL = "https://agrapp.agregatka.ru/"
}

var requestEventsList = mutableStateOf(RequestConfig())

var requestEventDetails = mutableStateOf(RequestConfig())

var TARGET_EVENT = mutableStateOf(EventItemDto())