package com.tagaev.mobileagregatcrm.domain

import kotlinx.serialization.Serializable


interface ApiOption {
    val label: String   // что показываем в UI
    val wire: String    // что отправляем в API
}

sealed interface Refiner : ApiOption {
    enum class OrderBy(
        override val label: String,
        override val wire: String
    ) : Refiner {
        DATE("Дата", "Дата"),
        DATE_LAST_MODIFICATION("Дата мод.", "ДатаМод"),
        KIND("Вид события", "ВидСобытия"),
        STATE("Состояние", "Состояние"),
        NUMBER("Номер", "Номер"),

        OFF("Без сортировки", ""),
    }

    enum class Dir(
        override val label: String,
        override val wire: String
    ) : Refiner {
        ASC("По возрастанию", "asc"),
        DESC("По убыванию", "desc"),
    }

    //ACTIVE("Активные","активно"),
    //    NO_ACTIVE("Не Активные","неактивно"),
    //    ALL("Все","Все");
    enum class Filter(
        override val label: String,
        override val wire: String
    ) : Refiner {
        ACTIVE("Активные", "активно"),
        DONE("Не Активные", "неактивно"),
        OFF("Все", ""),
    }
}

@Serializable
data class WorkOrderRefineState(
    val orderBy: Refiner.OrderBy = Refiner.OrderBy.DATE_LAST_MODIFICATION,
    val orderDir: Refiner.Dir = Refiner.Dir.DESC,
    val filter: Refiner.Filter = Refiner.Filter.OFF,
    val searchQuery: String = ""
) {
    companion object {
        val Default = WorkOrderRefineState()
    }
}

