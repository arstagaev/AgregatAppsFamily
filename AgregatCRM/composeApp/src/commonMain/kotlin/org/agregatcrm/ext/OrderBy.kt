package org.agregatcrm.ext

import org.agregatcrm.feature.OrderByOption
import org.agregatcrm.feature.OrderDirOption

fun String?.toOrderByOption(default: OrderByOption = OrderByOption.DATE): OrderByOption {
    val v = this?.trim()
    return OrderByOption.values().firstOrNull {
        it.wire.equals(v, ignoreCase = true) || it.label.equals(v, ignoreCase = true)
    } ?: default
}

fun String?.toOrderDirOption(default: OrderDirOption = OrderDirOption.DESC): OrderDirOption {
    val v = this?.trim()
    return OrderDirOption.values().firstOrNull {
        it.wire.equals(v, ignoreCase = true) || it.label.equals(v, ignoreCase = true)
    } ?: default
}

// ---------- Enum → String ----------
fun OrderByOption.toWireString(): String = this.wire
fun OrderByOption.toLabelString(): String = this.label

fun OrderDirOption.toWireString(): String = this.wire
fun OrderDirOption.toLabelString(): String = this.label