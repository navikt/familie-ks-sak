package no.nav.familie.ks.sak.kjerne.vedtak

import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType
    val kanDelesOpp: Boolean

    fun enumnavnTilString(): String
}

enum class Standardbegrunnelse : IVedtakBegrunnelse

@Converter
class StandardbegrunnelseListConverter :
    AttributeConverter<List<Standardbegrunnelse>, String> {

    override fun convertToDatabaseColumn(standardbegrunnelser: List<Standardbegrunnelse>) =
        konverterEnumsTilString(standardbegrunnelser)

    override fun convertToEntityAttribute(string: String?): List<Standardbegrunnelse> =
        konverterStringTilEnums(string)
}
