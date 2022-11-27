package no.nav.familie.ks.sak.data

import io.mockk.mockk
import no.nav.commons.foedselsnummer.testutils.FoedselsnummerGenerator
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.KJOENN
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
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
import no.nav.familie.ks.sak.api.dto.RegistrerSøknadDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.integrasjon.pdl.domene.ForelderBarnRelasjonInfo
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.Vedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.maksBeløp
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.Begrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.Personident
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Kjønn
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.bostedsadresse.GrBostedsadresse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.dødsfall.Dødsfall
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.sivilstand.GrSivilstand
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap.GrStatsborgerskap
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

val fødselsnummerGenerator = FoedselsnummerGenerator()

fun randomFnr(): String = fødselsnummerGenerator.foedselsnummer().asString

fun randomAktørId(): String = Random.nextLong(1000_000_000_000, 31_121_299_99999).toString()

fun randomAktør(fnr: String = randomFnr()): Aktør =
    Aktør(randomAktørId()).also {
        it.personidenter.add(
            randomPersonident(it, fnr)
        )
    }

fun randomPersonident(aktør: Aktør, fnr: String = randomFnr()): Personident =
    Personident(fødselsnummer = fnr, aktør = aktør)

fun fnrTilAktør(fnr: String, toSisteSiffrer: String = "00") = Aktør(fnr + toSisteSiffrer).also {
    it.personidenter.add(Personident(fnr, aktør = it))
}

fun lagPersonopplysningGrunnlag(
    behandlingId: Long = 0L,
    søkerPersonIdent: String = randomFnr(),
    barnasIdenter: List<String> = emptyList(), // FGB med register søknad steg har ikke barnasidenter
    barnasFødselsdatoer: List<LocalDate> = barnasIdenter.map { fnrTilFødselsdato(it) },
    søkerAktør: Aktør = fnrTilAktør(søkerPersonIdent).also {
        it.personidenter.add(
            Personident(
                fødselsnummer = søkerPersonIdent,
                aktør = it,
                aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer
            )
        )
    },
    barnAktør: List<Aktør> = barnasIdenter.map { fødselsnummer ->
        fnrTilAktør(fødselsnummer).also {
            it.personidenter.add(
                Personident(
                    fødselsnummer = fødselsnummer,
                    aktør = it,
                    aktiv = fødselsnummer == it.personidenter.first().fødselsnummer
                )
            )
        }
    },
    søkerDødsDato: LocalDate? = null
): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)

    val søker = Person(
        aktør = søkerAktør,
        type = PersonType.SØKER,
        personopplysningGrunnlag = personopplysningGrunnlag,
        fødselsdato = fnrTilFødselsdato(søkerPersonIdent),
        navn = "",
        kjønn = Kjønn.KVINNE
    ).also { søker ->
        søker.statsborgerskap =
            mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = søker))
        søker.bostedsadresser = mutableListOf()
        søker.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.GIFT, person = søker))
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
                kjønn = Kjønn.MANN
            ).also { barn ->
                barn.statsborgerskap =
                    mutableListOf(GrStatsborgerskap(landkode = "NOR", medlemskap = Medlemskap.NORDEN, person = barn))
                barn.bostedsadresser = mutableListOf()
                barn.sivilstander = mutableListOf(GrSivilstand(type = SIVILSTAND.UGIFT, person = barn))
            }
        )
    }
    return personopplysningGrunnlag
}

fun lagFagsak(aktør: Aktør = randomAktør(randomFnr()), id: Long = 0) = Fagsak(aktør = aktør, id = id)

private var gjeldendeBehandlingId: Long = abs(Random.nextLong(10000000))

private const val ID_INKREMENT = 50
fun nesteBehandlingId(): Long {
    gjeldendeBehandlingId += ID_INKREMENT
    return gjeldendeBehandlingId
}

