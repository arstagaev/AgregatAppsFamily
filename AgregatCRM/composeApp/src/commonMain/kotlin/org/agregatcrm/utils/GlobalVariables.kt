package org.agregatcrm.utils

import androidx.compose.runtime.mutableStateOf
import org.agregatcrm.feature.OrderByOption
import org.agregatcrm.feature.OrderDirOption


data class RequestConfig(
    var count: Int = 5,
    var ncount: Int = 50,

    var filterBy: String = "ПодразделениеКомпании",
    var filterVal: String = "Воронеж",

    var orderBy: OrderByOption = OrderByOption.DATE,
    var orderDir: OrderDirOption = OrderDirOption.ASC,
    var lastSuccessfulUpdate: String = ""
)

var requestEventsList = mutableStateOf(RequestConfig())

var requestEventDetails = mutableStateOf(RequestConfig())
