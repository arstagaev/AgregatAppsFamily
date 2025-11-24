package com.tagaev.data.models.qrscanner


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QRResponseTRS(
    @SerialName("ГарантийныйНомер")
    val warrantyNumber: String,
    @SerialName("Гравер")
    val graver: String,
    @SerialName("Дата")
    val date: String,
    @SerialName("Комментарий")
    val comment: String,
    @SerialName("Комплектация")
    val completion: String,
    @SerialName("КомплектацияДата")
    val completionDate: String,
    @SerialName("КомплектацияНомер")
    val completionNumber: String,
    @SerialName("Номенклатура")
    val nomenclature: String,
    @SerialName("Подразделение")
    val department: String,
    @SerialName("СрокГарантии")
    val warrantyPeriod: String,
    @SerialName("Статус")
    val status: String,
    @SerialName("ХарактеристикаНоменклатуры")
    val characteristicNomenclature: String
)