fun lagBehandling(
    fagsak: Fagsak = lagFagsak(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    opprettetÅrsak: BehandlingÅrsak,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL
): Behandling = Behandling(
    id = nesteBehandlingId(),
    fagsak = fagsak,
    type = type,
    opprettetÅrsak = opprettetÅrsak,
    kategori = kategori
).initBehandlingStegTilstand()

fun lagBehandlingStegTilstand(
    behandling: Behandling,
    behandlingSteg: BehandlingSteg,
    behandlingStegStatus: BehandlingStegStatus
) = behandling.behandlingStegTilstand.add(
    BehandlingStegTilstand(
        behandling = behandling,
        behandlingSteg = behandlingSteg,
        behandlingStegStatus = behandlingStegStatus
    )
)

fun lagArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling = ArbeidsfordelingPåBehandling(
    id = 123,
    behandlingId = behandlingId,
    behandlendeEnhetId = "4321",
    behandlendeEnhetNavn = "Test enhet",
    manueltOverstyrt = false
)

fun lagRegistrerSøknadDto() = RegistrerSøknadDto(
    søknad = SøknadDto(
        søkerMedOpplysninger = SøkerMedOpplysningerDto(ident = randomFnr()),
        barnaMedOpplysninger = listOf(BarnMedOpplysningerDto(ident = randomFnr())),
        endringAvOpplysningerBegrunnelse = ""
    ),
    bekreftEndringerViaFrontend = true
)

fun lagPdlPersonInfo(enkelPersonInfo: Boolean = false, erBarn: Boolean = false) = PdlPersonInfo(
    fødselsdato = if (erBarn) LocalDate.now().minusYears(1) else LocalDate.of(1987, 5, 1),
    navn = "John Doe",
    kjønn = KJOENN.MANN,
    forelderBarnRelasjoner = if (enkelPersonInfo) emptySet() else setOf(lagForelderBarnRelasjon()),
    bostedsadresser = listOf(lagBostedsadresse()),
    sivilstander = listOf(lagSivilstand()),
    statsborgerskap = listOf(lagStatsborgerskap())
)

fun lagForelderBarnRelasjon(): ForelderBarnRelasjonInfo = ForelderBarnRelasjonInfo(
    aktør = randomAktør(),
    relasjonsrolle = FORELDERBARNRELASJONROLLE.BARN,
    navn = "Ny barn",
    fødselsdato = LocalDate.now().minusYears(1)
)

fun lagBostedsadresse(): Bostedsadresse = Bostedsadresse(
    gyldigFraOgMed = LocalDate.of(2015, 1, 1),
    vegadresse = Vegadresse(
        matrikkelId = 1234,
        husnummer = "3",
        husbokstav = null,
        bruksenhetsnummer = null,
        adressenavn = "OTTO SVERDRUPS VEG",
        kommunenummer = "1560",
        postnummer = "6650",
        tilleggsnavn = null
    )
)

fun lagSivilstand(): Sivilstand = Sivilstand(type = SIVILSTAND.UGIFT, gyldigFraOgMed = LocalDate.of(2004, 12, 2))

fun lagStatsborgerskap(land: String = "NOR"): Statsborgerskap = Statsborgerskap(
    land = land,
    gyldigFraOgMed = LocalDate.of(1987, 9, 1),
    gyldigTilOgMed = null,
    bekreftelsesdato = LocalDate.of(1987, 9, 1)
)

fun lagInitieltTilkjentYtelse(behandling: Behandling) =
    TilkjentYtelse(behandling = behandling, opprettetDato = LocalDate.now(), endretDato = LocalDate.now())

fun lagAndelTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse? = null,
    behandling: Behandling,
    aktør: Aktør? = null,
    stønadFom: YearMonth = YearMonth.now().minusMonths(1),
    stønadTom: YearMonth = YearMonth.now().plusMonths(8),
    sats: Int = maksBeløp(),
    periodeOffset: Long? = null,
    forrigePeriodeOffset: Long? = null
) = AndelTilkjentYtelse(
    behandlingId = behandling.id,
    tilkjentYtelse = tilkjentYtelse ?: lagInitieltTilkjentYtelse(behandling),
    aktør = aktør ?: behandling.fagsak.aktør,
    kalkulertUtbetalingsbeløp = sats,
    stønadFom = stønadFom,
    stønadTom = stønadTom,
    type = YtelseType.ORDINÆR_KONTANTSTØTTE,
    sats = sats,
    prosent = BigDecimal(100),
    nasjonaltPeriodebeløp = sats,
    periodeOffset = periodeOffset,
    forrigePeriodeOffset = forrigePeriodeOffset
)

fun lagPerson(
    personopplysningGrunnlag: PersonopplysningGrunnlag,
    aktør: Aktør,
    personType: PersonType = PersonType.SØKER
): Person {
    val person = Person(
        type = personType,
        fødselsdato = fnrTilFødselsdato(aktør.aktivFødselsnummer()),
        kjønn = Kjønn.KVINNE,
        personopplysningGrunnlag = personopplysningGrunnlag,
        aktør = aktør
    )
    person.bostedsadresser = mutableListOf(GrBostedsadresse.fraBostedsadresse(lagBostedsadresse(), person))
    person.statsborgerskap =
        mutableListOf(GrStatsborgerskap.fraStatsborgerskap(lagStatsborgerskap(), Medlemskap.NORDEN, person))
    person.sivilstander = mutableListOf(GrSivilstand.fraSivilstand(lagSivilstand(), person))

    return person
}

