package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.IVedtakBegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<Vedtaksbegrunnelse>,
    val eøsBegrunnelser: List<Unit> = emptyList(),
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<IVedtakBegrunnelse> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList()
)

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
): UtvidetVedtaksperiodeMedBegrunnelser {
    val utbetalingsperiodeDetaljer = hentUtbetalingsperiodeDetaljer(
        andelerTilkjentYtelse = andelerTilkjentYtelse,
        personopplysningGrunnlag = personopplysningGrunnlag
    )

    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.toList(),
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer
    )
}
