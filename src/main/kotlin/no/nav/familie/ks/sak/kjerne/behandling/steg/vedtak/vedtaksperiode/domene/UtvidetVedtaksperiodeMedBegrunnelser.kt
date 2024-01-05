package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<NasjonalEllerFellesBegrunnelseDB>,
    val eøsBegrunnelser: List<EØSBegrunnelseDB> = emptyList(),
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<IBegrunnelse> = emptyList(),
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
) : Comparable<UtvidetVedtaksperiodeMedBegrunnelser> {
    override fun compareTo(other: UtvidetVedtaksperiodeMedBegrunnelser): Int {
        return if (this.type == Vedtaksperiodetype.AVSLAG) {
            1
        } else if (other.type == Vedtaksperiodetype.AVSLAG) {
            -1
        } else {
            (fom ?: TIDENES_MORGEN).compareTo(other.fom ?: TIDENES_MORGEN)
        }
    }
}

fun VedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    andelerTilkjentYtelse: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
): UtvidetVedtaksperiodeMedBegrunnelser {
    val utbetalingsperiodeDetaljer =
        hentUtbetalingsperiodeDetaljer(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
        )

    return UtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.toList(),
        eøsBegrunnelser = this.eøsBegrunnelser.toList(),
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        utbetalingsperiodeDetaljer = utbetalingsperiodeDetaljer,
    )
}

fun List<UtvidetVedtaksperiodeMedBegrunnelser>.tilTidslinje() =
    this.map {
        Periode(
            fom = it.fom,
            tom = it.tom,
            verdi = it,
        )
    }.tilTidslinje()
