package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.api.mapper.BehandlingMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.tidslinje.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal
import java.time.LocalDate

data class UtbetalingsperiodeResponsDto(
    val periodeFom: LocalDate,
    val periodeTom: LocalDate,
    val antallBarn: Int,
    val utbetaltPerMnd: Int,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetaljDto>,
    val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.UTBETALING,
)

data class UtbetalingsperiodeDetaljDto(
    val person: PersonResponsDto,
    val utbetaltPerMnd: Int,
    val erPåvirketAvEndring: Boolean,
    val prosent: BigDecimal,
    val ytelseType: YtelseType,
)

fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilUtbetalingsperiodeResponsDto(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): List<UtbetalingsperiodeResponsDto> {
    if (this.isEmpty()) return emptyList()
    return this.lagVertikalePerioder().tilPerioder().filtrerIkkeNull().map {
        val andelerForPeriode = it.verdi
        val sumUtbetalingsbeløp = andelerForPeriode.sumOf { andel -> andel.kalkulertUtbetalingsbeløp }
        UtbetalingsperiodeResponsDto(
            periodeFom = checkNotNull(it.fom),
            periodeTom = checkNotNull(it.tom),
            utbetaltPerMnd = sumUtbetalingsbeløp,
            antallBarn =
                andelerForPeriode.count { andel ->
                    personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
                },
            utbetalingsperiodeDetaljer = andelerForPeriode.tilUtbetalingsperiodeDetaljDto(personopplysningGrunnlag, adopsjonerIBehandling),
        )
    }
}

private fun Collection<AndelTilkjentYtelseMedEndreteUtbetalinger>.tilUtbetalingsperiodeDetaljDto(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    adopsjonerIBehandling: List<Adopsjon>,
): List<UtbetalingsperiodeDetaljDto> =
    this.map { andel ->
        val personForAndel =
            personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel")

        UtbetalingsperiodeDetaljDto(
            person = BehandlingMapper.lagPersonRespons(person = personForAndel, landKodeOgLandNavn = emptyMap(), adopsjon = adopsjonerIBehandling.firstOrNull { it.aktør == personForAndel.aktør }),
            utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
            erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
            prosent = andel.prosent,
            ytelseType = andel.type,
        )
    }

fun UtbetalingsperiodeDetalj.tilUtbetalingsperiodeDetaljDto(
    adopsjonerIBehandling: List<Adopsjon>,
) = UtbetalingsperiodeDetaljDto(
    person = BehandlingMapper.lagPersonRespons(person = this.person, landKodeOgLandNavn = emptyMap(), adopsjon = adopsjonerIBehandling.firstOrNull { it.aktør == this.person.aktør }),
    utbetaltPerMnd = this.utbetaltPerMnd,
    erPåvirketAvEndring = this.erPåvirketAvEndring,
    prosent = this.prosent,
    ytelseType = this.ytelseType,
)
