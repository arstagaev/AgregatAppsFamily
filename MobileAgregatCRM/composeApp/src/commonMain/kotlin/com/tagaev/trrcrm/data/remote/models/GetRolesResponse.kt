package com.tagaev.trrcrm.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetRolesResponse(
    @SerialName("roles")
    val roles: List<String>,
    @SerialName("Пользователь")
    val пользователь: String? = null,
    @SerialName("ПользовательGUID")
    val пользовательGUID: String? = null,
    @SerialName("Сотрудник")
    val сотрудник: String? = null,
    @SerialName("СотрудникGUID")
    val сотрудникGUID: String? = null
)

@Serializable
enum class UserRole(val shortTitle: String) {

    @SerialName("Полные права")
    FULL_ACCESS("Полные права"),

    @SerialName("Минимальный набор прав")
    MINIMAL_ACCESS("Минимальный доступ"),

    @SerialName("Обновление обмен ИФЗ")
    IFZ_SYNC("Обновление ИФЗ"),

    @SerialName("Пользователь")
    USER("Пользователь"),

    @SerialName("Пользователь1")
    USER1("Пользователь 1"),

    @SerialName("Создание пользователя")
    CREATE_USER("Создание пользователя"),

    @SerialName("Полные права laximo")
    LAXIMO_FULL("Полные права Laximo"),

    @SerialName("Бухгалтер")
    ACCOUNTANT("Бухгалтер"),

    @SerialName("Добавление и изменение номенклатуры")
    EDIT_NOMENCLATURE("Номенклатура"),

    @SerialName("Добавление и изменение типы КПП")
    EDIT_GEARBOX_TYPES("Типы КПП (ред.)"),

    @SerialName("Замена масла")
    OIL_CHANGE("Замена масла"),

    @SerialName("Контрагенты документы")
    CONTRACTOR_DOCS("Документы контрагентов"),

    @SerialName("Подтверждено расходы ДС")
    CASH_EXPENSES_APPROVED("Расходы ДС"),

    @SerialName("Автоработы")
    CAR_WORKS("Автоработы"),

    @SerialName("Проблемы ИТ")
    IT_ISSUES("Проблемы ИТ"),

    @SerialName("РКО оплата поставщику")
    SUPPLIER_PAYMENT("РКО / оплата"),

    @SerialName("Обработка вер1")
    PROCESSING_V1("Обработка v1"),

    @SerialName("Управление просрочками")
    OVERDUE_CONTROL("Просрочки"),

    @SerialName("Инкасация оборот денег")
    CASH_COLLECTION("Инкасация"),

    @SerialName("Инструктор")
    INSTRUCTOR("Инструктор"),

    @SerialName("Типы КПП")
    GEARBOX_TYPES("Типы КПП"),

    @SerialName("Тех помощь")
    TECH_SUPPORT("Тех. помощь"),

    @SerialName("Ошибки")
    ERRORS("Ошибки"),

    @SerialName("Все события")
    ALL_EVENTS("Все события"),

    @SerialName("Доступ ко всем реализациям")
    ALL_SALES_ACCESS("Все реализации"),

    @SerialName("Добавление запрещенного в ЗН")
    FORBIDDEN_ITEMS("Запрещённое в ЗН"),

    @SerialName("Отчеты учредителей")
    OWNER_REPORTS("Отчёты учредителей"),

    @SerialName("Согласование планов филиал")
    BRANCH_PLAN_APPROVAL("Планы (филиал)"),

    @SerialName("Согласование планов закупа УК")
    HQ_PURCHASE_PLAN_APPROVAL("Планы закупа УК"),

    @SerialName("Согласование планов закупа филиал")
    BRANCH_PURCHASE_PLAN_APPROVAL("Планы закупа филиал"),
}
