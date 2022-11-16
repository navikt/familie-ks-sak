package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsperiodeResponsDto(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val antallBarn: Int,
    val utbetaltPerMnd: Int,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljDto>
)

data class UtbetalingsperiodeDetaljDto(
    val person: PersonResponsDto,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal
)

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilUtbetalingsperiodeResponsDto(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeResponsDto> {
    if (this.isEmpty()) return emptyList()
    return this.lagVertikalePerioder().tilPerioder().filtrerIkkeNull().map {
        val andelerForPeriode = it.verdi
        val sumUtbetalingsbeløp = andelerForPeriode.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }
        UtbetalingsperiodeResponsDto(
            periodeFom = checkNotNull(it.fom),
            periodeTom = checkNotNull(it.tom),
            utbetaltPerMnd = sumUtbetalingsbeløp,
            antallBarn = andelerForPeriode.count { andel ->
                personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
            },
            utbetalingsperiodeDetaljer = andelerForPeriode.tilUtbetalingsperiodeDetaljDto(personopplysningGrunnlag)
        )
    }
}

private fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilUtbetalingsperiodeDetaljDto(
    personopplysningGrunnlag: PersonopplysningGrunnlag
): List<UtbetalingsperiodeDetaljDto> =
    this.map { andel ->
        val personForAndel = personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
            ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetaljDto(
            person = BehandlingMapper.lagPersonRespons(personForAndel, emptyMap()),
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent
        )
    }

fun UtbetalingsperiodeDetalj.tilUtbetalingsperiodeDetaljDto() = UtbetalingsperiodeDetaljDto(
    person = BehandlingMapper.lagPersonRespons(this.person, emptyMap()),
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent
)
