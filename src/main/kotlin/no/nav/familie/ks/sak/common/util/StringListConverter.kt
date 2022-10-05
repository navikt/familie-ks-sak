package no.nav.familie.ks.sak.common.util

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(stringList: List<String>): String {
        return stringList.joinToString(separator = SPLIT_CHAR)
    }

    override fun convertToEntityAttribute(string: String?): List<String> {
        return if (string.isNullOrBlank()) emptyList() else string.split(SPLIT_CHAR)
    }

    companion object {

        private const val SPLIT_CHAR = ";"
    }
}
