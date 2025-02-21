package no.nav.familie.ks.sak.api.mapper

import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.ks.sak.api.dto.ArbeidsfordelingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingPåVentDto
import no.nav.familie.ks.sak.api.dto.BehandlingResponsDto
import no.nav.familie.ks.sak.api.dto.BehandlingStegTilstandResponsDto
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelResponsDto
import no.nav.familie.ks.sak.api.dto.FeilutbetaltValutaDto
import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.api.dto.PersonResponsDto
import no.nav.familie.ks.sak.api.dto.PersonerMedAndelerResponsDto
import no.nav.familie.ks.sak.api.dto.RefusjonEøsDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.dto.TilbakekrevingResponsDto
import no.nav.familie.ks.sak.api.dto.TotrinnskontrollDto
import no.nav.familie.ks.sak.api.dto.UtbetalingsperiodeResponsDto
import no.nav.familie.ks.sak.api.dto.VedtakDto
import no.nav.familie.ks.sak.api.dto.YtelsePerioderDto
import no.nav.familie.ks.sak.api.dto.tilKompetanseDto
import no.nav.familie.ks.sak.api.dto.tilKorrigertEtterbetalingResponsDto
import no.nav.familie.ks.sak.api.dto.tilKorrigertVedtakResponsDto
import no.nav.familie.ks.sak.api.dto.tilUtenlandskPeriodebeløpDto
import no.nav.familie.ks.sak.api.dto.tilValutakursDto
import no.nav.familie.ks.sak.api.mapper.RegisterHistorikkMapper.lagRegisterHistorikkResponsDto
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.adopsjon.Adopsjon
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.slåSammenBack2BackAndelsperioderMedSammeBeløp
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetaling
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
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
        totrinnskontroll: TotrinnskontrollDto?,
        endretUtbetalingAndeler: List<EndretUtbetalingAndelResponsDto>,
        overgangsordningAndeler: List<OvergangsordningAndelDto>,
        endringstidspunkt: LocalDate,
        tilbakekreving: Tilbakekreving?,
        sisteVedtaksperiodeVisningDato: LocalDate?,
        feilutbetalteValuta: List<FeilutbetaltValutaDto>,
        kompetanser: List<Kompetanse>,
        refusjonEøs: List<RefusjonEøsDto>,
        utenlandskePeriodebeløp: List<UtenlandskPeriodebeløp>,
        valutakurser: List<Valutakurs>,
        korrigertEtterbetaling: KorrigertEtterbetaling?,
        korrigertVedtak: KorrigertVedtak?,
        brevmottakere: List<BrevmottakerDto>,
    ) = BehandlingResponsDto(
        behandlingId = behandling.id,
        steg = behandling.steg,
        stegTilstand =
            behandling.behandlingStegTilstand.map {
                BehandlingStegTilstandResponsDto(
                    it.behandlingSteg,
                    it.behandlingStegStatus,
                    it.årsak,
                    it.frist,
                )
            },
        status = behandling.status,
        resultat = behandling.resultat,
        type = behandling.type,
        kategori = behandling.kategori,
        årsak = behandling.opprettetÅrsak,
        søknadMottattDato = behandling.søknadMottattDato,
        opprettetTidspunkt = behandling.opprettetTidspunkt,
        aktivertTidspunkt = behandling.aktivertTidspunkt,
        endretAv = behandling.endretAv,
        arbeidsfordelingPåBehandling = lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling),
        søknadsgrunnlag = søknadsgrunnlag,
        personer = personer,
        personResultater =
            personResultater?.map { personResultat ->
                VilkårsvurderingMapper.lagPersonResultatRespons(personResultat)
            }
                ?: emptyList(),
        behandlingPåVent =
            behandling.behandlingStegTilstand
                .singleOrNull { it.behandlingStegStatus == BehandlingStegStatus.VENTER }
                ?.let { BehandlingPåVentDto(it.frist!!, it.årsak!!) },
        personerMedAndelerTilkjentYtelse = personerMedAndelerTilkjentYtelse,
        utbetalingsperioder = utbetalingsperioder,
        vedtak = vedtak,
        totrinnskontroll = totrinnskontroll,
        endretUtbetalingAndeler = endretUtbetalingAndeler,
        overgangsordningAndeler = overgangsordningAndeler,
        endringstidspunkt = utledEndringstidpunkt(endringstidspunkt, behandling),
        tilbakekreving = tilbakekreving?.let { lagTilbakekrevingRespons(it) },
        sisteVedtaksperiodeVisningDato = sisteVedtaksperiodeVisningDato,
        feilutbetaltValuta = feilutbetalteValuta,
        kompetanser = kompetanser.map { it.tilKompetanseDto() },
        utenlandskePeriodebeløp = utenlandskePeriodebeløp.map { it.tilUtenlandskPeriodebeløpDto() },
        valutakurser = valutakurser.map { it.tilValutakursDto() },
        refusjonEøs = refusjonEøs,
        korrigertEtterbetaling = korrigertEtterbetaling?.tilKorrigertEtterbetalingResponsDto(),
        korrigertVedtak = korrigertVedtak?.tilKorrigertVedtakResponsDto(),
        brevmottakere = brevmottakere,
    )

    private fun lagArbeidsfordelingRespons(arbeidsfordelingPåBehandling: ArbeidsfordelingPåBehandling) =
        ArbeidsfordelingResponsDto(
            behandlendeEnhetId = arbeidsfordelingPåBehandling.behandlendeEnhetId,
            behandlendeEnhetNavn = arbeidsfordelingPåBehandling.behandlendeEnhetNavn,
            manueltOverstyrt = arbeidsfordelingPåBehandling.manueltOverstyrt,
        )

    fun lagPersonRespons(
        person: Person,
        landKodeOgLandNavn: Map<String, String>?,
        adopsjon: Adopsjon?,
    ) = PersonResponsDto(
        type = person.type,
        fødselsdato = person.fødselsdato,
        personIdent = person.aktør.aktivFødselsnummer(),
        navn = person.navn,
        kjønn = KJOENN.valueOf(person.kjønn.name),
        målform = person.målform,
        dødsfallDato = person.dødsfall?.dødsfallDato,
        registerhistorikk = lagRegisterHistorikkResponsDto(person, landKodeOgLandNavn),
        adopsjonsdato = adopsjon?.adopsjonsdato,
    )

    fun lagPersonerMedAndelTilkjentYtelseRespons(
        personer: Set<Person>,
        andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    ) = andelerTilkjentYtelse
        .groupBy { it.aktør }
        .map { andelerForPerson ->
            val aktør = andelerForPerson.key
            val andeler = andelerForPerson.value

            val sammenslåtteAndeler =
                andeler
                    .groupBy { it.type }
                    .flatMap { it.value.slåSammenBack2BackAndelsperioderMedSammeBeløp() }
            PersonerMedAndelerResponsDto(
                personIdent = personer.find { person -> person.aktør == aktør }?.aktør?.aktivFødselsnummer(),
                beløp = sammenslåtteAndeler.sumOf { it.kalkulertUtbetalingsbeløp },
                stønadFom =
                    sammenslåtteAndeler.minOfOrNull { it.stønadFom }
                        ?: LocalDate.MIN.toYearMonth(),
                stønadTom =
                    sammenslåtteAndeler.maxOfOrNull { it.stønadTom }
                        ?: LocalDate.MAX.toYearMonth(),
                ytelsePerioder =
                    sammenslåtteAndeler.map { sammenslåttAndel ->
                        YtelsePerioderDto(
                            beløp = sammenslåttAndel.kalkulertUtbetalingsbeløp,
                            stønadFom = sammenslåttAndel.stønadFom,
                            stønadTom = sammenslåttAndel.stønadTom,
                            ytelseType = sammenslåttAndel.type,
                            skalUtbetales = sammenslåttAndel.prosent > BigDecimal.ZERO,
                        )
                    },
            )
        }

    private fun utledEndringstidpunkt(
        endringstidspunkt: LocalDate,
        behandling: Behandling,
    ) = when {
        endringstidspunkt == TIDENES_MORGEN || endringstidspunkt == TIDENES_ENDE -> null
        behandling.overstyrtEndringstidspunkt != null -> behandling.overstyrtEndringstidspunkt
        else -> endringstidspunkt
    }

    private fun lagTilbakekrevingRespons(tilbakekreving: Tilbakekreving) =
        TilbakekrevingResponsDto(
            valg = tilbakekreving.valg,
            varsel = tilbakekreving.varsel,
            begrunnelse = tilbakekreving.begrunnelse,
            tilbakekrevingsbehandlingId = tilbakekreving.tilbakekrevingsbehandlingId,
        )
}
