package no.nav.familie.ks.sak.data

import io.mockk.mockk
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.enhet.Enhet
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.HenlagtÅrsak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansResultatDto
import no.nav.familie.kontrakter.felles.klage.KlageinstansUtfall
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.EndretUtbetalingAndelRequestDto
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.barnehagelister.domene.Barnehagebarn
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.tilMånedÅrKort
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.KafkaConfig.Companion.BARNEHAGELISTE_TOPIC
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.EØSBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.NasjonalEllerFellesBegrunnelseDB
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.UtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VedtakFellesfelterSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggType
import no.nav.familie.ks.sak.kjerne.logg.domene.Logg
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.UtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonEnkel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import no.nav.familie.ks.sak.statistikk.saksstatistikk.RelatertBehandling
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(fødselsdato: LocalDate? = null): String = fødselsnummerGenerator.foedselsnummer(foedselsdato = fødselsdato).asString

fun randomAktørId(): String = Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(randomAktørId()).also {
        it.personidenter.add(
            randomPersonident(it, fnr),
        )
    }

fun randomPersonident(
    aktør: Aktør,
    fnr: String = randomFnr(),
): Personident = Personident(fødselsnummer = fnr, aktør = aktør)

fun fnrTilAktør(
    fnr: String,
    toSisteSiffrer: String = "00",
) = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

fun lagPersonopplysningGrunnlag(
    behandlingId: Long = 0L,
    søkerPersonIdent: String = randomFnr(fødselsdato = LocalDate.of(1947, 1, 1)),
    // FGB med register søknad steg har ikke barnasidenter
    barnasIdenter: List<String> = emptyList(),
    barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { fnrTilFødselsdato(it) },
    barnasDødsfallDatoer: List<LocalDate?> = barnasIdenter.map { null },
    søkerAktør: Aktør =
        fnrTilAktør(søkerPersonIdent).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = søkerPersonIdent,
                    aktør = it,
                    aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer,
                ),
            )
        },
    barnAktør: List<Aktør> =
        barnasIdenter.map { fødselsnummer ->
            fnrTilAktør(fødselsnummer).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = fødselsnummer,
                        aktør = it,
                        aktiv = fødselsnummer == it.personidenter.first().fødselsnummer,
                    ),
                )
            }
        },
    søkerDødsDato: LocalDate? = null,
    søkerNavn: String = "",
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    val søker =
        Person(
            aktør = søkerAktør,
            type = PersonType.SØKER,
            personopplysningGrunnlag = personopplysningGrunnlag,
            fødselsdato = fnrTilFødselsdato(søkerPersonIdent),
            navn = søkerNavn,
            kjønn = Kjønn.KVINNE,
        ).also { søker ->
            søker.statsborgerskap =
                mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
            søker.bostedsadresser = mutableListOf()
            søker.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.GIFT, person = søker))
        }

    søkerDødsDato?.let {
        søker.dødsfall = Dødsfall(1, søker, it, null, null, null)
    }

    personopplysningGrunnlag.personer.add(søker)

    barnAktør.mapIndexed { index, aktør ->
        personopplysningGrunnlag.personer.add(
            Person(
                aktør = aktør,
                type = PersonType.BARN,
                personopplysningGrunnlag = personopplysningGrunnlag,
                fødselsdato = barnasFødselsdatoer[index],
                navn = "",
                kjønn = Kjønn.MANN,
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf()
                barn.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = barn))
                barn.dødsfall =
                    barnasDødsfallDatoer.getOrNull(index)?.let {
                        Dødsfall(
                            person = barn,
                            dødsfallDato = it,
                            dødsfallAdresse = null,
                            dødsfallPostnummer = null,
                            dødsfallPoststed = null,
                        )
                    }
            },
        )
    }
    return personopplysningGrunnlag
}

fun Person.tilPersonEnkel() = PersonEnkel(this.type, this.aktør, this.fødselsdato, this.dødsfall?.dødsfallDato, this.målform)

fun lagFagsak(
    aktør: Aktør = randomAktør(randomFnr()),
    id: Long = 0,
    status: FagsakStatus = FagsakStatus.OPPRETTET,
) = Fagsak(aktør = aktør, id = id, status = status)

private var gjeldendeVedtakId: Long = abs(Random.nextLong(10000000))
private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))
private var gjeldendePersonId: Long = abs(Random.nextLong(10000000))
private var gjeldendeUtvidetVedtaksperiodeId: Long = abs(Random.nextLong(10000000))
private const val ID_INKREMENT = 50

fun nesteVedtakId(): Long {
    gjeldendeVedtakId += ID_INKREMENT
    return gjeldendeVedtakId
}

fun nestePersonId(): Long {
    gjeldendePersonId += ID_INKREMENT
    return gjeldendePersonId
}

fun lagLogg(
    behandlingId: Long,
    id: Long = 1L,
    opprettetAv: String = "saksbehandler",
    opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    type: LoggType = LoggType.BEHANDLING_OPPRETTET,
    tittel: String = "tittel",
    rolle: BehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
    tekst: String = "",
): Logg =
    Logg(
        id,
        opprettetAv,
        opprettetTidspunkt,
        behandlingId,
        type,
        tittel,
        rolle,
        tekst,
    )