fun lagVilkårsvurderingMedSøkersVilkår(
    søkerAktør: Aktør,
    behandling: Behandling,
    resultat: Resultat,
    søkerPeriodeFom: LocalDate? = LocalDate.now().minusMonths(1),
    søkerPeriodeTom: LocalDate? = LocalDate.now().plusYears(2)
): Vilkårsvurdering {
    val vilkårsvurdering = Vilkårsvurdering(
        behandling = behandling
    )
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = søkerAktør
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
                behandlingId = behandling.id
            ),
            VilkårResultat(
                personResultat = personResultat,
                vilkårType = Vilkår.MEDLEMSKAP,
                resultat = resultat,
                periodeFom = søkerPeriodeFom,
                periodeTom = søkerPeriodeTom,
                begrunnelse = "",
                behandlingId = behandling.id
            )
        )
    )
    personResultat.andreVurderinger.add(
        AnnenVurdering(
            personResultat = personResultat,
            resultat = resultat,
            type = AnnenVurderingType.OPPLYSNINGSPLIKT,
            begrunnelse = null
        )
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
    antallTimer: BigDecimal? = null
): VilkårResultat = VilkårResultat(
    id = id,
    personResultat = personResultat,
    vilkårType = vilkårType,
    resultat = resultat,
    periodeFom = periodeFom,
    periodeTom = periodeTom,
    begrunnelse = begrunnelse,
    behandlingId = behandlingId,
    utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
    antallTimer = antallTimer
)

fun lagVilkårResultaterForBarn(
    personResultat: PersonResultat,
    barnFødselsdato: LocalDate,
    barnehageplassPerioder: List<Pair<NullablePeriode, BigDecimal?>>,
    behandlingId: Long
): Set<VilkårResultat> {
    val vilkårResultaterForBarn = mutableSetOf<VilkårResultat>()
    Vilkår.hentVilkårFor(PersonType.BARN).forEach {
        when (it) {
            Vilkår.BARNETS_ALDER -> vilkårResultaterForBarn.add(
                lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    periodeFom = barnFødselsdato.plusYears(1),
                    periodeTom = barnFødselsdato.plusYears(2),
                    behandlingId = behandlingId
                )
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
                            antallTimer = perioderMedAntallTimer.second
                        )
                    }
                )
            }

            else -> vilkårResultaterForBarn.add(
                lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    periodeFom = barnFødselsdato,
                    periodeTom = null,
                    behandlingId = behandlingId
                )
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
    behandlingId: Long
): Set<VilkårResultat> {
    val vilkårResultaterForBarn = mutableSetOf<VilkårResultat>()
    Vilkår.hentVilkårFor(PersonType.BARN).forEach {
        when (it) {
            Vilkår.BOR_MED_SØKER -> {
                val vilkårResultatMedDeltBosted1 = lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    periodeFom = fom1,
                    periodeTom = tom1,
                    behandlingId = behandlingId,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                )
                vilkårResultaterForBarn.add(vilkårResultatMedDeltBosted1)
                if (fom2 != null && tom2 != null) {
                    val vilkårResultatMedDeltBosted2 = lagVilkårResultat(
                        personResultat = personResultat,
                        vilkårType = it,
                        periodeFom = fom2,
                        periodeTom = tom2,
                        behandlingId = behandlingId,
                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                    )
                    vilkårResultaterForBarn.add(vilkårResultatMedDeltBosted2)
                }
            }

            else -> vilkårResultaterForBarn.add(
                lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    periodeFom = fom1,
                    periodeTom = tom2 ?: tom1,
                    behandlingId = behandlingId
                )
            )
        }
    }
    return vilkårResultaterForBarn
}

fun lagEndretUtbetalingAndel(
    behandlingId: Long,
    person: Person,
    prosent: BigDecimal? = null,
    periodeFom: YearMonth = YearMonth.now().minusMonths(1),
    periodeTom: YearMonth = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate? = LocalDate.now().minusMonths(1)
): EndretUtbetalingAndel = EndretUtbetalingAndel(
    behandlingId = behandlingId,
    person = person,
    prosent = prosent,
    fom = periodeFom,
    tom = periodeTom,
    årsak = årsak,
    avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
    søknadstidspunkt = LocalDate.now().minusMonths(1),
    begrunnelse = "test"
)

fun lagVedtaksbegrunnelse(
    begrunnelse: Begrunnelse =
        Begrunnelse.INNVILGET_IKKE_BARNEHAGE,
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser = mockk()
) = Vedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
    begrunnelse = begrunnelse
)

