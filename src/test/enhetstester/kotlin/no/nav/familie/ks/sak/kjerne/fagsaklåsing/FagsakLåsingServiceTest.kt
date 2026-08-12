package no.nav.familie.ks.sak.kjerne.fagsaklåsing

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsluttSakRequest
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonEnkel
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonKlient
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.PubliserSaksstatistikkTask
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.kjerne.klage.KlagebehandlingHenter
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import no.nav.familie.kontrakter.felles.klage.BehandlingStatus as KlageBehandlingStatus

class FagsakLåsingServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakLåsingRepository = mockk<FagsakLåsingRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val taskService = mockk<TaskRepositoryWrapper>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val klagebehandlingHenter = mockk<KlagebehandlingHenter>()
    private val tilbakekrevingKlient = mockk<TilbakekrevingKlient>()

    private val fagsakLåsingService =
        FagsakLåsingService(
            fagsakRepository = fagsakRepository,
            fagsakLåsingRepository = fagsakLåsingRepository,
            integrasjonKlient = integrasjonKlient,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            arbeidsfordelingService = arbeidsfordelingService,
            featureToggleService = featureToggleService,
            taskService = taskService,
            behandlingRepository = behandlingRepository,
            behandlingService = behandlingService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            vedtakRepository = vedtakRepository,
            klagebehandlingHenter = klagebehandlingHenter,
            tilbakekrevingKlient = tilbakekrevingKlient,
        )

    @Nested
    inner class LåsFagsak {
        private val fagsak = lagFagsak(status = FagsakStatus.AVSLUTTET)
        private val arbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = "1234", enhetNavn = "Enhet")
        private val sisteVedtatteBehandling = lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, endretTidspunkt = LocalDateTime.now().minusYears(2))
        private val sisteStønadTom = YearMonth.now().minusYears(3)
        private val vedtaksdato = LocalDateTime.now().minusYears(2)
        private val personer = setOf(lagPersonEnkel(personType = PersonType.BARN, fødselsdato = LocalDate.now().minusYears(4)))
        private val åpenLogger = LoggerFactory.getLogger(FagsakLåsingService::class.java) as Logger
        private lateinit var listAppender: ListAppender<ILoggingEvent>
        private var opprinneligLoggnivå: Level? = null

        @BeforeEach
        fun setup() {
            every { featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK) } returns true
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns null
            every { personopplysningGrunnlagService.hentSøkerOgBarnPåFagsak(fagsak.id) } returns personer
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(sisteVedtatteBehandling)
            every { behandlingService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns sisteVedtatteBehandling
            every { behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id) } returns null
            every { tilkjentYtelseRepository.hentTilkjenteYtelserForBehandling(sisteVedtatteBehandling.id) } returns
                listOf(TilkjentYtelse(behandling = sisteVedtatteBehandling, stønadTom = sisteStønadTom, opprettetDato = LocalDate.now(), endretDato = LocalDate.now()))
            every { vedtakRepository.findByBehandlingAndAktivOptional(sisteVedtatteBehandling.id) } returns lagVedtak(behandling = sisteVedtatteBehandling, vedtaksDato = vedtaksdato)
            every { klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsak.id) } returns emptyList()
            every { tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsak.id) } returns emptyList()
            every { fagsakLåsingRepository.save(any()) } answers { firstArg() }
            every { fagsakRepository.save(fagsak) } returns fagsak
            every { taskService.save(any()) } answers { firstArg() }
            every { arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(fagsak.aktør.aktivFødselsnummer(), personer.map { it.aktør.aktivFødselsnummer() }, any()) } returns arbeidsfordelingsenhet
            every { integrasjonKlient.avsluttSak(any()) } just runs

            listAppender = ListAppender<ILoggingEvent>().apply { start() }
            opprinneligLoggnivå = åpenLogger.level
            åpenLogger.level = Level.INFO
            åpenLogger.addAppender(listAppender)
        }

        // Loggeren er delt for hele JVM-en, så appender og nivå må tilbakestilles etter hver test
        @AfterEach
        fun tearDown() {
            åpenLogger.detachAppender(listAppender)
            listAppender.stop()
            åpenLogger.level = opprinneligLoggnivå
        }

        @Test
        fun `skal låse avsluttet fagsak og sende melding til Joark`() {
            // Arrange
            val lagretLåsSlot = slot<FagsakLåsing>()
            val joarkRequestSlot = slot<AvsluttSakRequest>()

            // Mocker for å kunne sette `opprettetTidspunkt` til mer enn 30 dager siden
            val låstOppFagsakLåsing =
                mockk<FagsakLåsing>(relaxed = true) {
                    every { opprettetTidspunkt } returns LocalDateTime.now().minusDays(50)
                }

            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns låstOppFagsakLåsing
            every { fagsakLåsingRepository.save(capture(lagretLåsSlot)) } answers { firstArg() }
            every { integrasjonKlient.avsluttSak(capture(joarkRequestSlot)) } just runs

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.LÅST)
            assertThat(lagretLåsSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST)
            assertThat(lagretLåsSlot.captured.aktiv).isTrue()
            assertThat(lagretLåsSlot.captured.begrunnelse).isEqualTo("Automatisk låst iht. arkivloven fordi siste utbetaling eller vedtak på fagsaken var ${vedtaksdato.toLocalDate()}")
            assertThat(lagretLåsSlot.captured.tidspunkt).isEqualTo(vedtaksdato.toLocalDate().plusYears(1).atStartOfDay())
            assertThat(joarkRequestSlot.captured.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(joarkRequestSlot.captured.administrativEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            verify { taskService.save(match { it.type == PubliserSaksstatistikkTask.TASK_STEP_TYPE }) }
        }

        @Test
        fun `skal hoppe over låsing når toggle er av`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK) } returns false

            // Act
            fagsakLåsingService.låsFagsak(1)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Toggle for låsing av fagsak er av, hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.finnFagsak(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som ikke er AVSLUTTET`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Status for fagsak ${fagsak.id} er LØPENDE. Hopper ut av fagsaklåsing.")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som har åpen klagebehandling`() {
            // Arrange
            every { klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsak.id) } returns
                listOf(mockk { every { status } returns KlageBehandlingStatus.UTREDES })

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} har åpen klagebehandling. Hopper ut av fagsaklåsing.")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som har åpen tilbakekrevingsbehandling`() {
            // Arrange
            every { tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsak.id) } returns
                listOf(mockk { every { status } returns Behandlingsstatus.UTREDES })

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} har åpen tilbakekrevingsbehandling. Hopper ut av fagsaklåsing.")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak der siste KS-sak behandling ble avsluttet for under 1 år siden`() {
            // Arrange
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns
                listOf(lagBehandling(fagsak = fagsak, status = BehandlingStatus.AVSLUTTET, endretTidspunkt = LocalDateTime.now().minusMonths(6)))

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).contains("som er for under 1 år siden. Hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak der siste behandling ble henlagt for under 1 år siden`() {
            // Arrange
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns
                listOf(
                    sisteVedtatteBehandling,
                    lagBehandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                        resultat = Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        aktivertTidspunkt = LocalDateTime.now().minusMonths(2),
                        endretTidspunkt = LocalDateTime.now().minusMonths(2),
                    ),
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).contains("som er for under 1 år siden. Hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal låse fagsak der siste behandling ble henlagt for mer enn 1 år siden`() {
            // Arrange
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns
                listOf(
                    sisteVedtatteBehandling,
                    lagBehandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.AVSLUTTET,
                        resultat = Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET,
                        aktivertTidspunkt = LocalDateTime.now().minusMonths(18),
                        endretTidspunkt = LocalDateTime.now().minusMonths(18),
                    ),
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.LÅST)
            verify(exactly = 1) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak der siste klagebehandling ble avsluttet for under 1 år siden`() {
            // Arrange
            every { klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsak.id) } returns
                listOf(
                    mockk {
                        every { status } returns KlageBehandlingStatus.FERDIGSTILT
                        every { vedtaksdato } returns LocalDateTime.now().minusMonths(6)
                    },
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).contains("som er for under 1 år siden. Hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak der siste tilbakekrevingsbehandling ble avsluttet for under 1 år siden`() {
            // Arrange
            every { tilbakekrevingKlient.hentTilbakekrevingsbehandlinger(fagsak.id) } returns
                listOf(
                    mockk {
                        every { status } returns Behandlingsstatus.AVSLUTTET
                        every { vedtaksdato } returns LocalDateTime.now().minusMonths(6)
                    },
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).contains("som er for under 1 år siden. Hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som har åpen behandling`() {
            // Arrange
            every { behandlingRepository.findByFagsakAndAktivAndOpen(fagsak.id) } returns mockk()

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} har åpen behandling. Hopper ut av fagsaklåsing.")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal kaste Feil hvis fagsak har FagsakLåsing med hendelse LÅST`() {
            // Arrange
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.now().minusDays(1),
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Allerede låst",
                    aktiv = true,
                )

            // Act
            val feil = assertThrows<Feil> { fagsakLåsingService.låsFagsak(fagsak.id) }

            // Assert
            assertThat(feil.message).isEqualTo("Fagsak ${fagsak.id} med status AVSLUTTET har allerede en aktiv låsing.")
        }

        @Test
        fun `skal hoppe over fagsak som ble låst opp for under 30 dager siden`() {
            // Arrange
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.now().minusDays(1),
                    hendelse = FagsakLåsHendelse.LÅST_OPP,
                    begrunnelse = "Låst opp",
                    aktiv = true,
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} ble låst opp for under 30 dager siden. Hopper ut av fagsaklåsing.")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { taskService.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal kaste Feil hvis fagsak ikke finnes`() {
            // Arrange
            every { fagsakRepository.finnFagsak(999) } returns null

            // Act & Assert
            assertThrows<Feil> { fagsakLåsingService.låsFagsak(999) }
        }

        @Test
        fun `skal kaste Feil hvis det ikke finnes barn på fagsak`() {
            // Arrange
            every { personopplysningGrunnlagService.hentSøkerOgBarnPåFagsak(any()) } returns null

            // Act & Assert
            assertThrows<Feil> { fagsakLåsingService.låsFagsak(fagsak.id) }
        }

        @Test
        fun `skal hoppe over fagsak der siste utbetaling var for under 1 år siden`() {
            // Arrange
            every { tilkjentYtelseRepository.hentTilkjenteYtelserForBehandling(sisteVedtatteBehandling.id) } returns
                listOf(TilkjentYtelse(behandling = sisteVedtatteBehandling, stønadTom = YearMonth.now().minusMonths(6), opprettetDato = LocalDate.now(), endretDato = LocalDate.now()))

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert — ingen mutasjoner
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak der siste vedtak var for under 1 år siden`() {
            // Arrange
            every { vedtakRepository.findByBehandlingAndAktivOptional(sisteVedtatteBehandling.id) } returns
                lagVedtak(behandling = sisteVedtatteBehandling, vedtaksDato = LocalDateTime.now().minusMonths(6))

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert — ingen mutasjoner
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal bruke stønad tom-dato når vedtaksdato er null`() {
            // Arrange
            val lagretLåsSlot = slot<FagsakLåsing>()

            every { vedtakRepository.findByBehandlingAndAktivOptional(sisteVedtatteBehandling.id) } returns
                Vedtak(behandling = sisteVedtatteBehandling, vedtaksdato = null)
            every { fagsakLåsingRepository.save(capture(lagretLåsSlot)) } answers { firstArg() }

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.LÅST)
            assertThat(lagretLåsSlot.captured.begrunnelse).isEqualTo("Automatisk låst iht. arkivloven fordi siste utbetaling eller vedtak på fagsaken var ${sisteStønadTom.sisteDagIInneværendeMåned()}")
            assertThat(lagretLåsSlot.captured.tidspunkt).isEqualTo(sisteStønadTom.sisteDagIInneværendeMåned().plusYears(1).atStartOfDay())
        }

        @Test
        fun `skal bruke vedtaksdato når det ikke finnes tilkjent ytelse`() {
            // Arrange
            val lagretLåsSlot = slot<FagsakLåsing>()

            every { tilkjentYtelseRepository.hentTilkjenteYtelserForBehandling(sisteVedtatteBehandling.id) } returns emptyList()
            every { fagsakLåsingRepository.save(capture(lagretLåsSlot)) } answers { firstArg() }

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.LÅST)
            assertThat(lagretLåsSlot.captured.begrunnelse).isEqualTo("Automatisk låst iht. arkivloven fordi siste utbetaling eller vedtak på fagsaken var ${vedtaksdato.toLocalDate()}")
            assertThat(lagretLåsSlot.captured.tidspunkt).isEqualTo(vedtaksdato.toLocalDate().plusYears(1).atStartOfDay())
        }

        @Test
        fun `skal propagere exception fra Joark slik at transaksjonen ruller tilbake`() {
            // Arrange
            every { integrasjonKlient.avsluttSak(any()) } throws RuntimeException("Joark er nede")

            // Act & Assert
            assertThrows<RuntimeException> { fagsakLåsingService.låsFagsak(fagsak.id) }
        }
    }

    @Nested
    inner class LåsOppFagsak {
        @BeforeEach
        fun setUp() {
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(any()) } returns null
            every { fagsakLåsingRepository.save(any()) } answers { firstArg() }
            every { fagsakRepository.save(any()) } answers { firstArg() }
            every { integrasjonKlient.gjenåpneSakIDokarkiv(any()) } just runs
        }

        @Test
        fun `skal opprette FagsakLåsing med hendelse LÅST_OPP`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val låsingSlot = slot<FagsakLåsing>()
            every { fagsakLåsingRepository.save(capture(låsingSlot)) } answers { firstArg() }
            every { taskService.save(any()) } answers { firstArg() }

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(låsingSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST_OPP)
            assertThat(låsingSlot.captured.begrunnelse).isEqualTo("En god grunn")
            assertThat(låsingSlot.captured.fagsak.id).isEqualTo(fagsak.id)
            verify { taskService.save(match { it.type == PubliserSaksstatistikkTask.TASK_STEP_TYPE }) }
        }

        @Test
        fun `skal sette fagsak status til AVSLUTTET`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { taskService.save(any()) } answers { firstArg() }

            // Act
            val oppdatertFagsak = fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(oppdatertFagsak.status).isEqualTo(FagsakStatus.AVSLUTTET)
            verify { fagsakRepository.save(match { it.status == FagsakStatus.AVSLUTTET }) }
            verify { taskService.save(match { it.type == PubliserSaksstatistikkTask.TASK_STEP_TYPE }) }
        }

        @Test
        fun `skal kalle gjenåpneSak på integrasjonsklienten med riktige verdier`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val requestSlot = slot<GjenåpneSakRequest>()
            every { integrasjonKlient.gjenåpneSakIDokarkiv(capture(requestSlot)) } just runs
            every { taskService.save(any()) } answers { firstArg() }

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")

            // Assert
            val request = requestSlot.captured
            assertThat(request.tema).isEqualTo(Tema.KON)
            assertThat(request.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(request.fagsaksystem).isEqualTo(Fagsystem.KONT)
            assertThat(request.bruker.idType).isEqualTo(BrukerIdType.FNR)
            assertThat(request.bruker.id).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            verify { taskService.save(match { it.type == PubliserSaksstatistikkTask.TASK_STEP_TYPE }) }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis fagsak ikke har status LÅST`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
                }

            assertThat(feil.message).contains("LÅST")
            assertThat(feil.message).contains("LØPENDE")
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis begrunnelse er blank`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "   ")
            }

            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal ikke kalle integrasjonsklienten hvis fagsak har feil status`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.OPPRETTET)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
            }

            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
        }
    }
}