fun lagBehandling(
    fagsak: Fagsak = lagFagsak(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    opprettetÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    resultat: Behandlingsresultat = Behandlingsresultat.IKKE_VURDERT,
    aktiv: Boolean = true,
    status: BehandlingStatus = BehandlingStatus.UTREDES,
    id: Long = 0L,
    aktivertTidspunkt: LocalDateTime = LocalDateTime.now(),
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
    lagBehandlingStegTilstander: (behandling: Behandling) -> Set<BehandlingStegTilstand> = {
        setOf(
            BehandlingStegTilstand(
                behandling = it,
                behandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG,
            ),
        )
    },
): Behandling {
    val behandling =
        Behandling(
            id = id,
            fagsak = fagsak,
            type = type,
            opprettetÅrsak = opprettetÅrsak,
            kategori = kategori,
            resultat = resultat,
            aktiv = aktiv,
            status = status,
            aktivertTidspunkt = aktivertTidspunkt,
        )
    behandling.behandlingStegTilstand.addAll(lagBehandlingStegTilstander(behandling))
    behandling.endretTidspunkt = endretTidspunkt
    return behandling
}

fun lagBehandlingStegTilstand(
    behandling: Behandling,
    behandlingSteg: BehandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG,
    behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.KLAR,
    frist: LocalDate? = null,
    årsak: VenteÅrsak? = null,
): BehandlingStegTilstand =
    BehandlingStegTilstand(
        behandling = behandling,
        behandlingSteg = behandlingSteg,
        behandlingStegStatus = behandlingStegStatus,
        frist = frist,
        årsak = årsak,
    )

fun lagArbeidsfordelingPåBehandling(
    id: Long = 0,
    behandlingId: Long,
    behandlendeEnhetId: String = "4321",
    behandlendeEnhetNavn: String = "Test enhet",
    manueltOverstyrt: Boolean = false,
): ArbeidsfordelingPåBehandling =
    ArbeidsfordelingPåBehandling(
        id = id,
        behandlingId = behandlingId,
        behandlendeEnhetId = behandlendeEnhetId,
        behandlendeEnhetNavn = behandlendeEnhetNavn,
        manueltOverstyrt = manueltOverstyrt,
    )

fun lagEnhet(
    enhetsnummer: String,
    enhetsnavn: String = "Navn",
): Enhet = Enhet(enhetsnummer = enhetsnummer, enhetsnavn = enhetsnavn)

fun lagRegistrerSøknadDto() =
    RegistrerSøknadDto(
        søknad =
            SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = randomFnr()),
                barnaMedOpplysninger = listOf(BarnMedOpplysningerDto(ident = randomFnr())),
                endringAvOpplysningerBegrunnelse = "",
            ),
        bekreftEndringerViaFrontend = true,
    )

fun lagPdlPersonInfo(
    enkelPersonInfo: Boolean = false,
    erBarn: Boolean = false,
) = PdlPersonInfo(
    fødselsdato = if (erBarn) LocalDate.now().minusYears(1) else LocalDate.of(1987, 5, 1),
    navn = "John Doe",
    kjønn = KJOENN.MANN,
    forelderBarnRelasjoner = if (enkelPersonInfo) emptySet() else setOf(lagForelderBarnRelasjon()),
    bostedsadresser = listOf(lagBostedsadresse()),
    sivilstander = listOf(lagSivilstand()),
    statsborgerskap = listOf(lagStatsborgerskap()),
)

fun lagForelderBarnRelasjon(): ForelderBarnRelasjonInfo =
    ForelderBarnRelasjonInfo(
        aktør = randomAktør(),
        relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
        navn = "Ny barn",
        fødselsdato = LocalDate.now().minusYears(1),
    )

fun lagBostedsadresse(): Bostedsadresse =
    Bostedsadresse(
        gyldigFraOgMed = LocalDate.of(2015, 1, 1),
        vegadresse =
            Vegadresse(
                matrikkelId = 1234,
                husnummer = "3",
                husbokstav = null,
                bruksenhetsnummer = null,
                adressenavn = "OTTO SVERDRUPS VEG",
                kommunenummer = "1560",
                postnummer = "6650",
                tilleggsnavn = null,
            ),
    )

fun lagSivilstand(): Sivilstand = Sivilstand(type = SIVILSTANDTYPE.UGIFT, gyldigFraOgMed = LocalDate.of(2004, 12, 2))

fun lagStatsborgerskap(land: String = "NOR"): Statsborgerskap =
    Statsborgerskap(
        land = land,
        gyldigFraOgMed = LocalDate.of(1987, 9, 1),
        gyldigTilOgMed = null,
        bekreftelsesdato = LocalDate.of(1987, 9, 1),
    )

fun lagInitieltTilkjentYtelse(behandling: Behandling) = TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())

fun lagAndelTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse? = null,
    behandling: Behandling = lagBehandling(),
    aktør: Aktør? = null,
    stønadFom: YearMonth = YearMonth.now().minusMonths(1),
    stønadTom: YearMonth = YearMonth.now().plusMonths(8),
    sats: Int = maksBeløp(),
    periodeOffset: Long? = null,
    forrigePeriodeOffset: Long? = null,
    kalkulertUtbetalingsbeløp: Int = sats,
    ytelseType: YtelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
    prosent: BigDecimal = BigDecimal(100),
    nasjonaltPeriodebeløp: Int = sats,
    differanseberegnetPeriodebeløp: Int? = null,
    id: Long = 0L,
) = AndelTilkjentYtelse(
    id = id,
    behandlingId = behandling.id,
    tilkjentYtelse = tilkjentYtelse ?: lagInitieltTilkjentYtelse(behandling),
    aktør = aktør ?: behandling.fagsak.aktør,
    kalkulertUtbetalingsbeløp = kalkulertUtbetalingsbeløp,
    stønadFom = stønadFom,
    stønadTom = stønadTom,
    type = ytelseType,
    sats = sats,
    prosent = prosent,
    nasjonaltPeriodebeløp = nasjonaltPeriodebeløp,
    periodeOffset = periodeOffset,
    forrigePeriodeOffset = forrigePeriodeOffset,
    kildeBehandlingId = behandling.id,
    differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
)

fun lagPerson(
    personopplysningGrunnlag: PersonopplysningGrunnlag = mockk(relaxed = true),
    aktør: Aktør = randomAktør(),
    personType: PersonType = PersonType.SØKER,
    fødselsdato: LocalDate = fnrTilFødselsdato(aktør.aktivFødselsnummer()),
    dødsfall: Dødsfall? = null,
): Person {
    val person =
        Person(
            type = personType,
            fødselsdato = fødselsdato,
            kjønn = Kjønn.KVINNE,
            personopplysningGrunnlag = personopplysningGrunnlag,
            aktør = aktør,
            dødsfall = dødsfall,
        )
    person.bostedsadresser = mutableListOf(GrBostedsadresse.fraBostedsadresse(lagBostedsadresse(), person))
    person.statsborgerskap =
        mutableListOf(GrStatsborgerskap.fraStatsborgerskap(lagStatsborgerskap(), Medlemskap.NORDEN, person))
    person.sivilstander = mutableListOf(GrSivilstand.fraSivilstand(lagSivilstand(), person))

    return person
}

