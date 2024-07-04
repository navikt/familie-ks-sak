package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ks.sak.api.dto.tilKompetanseDto
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.tidslinje.tomTidslinje
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagKompetanse
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.jan
import no.nav.familie.ks.sak.kjerne.eøs.util.KompetanseBuilder
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.EndretUtbetalingAndelTidslinjeService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjer
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class KompetanseServiceTest {
    private val kompetanseRepository: EøsSkjemaRepository<Kompetanse> = mockEøsSkjemaRepository()
    private val personidentService: PersonidentService = mockk()
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService = mockk()
    private val endretUtbetalingAndelTidslinjeService: EndretUtbetalingAndelTidslinjeService = mockk()
    private val kompetanseService: KompetanseService =
        KompetanseService(
            kompetanseRepository = kompetanseRepository,
            kompetanseEndringsAbonnenter = emptyList(),
            personidentService = personidentService,
            vilkårsvurderingTidslinjeService = vilkårsvurderingTidslinjeService,
            endretUtbetalingAndelTidslinjeService = endretUtbetalingAndelTidslinjeService,
        )

    private val søker = randomAktør()
    private val barn1 = randomAktør()
    private val barn2 = randomAktør()
    private val barn3 = randomAktør()
    private val behandling = lagBehandling(kategori = BehandlingKategori.EØS)
    private val behandlingId = BehandlingId(behandling.id)

    @BeforeEach
    fun init() {
        every { personidentService.hentAktør(barn1.aktivFødselsnummer()) } returns barn1
        every { personidentService.hentAktør(barn2.aktivFødselsnummer()) } returns barn2
        every { personidentService.hentAktør(barn3.aktivFødselsnummer()) } returns barn3
        every { endretUtbetalingAndelTidslinjeService.hentBarnasHarEtterbetaling3MånedTidslinjer(behandlingId.id) } returns
            emptyMap()
        kompetanseRepository.deleteAll()
    }

    @Test
    fun `oppdaterKompetanse bare reduksjon av periode skal ikke føre til endring i kompetansen`() {
        // 2022.01-2022.08 for barn1 med resultat  NORGE_ER_SEKUNDÆRLAND
        val eksisterendeKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 8),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                barnAktører = setOf(barn1),
            ).lagreTil(kompetanseRepository)

        // oppdatering er bare reduksjon i periode 2022.03-2022.07
        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 3),
                tom = YearMonth.of(2022, 7),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                barnAktører = setOf(barn1),
            )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())
        // Det forventer ingen endring når det bare er endring i periode
        assertThat(listOf(eksisterendeKompetanse)).containsExactlyInAnyOrderElementsOf(
            kompetanseService.hentKompetanser(behandlingId),
        )
    }

    @Test
    fun `oppdaterKompetanse oppdatering som splitter kompetanse fulgt av sletting skal returnere til utgangspunktet`() {
        // kompetanse med tre barn mellom 2022.01-2022.09, men andre verdiene er ikke satt enda
        val eksisterendeKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 9),
                barnAktører = setOf(barn1, barn2, barn3),
                annenForeldersAktivitetsland = null,
                annenForeldersAktivitet = null,
                barnetsBostedsland = null,
                søkersAktivitetsland = null,
                søkersAktivitet = null,
            ).lagreTil(kompetanseRepository)

        // oppdateringen for barn2 og barn3 for periode 2022.03-2022.04 med primærland
        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 3),
                tom = YearMonth.of(2022, 4),
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                barnAktører = setOf(barn2, barn3),
            )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppdatering medfører splitt i kompetanse perioder
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 4)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 4),
            barnAktører = setOf(barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[1],
        )
        assertKompetanse(
            // periode med null resultat for barn1 i denne perioden
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 4),
            barnAktører = setOf(barn1),
            hentetKompetanse = kompetanser[2],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 5),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[3],
        )

        // Hvis SB fjener oppdateringen, retuneres det til eksisterde kompetanse
        val kompetanseSomSkalSlettes =
            kompetanseService
                .hentKompetanser(behandlingId)
                .first { it == oppdateresKompetanse }
        kompetanseService.slettKompetanse(kompetanseSomSkalSlettes.id)

        assertThat(listOf(eksisterendeKompetanse)).containsExactlyInAnyOrderElementsOf(
            kompetanseService.hentKompetanser(behandlingId),
        )
    }

    @Test
    fun `oppdatering som endrer deler av en kompetanse, skal resultarere i en splitt`() {
        val eksisterendeKompetanse1 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 3),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        val eksisterendeKompetanse2 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 9),
                barnAktører = setOf(barn2, barn3),
            )
        val eksisterendeKompetanse3 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 7),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        listOf(eksisterendeKompetanse1, eksisterendeKompetanse2, eksisterendeKompetanse3).lagreTil(kompetanseRepository)

        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 2),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            )
        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppdatering medfører splitt i kompetanse perioder
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 3)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[1],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[2],
        )
    }

    @Test
    fun `oppdaterKompetanse skal kunne sende inn oppdatering som overlapper flere kompetanser`() {
        val eksisterendeKompetanse1 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 3),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        val eksisterendeKompetanse2 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 9),
                barnAktører = setOf(barn2, barn3),
            )
        val eksisterendeKompetanse3 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 7),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        listOf(eksisterendeKompetanse1, eksisterendeKompetanse2, eksisterendeKompetanse3).lagreTil(kompetanseRepository)

        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 3),
                tom = YearMonth.of(2022, 5),
                barnAktører = setOf(barn1, barn2, barn3),
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            )
        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppdatering medfører splitt i kompetanse perioder
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 5)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[1],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 5),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[2],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 6),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[3],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 6),
            tom = YearMonth.of(2022, 9),
            barnAktører = setOf(barn2, barn3),
            hentetKompetanse = kompetanser[4],
        )
    }

    @Test
    fun `oppdaterKompetanse skal kunne lukke åpen kompetanse ved å sende inn identisk skjema med tom dato`() {
        // Åpen (tom dato er null) kompetanse med sekundærland for tre barn
        lagKompetanse(
            behandlingId = behandlingId.id,
            fom = YearMonth.of(2022, 1),
            tom = null,
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
        ).lagreTil(kompetanseRepository)

        // Endrer kun tom dato fra null til en gitt dato
        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 3),
                barnAktører = setOf(barn1, barn2, barn3),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppretter et tomt skjema fra oppdatert dato og fremover
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 2)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 3),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 4),
            tom = null,
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[1],
        )
    }

    @Test
    fun `oppdaterKompetanse skal kunne forkorte tom dato ved å sende inn identisk skjema med tidligere tom dato`() {
        // Kompetanse med sekundærland for tre barn med tom dato
        lagKompetanse(
            behandlingId = behandlingId.id,
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
        ).lagreTil(kompetanseRepository)

        // Endrer kun tom dato til tidligere tidspunkt
        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 3),
                barnAktører = setOf(barn1, barn2, barn3),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppretter et tomt skjema fra oppdatert dato og fremover til original tom dato
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 2)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = YearMonth.of(2022, 3),
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 4),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1, barn2, barn3),
            hentetKompetanse = kompetanser[1],
        )
    }

    @Test
    fun `oppdaterKompetanse skal opprette tomt skjema for barn som fjernes fra ellers uendret skjema`() {
        // Åpen (tom dato er null) kompetanse med sekundærland for tre barn
        lagKompetanse(
            behandlingId = behandlingId.id,
            fom = YearMonth.of(2022, 1),
            tom = null,
            barnAktører = setOf(barn1, barn2, barn3),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
        ).lagreTil(kompetanseRepository)

        // Fjerner barn3 fra gjeldende skjema, ellers likt
        val oppdateresKompetanse =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = null,
                barnAktører = setOf(barn1, barn2),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )

        kompetanseService.oppdaterKompetanse(behandlingId, oppdateresKompetanse.tilKompetanseDto())

        // oppretter et tomt skjema for samme periode for barn3 som var fjernet
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 2)

        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = null,
            barnAktører = setOf(barn3),
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 1),
            tom = null,
            barnAktører = setOf(barn1, barn2),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[1],
        )
    }

    @Test
    fun `tilpassKompetanse skal opprette kompetanseskjema med ingen sluttdato når regelverk-tidslinjer fortsetter etter nåtidspunktet`() {
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(any()) } returns tomTidslinje()

        val fom = LocalDate.now().minusMonths(3).førsteDagIInneværendeMåned()
        val tom = fom.plusMonths(10).sisteDagIMåned()
        val tomForBarn2 = fom.plusMonths(6).sisteDagIMåned()

        val vilkårsvurdering =
            lagVilkårsvurdering(
                // søkersperiode avslutter etter nå tidspunkt
                søkersperiode = Periode(fom, tom),
                barnasperioder =
                    mapOf(
                        barn1 to Pair(fom, tom),
                        barn2 to Pair(fom, tomForBarn2),
                    ),
                // begge barnasperiode avslutter etter nå tidspunkt
            )
        val vilkårsvurderingTidslinjer = lagVilkårsvurderingTidslinjer(vilkårsvurdering)

        every { vilkårsvurderingTidslinjeService.lagVilkårsvurderingTidslinjer(behandlingId.id) } returns vilkårsvurderingTidslinjer

        kompetanseService.tilpassKompetanse(behandlingId)

        // henter kompetanse perioder som opprettes automatisk etter vilkårsvurdering
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 1)

        assertKompetanse(
            fom = fom.plusMonths(1).toYearMonth(),
            tom = null,
            barnAktører = setOf(barn1, barn2),
            hentetKompetanse = kompetanser[0],
        )
    }

    @Test
    fun `tilpassKompetanse skal opprette kompetanse med sluttdato når et av barnets regelverk-tidslinjer avsluttes før nåtidspunktet`() {
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(any()) } returns tomTidslinje()

        val fom = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned()
        val tom = fom.plusMonths(10).sisteDagIMåned()
        val tomForBarn2 = fom.plusMonths(3).sisteDagIMåned()

        val vilkårsvurdering =
            lagVilkårsvurdering(
                // søkersperiode avslutter etter nåtidspunkt
                søkersperiode = Periode(fom, tom),
                barnasperioder =
                    mapOf(
                        // barnetsperiode avslutter etter nåtidspunkt
                        barn1 to Pair(fom, tom),
                        // barnetsperiode avslutter før nåtidspunkt
                        barn2 to Pair(fom, tomForBarn2),
                    ),
            )
        val vilkårsvurderingTidslinjer = lagVilkårsvurderingTidslinjer(vilkårsvurdering)

        every { vilkårsvurderingTidslinjeService.lagVilkårsvurderingTidslinjer(behandlingId.id) } returns vilkårsvurderingTidslinjer

        kompetanseService.tilpassKompetanse(behandlingId)

        // henter kompetanse perioder som opprettes automatisk etter vilkårsvurdering
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 2)

        assertKompetanse(
            fom = fom.plusMonths(1).toYearMonth(),
            tom = tomForBarn2.minusMonths(1).toYearMonth(),
            barnAktører = setOf(barn1, barn2),
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = tomForBarn2.toYearMonth(),
            tom = null,
            barnAktører = setOf(barn1),
            hentetKompetanse = kompetanser[1],
        )
    }

    @Test
    fun `tilpassKompetanse skal tilpasse kompetanser til endrede regelverk-tidslinjer`() {
        every { vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(any()) } returns tomTidslinje()

        val eksisterendeKompetanse1 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 2),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        val eksisterendeKompetanse2 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 1),
                tom = YearMonth.of(2022, 2),
                barnAktører = setOf(barn2, barn3),
            )
        val eksisterendeKompetanse3 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 3),
                tom = YearMonth.of(2022, 5),
                barnAktører = setOf(barn1, barn2, barn3),
                resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            )
        val eksisterendeKompetanse4 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 6),
                tom = YearMonth.of(2022, 7),
                barnAktører = setOf(barn1),
                resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            )
        val eksisterendeKompetanse5 =
            lagKompetanse(
                behandlingId = behandlingId.id,
                fom = YearMonth.of(2022, 6),
                tom = YearMonth.of(2022, 9),
                barnAktører = setOf(barn2, barn3),
            )
        listOf(
            eksisterendeKompetanse1,
            eksisterendeKompetanse2,
            eksisterendeKompetanse3,
            eksisterendeKompetanse4,
            eksisterendeKompetanse5,
        ).lagreTil(kompetanseRepository)

        val fom = LocalDate.of(2022, 1, 1)
        val tom = fom.plusMonths(10).sisteDagIMåned()
        val fomForBarn2 = fom.plusMonths(2)
        val tomForBarn2 = fomForBarn2.plusMonths(3).sisteDagIMåned()

        val vilkårsvurdering =
            lagVilkårsvurdering(
                // søkersperiode 2022-01-2022-11
                søkersperiode = Periode(fom, tom),
                barnasperioder =
                    mapOf(
                        // barn1 periode 2022-01-2022-11
                        barn1 to Pair(fom, tom),
                        // barn2 periode 2022-03-2022-06
                        barn2 to Pair(fomForBarn2, tomForBarn2),
                        // barn3 periode 2022-01-null
                        barn3 to Pair(fom, null),
                    ),
            )
        val vilkårsvurderingTidslinjer = lagVilkårsvurderingTidslinjer(vilkårsvurdering)

        every { vilkårsvurderingTidslinjeService.lagVilkårsvurderingTidslinjer(behandlingId.id) } returns
            vilkårsvurderingTidslinjer

        kompetanseService.tilpassKompetanse(behandlingId)

        // henter kompetanse perioder som tilpasses etter endringer i vilkårsvurdering
        val kompetanser = kompetanseService.hentKompetanser(behandlingId).sortedBy { it.fom }
        assertThat(kompetanser.size == 6)

        assertKompetanse(
            fom = YearMonth.of(2022, 2),
            tom = YearMonth.of(2022, 2),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[0],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 3),
            tom = YearMonth.of(2022, 3),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[1],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 4),
            tom = YearMonth.of(2022, 5),
            barnAktører = setOf(barn1, barn2),
            resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
            hentetKompetanse = kompetanser[2],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 6),
            tom = YearMonth.of(2022, 7),
            barnAktører = setOf(barn1),
            resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
            hentetKompetanse = kompetanser[3],
        )
        assertKompetanse(
            fom = YearMonth.of(2022, 8),
            tom = YearMonth.of(2022, 10),
            barnAktører = setOf(barn1),
            hentetKompetanse = kompetanser[4],
        )
    }

    @Test
    fun `skal kopiere over kompetanse-skjema fra forrige behandling til ny behandling`() {
        val behandlingId1 = BehandlingId(10L)
        val behandlingId2 = BehandlingId(11L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val kompetanser =
            KompetanseBuilder(jan(2020), behandlingId1)
                .medKompetanse(
                    "SSS",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = true,
                ).medKompetanse(
                    "---------",
                    barn2,
                    barn3,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = false,
                ).medKompetanse(
                    "   SSSS",
                    barn1,
                    annenForeldersAktivitetsland = null,
                    erAnnenForelderOmfattetAvNorskLovgivning = true,
                ).lagreTil(kompetanseRepository)

        kompetanseService.kopierOgErstattKompetanser(behandlingId1, behandlingId2)

        val kompetanserBehandling2 = kompetanseService.hentKompetanser(behandlingId2)

        assertThat(kompetanserBehandling2).hasSize(kompetanserBehandling2.size).containsAll(kompetanser)

        kompetanserBehandling2.forEach {
            assertEquals(behandlingId2.id, it.behandlingId)
        }

        val kompetanserBehandling1 = kompetanseService.hentKompetanser(behandlingId1)

        kompetanserBehandling1.forEach {
            assertEquals(behandlingId1.id, it.behandlingId)
        }

        assertThat(kompetanserBehandling1).hasSize(kompetanserBehandling1.size).containsAll(kompetanser)
    }

    @Test
    fun `skal kopiere kompetanser fra en behandling til en annen behandling, og overskrive eksisterende`() {
        val behandlingId1 = BehandlingId(10L)
        val behandlingId2 = BehandlingId(22L)
        val barn1 = tilfeldigPerson(personType = PersonType.BARN)
        val barn2 = tilfeldigPerson(personType = PersonType.BARN)
        val barn3 = tilfeldigPerson(personType = PersonType.BARN)

        val kompetanser1 =
            KompetanseBuilder(jan(2020), behandlingId1)
                .medKompetanse("SS   SS", barn1)
                .medKompetanse("  PPP", barn1, barn2, barn3)
                .medKompetanse("--   ----", barn2, barn3)
                .lagreTil(kompetanseRepository)

        KompetanseBuilder(jan(2020), behandlingId2)
            .medKompetanse("PPPSSSPPPPPPP", barn1, barn2, barn3)
            .lagreTil(kompetanseRepository)

        kompetanseService.kopierOgErstattKompetanser(behandlingId1, behandlingId2)

        val kompetanserBehandling2EtterEndring = kompetanseService.hentKompetanser(behandlingId2)

        assertThat(kompetanserBehandling2EtterEndring).hasSize(kompetanserBehandling2EtterEndring.size).containsAll(kompetanser1)

        kompetanserBehandling2EtterEndring.forEach {
            assertEquals(behandlingId2.id, it.behandlingId)
        }

        val kompetanserBehandling1EtterEndring = kompetanseService.hentKompetanser(behandlingId1)

        assertThat(kompetanserBehandling1EtterEndring).hasSize(kompetanserBehandling1EtterEndring.size).containsAll(kompetanser1)

        kompetanserBehandling1EtterEndring.forEach {
            assertEquals(behandlingId1.id, it.behandlingId)
        }
    }

    private fun lagVilkårsvurdering(
        søkersperiode: Periode,
        barnasperioder: Map<Aktør, Pair<LocalDate?, LocalDate?>>,
    ): Vilkårsvurdering {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker,
                behandling = behandling,
                resultat = Resultat.OPPFYLT,
                søkerPeriodeFom = søkersperiode.fom,
                søkerPeriodeTom = søkersperiode.tom,
                regelverk = Regelverk.EØS_FORORDNINGEN,
            )
        val personResultaterForBarna =
            barnasperioder.map { (aktør, periode) ->
                val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = aktør)
                val vilkårResultater =
                    Vilkår.hentVilkårFor(PersonType.BARN).map {
                        lagVilkårResultat(
                            personResultat = personResultat,
                            vilkårType = it,
                            periodeFom = periode.first,
                            periodeTom = periode.second,
                            behandlingId = behandling.id,
                            regelverk = Regelverk.EØS_FORORDNINGEN,
                        )
                    }
                personResultat.setSortedVilkårResultater(vilkårResultater.toSet())
                personResultat
            }
        return vilkårsvurdering.apply { personResultater += personResultaterForBarna }
    }

    private fun lagVilkårsvurderingTidslinjer(vilkårsvurdering: Vilkårsvurdering) =
        VilkårsvurderingTidslinjer(
            vilkårsvurdering = vilkårsvurdering,
            personopplysningGrunnlag =
                lagPersonopplysningGrunnlag(
                    behandlingId = behandlingId.id,
                    søkerPersonIdent = søker.aktivFødselsnummer(),
                    søkerAktør = søker,
                    barnasIdenter = listOf(barn1.aktivFødselsnummer(), barn2.aktivFødselsnummer()),
                    barnAktør = listOf(barn1, barn2),
                ),
            erToggleForLovendringAugust2024På = true,
        )

    private fun assertKompetanse(
        fom: YearMonth,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
        resultat: KompetanseResultat? = null,
        hentetKompetanse: Kompetanse,
    ) {
        assertThat(fom).isEqualTo(hentetKompetanse.fom)
        assertThat(tom).isEqualTo(hentetKompetanse.tom)
        assertThat(barnAktører).containsExactlyInAnyOrderElementsOf(hentetKompetanse.barnAktører)
        assertThat(resultat).isEqualTo(hentetKompetanse.resultat)
    }
}
