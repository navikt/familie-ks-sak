package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import mockAdopsjonService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.tilDagMånedÅr
import no.nav.familie.ks.sak.common.util.tilKortString
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVedtaksbegrunnelse
import no.nav.familie.ks.sak.data.lagVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelse
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelseType
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityBegrunnelserResponsDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.tilUtvidetVedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.BeregnAndelTilkjentYtelseService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFebruar2025.LovverkFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.LovverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.BegrunnelseType
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalOgFellesBegrunnelseDataDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.brevperioder.BrevPeriodeType
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BrevPeriodeContextTest {
    @Test
    fun `genererBrevPeriodeDto skal gi riktig output for innvilgetIkkeBarnehage-begrunnelse når alle vilkår er oppfylt`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater = emptyList(),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = false,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            listOf(barnFødselsdato.plusYears(1).førsteDagINesteMåned().tilDagMånedÅr()),
            brevPeriodeDto?.fom,
        )
        Assertions.assertEquals(
            listOf(
                "til " +
                    barnFødselsdato
                        .plusYears(2)
                        .minusMonths(1)
                        .sisteDagIMåned()
                        .tilDagMånedÅr() +
                    " ",
            ),
            brevPeriodeDto?.tom,
        )
        Assertions.assertEquals(listOf("7 500"), brevPeriodeDto?.belop)
        Assertions.assertEquals(listOf("1"), brevPeriodeDto?.antallBarn)
        Assertions.assertEquals(listOf(barnFødselsdato.tilKortString()), brevPeriodeDto?.barnasFodselsdager)
        Assertions.assertEquals(listOf(BrevPeriodeType.INNVILGELSE.apiNavn), brevPeriodeDto?.type)
        Assertions.assertEquals(listOf("1"), brevPeriodeDto?.antallBarnMedUtbetaling)
        Assertions.assertEquals(listOf("0"), brevPeriodeDto?.antallBarnMedNullutbetaling)
        Assertions.assertEquals(listOf(barnFødselsdato.tilKortString()), brevPeriodeDto?.fodselsdagerBarnMedUtbetaling)
        Assertions.assertEquals(listOf(""), brevPeriodeDto?.fodselsdagerBarnMedNullutbetaling)

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = "innvilgetIkkeBarnehage",
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = true,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "7 500",
                antallTimerBarnehageplass = "0",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }

    @Test
    fun `genererBrevPeriodeDto skal gi riktig output for innvilgetDeltidBarnehage-begrunnelse ved 17 timer barnehageplass`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater =
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNEHAGEPLASS,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                antallTimer = BigDecimal.valueOf(17),
                            ),
                        ),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = false,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = "innvilgetDeltidBarnehage",
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = true,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "3 000",
                antallTimerBarnehageplass = "17",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }

    @Test
    fun `genererBrevPeriodeDto skal gi riktig output for innvilgetDeltidBarnehageAdopsjon ved 17 timer barnehageplass`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater =
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNEHAGEPLASS,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                antallTimer = BigDecimal.valueOf(17),
                            ),
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNETS_ALDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                            ),
                        ),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE_ADOPSJON),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = false,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = "innvilgetDeltidBarnehageAdopsjon",
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = true,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "3 000",
                antallTimerBarnehageplass = "17",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }

    @Test
    fun `genererBrevPeriodeDto skal gi riktig output for innvilgetIkkeBarnehageAdopsjon`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater =
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNETS_ALDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                            ),
                        ),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = false,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = "innvilgetIkkeBarnehageAdopsjon",
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = true,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "7 500",
                antallTimerBarnehageplass = "0",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }

    @Test
    fun `genererBrevPeriodeDto skal gi true for gjelderAnnenForelder feltet dersom annen forelder ikke er vurdert i MedlemskapAnnenForelder vilkåret`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater =
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNETS_ALDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                            ),
                            lagVilkårResultat(
                                vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                                resultat = Resultat.OPPFYLT,
                            ),
                        ),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = true,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON.sanityApiNavn,
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = true,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "7 500",
                antallTimerBarnehageplass = "0",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }

    @Test
    fun `genererBrevPeriodeDto skal gi false for gjelderAnnenForelder feltet dersom annen forelder ikke er vurdert i MedlemskapAnnenForelder vilkåret`() {
        val barnFødselsdato = LocalDate.of(2021, 3, 15)

        val personerIbehandling =
            listOf(
                PersonIBehandling(
                    personType = PersonType.SØKER,
                    fødselsDato = LocalDate.now().minusYears(20),
                    overstyrendeVilkårResultater = emptyList(),
                ),
                PersonIBehandling(
                    personType = PersonType.BARN,
                    fødselsDato = barnFødselsdato,
                    overstyrendeVilkårResultater =
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BARNETS_ALDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                            ),
                            lagVilkårResultat(
                                vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                                periodeFom = barnFødselsdato.plusYears(1),
                                periodeTom = barnFødselsdato.plusYears(2),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.ADOPSJON),
                                resultat = Resultat.IKKE_AKTUELT,
                            ),
                        ),
                ),
            )

        val brevPeriodeDto =
            lagBrevPeriodeContext(
                personerIBehandling = personerIbehandling,
                begrunnelser = listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON),
                vedtaksperiodeType = Vedtaksperiodetype.UTBETALING,
                skalOppretteEndretUtbetalingAndeler = true,
            ).genererBrevPeriodeDto()

        Assertions.assertEquals(
            NasjonalOgFellesBegrunnelseDataDto(
                vedtakBegrunnelseType = BegrunnelseType.INNVILGET,
                apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE_ADOPSJON.sanityApiNavn,
                sanityBegrunnelseType = SanityBegrunnelseType.STANDARD,
                gjelderSoker = false,
                gjelderAndreForelder = false,
                barnasFodselsdatoer = barnFødselsdato.tilKortString(),
                antallBarn = 1,
                maanedOgAarBegrunnelsenGjelderFor = "april 2022",
                maalform = "bokmaal",
                belop = "7 500",
                antallTimerBarnehageplass = "0",
                soknadstidspunkt = "",
                maanedOgAarFoorVedtaksperiode = "mars 2022",
            ),
            brevPeriodeDto?.begrunnelser?.single(),
        )
    }
}

