package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakBegrunnelseType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import java.time.LocalDate

data class VedtaksperiodeMedFriteksterDto(
    val fritekster: List<String> = emptyList()
)

data class VedtaksperiodeMedStandardbegrunnelserDto(
    val standardbegrunnelser: List<String>
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
    val standardbegrunnelse: String,
    val vedtakBegrunnelseSpesifikasjon: String,
    val vedtakBegrunnelseType: VedtakBegrunnelseType
)

fun Vedtaksbegrunnelse.tilRestVedtaksbegrunnelse() = VedtaksbegrunnelseDto(
    standardbegrunnelse = this.standardbegrunnelse.enumnavnTilString(),
    vedtakBegrunnelseType = this.standardbegrunnelse.vedtakBegrunnelseType,
    vedtakBegrunnelseSpesifikasjon = this.standardbegrunnelse.enumnavnTilString()
)
