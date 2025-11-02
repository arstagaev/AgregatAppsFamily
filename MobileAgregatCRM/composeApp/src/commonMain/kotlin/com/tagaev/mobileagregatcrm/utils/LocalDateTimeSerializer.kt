package com.tagaev.mobileagregatcrm.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor

@OptIn(FormatStringsInDatetimeFormats::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTimeRU", PrimitiveKind.STRING)

    // Парсер, допускающий однозначный час (H)
    private val parseH = LocalDateTime.Format {
        byUnicodePattern("dd.MM.uuuu H:mm:ss")
    }

    // Фолбэк: строго двухзначный час (HH)
    private val parseHH = LocalDateTime.Format {
        byUnicodePattern("dd.MM.uuuu HH:mm:ss")
    }

    // Печать всегда в едином виде с ведущими нулями
    private val printHH = LocalDateTime.Format {
        byUnicodePattern("dd.MM.uuuu HH:mm:ss")
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val raw = decoder.decodeString().trim()
        require(raw.isNotEmpty()) { "Empty date string" }

        return try {
            LocalDateTime.parse(raw, parseH)
        } catch (_: Throwable) {
            try {
                LocalDateTime.parse(raw, parseHH)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Failed to parse LocalDateTime from '$raw'", e)
            }
        }
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(printHH))
    }
}

//@OptIn(FormatStringsInDatetimeFormats::class)
//object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
//    private val format = LocalDateTime.Format {
//        dayOfMonth(); char('.'); monthNumber(); char('.'); year()
//        char(' '); hour(); char(':'); minute(); char(':'); second()
//    }
//
//    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
//
//    override fun serialize(encoder: Encoder, value: LocalDateTime) {
//        encoder.encodeString(value.format(format))
//    }
//
//    override fun deserialize(decoder: Decoder): LocalDateTime {
//        return LocalDateTime.parse(decoder.decodeString(), format)
//    }
//}