fun lagVedtaksperiodeMedBegrunnelser(
    vedtak: Vedtak = Vedtak(behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)),
    fom: LocalDate? = LocalDate.now().withDayOfMonth(1),
    tom: LocalDate? = LocalDate.now().let { it.withDayOfMonth(it.lengthOfMonth()) },
    type: Vedtaksperiodetype = Vedtaksperiodetype.FORTSATT_INNVILGET,
    begrunnelser: MutableSet<Vedtaksbegrunnelse> = mutableSetOf(lagVedtaksbegrunnelse()),
    fritekster: MutableList<VedtaksbegrunnelseFritekst> = mutableListOf()
) = VedtaksperiodeMedBegrunnelser(
    vedtak = vedtak,
    fom = fom,
    tom = tom,
    type = type,
    begrunnelser = begrunnelser,
    fritekster = fritekster
)

fun lagPersonResultat(
    vilkårsvurdering: Vilkårsvurdering,
    aktør: Aktør,
    resultat: Resultat,
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    lagFullstendigVilkårResultat: Boolean = false,
    personType: PersonType = PersonType.BARN,
    vilkårType: Vilkår = Vilkår.BOSATT_I_RIKET,
    erDeltBosted: Boolean = false
): PersonResultat {
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = aktør
    )

    if (lagFullstendigVilkårResultat) {
        personResultat.setSortedVilkårResultater(
            Vilkår.hentVilkårFor(personType).map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = it,
                    resultat = resultat,
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id,
                    utdypendeVilkårsvurderinger = listOfNotNull(
                        when {
                            erDeltBosted && it == Vilkår.BOR_MED_SØKER -> UtdypendeVilkårsvurdering.DELT_BOSTED
                            else -> null
                        }
                    )
                )
            }.toSet()
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
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )
    }
    return personResultat
}

fun lagTilkjentYtelse(utbetalingsoppdrag: Utbetalingsoppdrag, behandling: Behandling) =
    TilkjentYtelse(
        utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now()
    )

fun lagUtbetalingsoppdrag(utbetalingsperiode: List<Utbetalingsperiode>) = Utbetalingsoppdrag(
    kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
    fagSystem = "KS",
    saksnummer = "",
    aktoer = UUID.randomUUID().toString(),
    saksbehandlerId = "",
    avstemmingTidspunkt = LocalDateTime.now(),
    utbetalingsperiode = utbetalingsperiode
)

fun lagUtbetalingsperiode(opphør: Opphør? = null) = Utbetalingsperiode(
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
    behandlingId = 0
)

fun lagØkonomiSimuleringMottaker(
    behandling: Behandling,
    økonomiSimuleringPosteringer: List<ØkonomiSimuleringPostering> = emptyList()
) = ØkonomiSimuleringMottaker(
    mottakerType = MottakerType.BRUKER,
    mottakerNummer = "",
    behandling = behandling,
    økonomiSimuleringPostering = økonomiSimuleringPosteringer
)

fun lagØkonomiSimuleringPostering(
    behandling: Behandling,
    fom: LocalDate,
    tom: LocalDate,
    beløp: BigDecimal,
    forfallsdato: LocalDate
) = ØkonomiSimuleringPostering(
    økonomiSimuleringMottaker = lagØkonomiSimuleringMottaker(
        behandling = behandling
    ),
    fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
    fom = fom,
    tom = tom,
    betalingType = BetalingType.DEBIT,
    beløp = beløp,
    posteringType = PosteringType.YTELSE,
    forfallsdato = forfallsdato,
    utenInntrekk = false
)

fun lagSimulertMottaker(simulertePosteringer: List<SimulertPostering>) = SimuleringMottaker(
    mottakerType = MottakerType.BRUKER,
    mottakerNummer = "",
    simulertPostering = simulertePosteringer
)

fun lagSimulertPostering(fom: LocalDate, tom: LocalDate, beløp: BigDecimal, forfallsdato: LocalDate) =
    SimulertPostering(
        fagOmrådeKode = FagOmrådeKode.KONTANTSTØTTE,
        fom = fom,
        tom = tom,
        betalingType = BetalingType.DEBIT,
        beløp = beløp,
        posteringType = PosteringType.YTELSE,
        forfallsdato = forfallsdato,
        utenInntrekk = true
    )

fun fnrTilFødselsdato(fnr: String): LocalDate {
    val day = fnr.substring(0, 2).toInt()
    val month = fnr.substring(2, 4).toInt().let {
        if (it - 40 > 0) {
            it - 40
        }
        it
    }
    val year = fnr.substring(4, 6).toInt().let { if (it < (LocalDate.now().year - 2000)) it + 2000 else it + 1900 }
    return LocalDate.of(year, month, day)
}

fun årMåned(årMåned: String) = YearMonth.parse(årMåned)
fun dato(s: String) = LocalDate.parse(s)
