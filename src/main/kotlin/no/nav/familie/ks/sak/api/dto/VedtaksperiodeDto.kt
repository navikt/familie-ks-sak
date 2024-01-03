package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.støtterFritekst
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
    val skalGenererePerioderForFortsattInnvilget: Boolean,
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
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(
    sanityBegrunnelser: List<SanityBegrunnelse>,
): UtvidetVedtaksperiodeMedBegrunnelserDto {
    return UtvidetVedtaksperiodeMedBegrunnelserDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilVedtaksbegrunnelseDto(sanityBegrunnelser) },
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer.map { it.tilUtbetalingsperiodeDetaljDto() },
        gyldigeBegrunnelser = this.gyldigeBegrunnelser.map { it.enumnavnTilString() },
        eøsBegrunnelser = this.eøsBegrunnelser.map { it.tilEøsBegrunnelseDto(sanityBegrunnelser) }
    )
}

data class BegrunnelseDto(
    val begrunnelse: String,
    val begrunnelseType: BegrunnelseType,
    val støtterFritekst: Boolean,
)

fun NasjonalEllerFellesBegrunnelseDB.tilVedtaksbegrunnelseDto(sanityBegrunnelser: List<SanityBegrunnelse>) =
    BegrunnelseDto(
        begrunnelse = nasjonalEllerFellesBegrunnelse.enumnavnTilString(),
        begrunnelseType = nasjonalEllerFellesBegrunnelse.begrunnelseType,
        støtterFritekst = nasjonalEllerFellesBegrunnelse.støtterFritekst(sanityBegrunnelser),
    )

fun EØSBegrunnelseDB.tilEøsBegrunnelseDto(sanityBegrunnelser: List<SanityBegrunnelse>) =
    BegrunnelseDto(
        begrunnelse = this.begrunnelse.enumnavnTilString(),
        begrunnelseType = this.begrunnelse.begrunnelseType,
        støtterFritekst = this.begrunnelse.støtterFritekst(sanityBegrunnelser),
    )

