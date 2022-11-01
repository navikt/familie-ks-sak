package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

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

// TODO: Må legge inn faktiske begrunnelser vi skal ha her
enum class Standardbegrunnelse : IVedtakBegrunnelse {
    DUMMY {
        override val sanityApiNavn = "dummyApiNavn"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
    },
    AVSLAG_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    };


    override val kanDelesOpp = false
    override fun enumnavnTilString() = this.name
}

@Converter
class StandardbegrunnelseListConverter :
    AttributeConverter<List<Standardbegrunnelse>, String> {

    override fun convertToDatabaseColumn(standardbegrunnelser: List<Standardbegrunnelse>) =
        konverterEnumsTilString(standardbegrunnelser)

    override fun convertToEntityAttribute(string: String?): List<Standardbegrunnelse> =
        konverterStringTilEnums(string)
}

val endretUtbetalingsperiodeBegrunnelser: List<Standardbegrunnelse> = listOf(

    //TODO: Legg til standardbegrunnelser
)
