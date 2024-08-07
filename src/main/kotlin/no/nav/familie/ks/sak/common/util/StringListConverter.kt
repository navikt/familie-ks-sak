package no.nav.familie.ks.sak.common.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(stringList: List<String>): String = stringList.joinToString(separator = SPLIT_CHAR)

    override fun convertToEntityAttribute(string: String?): List<String> = if (string.isNullOrBlank()) emptyList() else string.split(SPLIT_CHAR)

    companion object {
        private const val SPLIT_CHAR = ";"
    }
}