fun tilfeldigPerson(
    fødselsdato: LocalDate = LocalDate.now(),
    personType: PersonType = PersonType.BARN,
    kjønn: Kjønn = Kjønn.MANN,
    aktør: Aktør = randomAktør(),
    personId: Long = nestePersonId(),
    dødsfall: Dødsfall? = null,
) = Person(
    id = personId,
    aktør = aktør,
    fødselsdato = fødselsdato,
    type = personType,
    personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = 0),
    navn = "",
    kjønn = kjønn,
    målform = Målform.NB,
    dødsfall = dødsfall,
).apply { sivilstander = mutableListOf(GrSivilstand(type = SIVILSTANDTYPE.UGIFT, person = this)) }

fun lagVilkårsvurderingMedSøkersVilkår(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat = Resultat.OPPFYLT,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2),
    regelverk: Regelverk = Regelverk.NASJONALE_REGLER,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
    personResultat.setSortedVilkårResultater(
        setOf(
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id,
                vurderesEtter = regelverk,
                utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id,
                vurderesEtter = regelverk,
                utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            ),
        ) +
            if (regelverk == Regelverk.EØS_FORORDNINGEN) {
                setOf(
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = resultat,
                        periodeFom = søkerPeriodeFom,
                        periodeTom = søkerPeriodeTom,
                        begrunnelse = "",
                        behandlingId = behandling.id,
                        vurderesEtter = regelverk,
                        utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
                    ),
                )
            } else {
                emptySet()
            },
    )

    personResultat.andreVurderinger.add(
        AnnenVurdering(
            personResultat = personResultat,
            resultat = resultat,
            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
            begrunnelse = null,
        ),
    )

    vilkårsvurdering.personResultater = setOf(personResultat)
    return vilkårsvurdering
}

fun lagVilkårResultat(
    id: Long = 0,
    personResultat: PersonResultat = mockk(relaxed = true),
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    resultat: Resultat = Resultat.OPPFYLT,
    periodeFom: LocalDate? = LocalDate.now().minusMonths(3),
    periodeTom: LocalDate? = LocalDate.now(),
    begrunnelse: String = "",
    behandlingId: Long = 0,
    utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
    regelverk: Regelverk = Regelverk.NASJONALE_REGLER,
    antallTimer: BigDecimal? = null,
    søkerHarMeldtFraOmBarnehageplass: Boolean? = null,
    erEksplisittAvslagPåSøknad: Boolean? = null,
): VilkårResultat =
    VilkårResultat(
        id = id,
        personResultat = personResultat,
        vilkårType = vilkårType,
        resultat = resultat,
        periodeFom = periodeFom,
        periodeTom = periodeTom,
        begrunnelse = begrunnelse,
        behandlingId = behandlingId,
        utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
        vurderesEtter = regelverk,
        antallTimer = antallTimer,
        søkerHarMeldtFraOmBarnehageplass = søkerHarMeldtFraOmBarnehageplass,
        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
    )

fun lagVilkårResultaterForBarn(
    personResultat: PersonResultat,
    barnFødselsdato: LocalDate,
    barnehageplassPerioder: List<Pair<NullablePeriode, BigDecimal?>>,
    behandlingId: Long,
    regelverk: Regelverk = Regelverk.NASJONALE_REGLER,
    periodeTom: LocalDate? = null,
): Set<VilkårResultat> {
    val vilkårResultaterForBarn = mutableSetOf<VilkårResultat>()
    Vilkår.hentVilkårFor(PersonType.BARN).forEach {
        when (it) {
            Vilkår.BARNETS_ALDER ->
                vilkårResultaterForBarn.add(
                    lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = it,
                        periodeFom = barnFødselsdato.plusYears(1),
                        periodeTom = barnFødselsdato.plusYears(2),
                        behandlingId = behandlingId,
                        regelverk = regelverk,
                    ),
                )

            Vilkår.BARNEHAGEPLASS -> {
                vilkårResultaterForBarn.addAll(
                    barnehageplassPerioder.map { perioderMedAntallTimer ->
                        lagVilkårResultat(
                            personResultat = personResultat,
                            vilkårType = it,
                            periodeFom = perioderMedAntallTimer.first.fom,
                            periodeTom = perioderMedAntallTimer.first.tom,
                            behandlingId = behandlingId,
                            antallTimer = perioderMedAntallTimer.second,
                            resultat =
                                if (perioderMedAntallTimer.second == null ||
                                    perioderMedAntallTimer.second!! < BigDecimal(33)
                                ) {
                                    Resultat.OPPFYLT
                                } else {
                                    Resultat.IKKE_OPPFYLT
                                },
                        )
                    },
                )
            }

            else ->
                vilkårResultaterForBarn.add(
                    lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = it,
                        periodeFom = barnFødselsdato,
                        periodeTom = periodeTom,
                        behandlingId = behandlingId,
                        regelverk = regelverk,
                    ),
                )
        }
    }
    return vilkårResultaterForBarn
}

fun lagVilkårResultaterForDeltBosted(
    personResultat: PersonResultat,
    fom1: LocalDate,
    tom1: LocalDate,
    fom2: LocalDate? = null,
    tom2: LocalDate? = null,
    behandlingId: Long,
): Set<VilkårResultat> {
    val vilkårResultaterForBarn = mutableSetOf<VilkårResultat>()
    Vilkår.hentVilkårFor(PersonType.BARN).forEach {
        when (it) {
            Vilkår.BOR_MED_SØKER -> {
                val vilkårResultatMedDeltBosted1 =
                    lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = it,
                        periodeFom = fom1,
                        periodeTom = tom1,
                        behandlingId = behandlingId,
                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    )
                vilkårResultaterForBarn.add(vilkårResultatMedDeltBosted1)
                if (fom2 != null && tom2 != null) {
                    val vilkårResultatMedDeltBosted2 =
                        lagVilkårResultat(
                            personResultat = personResultat,
                            vilkårType = it,
                            periodeFom = fom2,
                            periodeTom = tom2,
                            behandlingId = behandlingId,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                        )
                    vilkårResultaterForBarn.add(vilkårResultatMedDeltBosted2)
                }
            }

            else ->
                vilkårResultaterForBarn.add(
                    lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = it,
                        periodeFom = fom1,
                        periodeTom = tom2 ?: tom1,
                        behandlingId = behandlingId,
                    ),
                )
        }
    }
    return vilkårResultaterForBarn
}

