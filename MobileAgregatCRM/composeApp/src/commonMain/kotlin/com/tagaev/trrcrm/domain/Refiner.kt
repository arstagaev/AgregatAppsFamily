package com.tagaev.trrcrm.domain

import kotlinx.serialization.Serializable


interface ApiOption {
    val label: String   // что показываем в UI
    val wire: String    // что отправляем в API
}

// OrderBy Dir Status SearchQueryType Filter
sealed interface Refiner : ApiOption {
    enum class WorkOrderRepairType(
        override val label: String,
        override val wire: String,
    ) : Refiner {
        OFF("Без фильтра", ""),
        FREE_DIAGNOSTIC("Бесплатная диагностика", "Бесплатная"),
        FREE_REPAIR("Бесплатный ремонт", "Бесплатный"),
        WARRANTY_REPAIR("Гарантийный ремонт", "Гарантийный"),
        WARRANTY_REPAIR_NETWORK("Гарантийный ремонт (Сеть)", "Сеть"),
        WARRANTY_TRS("Гарантия (TRS)", "TRS"),
        WARRANTY_TRS_NETWORK("Гарантия (TRS) (СЕТЬ)", "TRSСЕТЬ"),
        DIAGNOSTIC("Диагностика", "Диагностика"),
        UNIT_REPLACEMENT("Замена агрегата", "агрегата"),
        FLUID_REPLACEMENT_FULL("Замена жидкости (ПОЛНАЯ)", "ПОЛНАЯ"),
        FLUID_REPLACEMENT_PARTIAL("Замена жидкости (ЧАСТИЧНАЯ)", "ЧАСТИЧНАЯ"),
        PARTS_REPLACEMENT("Замена запасных частей", "запасных"),
        VEHICLE_COMPLECTATION("Комплектация автомобиля", "Комплектация"),
        MINOR_REPAIR("Мелкосрочный ремонт", "Мелкосрочный"),
        TO_STOCK("На склад", "склад"),
        PROGRAMMING("Программирование", "Программирование"),
        TORQUE_CONVERTER_REPAIR("Ремонт гидротрансформатора", "гидротрансформатора"),
        UNITS_REPAIR("Ремонт узлов и агрегатов", "узлов"),
        T01("T01", "T01"),
        T02("T02", "T02");

        companion object {
            const val API_FIELD: String = "ВидРемонта"
        }
    }

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
    enum class Status(
        override val label: String,
        override val wire: String
    ) : Refiner {
        ACTIVE("Активные", "активно"),
        DONE("Не Активные", "неактивно"),
        OFF("Все", ""),
    }

    enum class SearchQueryType(
        override val label: String,
        override val wire: String
    ) : Refiner {
        TOPIC("По теме события","Тема"),
        CODE("По номеру события/ЗН","Номер"),
        AUTHOR("По автору", "Автор"),
        MANAGER("По менеджеру", "Менеджер"),
        COUNTERPARTY("По контрагенту", "Контрагент"),
        MASTER("По мастеру", "Мастер"),
        KIT_CHARACTERISTIC("По характеристике комплекта", "ХарактеристикаКомплекта"),
        AUTO("По автомобилю в ЗН", "Автомобиль"),
        LICENSE_PLATE("По гос. номеру в ЗН", "АвтомобильГосномер"),
        VIN_NUMBER("По VIN в ЗН", "АвтомобильВин"),
        FIX_TYPE("По виду ремонта в ЗН", "ВидРемонта"),
        CLIENT("По заказчику (клиенту) в ЗН", "Заказчик"),
        ROUTE("По маршруту", "Маршрут"),
        CARRIER("По грузоперевозчику", "Грузоперевозчик"),
        /** Документ «ВходящиеЗаявки» и др. с реквизитом «СутьОбращения». */
        SUBJECT_MATTER("По сути обращения", "СутьОбращения"),
        /** Телефон клиента (реквизит «Телефон»). */
        PHONE("По телефону", "Телефон"),
        /** Справочник «ШаблоныРемонта» (калькуляция). */
        REPAIR_TEMPLATE_MODEL("Модель", "Модель"),
        REPAIR_TEMPLATE_NAME("Наименование", "Наименование"),
        REPAIR_TEMPLATE_CODE("Код", "Код"),
        REPAIR_TEMPLATE_YEAR_FROM("Год от", "ГодОт"),
        REPAIR_TEMPLATE_YEAR_TO("Год до", "ГодДо"),
        REPAIR_TEMPLATE_TRANSMISSION("Тип КПП", "ТипКПП"),
        REPAIR_TEMPLATE_ENGINE("ДВС", "ДВС"),
        REPAIR_TEMPLATE_REPAIR_KIND("Вид ремонта", "ВидРемонта"),
        ;

        companion object {
            /** Реквизиты `filterby` для справочника «ШаблоныРемонта» (экран Калькуляция). */
            val repairTemplateCatalogSearchTypes: Set<SearchQueryType> = setOf(
                REPAIR_TEMPLATE_MODEL,
                REPAIR_TEMPLATE_NAME,
                REPAIR_TEMPLATE_CODE,
                REPAIR_TEMPLATE_YEAR_FROM,
                REPAIR_TEMPLATE_YEAR_TO,
                REPAIR_TEMPLATE_TRANSMISSION,
                REPAIR_TEMPLATE_ENGINE,
                REPAIR_TEMPLATE_REPAIR_KIND,
            )
        }
    }

    enum class Filter(
        override val label: String,
        override val wire: String
    ) : Refiner {
        DEPARTMENT("Подразделение","ПодразделениеКомпании"),

        OFF("Без фильтра","")
    }
}

@Serializable
data class RefineState(
    val orderBy: Refiner.OrderBy = Refiner.OrderBy.DATE_LAST_MODIFICATION,

    val orderDir: Refiner.Dir = Refiner.Dir.DESC,

    val status: Refiner.Status = Refiner.Status.OFF,

    val filter: Refiner.Filter = Refiner.Filter.OFF,
    val filterValue: String = "",

    val repairType: Refiner.WorkOrderRepairType = Refiner.WorkOrderRepairType.OFF,

    val searchQuery: String = "",
    val searchQueryType: Refiner.SearchQueryType = Refiner.SearchQueryType.TOPIC
) {
    companion object Companion {
        val Default = RefineState()
    }
}

