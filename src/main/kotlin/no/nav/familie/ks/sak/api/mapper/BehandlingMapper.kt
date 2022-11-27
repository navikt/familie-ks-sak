package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.api.dto.ArbeidsfordelingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingStegTilstandResponsDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.PersonerMedAndelerResponsDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.UtbetalingsperiodeResponsDto
import no.nav.familie.ks.sak.api.dto.VedtakDto
import no.nav.familie.ks.sak.api.dto.YtelsePerioderDto
import no.nav.familie.ks.sak.api.mapper.RegisterHistorikkMapper.lagRegisterHistorikkResponsDto
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.slåSammenBack2BackAndelsperioderMedSammeBeløp
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
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
        utbetalingsperioder: List<UtbetalingsperiodeResponsDto>,
        vedtak: VedtakDto?,
        endreteUtbetalingerMedAndeler: List<EndretUtbetalingAndelDto>
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
            utbetalingsperioder = utbetalingsperioder,
            vedtak = vedtak,
            endretUtbetalingAndeler = endreteUtbetalingerMedAndeler
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
        registerhistorikk = if (landKodeOgLandNavn.isNotEmpty()) lagRegisterHistorikkResponsDto(
            person,
            landKodeOgLandNavn
        ) else null
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
}
