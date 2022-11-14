package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vedtaksperiodetype")
@JsonSubTypes(
    JsonSubTypes.Type(value = Utbetalingsperiode::class, name = "UTBETALING"),
    JsonSubTypes.Type(value = Avslagsperiode::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = Opphørsperiode::class, name = "OPPHØR")
)
interface Vedtaksperiode {

    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype(val tillatteBegrunnelsestyper: List<VedtakBegrunnelseType>) {
    UTBETALING(
        listOf(
            VedtakBegrunnelseType.INNVILGET,
            VedtakBegrunnelseType.REDUKSJON,
            VedtakBegrunnelseType.FORTSATT_INNVILGET,
            VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING,
            VedtakBegrunnelseType.ENDRET_UTBETALING
        )
    ),
    OPPHØR(listOf(VedtakBegrunnelseType.OPPHØR, VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING)),
    AVSLAG(listOf(VedtakBegrunnelseType.AVSLAG)),
    FORTSATT_INNVILGET(listOf(VedtakBegrunnelseType.FORTSATT_INNVILGET))
}

fun Vedtaksperiode.tilVedtaksperiodeMedBegrunnelse(
    vedtak: Vedtak
): VedtaksperiodeMedBegrunnelser =
    VedtaksperiodeMedBegrunnelser(
        fom = this.periodeFom,
        tom = this.periodeTom,
        vedtak = vedtak,
        type = this.vedtaksperiodetype
    )
