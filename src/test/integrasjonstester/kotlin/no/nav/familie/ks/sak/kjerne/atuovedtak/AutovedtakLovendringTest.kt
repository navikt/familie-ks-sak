package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.atuovedtak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.util.LocalDateTimeProvider
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagLogg
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.autovedtak.AutovedtakLovendringService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AutovedtakLovendringTest(
    @Autowired private val autovedtakLovendringService: AutovedtakLovendringService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personRepository: PersonRepository,
    @Autowired private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : OppslagSpringRunnerTest() {
    @MockkBean
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockkBean
    private lateinit var sakStatistikkService: SakStatistikkService

    @MockkBean
    private lateinit var unleashNextMedContextService: UnleashNextMedContextService

    @MockkBean
    private lateinit var personService: PersonService

    @MockkBean
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    @MockkBean
    private lateinit var localDateTimeProvider: LocalDateTimeProvider

    @MockkBean
    private lateinit var loggService: LoggService

    @BeforeEach
    fun setUp() {
        justRun { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }
        justRun { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
        every { unleashNextMedContextService.isEnabled(any()) } returns true
        every { personService.lagPerson(any(), any(), any(), any(), any()) } answers {
            val aktør = firstArg<Aktør>()
            personRepository.findByAktør(aktør).first()
        }
        justRun { loggService.opprettBehandlingLogg(any()) }
        justRun { loggService.opprettVilkårsvurderingLogg(any(), any(), any()) }

        justRun { utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any(), any()) }
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk gir endret utbetaling`() {
        // arrange
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)

        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn, behandling)

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(7).toYearMonth()) // 7 måneder som i nytt regelverk
    }

    @Test
    fun `automatisk revurdering av fagsak som ikke blir truffet av nytt regelverk gir ikke endret utbetaling`() {
        // arrange
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)
        val fødselsdatoBarn = LocalDate.of(2021, 4, 1)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)

        lagTilkjentYtelse(null)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = fødselsdatoBarn.plusYears(2).minusMonths(1).toYearMonth() // 11 måneder som i gammelt regelverk
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = stønadFom,
                    stønadTom = stønadTom,
                ),
            ),
        )

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.FORTSATT_OPPHØRT)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(stønadFom)
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(stønadTom)
    }

    @Test
    fun `automatisk revurdering av fagsak som har fremtidig opphør beholder fremtidig opphør`() {
        // arrange
        opprettSøkerFagsakOgBehandling(fagsakStatus = FagsakStatus.LØPENDE, behandlingStatus = BehandlingStatus.AVSLUTTET, behandlingResultat = Behandlingsresultat.INNVILGET)

        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = behandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )

        val datoForBarnehageplass = fødselsdatoBarn.plusYears(1).plusMonths(4).plusDays(12) // 13. august 2024
        lagVilkårsvurderingMedFremtidigOpphør(fødselsdatoBarn, datoForBarnehageplass)

        lagTilkjentYtelse(null)
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth(),
                    stønadTom = datoForBarnehageplass.minusMonths(1).toYearMonth(),
                ),
            ),
        )

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_OG_OPPHØRT)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(datoForBarnehageplass.toYearMonth())

        val vilkårResultatRevurdering =
            vilkårsvurderingRepository
                .finnAktivForBehandling(nyBehandling.id)!!
                .personResultater
                .find { !it.erSøkersResultater() }!!
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BARNEHAGEPLASS && it.harMeldtBarnehageplassOgErFulltidIBarnehage() }
        assertThat(vilkårResultatRevurdering.size).isEqualTo(1)
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk skal snike i køen`() {
        // arrange

        opprettSøkerFagsakOgBehandling(
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingResultat = Behandlingsresultat.INNVILGET,
        )

        val avsluttetFørstegangsBehandling =
            lagreBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    status = BehandlingStatus.AVSLUTTET,
                    resultat = Behandlingsresultat.INNVILGET,
                    aktiv = false,
                ),
            )

        mockSnikIKøenValidering()

        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)

        opprettPersonopplysningGrunnlagOgPersonForBehandling(
            behandlingId = avsluttetFørstegangsBehandling.id,
            lagBarn = true,
            fødselsdatoBarn = fødselsdatoBarn,
        )
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn, avsluttetFørstegangsBehandling)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn, avsluttetFørstegangsBehandling)

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id)

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(BehandlingÅrsak.LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(7).toYearMonth()) // 7 måneder som i nytt regelverk

        val behandlingSomBlirSneketForbi = behandlingRepository.hentBehandling(behandling.id)
        assertThat(behandlingSomBlirSneketForbi.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
    }

    // trengs for at validering på at behandling som skal snikes forbi ikke er endret siste 4 timer skal passere
    private fun mockSnikIKøenValidering() {
        every { localDateTimeProvider.now() } returns LocalDateTime.now().plusHours(5)

        val logg =
            lagLogg(
                behandlingId = behandling.id,
                opprettetTidspunkt = LocalDateTime.now().minusHours(5),
            )
        every { loggService.hentLoggForBehandling(any()) } returns listOf(logg)
        justRun { loggService.opprettSettPåMaskinellVent(any(), any()) }
    }

    private fun lagTilkjentytelseMedAndelForBarn(
        fødselsdatoBarn: LocalDate,
        behandling: Behandling,
    ) {
        lagTilkjentYtelse(null)
        tilkjentYtelse.andelerTilkjentYtelse.add(
            andelTilkjentYtelseRepository.save(
                lagAndelTilkjentYtelse(
                    tilkjentYtelse = tilkjentYtelse,
                    behandling = behandling,
                    aktør = barn,
                    stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth(),
                    stønadTom = fødselsdatoBarn.plusYears(2).minusMonths(1).toYearMonth(), // 11 måneder som i gammelt regelverk
                ),
            ),
        )
    }

    private fun lagVilkårsvurderingEtterGammeltRegelverk(
        fødselsdatoBarn: LocalDate,
        behandling: Behandling = this.behandling,
    ) {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandling = behandling,
            )
        vilkårsvurdering.personResultater = lagPersonResultater(vilkårsvurdering, fødselsdatoBarn).toSet()
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    private fun lagVilkårsvurderingMedFremtidigOpphør(
        fødselsdatoBarn: LocalDate,
        datoForBarnehageplass: LocalDate,
    ) {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandling = behandling,
            )
        val mutablePersonResultater = lagPersonResultater(vilkårsvurdering, fødselsdatoBarn)

        val personResultatBarn = mutablePersonResultater.find { !it.erSøkersResultater() }!!
        val barnehagevilkårIkkeBarnehageplass =
            personResultatBarn
                .vilkårResultater
                .find { it.vilkårType == Vilkår.BARNEHAGEPLASS }!!

        barnehagevilkårIkkeBarnehageplass.periodeTom = datoForBarnehageplass

        val barnetHarBarnehageplassVilkår =
            VilkårResultat(
                personResultat = personResultatBarn,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = datoForBarnehageplass.plusDays(1),
                periodeTom = fødselsdatoBarn.plusYears(2).minusMonths(1), // barnets alder-vilkår.periode_tom
                behandlingId = behandling.id,
                resultat = Resultat.IKKE_OPPFYLT,
                antallTimer = BigDecimal.valueOf(42),
                søkerHarMeldtFraOmBarnehageplass = true,
                begrunnelse = "Barnet har barnehageplass",
            )
        personResultatBarn.vilkårResultater.add(barnetHarBarnehageplassVilkår)

        vilkårsvurdering.personResultater = mutablePersonResultater.toSet()
        vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    private fun lagPersonResultater(
        vilkårsvurdering: Vilkårsvurdering,
        fødselsdatoBarn: LocalDate,
    ): Set<PersonResultat> {
        val søkersPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = søker,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdatoBarn,
                periodeTom = null,
                personType = PersonType.SØKER,
                lagFullstendigVilkårResultat = true,
            )
        val barnetsPersonResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barn,
                resultat = Resultat.OPPFYLT,
                periodeFom = fødselsdatoBarn,
                periodeTom = null,
                personType = PersonType.BARN,
                lagFullstendigVilkårResultat = true,
            )
        val barnetsAlderVilkår = barnetsPersonResultat.vilkårResultater.find { it.vilkårType == Vilkår.BARNETS_ALDER }!!
        barnetsAlderVilkår.periodeFom = fødselsdatoBarn.plusYears(1)
        barnetsAlderVilkår.periodeTom = fødselsdatoBarn.plusYears(2).minusMonths(1)

        return mutableSetOf(barnetsPersonResultat, søkersPersonResultat)
    }
}
