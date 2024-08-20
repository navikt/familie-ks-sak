package no.nav.familie.ks.sak.no.nav.familie.ks.sak.kjerne.autovedtak

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.familie.ks.sak.OppslagSpringRunnerTest
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.LocalDateProvider
import no.nav.familie.ks.sak.common.util.LocalDateTimeProvider
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagLogg
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.autovedtak.AutovedtakLovendringService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat.INNVILGET
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.BrevKlient
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse.OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus.LØPENDE
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class AutovedtakLovendringTest(
    @Autowired private val autovedtakLovendringService: AutovedtakLovendringService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personRepository: PersonRepository,
    @Autowired private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val totrinnskontrollService: TotrinnskontrollService,
) : OppslagSpringRunnerTest() {
    @SpykBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var brevklient: BrevKlient

    @MockkBean
    private lateinit var simuleringService: SimuleringService

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

    @SpykBean
    private lateinit var localDateProvider: LocalDateProvider

    @MockkBean
    private lateinit var loggService: LoggService

    @MockkBean
    private lateinit var integrasjonClient: IntegrasjonClient

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        justRun { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) }
        justRun { sakStatistikkService.opprettSendingAvBehandlingensTilstand(any(), any()) }
        every { unleashNextMedContextService.isEnabled(any()) } returns true
        every { personService.lagPerson(any(), any(), any(), any(), any()) } answers {
            val aktør = firstArg<Aktør>()
            personRepository.findByAktør(aktør).first()
        }

        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = 1234,
                behandlendeEnhetId = "1234",
                behandlendeEnhetNavn = "MockEnhetNavn",
            )
        every { integrasjonClient.hentLandkoderISO2() } returns mapOf(Pair("NO", "NORGE"))

        justRun { loggService.opprettBehandlingLogg(any()) }
        justRun { loggService.opprettVilkårsvurderingLogg(any(), any(), any()) }
        justRun { loggService.opprettBeslutningOmVedtakLogg(any(), any(), any()) }

        justRun { utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any(), any()) }
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk gir endret utbetaling`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn = fødselsdatoBarn)

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = behandling.fagsak.id)!!

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(7).toYearMonth()) // 7 måneder som i nytt regelverk

        assertTotrinnskontroll(nyBehandling)
    }

    @Test
    fun `automatisk revurdering av fagsak som ikke blir truffet av nytt regelverk gir ikke endret utbetaling`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2021, 4, 1)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = fødselsdatoBarn.plusYears(2).minusMonths(1).toYearMonth() // 11 måneder som i gammelt regelverk

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingEtterGammeltRegelverk(fødselsdatoBarn)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn = fødselsdatoBarn, stønadFom = stønadFom, stønadTom = stønadTom)

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id)!!

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.FORTSATT_OPPHØRT)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(stønadFom)
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(stønadTom)

        assertTotrinnskontroll(nyBehandling)
    }

    @Test
    fun `automatisk revurdering av fagsak som har fremtidig opphør med begrunnelse og økning i andeler beholder fremtidig opphør og sender brev`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        val datoForBarnehageplass = LocalDate.of(2024, 9, 1)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = datoForBarnehageplass.minusMonths(1).toYearMonth()

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingMedFremtidigOpphør(fødselsdatoBarn, datoForBarnehageplass)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn = fødselsdatoBarn, stønadFom = stønadFom, stønadTom = stønadTom)
        lagVedtak()
        opprettVedtaksperiodeMedBegrunnelser(begrunnelser = listOf(OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS))

        every { brevklient.genererBrev(any(), any()) } returns "brev".toByteArray()
        every { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) } returns emptyList()
        every { localDateProvider.now() } returns LocalDate.of(2024, 8, 1)
        every { behandlingService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns behandling

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id, erFremtidigOpphør = true)!!

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(LOVENDRING_2024)
        assertThat(nyBehandling.behandlingStegTilstand.map { it.behandlingSteg }).containsAll(setOf(BehandlingSteg.SIMULERING, BehandlingSteg.VEDTAK))
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(datoForBarnehageplass.toYearMonth())

        val vilkårResultatRevurdering =
            vilkårsvurderingRepository
                .finnAktivForBehandling(nyBehandling.id)!!
                .personResultater
                .flatMap { it.vilkårResultater }
                .single { it.vilkårType == Vilkår.BARNEHAGEPLASS && it.harMeldtBarnehageplassOgErFulltidIBarnehage() }

        assertThat(vilkårResultatRevurdering.periodeFom).isEqualTo(datoForBarnehageplass)

        verify { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) }
        verify { brevklient.genererBrev(any(), any()) }

        assertTotrinnskontroll(nyBehandling)
    }

    @Test
    fun `automatisk revurdering av fagsak som har fremtidig opphør kaster en exception hvis det er nye andeler i august`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        val datoForBarnehageplass = LocalDate.of(2024, 8, 1)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = datoForBarnehageplass.minusMonths(1).toYearMonth()

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingMedFremtidigOpphør(fødselsdatoBarn, datoForBarnehageplass)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn = fødselsdatoBarn, stønadFom = stønadFom, stønadTom = stønadTom)
        lagVedtak()
        opprettVedtaksperiodeMedBegrunnelser(begrunnelser = listOf(OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS))

        every { brevklient.genererBrev(any(), any()) } returns "brev".toByteArray()
        every { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) } returns emptyList()
        every { localDateProvider.now() } returns LocalDate.of(2024, 8, 1)
        every { behandlingService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns behandling

        // act & assert
        val exception =
            assertThrows<Feil> {
                autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id, erFremtidigOpphør = true)
            }
        assertThat(exception.message).isEqualTo(
            "Forrige behandling har opphør i august. Nåværende behandling har opphør i september. Disse tilfellene skal ikke revurderes",
        )

        verify(exactly = 0) { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) }
        verify(exactly = 0) { brevklient.genererBrev(any(), any()) }
    }

    @Test
    fun `automatisk revurdering av fagsak som har fremtidig opphør kaster en exception hvis det er mer enn en ny andel`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2023, 4, 1)
        val datoForBarnehageplass = LocalDate.of(2024, 9, 1)
        val stønadFom = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth()
        val stønadTom = datoForBarnehageplass.minusMonths(1).toYearMonth()

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingMedFremtidigOpphør(fødselsdatoBarn, datoForBarnehageplass)
        lagTilkjentytelseMedAndelForBarn(
            fødselsdatoBarn = fødselsdatoBarn,
            stønadFom = stønadFom,
            stønadTom = stønadTom.minusMonths(2),
        )
        lagVedtak()
        opprettVedtaksperiodeMedBegrunnelser(begrunnelser = listOf(OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS))

        every { brevklient.genererBrev(any(), any()) } returns "brev".toByteArray()
        every { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) } returns emptyList()
        every { localDateProvider.now() } returns LocalDate.of(2024, 8, 1)
        every { behandlingService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns behandling

        // act & assert
        val exception =
            assertThrows<Feil> {
                autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id, erFremtidigOpphør = true)
            }
        assertThat(exception.message).isEqualTo(
            "Antall måneder differanse mellom forrige og nåværende utbetaling overstiger 1",
        )

        verify(exactly = 0) { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) }
        verify(exactly = 0) { brevklient.genererBrev(any(), any()) }
    }

    @Test
    fun `automatisk revurdering av fagsak som har fremdtidig opphør, men ikke økning i andeler sender ikke brev`() {
        // arrange
        val fødselsdatoBarn = LocalDate.of(2023, 5, 1)
        val datoForBarnehageplass = fødselsdatoBarn.plusYears(1).plusMonths(6)
        val stønadFom = fødselsdatoBarn.plusYears(1).toYearMonth()
        val stønadTom = datoForBarnehageplass.toYearMonth()

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingStatus = AVSLUTTET, behandlingResultat = INNVILGET)
        opprettPersonopplysningGrunnlagOgPersonForBehandling(lagBarn = true, fødselsdatoBarn = fødselsdatoBarn)
        lagVilkårsvurderingMedFremtidigOpphør(fødselsdatoBarn, datoForBarnehageplass)
        lagTilkjentytelseMedAndelForBarn(fødselsdatoBarn = fødselsdatoBarn, stønadFom = stønadFom, stønadTom = stønadTom)
        lagVedtak()
        opprettVedtaksperiodeMedBegrunnelser(begrunnelser = listOf(OPPHØR_FRAMTIDIG_OPPHØR_BARNEHAGEPLASS))

        every { brevklient.genererBrev(any(), any()) } returns "brev".toByteArray()
        every { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) } returns emptyList()
        every { localDateProvider.now() } returns LocalDate.of(2024, 8, 1)
        every { behandlingService.hentSisteBehandlingSomErIverksatt(fagsak.id) } returns behandling

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id, erFremtidigOpphør = true)!!

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(LOVENDRING_2024)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(datoForBarnehageplass.toYearMonth())

        val vilkårResultatRevurdering =
            vilkårsvurderingRepository
                .finnAktivForBehandling(nyBehandling.id)!!
                .personResultater
                .flatMap { it.vilkårResultater }
                .single { it.vilkårType == Vilkår.BARNEHAGEPLASS && it.harMeldtBarnehageplassOgErFulltidIBarnehage() }

        assertThat(vilkårResultatRevurdering.periodeFom).isEqualTo(datoForBarnehageplass)

        verify(inverse = true) { simuleringService.oppdaterSimuleringPåBehandling(any<Long>()) }
        verify(inverse = true) { brevklient.genererBrev(any(), any()) }

        assertTotrinnskontroll(nyBehandling)
    }

    @Test
    fun `automatisk revurdering av fagsak som blir truffet av nytt regelverk skal snike i køen`() {
        // arrange

        opprettSøkerFagsakOgBehandling(fagsakStatus = LØPENDE, behandlingResultat = INNVILGET)

        val avsluttetFørstegangsBehandling =
            lagreBehandling(
                lagBehandling(
                    fagsak = fagsak,
                    status = AVSLUTTET,
                    resultat = INNVILGET,
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
        lagTilkjentytelseMedAndelForBarn(behandling = avsluttetFørstegangsBehandling, fødselsdatoBarn = fødselsdatoBarn)

        // act
        val nyBehandling = autovedtakLovendringService.revurderFagsak(fagsakId = fagsak.id)!!

        // assert
        assertThat(nyBehandling.opprettetÅrsak).isEqualTo(LOVENDRING_2024)
        assertThat(nyBehandling.resultat).isEqualTo(Behandlingsresultat.ENDRET_UTBETALING)
        assertThat(nyBehandling.steg).isEqualTo(BehandlingSteg.IVERKSETT_MOT_OPPDRAG)

        val andelTilkjentYtelseNy = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(nyBehandling.id).single()
        assertThat(andelTilkjentYtelseNy.stønadFom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth())
        assertThat(andelTilkjentYtelseNy.stønadTom).isEqualTo(fødselsdatoBarn.plusYears(1).plusMonths(7).toYearMonth()) // 7 måneder som i nytt regelverk

        val behandlingSomBlirSneketForbi = behandlingRepository.hentBehandling(behandling.id)
        assertThat(behandlingSomBlirSneketForbi.status).isEqualTo(BehandlingStatus.SATT_PÅ_MASKINELL_VENT)

        assertTotrinnskontroll(nyBehandling)
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
        behandling: Behandling = this.behandling,
        fødselsdatoBarn: LocalDate = LocalDate.now(),
        stønadFom: YearMonth = fødselsdatoBarn.plusYears(1).plusMonths(1).toYearMonth(),
        stønadTom: YearMonth = fødselsdatoBarn.plusYears(1).plusMonths(11).toYearMonth(), // 11 måneder som i gammelt regelverk
    ) {
        lagTilkjentYtelse()
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
    }

    private fun lagVilkårsvurderingEtterGammeltRegelverk(
        fødselsdatoBarn: LocalDate = LocalDate.now(),
        behandling: Behandling = this.behandling,
    ): Vilkårsvurdering {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandling = behandling,
            )
        vilkårsvurdering.personResultater = lagPersonResultater(vilkårsvurdering, fødselsdatoBarn).toSet()
        return vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    private fun lagVilkårsvurderingMedFremtidigOpphør(
        fødselsdatoBarn: LocalDate = LocalDate.now(),
        datoForBarnehageplass: LocalDate = LocalDate.now(),
    ) {
        val vilkårsvurdering =
            Vilkårsvurdering(
                behandling = behandling,
            )
        val mutablePersonResultater = lagPersonResultater(vilkårsvurdering, fødselsdatoBarn)

        val personResultatBarn = mutablePersonResultater.first { !it.erSøkersResultater() }
        val barnehagevilkårIkkeBarnehageplass = personResultatBarn.vilkårResultater.first { it.vilkårType == Vilkår.BARNEHAGEPLASS }

        barnehagevilkårIkkeBarnehageplass.periodeTom = datoForBarnehageplass.minusDays(1)

        val barnetHarBarnehageplassVilkår =
            VilkårResultat(
                personResultat = personResultatBarn,
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = datoForBarnehageplass,
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

    private fun assertTotrinnskontroll(nyBehandling: Behandling) {
        val totrinnskontroll = totrinnskontrollService.finnAktivForBehandling(nyBehandling.id)
        assertThat(totrinnskontroll).isNotNull
        assertThat(totrinnskontroll!!.godkjent).isTrue()
        assertThat(totrinnskontroll.saksbehandler).isEqualTo(SikkerhetContext.SYSTEM_NAVN)
        assertThat(totrinnskontroll.beslutter).isEqualTo(SikkerhetContext.SYSTEM_NAVN)
    }
}