fun lagEndretUtbetalingAndel(
    id: Long = 0L,
    behandlingId: Long = 0L,
    person: Person? = lagPerson(aktør = randomAktør()),
    prosent: BigDecimal? = null,
    periodeFom: YearMonth? = YearMonth.now().minusMonths(1),
    periodeTom: YearMonth? = YearMonth.now(),
    årsak: Årsak? = Årsak.ALLEREDE_UTBETALT,
    søknadstidspunkt: LocalDate? = LocalDate.now().minusMonths(1),
    begrunnelse: String? = "test",
    begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
    erEksplisittAvslagPåSøknad: Boolean? = false,
): EndretUtbetalingAndel =
    EndretUtbetalingAndel(
        id = id,
        behandlingId = behandlingId,
        person = person,
        prosent = prosent,
        fom = periodeFom,
        tom = periodeTom,
        årsak = årsak,
        søknadstidspunkt = søknadstidspunkt,
        begrunnelse = begrunnelse,
        vedtaksbegrunnelser = begrunnelser,
        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
    )

fun lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak = Årsak.ALLEREDE_UTBETALT,
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    andelTilkjentYtelser: MutableList<AndelTilkjentYtelse> = mutableListOf(),
    begrunnelse: String? = "test",
    begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
    erEksplisittAvslagPåSøknad: Boolean? = false,
): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
    val eua =
        EndretUtbetalingAndel(
            id = id,
            behandlingId = behandlingId,
            person = person,
            prosent = prosent,
            fom = fom,
            tom = tom,
            årsak = årsak,
            søknadstidspunkt = søknadstidspunkt,
            begrunnelse = begrunnelse,
            vedtaksbegrunnelser = begrunnelser,
            erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
        )

    return EndretUtbetalingAndelMedAndelerTilkjentYtelse(eua, andelTilkjentYtelser)
}

fun lagVedtaksbegrunnelse(
    nasjonalEllerFellesBegrunnelse: NasjonalEllerFellesBegrunnelse =
        NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk(),
) = NasjonalEllerFellesBegrunnelseDB(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    nasjonalEllerFellesBegrunnelse = nasjonalEllerFellesBegrunnelse,
)

fun lagEØSVedtaksbegrunnelse(
    begrunnelse: EØSBegrunnelse =
        EØSBegrunnelse.INNVILGET_PRIMÆRLAND_ALENEANSVAR,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk(),
) = EØSBegrunnelseDB(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    begrunnelse = begrunnelse,
)

fun lagVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak = Vedtak(behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: (vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser) -> List<NasjonalEllerFellesBegrunnelseDB> = { emptyList() },
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf(),
): VedtaksperiodeMedBegrunnelser {
    val vedtaksperiodeMedBegrunnelser =
        VedtaksperiodeMedBegrunnelser(
            vedtak = vedtak,
            fom = fom,
            tom = tom,
            type = type,
            fritekster = fritekster,
        )
    vedtaksperiodeMedBegrunnelser.settBegrunnelser(begrunnelser(vedtaksperiodeMedBegrunnelser))
    return vedtaksperiodeMedBegrunnelser
}

fun lagUtvidetVedtaksperiodeMedBegrunnelser(
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: List<NasjonalEllerFellesBegrunnelseDB> = emptyList(),
    eøsBegrunnelser: List<EØSBegrunnelseDB> = emptyList(),
): UtvidetVedtaksperiodeMedBegrunnelser = UtvidetVedtaksperiodeMedBegrunnelser(id = 0, fom = fom, tom = tom, type = type, begrunnelser = begrunnelser, eøsBegrunnelser = eøsBegrunnelser, støtterFritekst = false)

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    erDeltBosted: Boolean = false,
    erEksplisittAvslagPåSøknad: Boolean = false,
): PersonResultat {
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør,
        )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår
                .hentVilkårFor(personType)
                .map {
                    VilkårResultat(
                        personResultat = personResultat,
                        periodeFom = periodeFom,
                        periodeTom = periodeTom,
                        vilkårType = it,
                        resultat = resultat,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                        utdypendeVilkårsvurderinger =
                            listOfNotNull(
                                when {
                                    erDeltBosted && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED
                                    else -> null
                                },
                            ),
                    )
                }.toSet(),
        )
    } else {
        personResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = vilkårType,
                    resultat = resultat,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                ),
            ),
        )
    }
    return personResultat
}

fun lagPersonResultatFraVilkårResultater(
    vilkårResultater: Set<VilkårResultat>,
    aktør: Aktør,
): PersonResultat {
    val vilkårsvurdering =
        lagVilkårsvurdering(
            behandling = lagBehandling(),
            resultat = Resultat.OPPFYLT,
            søkerAktør = randomAktør(),
        )
    val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)

    personResultat.setSortedVilkårResultater(vilkårResultater)

    return personResultat
}

fun lagBeregnetUtbetalingsoppdrag(
    vedtak: Vedtak,
    utbetlingsperioder: List<no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode> = emptyList(),
) = BeregnetUtbetalingsoppdragLongId(
    no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag(
        aktoer = "",
        fagSystem = "KS",
        saksnummer = "1234",
        kodeEndring = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.KodeEndring.NY,
        saksbehandlerId = "123abc",
        utbetalingsperiode = utbetlingsperioder,
    ),
    listOf(AndelMedPeriodeIdLongId(id = 0L, periodeId = 0L, forrigePeriodeId = null, kildeBehandlingId = vedtak.behandling.id)),
)

