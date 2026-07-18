package com.tagaev.trrcrm.domain

import com.tagaev.trrcrm.ui.i18n.tr

/** Localized UI label for refine options; keeps API [ApiOption.wire]/[ApiOption.label] unchanged. */
fun ApiOption.localizedLabel(): String {
    val key = when (label) {
        "Без фильтра" -> "filter_bez_filtra"
        "Бесплатная диагностика" -> "filter_besplatnaya_diagnostika"
        "Бесплатный ремонт" -> "filter_besplatnyy_remont"
        "Гарантийный ремонт" -> "filter_garantiynyy_remont"
        "Гарантийный ремонт (Сеть)" -> "filter_garantiynyy_remont_set"
        "Гарантия (TRS)" -> "filter_garantiya_trs"
        "Гарантия (TRS) (СЕТЬ)" -> "filter_garantiya_trs_set"
        "Диагностика" -> "filter_diagnostika"
        "Замена агрегата" -> "filter_zamena_agregata"
        "Замена жидкости (ПОЛНАЯ)" -> "filter_zamena_zhidkosti_polnaya"
        "Замена жидкости (ЧАСТИЧНАЯ)" -> "filter_zamena_zhidkosti_chastichnaya"
        "Замена запасных частей" -> "filter_zamena_zapasnyh_chastey"
        "Комплектация автомобиля" -> "filter_komplektatsiya_avtomobilya"
        "Мелкосрочный ремонт" -> "filter_melkosrochnyy_remont"
        "На склад" -> "filter_na_sklad"
        "Программирование" -> "filter_programmirovanie"
        "Ремонт гидротрансформатора" -> "filter_remont_gidrotransformatora"
        "Ремонт узлов и агрегатов" -> "filter_remont_uzlov_i_agregatov"
        "Дата" -> "filter_data"
        "Дата мод." -> "filter_data_mod"
        "Вид события" -> "events_vid_sobytiya"
        "Состояние" -> "work_order_sostoyanie"
        "Номер" -> "filter_nomer"
        "Без сортировки" -> "filter_bez_sortirovki"
        "По возрастанию" -> "filter_po_vozrastaniyu"
        "По убыванию" -> "filter_po_ubyvaniyu"
        "Активные" -> "filter_aktivnye"
        "Не Активные" -> "filter_ne_aktivnye"
        "Все" -> "feed_vse"
        "По теме события" -> "filter_po_teme_sobytiya"
        "По назначению" -> "filter_po_naznacheniyu"
        "По номеру события/ЗН" -> "filter_po_nomeru_sobytiya_zn"
        "По автору" -> "filter_po_avtoru"
        "По менеджеру" -> "filter_po_menedzheru"
        "По контрагенту" -> "filter_po_kontragentu"
        "По мастеру" -> "complectation_po_masteru"
        "Подразделение" -> "events_podrazdelenie"
        else -> null
    }
    return key?.let { tr(it) } ?: label
}
