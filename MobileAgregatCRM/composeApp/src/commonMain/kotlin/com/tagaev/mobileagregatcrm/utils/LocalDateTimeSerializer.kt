package org.agregatcrm.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(FormatStringsInDatetimeFormats::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val format = LocalDateTime.Format {
        dayOfMonth(); char('.'); monthNumber(); char('.'); year()
        char(' '); hour(); char(':'); minute(); char(':'); second()
    }

    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(format))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), format)
    }
}