fun lagUtbetalingsperiode(vedtak: Vedtak) =
    no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        periodeId = 0L,
        forrigePeriodeId = null,
        datoForVedtak = LocalDate.now(),
        klassifisering = "KS",
        vedtakdatoFom = LocalDate.now().minusMonths(5),
        vedtakdatoTom = LocalDate.now(),
        sats = maksBeløp().toBigDecimal(),
        satsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
        behandlingId = vedtak.behandling.id,
        utbetalesTil = "",
    )

fun lagTilkjentYtelse(
    utbetalingsoppdrag: Utbetalingsoppdrag,
    behandling: Behandling,
) = TilkjentYtelse(
    utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
    behandling = behandling,
    opprettetDato = LocalDate.now(),
    endretDato = LocalDate.now(),
)

fun lagUtbetalingsoppdrag(utbetalingsperiode: List<Utbetalingsperiode>) =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        fagSystem = "KS",
        saksnummer = "",
        aktoer = UUID.randomUUID().toString(),
        saksbehandlerId = "",
        avstemmingTidspunkt = LocalDateTime.now(),
        utbetalingsperiode = utbetalingsperiode,
    )

fun lagUtbetalingsperiode(opphør: Opphør? = null) =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = false,
        opphør = opphør,
        periodeId = 0,
        datoForVedtak = LocalDate.now(),
        klassifisering = "KS",
        vedtakdatoFom = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
        vedtakdatoTom = LocalDate.now().minusMonths(1).sisteDagIMåned(),
        sats = BigDecimal("1054"),
        satsType = Utbetalingsperiode.SatsType.MND,
        utbetalesTil = "",
        behandlingId = 0,
    )

fun lagØkonomiSimuleringMottaker(
    behandling: Behandling,
    økonomiSimuleringPosteringer: List<ØkonomiSimuleringPostering> = emptyList(),
) = ØkonomiSimuleringMottaker(
    mottakerType = MottakerType.BRUKER,
    mottakerNummer = "",
    behandling = behandling,
    økonomiSimuleringPostering = økonomiSimuleringPosteringer,
)

fun lagØkonomiSimuleringPostering(
    behandling: Behandling,
    fom: LocalDate,
    tom: LocalDate,
    beløp: BigDecimal,
    forfallsdato: LocalDate,
    posteringType: PosteringType = PosteringType.YTELSE,
) = ØkonomiSimuleringPostering(
    økonomiSimuleringMottaker =
        lagØkonomiSimuleringMottaker(
            behandling = behandling,
        ),
    fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
    fom = fom,
    tom = tom,
    betalingType = BetalingType.DEBIT,
    beløp = beløp,
    posteringType = posteringType,
    forfallsdato = forfallsdato,
    utenInntrekk = false,
)

fun lagSimulertMottaker(simulertePosteringer: List<SimulertPostering>) =
    SimuleringMottaker(
        mottakerType = MottakerType.BRUKER,
        mottakerNummer = "",
        simulertPostering = simulertePosteringer,
    )

fun lagSimulertPostering(
    fom: LocalDate,
    tom: LocalDate,
    beløp: BigDecimal,
    forfallsdato: LocalDate,
) = SimulertPostering(
    fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
    fom = fom,
    tom = tom,
    betalingType = BetalingType.DEBIT,
    beløp = beløp,
    posteringType = PosteringType.YTELSE,
    forfallsdato = forfallsdato,
    utenInntrekk = true,
)

fun fnrTilFødselsdato(fnr: String): LocalDate {
    val day = fnr.substring(0, 2).toInt()
    val month =
        fnr.substring(2, 4).toInt().let {
            if (it - 40 > 0) {
                it - 40
            }
            it
        }
    val year = fnr.substring(4, 6).toInt().let { if (it <= (LocalDate.now().year - 2000)) it + 2000 else it + 1900 }
    return LocalDate.of(year, month, day)
}

fun årMåned(årMåned: String) = YearMonth.parse(årMåned)

fun dato(s: String) = LocalDate.parse(s)

fun lagUtbetalingsperiodeDetalj(
    person: Person,
    ytelseType: YtelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
    utbetaltPerMnd: Int = 1,
    erPåvirketAvEndring: Boolean = false,
    prosent: BigDecimal = BigDecimal.valueOf(100),
): UtbetalingsperiodeDetalj =
    UtbetalingsperiodeDetalj(
        person = person,
        ytelseType = ytelseType,
        utbetaltPerMnd = utbetaltPerMnd,
        erPåvirketAvEndring = erPåvirketAvEndring,
        prosent = prosent,
    )

fun lagDødsfall(
    person: Person,
    dødsfallDato: LocalDate = LocalDate.now(),
    dødsfallAdresse: String? = "",
    dødsfallPostnummer: String? = "",
    dødsfallPoststed: String? = "",
) = Dødsfall(
    person = person,
    dødsfallDato = dødsfallDato,
    dødsfallAdresse = dødsfallAdresse,
    dødsfallPostnummer = dødsfallPostnummer,
    dødsfallPoststed = dødsfallPoststed,
)

fun lagVilkårsvurderingOppfylt(
    personer: Collection<Person>,
    behandling: Behandling = lagBehandling(),
    erEksplisittAvslagPåSøknad: Boolean = false,
    skalOppretteEøsSpesifikkeVilkår: Boolean = false,
    periodeFom: LocalDate? = null,
    periodeTom: LocalDate? = null,
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )

    val personResultater =
        personer
            .map { person ->
                val personResultat =
                    PersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = person.aktør,
                    )

                personResultat.setSortedVilkårResultater(
                    Vilkår
                        .hentVilkårFor(person.type, skalOppretteEøsSpesifikkeVilkår)
                        .map {
                            VilkårResultat(
                                personResultat = personResultat,
                                periodeFom =
                                    when (it) {
                                        Vilkår.BARNETS_ALDER -> person.fødselsdato.plusMonths(13)
                                        else -> periodeFom ?: person.fødselsdato.plusMonths(13)
                                    },
                                periodeTom =
                                    when {
                                        person.type == PersonType.SØKER -> null
                                        it == Vilkår.BARNETS_ALDER -> person.fødselsdato.plusMonths(19)
                                        else -> periodeTom ?: person.fødselsdato.plusMonths(19)
                                    },
                                vilkårType = it,
                                resultat = Resultat.OPPFYLT,
                                begrunnelse = "",
                                behandlingId = vilkårsvurdering.behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                                erEksplisittAvslagPåSøknad = erEksplisittAvslagPåSøknad,
                            )
                        }.toSet(),
                )
                personResultat
            }.toSet()

    vilkårsvurdering.personResultater = personResultater

    return vilkårsvurdering
}