data class PersonIBehandling(
    val personType: PersonType,
    val fødselsDato: LocalDate,
    val overstyrendeVilkårResultater: List<VilkårResultat>,
)

/***
 * Lager vilkårsvurdering med alle vilkår oppfylt med fom=fødelsdato og tom=null for søker
 * og fom=fødelsdato + et år og tom=fødelsdato + to år for barn,
 * med mindre noe annet er spesifisert i overstyrendeVilkårResultater.
 *
 * Beregner andeler tilkjent ytelse ut fra vilkårResultatene og returnerer en BrevPeriodeContext
 * samtidig med den første andelen tilkjent ytelsen.
 */
fun lagBrevPeriodeContext(
    personerIBehandling: List<PersonIBehandling>,
    begrunnelser: List<NasjonalEllerFellesBegrunnelse>,
    vedtaksperiodeType: Vedtaksperiodetype,
    skalOppretteEndretUtbetalingAndeler: Boolean = false,
): BrevPeriodeContext {
    val barnIBehandling = personerIBehandling.filter { it.personType == PersonType.BARN }

    val persongrunnlag =
        lagPersonopplysningGrunnlag(
            barnasIdenter = barnIBehandling.map { randomFnr() },
            barnasFødselsdatoer = barnIBehandling.map { it.fødselsDato },
        )

    val barnPersonResultater =
        persongrunnlag
            .barna
            .zip(barnIBehandling)
            .map { (person, personIBehandling) ->
                lagPersonResultat(
                    person = person,
                    overstyrendeVilkårResultater = personIBehandling.overstyrendeVilkårResultater,
                )
            }

    val søkerPersonResultat =
        lagPersonResultat(
            person = persongrunnlag.søker,
            overstyrendeVilkårResultater =
                personerIBehandling.find { it.personType == PersonType.SØKER }?.overstyrendeVilkårResultater
                    ?: emptyList(),
        )

    val personResultater = barnPersonResultater + søkerPersonResultat

    val vilkårsvurdering = mockk<Vilkårsvurdering>(relaxed = true)
    every { vilkårsvurdering.personResultater } returns personResultater.toSet()

    val tilkjentYtelse = mockk<TilkjentYtelse>()
    every { tilkjentYtelse.behandling.id } returns 1

    val andelerTilkjentYtelse =
        BeregnAndelTilkjentYtelseService(
            andelGeneratorLookup = AndelGenerator.Lookup(listOf(LovverkFørFebruar2025AndelGenerator(), LovverkFebruar2025AndelGenerator())),
            adopsjonService = mockAdopsjonService(),
        ).beregnAndelerTilkjentYtelse(personopplysningGrunnlag = persongrunnlag, vilkårsvurdering = vilkårsvurdering, tilkjentYtelse = tilkjentYtelse)

    val vedtaksperiodeMedBegrunnelser =
        lagVedtaksperiodeMedBegrunnelser(
            fom = andelerTilkjentYtelse.first().stønadFom.førsteDagIInneværendeMåned(),
            tom = andelerTilkjentYtelse.first().stønadTom.sisteDagIInneværendeMåned(),
            begrunnelser = {
                begrunnelser.map { lagVedtaksbegrunnelse(it) }.toList()
            },
            type = vedtaksperiodeType,
        )

    val endretUtbetalingAndeler =
        if (skalOppretteEndretUtbetalingAndeler) {
            persongrunnlag.barna.map {
                EndretUtbetalingAndel(
                    behandlingId = 0,
                    person = it,
                    fom = YearMonth.of(2020, 12),
                    tom = vedtaksperiodeMedBegrunnelser.fom?.toYearMonth()?.minusMonths(1),
                    årsak = Årsak.ETTERBETALING_3MND,
                    prosent = BigDecimal(0),
                    begrunnelse = "Test formål",
                    søknadstidspunkt = LocalDate.of(2020, 12, 12),
                )
            }
        } else {
            emptyList()
        }

    val andelTilkjentYtelserMedEndreteUtbetalinger =
        andelerTilkjentYtelse.map { AndelTilkjentYtelseMedEndreteUtbetalinger(it, endretUtbetalingAndeler) }

    return BrevPeriodeContext(
        utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                persongrunnlag,
                andelTilkjentYtelserMedEndreteUtbetalinger,
                emptyList(),
            ),
        sanityBegrunnelser = lagSanityBegrunnelserFraDump(),
        personopplysningGrunnlag = persongrunnlag,
        personResultater = personResultater,
        andelTilkjentYtelserMedEndreteUtbetalinger = andelTilkjentYtelserMedEndreteUtbetalinger,
        uregistrerteBarn = emptyList(),
        kompetanser = emptyList(),
        landkoder = LANDKODER,
        erFørsteVedtaksperiode = false,
        overgangsordningAndeler = emptyList(),
        adopsjonerIBehandling = emptyList(),
    )
}

