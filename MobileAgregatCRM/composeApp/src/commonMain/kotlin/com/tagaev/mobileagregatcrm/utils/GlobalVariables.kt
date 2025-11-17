package com.tagaev.mobileagregatcrm.utils

import androidx.compose.runtime.mutableStateOf
import com.tagaev.mobileagregatcrm.feature.OrderByOption
import com.tagaev.mobileagregatcrm.feature.OrderDirOption
import com.tagaev.mobileagregatcrm.models.EventItemDto


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

//object CONST {
////    const val BASE_URL = "https://agrapp.agregatka.ru/"
////    var TOKEN = "95AA8A6F209270E6BA02F21BEAE4A2BC75B192A815707D42AFF3E0862CD82898"
//    const val VERSION = "1.4.5"
//}

//var requestEventsList = mutableStateOf(RequestConfig())
//
//var requestEventDetails = mutableStateOf(RequestConfig())

var TARGET_EVENT = mutableStateOf(EventItemDto())