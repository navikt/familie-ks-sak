package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.api.dto.ArbeidsfordelingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingStegTilstandResponsDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.PersonerMedAndelerResponsDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.UtbetalingsperiodeDetaljDto
import no.nav.familie.ks.sak.api.dto.UtbetalingsperiodeResponsDto
import no.nav.familie.ks.sak.api.dto.YtelsePerioderDto
import no.nav.familie.ks.sak.api.mapper.RegisterHistorikkMapper.lagRegisterHistorikkResponsDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.slåSammenBack2BackAndelsperioderMedSammeBeløp
import no.nav.familie.ks.sak.kjerne.beregning.lagVertikalePerioder
import no.nav.familie.ks.sak.kjerne.beregning.tilSumTidslinje
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import java.math.BigDecimal
import java.time.LocalDate

object BehandlingMapper {

    fun lagBehandlingRespons(
        behandling: Behandling,
        arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling,
        søknadsgrunnlag: SøknadDto?,
        personer: List<PersonResponsDto>,
        personResultater: List<PersonResultat>?,
        personerMedAndelerTilkjentYtelse: List<PersonerMedAndelerResponsDto>,
        utbetalingsperioder: List<UtbetalingsperiodeResponsDto>
    ) =
        BehandlingResponsDto(
            behandlingId = behandling.id,
            steg = behandling.steg,
            stegTilstand = behandling.behandlingStegTilstand.map {
                BehandlingStegTilstandResponsDto(
                    it.behandlingSteg,
                    it.behandlingStegStatus,
                    it.årsak,
                    it.frist
                )
            },
            status = behandling.status,
            resultat = behandling.resultat,
            type = behandling.type,
            kategori = behandling.kategori,
            årsak = behandling.opprettetÅrsak,
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            endretAv = behandling.endretAv,
            arbeidsfordelingPåBehandling = lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling),
            søknadsgrunnlag = søknadsgrunnlag,
            personer = personer,
            personResultater = personResultater?.map { VilkårsvurderingMapper.lagPersonResultatRespons(it) }
                ?: emptyList(),
            behandlingPåVent = behandling.behandlingStegTilstand.singleOrNull { it.behandlingStegStatus == BehandlingStegStatus.VENTER }
                ?.let { BehandlingPåVentResponsDto(it.frist!!, it.årsak!!) },
            personerMedAndelerTilkjentYtelse = personerMedAndelerTilkjentYtelse,
            utbetalingsperioder = utbetalingsperioder
        )

    private fun lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling) =
        ArbeidsfordelingResponsDto(
            behandlendeEnhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
            behandlendeEnhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            manueltOverstyrt = arbeidsfordelingPåBehandling.manueltOverstyrt
        )

    fun lagPersonRespons(person: Person, landKodeOgLandNavn: Map<String, String>) = PersonResponsDto(
        type = person.type,
        fødselsdato = person.fødselsdato,
        personIdent = person.aktør.aktivFødselsnummer(),
        navn = person.navn,
        kjønn = KJOENN.valueOf(person.kjønn.name),
        målform = person.målform,
        dødsfallDato = person.dødsfall?.dødsfallDato,
        registerhistorikk = if (landKodeOgLandNavn.isNotEmpty()) lagRegisterHistorikkResponsDto(person, landKodeOgLandNavn) else null
    )

    fun lagPersonerMedAndelTilkjentYtelseRespons(
        personer: Set<Person>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>
    ) =
        andelerTilkjentYtelse.groupBy { it.aktør }
            .map { andelerForPerson ->
                val aktør = andelerForPerson.key
                val andeler = andelerForPerson.value

                val sammenslåtteAndeler = andeler.groupBy { it.type }
                    .flatMap { it.value.slåSammenBack2BackAndelsperioderMedSammeBeløp() }
                PersonerMedAndelerResponsDto(
                    personIdent = personer.find { person -> person.aktør == aktør }?.aktør?.aktivFødselsnummer(),
                    beløp = sammenslåtteAndeler.sumOf { it.kalkulertUtbetalingsbeløp },
                    stønadFom = sammenslåtteAndeler.minOfOrNull { it.stønadFom }
                        ?: LocalDate.MIN.toYearMonth(),
                    stønadTom = sammenslåtteAndeler.maxOfOrNull { it.stønadTom }
                        ?: LocalDate.MAX.toYearMonth(),
                    ytelsePerioder = sammenslåtteAndeler.map { sammenslåttAndel ->
                        YtelsePerioderDto(
                            beløp = sammenslåttAndel.kalkulertUtbetalingsbeløp,
                            stønadFom = sammenslåttAndel.stønadFom,
                            stønadTom = sammenslåttAndel.stønadTom,
                            ytelseType = sammenslåttAndel.type,
                            skalUtbetales = sammenslåttAndel.prosent > BigDecimal.ZERO
                        )
                    }
                )
            }

    fun lagUtbetalingsperioder(
        personopplysningGrunnlag: PersonopplysningGrunnlag,
        andelerTilkjentYtelseMedEndreteUtbetalinger: List<AndelTilkjentYtelseMedEndreteUtbetalinger>
    ): List<UtbetalingsperiodeResponsDto> {
        if (andelerTilkjentYtelseMedEndreteUtbetalinger.isEmpty()) return emptyList()
        val vertikalePerioder = andelerTilkjentYtelseMedEndreteUtbetalinger.lagVertikalePerioder()
        return vertikalePerioder.tilSumTidslinje().tilPerioder().filtrerIkkeNull().map {
            val periode = vertikalePerioder.tilPerioder().first { periode -> it.fom == periode.fom && it.tom == periode.tom }
            val andelerForPeriode = checkNotNull(periode.verdi)
            UtbetalingsperiodeResponsDto(
                periodeFom = checkNotNull(it.fom),
                periodeTom = checkNotNull(it.tom),
                utbetaltPerMnd = checkNotNull(it.verdi),
                antallBarn = andelerForPeriode.count { andel ->
                    personopplysningGrunnlag.barna.any { barn -> barn.aktør == andel.aktør }
                },
                utbetalingsperiodeDetaljer = andelerForPeriode.lagUtbetalingsperiodeDetaljer(personopplysningGrunnlag)
            )
        }
    }

    private fun List<AndelTilkjentYtelseMedEndreteUtbetalinger>.lagUtbetalingsperiodeDetaljer(
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): List<UtbetalingsperiodeDetaljDto> =
        this.map { andel ->
            val personForAndel = personopplysningGrunnlag.personer.find { person -> andel.aktør == person.aktør }
                ?: throw Feil("Fant ikke personopplysningsgrunnlag for andel")

            UtbetalingsperiodeDetaljDto(
                person = lagPersonRespons(personForAndel, emptyMap()),
                utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
                erPåvirketAvEndring = andel.endreteUtbetalinger.isNotEmpty(),
                prosent = andel.prosent
            )
        }
}