fun lagPersonResultat(
    person: Person,
    overstyrendeVilkårResultater: List<VilkårResultat> = emptyList(),
): PersonResultat {
    val personResultat =
        PersonResultat(
            vilkårsvurdering = mockk(relaxed = true),
            aktør = person.aktør,
        )

    personResultat.setSortedVilkårResultater(
        lagVilkårResultater(
            person = person,
            overstyrendeVilkårResultater = overstyrendeVilkårResultater,
            personResultat = personResultat,
        ).toSet(),
    )

    return personResultat
}

fun lagVilkårResultater(
    person: Person,
    overstyrendeVilkårResultater: List<VilkårResultat> = emptyList(),
    personResultat: PersonResultat,
): List<VilkårResultat> {
    val vilkårResultaterForBarn =
        Vilkår
            .hentVilkårFor(person.type)
            .filter { vilkår -> overstyrendeVilkårResultater.none { it.vilkårType == vilkår } }
            .map {
                lagVilkårResultat(
                    personResultat = personResultat,
                    vilkårType = it,
                    periodeFom = if (person.type == PersonType.SØKER) person.fødselsdato else person.fødselsdato.plusYears(1),
                    periodeTom = if (person.type == PersonType.SØKER) null else person.fødselsdato.plusYears(2),
                    behandlingId = 0L,
                    antallTimer = null,
                )
            }
    return vilkårResultaterForBarn + overstyrendeVilkårResultater
}

fun lagSanityBegrunnelserFraDump(): List<SanityBegrunnelse> {
    val fil = File("./src/test/resources/sanityDump/begrunnelser.json")

    return objectMapper
        .readValue(
            fil.readText(),
            SanityBegrunnelserResponsDto::class.java,
        ).result
        .map { it.tilSanityBegrunnelse() }
}
