package com.tagaev.mobileagregatcrm.domain

enum class UserRole(
    val ru: String,
    val en: String
) {
    FULL_ACCESS(
        ru = "Полные права",
        en = "Full access"
    ),

    MINIMAL_ACCESS(
        ru = "Минимальный набор прав",
        en = "Minimal rights"
    ),

    IFZ_SYNC_UPDATE(
        ru = "Обновление обмен ИФЗ",
        en = "IFZ sync update"
    ),

    USER(
        ru = "Пользователь",
        en = "User"
    ),

    USER1(
        ru = "Пользователь1",
        en = "User 1"
    ),

    USER_CREATE(
        ru = "Создание пользователя",
        en = "User create"
    ),

    LAXIMO_FULL(
        ru = "Полные права laximo",
        en = "Laximo full"
    ),

    ACCOUNTANT(
        ru = "Бухгалтер",
        en = "Accountant"
    ),

    ITEM_EDIT(
        ru = "Добавление и изменение номенклатуры",
        en = "Item edit"
    ),

    GEARBOX_TYPE_EDIT(
        ru = "Добавление и изменение типы КПП",
        en = "Gearbox types edit"
    ),

    OIL_CHANGE(
        ru = "Замена масла",
        en = "Oil change"
    ),

    COUNTERPARTY_DOCS(
        ru = "Контрагенты документы",
        en = "Counterparty docs"
    ),

    CASH_EXPENSE_APPROVE(
        ru = "Подтверждено расходы ДС",
        en = "Cash expense approve"
    ),

    AUTO_WORKS(
        ru = "Автоработы",
        en = "Auto works"
    ),

    IT_ISSUES(
        ru = "Проблемы ИТ",
        en = "IT issues"
    ),

    RKO_SUPPLIER_PAY(
        ru = "РКО оплата поставщику",
        en = "RKO supplier pay"
    ),

    PROCESSING_V1(
        ru = "Обработка вер1",
        en = "Processing v1"
    ),

    OVERDUE_MANAGEMENT(
        ru = "Управление просрочками",
        en = "Overdue mgmt"
    ),

    CASH_COLLECTION_FLOW(
        ru = "Инкасация оборот денег",
        en = "Cash collection"
    ),

    INSTRUCTOR(
        ru = "Инструктор",
        en = "Instructor"
    ),

    GEARBOX_TYPES_VIEW(
        ru = "Типы КПП",
        en = "Gearbox types"
    ),

    TECH_SUPPORT(
        ru = "Тех помощь",
        en = "Tech support"
    ),

    ERRORS(
        ru = "Ошибки",
        en = "Errors"
    ),

    ALL_EVENTS(
        ru = "Все события",
        en = "All events"
    ),

    ALL_SALES_ACCESS(
        ru = "Доступ ко всем реализациям",
        en = "All sales access"
    ),

    ZN_FORBIDDEN_ADD(
        ru = "Добавление запрещенного в ЗН",
        en = "ZN forbidden add"
    ),

    FOUNDERS_REPORTS(
        ru = "Отчеты учредителей",
        en = "Owners reports"
    ),

    BRANCH_PLAN_APPROVAL(
        ru = "Согласование планов филиал",
        en = "Branch plan approve"
    ),

    HQ_PURCHASE_PLAN_APPROVAL(
        ru = "Согласование планов закупа УК",
        en = "HQ purchase approve"
    ),

    BRANCH_PURCHASE_PLAN_APPROVAL(
        ru = "Согласование планов закупа филиал",
        en = "Branch purchase approve"
    );
}