// EØS
fun lagKompetanse(
    behandlingId: Long = 0,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    resultat: KompetanseResultat? = null,
    annenForeldersAktivitetsland: String? = "DK",
    annenForeldersAktivitet: KompetanseAktivitet? = KompetanseAktivitet.I_ARBEID,
    barnetsBostedsland: String? = "NO",
    søkersAktivitet: KompetanseAktivitet? = KompetanseAktivitet.ARBEIDER,
    søkersAktivitetsland: String? = "SE",
): Kompetanse {
    val kompetanse =
        Kompetanse(
            fom = fom,
            tom = tom,
            barnAktører = barnAktører,
            resultat = resultat,
            annenForeldersAktivitetsland = annenForeldersAktivitetsland,
            annenForeldersAktivitet = annenForeldersAktivitet,
            barnetsBostedsland = barnetsBostedsland,
            søkersAktivitet = søkersAktivitet,
            søkersAktivitetsland = søkersAktivitetsland,
        )
    kompetanse.behandlingId = behandlingId
    return kompetanse
}

fun lagUtenlandskPeriodebeløp(
    behandlingId: Long = lagBehandling().id,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    beløp: BigDecimal? = null,
    valutakode: String? = null,
    intervall: Intervall? = null,
    utbetalingsland: String = "",
) = UtenlandskPeriodebeløp(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    valutakode = valutakode,
    beløp = beløp,
    intervall = intervall,
    utbetalingsland = utbetalingsland,
).also { it.behandlingId = behandlingId }

fun lagValutakurs(
    behandlingId: Long = lagBehandling().id,
    fom: YearMonth? = null,
    tom: YearMonth? = null,
    barnAktører: Set<Aktør> = emptySet(),
    valutakursdato: LocalDate? = null,
    valutakode: String? = null,
    kurs: BigDecimal? = null,
) = Valutakurs(
    fom = fom,
    tom = tom,
    barnAktører = barnAktører,
    valutakursdato = valutakursdato,
    valutakode = valutakode,
    kurs = kurs,
).also { it.behandlingId = behandlingId }

fun lagAndelTilkjentYtelse(
    fom: YearMonth,
    tom: YearMonth,
    ytelseType: YtelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
    beløp: Int = 7500,
    behandling: Behandling = lagBehandling(),
    person: Person = tilfeldigPerson(),
    aktør: Aktør = person.aktør,
    periodeIdOffset: Long? = null,
    forrigeperiodeIdOffset: Long? = null,
    tilkjentYtelse: TilkjentYtelse? = null,
    prosent: BigDecimal = BigDecimal(100),
    kildeBehandlingId: Long? = behandling.id,
    differanseberegnetPeriodebeløp: Int? = null,
    id: Long = 0,
    sats: Int = 7500,
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        id = id,
        aktør = aktør,
        behandlingId = behandling.id,
        tilkjentYtelse = tilkjentYtelse ?: lagInitiellTilkjentYtelse(behandling),
        kalkulertUtbetalingsbeløp = beløp,
        nasjonaltPeriodebeløp = beløp,
        stønadFom = fom,
        stønadTom = tom,
        type = ytelseType,
        periodeOffset = periodeIdOffset,
        forrigePeriodeOffset = forrigeperiodeIdOffset,
        sats = sats,
        prosent = prosent,
        kildeBehandlingId = kildeBehandlingId,
        differanseberegnetPeriodebeløp = differanseberegnetPeriodebeløp,
    )

fun lagUtfyltOvergangsordningAndel(
    id: Long = 0,
    behandling: Behandling = lagBehandling(),
    person: Person = lagPerson(aktør = randomAktør()),
    antallTimer: BigDecimal = BigDecimal.ZERO,
    deltBosted: Boolean = false,
    fom: YearMonth = YearMonth.now(),
    tom: YearMonth = YearMonth.now(),
): UtfyltOvergangsordningAndel =
    UtfyltOvergangsordningAndel(
        id = id,
        behandlingId = behandling.id,
        person = person,
        antallTimer = antallTimer,
        deltBosted = deltBosted,
        fom = fom,
        tom = tom,
    )

fun lagInitiellTilkjentYtelse(
    behandling: Behandling = lagBehandling(),
    utbetalingsoppdrag: String? = null,
): TilkjentYtelse =
    TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now(),
        utbetalingsoppdrag = utbetalingsoppdrag,
    )

fun lagTestPersonopplysningGrunnlag(
    behandlingId: Long,
    vararg personer: Person,
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    personopplysningGrunnlag.personer.addAll(
        personer.map { it.copy(personopplysningGrunnlag = personopplysningGrunnlag) },
    )
    return personopplysningGrunnlag
}

fun lagVedtak(
    behandling: Behandling = lagBehandling(),
    stønadBrevPdF: ByteArray? = null,
) = Vedtak(
    id = nesteVedtakId(),
    behandling = behandling,
    vedtaksdato = LocalDateTime.now(),
    stønadBrevPdf = stønadBrevPdF,
)

fun lagVilkårsvurdering(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2),
    medAndreVurderinger: Boolean = true,
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            behandling = behandling,
        )
    val personResultat =
        PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = søkerAktør,
        )
    personResultat.setSortedVilkårResultater(
        setOf(
            VilkårResultat(
                behandlingId = behandling.id,
                personResultat = personResultat,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
            ),
            VilkårResultat(
                behandlingId = behandling.id,
                personResultat = personResultat,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
            ),
        ),
    )
    if (medAndreVurderinger) {
        personResultat.andreVurderinger.add(
            AnnenVurdering(
                personResultat = personResultat,
                resultat = resultat,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = null,
            ),
        )
    }

    vilkårsvurdering.personResultater = setOf(personResultat)
    return vilkårsvurdering
}

