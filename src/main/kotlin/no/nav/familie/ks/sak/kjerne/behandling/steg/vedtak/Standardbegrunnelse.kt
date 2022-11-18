package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import no.nav.familie.ks.sak.common.util.konverterEnumsTilString
import no.nav.familie.ks.sak.common.util.konverterStringTilEnums
import javax.persistence.AttributeConverter
import javax.persistence.Converter

interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType

    fun enumnavnTilString(): String
}

enum class Standardbegrunnelse : IVedtakBegrunnelse {
    INNVILGET_IKKE_BARNEHAGE {
        override val sanityApiNavn = "innvilgetIkkeBarnehage"
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
    },
    INNVILGET_IKKE_BARNEHAGE_ADOPSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetIkkeBarnehageAdopsjon"
    },
    INNVILGET_DELTID_BARNEHAGE {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehage"
    },
    INNVILGET_DELTID_BARNEHAGE_ADOPSJON {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET
        override val sanityApiNavn = "innvilgetDeltidBarnehageAdopsjon"
    },
    AVSLAG_UREGISTRERT_BARN {
        override val vedtakBegrunnelseType = VedtakBegrunnelseType.AVSLAG
        override val sanityApiNavn = "avslagUregistrertBarn"
    };
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

    // TODO: Legg til standardbegrunnelser
)
