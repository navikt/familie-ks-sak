package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import java.time.LocalDate

data class VedtaksperiodeMedFriteksterDto(
    val fritekster: List<String> = emptyList()
)

data class VedtaksperiodeMedBegrunnelserDto(
    val begrunnelser: List<String>
)

data class GenererVedtaksperioderForOverstyrtEndringstidspunktDto(
    val behandlingId: Long,
    val overstyrtEndringstidspunkt: LocalDate
)

data class GenererFortsattInnvilgetVedtaksperioderDto(
    val skalGenererePerioderForFortsattInnvilget: Boolean,
    val behandlingId: Long
)

data class UtvidetVedtaksperiodeMedBegrunnelserDto(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<VedtaksbegrunnelseDto>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<String>,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljDto> = emptyList()
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(): UtvidetVedtaksperiodeMedBegrunnelserDto {
    return UtvidetVedtaksperiodeMedBegrunnelserDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse() },
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilUtbetalingsperiodeDetaljDto() },
        gyldigeBegrunnelser = this.gyldigeBegrunnelser.map { it.enumnavnTilString() }
    )
}

data class VedtaksbegrunnelseDto(
    val begrunnelse: String,
    val vedtakBegrunnelseSpesifikasjon: String,
    val begrunnelseType: BegrunnelseType
)

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() = VedtaksbegrunnelseDto(
    begrunnelse = this.begrunnelse.enumnavnTilString(),
    begrunnelseType = this.begrunnelse.begrunnelseType,
    vedtakBegrunnelseSpesifikasjon = this.begrunnelse.enumnavnTilString()
)