fun lagSanityBegrunnelse(
    apiNavn: String,
    støtterFritekst: Boolean = false,
    resultat: SanityResultat = SanityResultat.INNVILGET,
    hjemler: List<String> = emptyList(),
    hjemlerEøsForordningen883: List<String> = emptyList(),
    hjemlerEøsForordningen987: List<String> = emptyList(),
    hjemlerSeperasjonsavtalenStorbritannina: List<String> = emptyList(),
): SanityBegrunnelse =
    SanityBegrunnelse(
        apiNavn = apiNavn,
        navnISystem = "",
        type = SanityBegrunnelseType.STANDARD,
        vilkår = emptyList(),
        rolle = emptyList(),
        utdypendeVilkårsvurderinger = emptyList(),
        triggere = emptyList(),
        hjemler = hjemler,
        endringsårsaker = emptyList(),
        endretUtbetalingsperiode = emptyList(),
        støtterFritekst = støtterFritekst,
        skalAlltidVises = false,
        ikkeIBruk = false,
        resultat = resultat,
        hjemlerEØSForordningen883 = hjemlerEøsForordningen883,
        hjemlerEØSForordningen987 = hjemlerEøsForordningen987,
        hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina,
    )

fun lagSammensattKontrollsak(
    id: Long = 0L,
    behandlingId: Long,
    fritekst: String = "",
): SammensattKontrollsak =
    SammensattKontrollsak(
        id,
        behandlingId,
        fritekst,
    )

fun lagVedtakFellesfelterSammensattKontrollsakDto(
    enhet: String = "enhet",
    saksbehandler: String = "saksbehandler",
    beslutter: String = "beslutter",
    søkerNavn: String = "søkerNavn",
    søkerFødselsnummer: String = "søkerFødselsnummer",
    sammensattKontrollsakFritekst: String = "sammensattKontrollsakFritekst",
) = VedtakFellesfelterSammensattKontrollsakDto(
    enhet,
    saksbehandler,
    beslutter,
    søkerNavn,
    søkerFødselsnummer,
    sammensattKontrollsakFritekst,
)

fun lagKorrigertVedtak(
    behandling: Behandling,
    vedtaksdato: LocalDate = LocalDate.now(),
    begrunnelse: String? = null,
    aktiv: Boolean = true,
): KorrigertVedtak =
    KorrigertVedtak(
        behandling = behandling,
        vedtaksdato = vedtaksdato,
        begrunnelse = begrunnelse,
        aktiv = aktiv,
    )

fun lagToTrinnskontroll(
    behandling: Behandling = lagBehandling(),
    saksbehandler: String = "saksbehandler",
    beslutter: String = "beslutter",
    saksbehandlerId: String = "1234",
    godkjent: Boolean = true,
): Totrinnskontroll =
    Totrinnskontroll(
        behandling = behandling,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        saksbehandlerId = saksbehandlerId,
        godkjent = godkjent,
    )

fun lagVilkårsvurdering(
    id: Long = 0L,
    behandling: Behandling = lagBehandling(),
    aktiv: Boolean = true,
    lagPersonResultat: (vilkårsvurdering: Vilkårsvurdering) -> Set<PersonResultat> = {
        setOf(
            lagPersonResultat(
                vilkårsvurdering = it,
                aktør = randomAktør(),
            ),
        )
    },
): Vilkårsvurdering {
    val vilkårsvurdering =
        Vilkårsvurdering(
            id = id,
            behandling = behandling,
            aktiv = aktiv,
        )
    val personResultat = lagPersonResultat(vilkårsvurdering)
    vilkårsvurdering.personResultater = personResultat
    return vilkårsvurdering
}

fun lagPersonResultat(
    id: Long = 0L,
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør = randomAktør(),
    lagVilkårResultater: (personResultat: PersonResultat) -> Set<VilkårResultat> = {
        setOf(
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
            lagVilkårResultat(
                behandlingId = vilkårsvurdering.behandling.id,
                personResultat = it,
                vilkårType = Vilkår.LOVLIG_OPPHOLD,
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(1),
                periodeTom = LocalDate.now().plusYears(2),
                begrunnelse = "",
            ),
        )
    },
    lagAnnenVurdering: (personResultat: PersonResultat) -> Set<AnnenVurdering> = {
        setOf(
            AnnenVurdering(
                personResultat = it,
                resultat = Resultat.OPPFYLT,
                type = AnnenVurderingType.OPPLYSNINGSPLIKT,
                begrunnelse = null,
            ),
        )
    },
): PersonResultat {
    val personResultat =
        PersonResultat(
            id = id,
            vilkårsvurdering = vilkårsvurdering,
            aktør = aktør,
        )
    personResultat.setSortedVilkårResultater(lagVilkårResultater(personResultat))
    personResultat.andreVurderinger.addAll(lagAnnenVurdering(personResultat))
    return personResultat
}

fun lagNasjonalOgFellesBegrunnelseDataDto(
    vedtakBegrunnelseType: BegrunnelseType = BegrunnelseType.INNVILGET,
    apiNavn: String = "innvilgetIkkeBarnehage",
    sanityBegrunnelseType: SanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
    gjelderSoker: Boolean = false,
    gjelderAndreForelder: Boolean = true,
    barnasFodselsdatoer: LocalDate = LocalDate.now(),
    antallBarn: Int = 1,
    maanedOgAarBegrunnelsenGjelderFor: YearMonth = YearMonth.now(),
    maalform: String = "bokmaal",
    belop: Int = 7500,
    antallTimerBarnehageplass: Int = 0,
    soknadstidspunkt: LocalDate = LocalDate.now(),
    månedOgÅrFørVedtaksperiode: YearMonth = YearMonth.now().minusMonths(1),
): NasjonalOgFellesBegrunnelseDataDto =
    NasjonalOgFellesBegrunnelseDataDto(
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        apiNavn = apiNavn,
        sanityBegrunnelseType = sanityBegrunnelseType,
        gjelderSoker = gjelderSoker,
        gjelderAndreForelder = gjelderAndreForelder,
        barnasFodselsdatoer = barnasFodselsdatoer.tilKortString(),
        antallBarn = antallBarn,
        maanedOgAarBegrunnelsenGjelderFor = maanedOgAarBegrunnelsenGjelderFor.tilKortString(),
        maalform = maalform,
        belop = belop.toString(),
        antallTimerBarnehageplass = antallTimerBarnehageplass.toString(),
        soknadstidspunkt = soknadstidspunkt.tilKortString(),
        maanedOgAarFoerVedtaksperiode = månedOgÅrFørVedtaksperiode.tilMånedÅrKort(),
    )

