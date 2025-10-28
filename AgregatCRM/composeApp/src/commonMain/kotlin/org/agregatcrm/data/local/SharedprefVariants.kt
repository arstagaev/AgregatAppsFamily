package org.agregatcrm.data.local

enum class SharedprefVariants(
    val key: String,
    val defaultValue: Any
) {
    FILTER_COUNT(key = "FILTER_COUNT", defaultValue = 5),
    FILTER_N_COUNT(key = "FILTER_N_COUNT", defaultValue = 0),
    FILTER_BY(key = "FILTER_BY", defaultValue = "ПодразделениеКомпании"),
    FILTER_VAL(key = "FILTER_VAL", defaultValue = "Казань"),

    ORDER_BY(key = "ORDER_BY", defaultValue = "Дата"),
    ORDER_DIR(key = "ORDER_DIR", defaultValue = "desc"),

    isSHOW_TOP_CONTROLS(key = "isSHOW_TOP_CONTROLS", defaultValue = true),
}
