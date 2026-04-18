package com.tagaev.trrcrm.domain

/**
 * Строка поиска по [Refiner.SearchQueryType.KIT_CHARACTERISTIC] (ХарактеристикаКомплекта) в getitemslist.
 * Пример: из «с/н ЦБ153214» → «ЦБ153214».
 */
fun complectationSearchTokenFromNomenclatureCharacteristic(raw: String): String {
    var s = raw.trim()
    if (s.isEmpty()) return ""
    s = s.replaceFirst(cyrillicSnPrefix, "")
    s = s.replaceFirst(cyrillicSnDashPrefix, "")
    return s.trim()
}

private val cyrillicSnPrefix = Regex("""^(?u)(?i)с\s*[/\u002f]\s*н\s*""")
private val cyrillicSnDashPrefix = Regex("""^(?u)(?i)с\s*-\s*н\s*""")
