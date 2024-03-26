package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import java.time.LocalDate

data class VedtaksperiodeMedFriteksterDto(
    val fritekster: List<String> = emptyList(),
)

data class VedtaksperiodeMedBegrunnelserDto(
    val begrunnelser: List<String>,
)

data class GenererVedtaksperioderForOverstyrtEndringstidspunktDto(
    val behandlingId: Long,
    val overstyrtEndringstidspunkt: LocalDate,
)

data class GenererFortsattInnvilgetVedtaksperioderDto(
    val behandlingId: Long,
)

data class UtvidetVedtaksperiodeMedBegrunnelserDto(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<BegrunnelseDto>,
    val eøsBegrunnelser: List<BegrunnelseDto>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<String>,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljDto> = emptyList(),
    val støtterFritekst: Boolean,
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(): UtvidetVedtaksperiodeMedBegrunnelserDto {
    return UtvidetVedtaksperiodeMedBegrunnelserDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilVedtaksbegrunnelseDto() },
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilUtbetalingsperiodeDetaljDto() },
        gyldigeBegrunnelser = this.gyldigeBegrunnelser.map { it.enumnavnTilString() },
        eøsBegrunnelser = this.eøsBegrunnelser.map { it.tilEøsBegrunnelseDto() },
        støtterFritekst = this.støtterFritekst,
    )
}

data class BegrunnelseDto(
    val begrunnelse: String,
    val begrunnelseType: BegrunnelseType,
)

fun NasjonalEllerFellesBegrunnelseDB.tilVedtaksbegrunnelseDto() =
    BegrunnelseDto(
        begrunnelse = nasjonalEllerFellesBegrunnelse.enumnavnTilString(),
        begrunnelseType = nasjonalEllerFellesBegrunnelse.begrunnelseType,
    )

fun EØSBegrunnelseDB.tilEøsBegrunnelseDto() =
    BegrunnelseDto(
        begrunnelse = this.begrunnelse.enumnavnTilString(),
        begrunnelseType = this.begrunnelse.begrunnelseType,
    )