fun lagBrevmottakerDto(
    id: Long,
    type: no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType = no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
    navn: String = "Test Testesen",
    adresselinje1: String = "En adresse her",
    adresselinje2: String? = null,
    postnummer: String = "0661",
    poststed: String = "Oslo",
    landkode: String = "NO",
) = BrevmottakerDto(
    id = id,
    type = type,
    navn = navn,
    adresselinje1 = adresselinje1,
    adresselinje2 = adresselinje2,
    postnummer = postnummer,
    poststed = poststed,
    landkode = landkode,
)

fun lagEndretUtbetalingAndelRequestDto(
    id: Long = 0L,
    personIdent: String = "12345678903",
    prosent: BigDecimal = BigDecimal(100),
    fom: YearMonth = YearMonth.now().minusYears(1),
    tom: YearMonth = YearMonth.now(),
    årsak: Årsak = Årsak.ALLEREDE_UTBETALT,
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    begrunnelse: String = "en begrunnelse",
    erEksplisittAvslagPåSøknad: Boolean? = false,
    begrunnelser: List<NasjonalEllerFellesBegrunnelse> = emptyList(),
) = EndretUtbetalingAndelRequestDto(
    id,
    personIdent,
    prosent,
    fom,
    tom,
    årsak,
    søknadstidspunkt,
    begrunnelse,
    erEksplisittAvslagPåSøknad,
    begrunnelser,
)

fun lagRefusjonEøs(
    behandlingId: Long = 0L,
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now().plusMonths(1),
    refusjonsbeløp: Int = 0,
    land: String = "NO",
    refusjonAvklart: Boolean = true,
    id: Long = 0L,
): RefusjonEøs =
    RefusjonEøs(
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        refusjonsbeløp = refusjonsbeløp,
        land = land,
        refusjonAvklart = refusjonAvklart,
        id = id,
    )

fun lagKlagebehandlingDto(
    id: UUID = UUID.randomUUID(),
    fagsakId: UUID = UUID.randomUUID(),
    status: no.nav.familie.kontrakter.felles.klage.BehandlingStatus = no.nav.familie.kontrakter.felles.klage.BehandlingStatus.FERDIGSTILT,
    opprettet: LocalDateTime = LocalDateTime.now(),
    mottattDato: LocalDate = LocalDate.now(),
    resultat: BehandlingResultat? = BehandlingResultat.MEDHOLD,
    årsak: no.nav.familie.kontrakter.felles.klage.Årsak? = null,
    vedtaksdato: LocalDateTime? = LocalDateTime.now(),
    klageinstansResultat: List<KlageinstansResultatDto> = emptyList(),
    henlagtÅrsak: HenlagtÅrsak? = null,
) = KlagebehandlingDto(
    id = id,
    fagsakId = fagsakId,
    status = status,
    opprettet = opprettet,
    mottattDato = mottattDato,
    resultat = resultat,
    årsak = årsak,
    vedtaksdato = vedtaksdato,
    klageinstansResultat = klageinstansResultat,
    henlagtÅrsak = henlagtÅrsak,
)

fun lagKlageinstansResultatDto(
    type: BehandlingEventType = BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET,
    utfall: KlageinstansUtfall? = KlageinstansUtfall.MEDHOLD,
    mottattEllerAvsluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
    journalpostReferanser: List<String> = emptyList(),
    årsakFeilregistrert: String? = null,
): KlageinstansResultatDto =
    KlageinstansResultatDto(
        type = type,
        utfall = utfall,
        mottattEllerAvsluttetTidspunkt = mottattEllerAvsluttetTidspunkt,
        journalpostReferanser = journalpostReferanser,
        årsakFeilregistrert = årsakFeilregistrert,
    )

fun lagRelatertBehandling(
    id: String = "1",
    vedtattTidspunkt: LocalDateTime = LocalDateTime.now(),
    fagsystem: RelatertBehandling.Fagsystem = RelatertBehandling.Fagsystem.KS,
): RelatertBehandling =
    RelatertBehandling(
        id = id,
        vedtattTidspunkt = vedtattTidspunkt,
        fagsystem = fagsystem,
    )

fun lagBarnehageBarn(
    id: UUID = UUID.randomUUID(),
    ident: String = randomFnr(),
    fom: LocalDate = LocalDate.now().minusMonths(1),
    tom: LocalDate = LocalDate.now().plusMonths(1),
    antallTimerIBarnehage: Double = 45.0,
    endringstype: String = "",
    kommuneNavn: String = "Kommune Navn",
    kommuneNr: String = "1234",
    arkivReferanse: String = UUID.randomUUID().toString(),
    kildeTopic: String = BARNEHAGELISTE_TOPIC,
    endretTidspunkt: LocalDateTime = LocalDateTime.now(),
): Barnehagebarn {
    val barnehagebarn =
        Barnehagebarn(
            id = id,
            ident = ident,
            fom = fom,
            tom = tom,
            antallTimerIBarnehage = antallTimerIBarnehage,
            endringstype = endringstype,
            kommuneNavn = kommuneNavn,
            kommuneNr = kommuneNr,
            arkivReferanse = arkivReferanse,
            kildeTopic = kildeTopic,
        )
    barnehagebarn.endretTidspunkt = endretTidspunkt
    return barnehagebarn
}
