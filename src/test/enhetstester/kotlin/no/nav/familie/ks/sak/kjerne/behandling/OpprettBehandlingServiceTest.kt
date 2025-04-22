package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagNyEksternBehandlingRelasjon
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingMetrikker
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.UUID

class OpprettBehandlingServiceTest {
    private val personidentService = mockk<PersonidentService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val vedtakService = mockk<VedtakService>()
    private val loggService = mockk<LoggService>()
    private val stegService = mockk<StegService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val taskService = mockk<TaskService>()
    private val behandlingMetrikker = mockk<BehandlingMetrikker>()
    private val unleashNextMedContextService = mockk<UnleashNextMedContextService>()
    private val eksternBehandlingRelasjonService = mockk<EksternBehandlingRelasjonService>()

    private val opprettBehandlingService =
        OpprettBehandlingService(
            personidentService = personidentService,
            arbeidsfordelingService = arbeidsfordelingService,
            vedtakService = vedtakService,
            loggService = loggService,
            fagsakRepository = fagsakRepository,
            behandlingRepository = behandlingRepository,
            taskService = taskService,
            stegService = stegService,
            behandlingMetrikker = behandlingMetrikker,
            unleashService = unleashNextMedContextService,
            eksternBehandlingRelasjonService = eksternBehandlingRelasjonService,
        )

    private val søker = randomAktør()
    private val søkersIdent = søker.personidenter.first { personIdent -> personIdent.aktiv }.fødselsnummer
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @BeforeEach
    fun beforeEach() {
        every { personidentService.hentAktør(any()) } returns søker
        every { fagsakRepository.finnFagsakForAktør(søker) } returns fagsak
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns behandling.copy(status = BehandlingStatus.AVSLUTTET, aktiv = true)
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns emptyList()
        every { behandlingRepository.saveAndFlush(any()) } returns behandling
        every { behandlingRepository.save(any()) } returns behandling
        every { arbeidsfordelingService.fastsettBehandlendeEnhet(any(), any()) } just runs
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any()) } just runs
        every { loggService.opprettBehandlingLogg(any()) } just runs
        every { taskService.save(any()) } returns OpprettOppgaveTask.opprettTask(behandling.id, Oppgavetype.BehandleSak, LocalDate.now())
        every { stegService.utførSteg(any(), any()) } returns Unit
        every { behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(behandling) } just runs
    }

    @Nested
    inner class OpprettBehandling {
        @Test
        fun `skal opprette behandling når det finnes en fagsak tilknyttet søker og det ikke allerede eksisterer en aktiv behandling som ikke er avsluttet`() {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL,
                )

            // Act
            val opprettetBehandling = opprettBehandlingService.opprettBehandling(opprettBehandlingDto)

            // Assert
            // Validerer at tidligere aktiv behandling blir satt til inaktiv
            verify {
                behandlingRepository.saveAndFlush(
                    withArg {
                        assertThat(it.aktiv).isFalse()
                    },
                )
            }
            // Validerer at ny behandling blir satt til aktiv og inneholder forventede felter
            verify {
                behandlingRepository.save(
                    withArg {
                        assertThat(it.aktiv).isTrue()
                        assertThat(BehandlingÅrsak.SØKNAD).isEqualTo(it.opprettetÅrsak)
                        assertThat(BehandlingType.FØRSTEGANGSBEHANDLING).isEqualTo(it.type)
                        assertThat(BehandlingKategori.NASJONAL).isEqualTo(it.kategori)
                        assertThat(BehandlingSteg.REGISTRERE_PERSONGRUNNLAG).isEqualTo(it.steg)
                        assertThat(fagsak).isEqualTo(it.fagsak)
                    },
                )
            }
            // Validerer at "BehandleSak"-oppgave blir opprettet
            verify(exactly = 2) { taskService.save(any()) }
            assertThat(opprettetBehandling).isNotNull()
        }

        @Test
        fun `skal kaste feil dersom det ikke finnes noen fagsak tilknyttet søker`() {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL,
                )

            every { fagsakRepository.finnFagsakForAktør(søker) } returns null

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Kan ikke lage behandling på person uten tilknyttet fagsak.")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["OPPRETTET", "UTREDES", "FATTER_VEDTAK", "IVERKSETTER_VEDTAK"],
        )
        fun `skal kaste feil dersom det allerede finnes en aktiv behandling som ikke er avsluttet`(
            behandlingStatus: BehandlingStatus,
        ) {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL,
                )

            every {
                behandlingRepository.findByFagsakAndAktiv(fagsak.id)
            } returns
                behandling.copy(
                    aktiv = true,
                    status = behandlingStatus,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }

            assertThat(exception.message).isEqualTo("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"],
        )
        fun `skal kaste feil dersom behandlingkategori ikke er satt og behandlingstype er FØRSTEGANGSBEHANDLING eller REVURDERING og behandlingsårsak er SØKNAD`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = behandlingType,
                )

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    opprettBehandlingService.opprettBehandling(
                        opprettBehandlingDto,
                    )
                }
            assertThat(exception.message).isEqualTo("Behandling med type ${behandlingType.visningsnavn} og årsak Søknad krever behandlingskategori")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["SØKNAD", "BARNEHAGELISTE"],
        )
        fun `skal kaste feil dersom behandlingType er TEKNISK_ENDRING og behandlingÅrsak ikke samsvarer`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    behandlingÅrsak = behandlingÅrsak,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Behandling med TEKNISK_ENDRING og årsak $behandlingÅrsak samsvarer ikke.")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingÅrsak::class,
            names = ["ÅRLIG_KONTROLL", "DØDSFALL", "NYE_OPPLYSNINGER", "KLAGE", "KORREKSJON_VEDTAKSBREV", "SATSENDRING"],
        )
        fun `skal kaste feil dersom behandlingType er TEKNISK_ENDRING eller FØRSTEGANGSBEHANDLING og behandlingÅrsak ikke samsvarer`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            // Arrange
            val opprettBehandlingDtoTekniskEndring =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    behandlingÅrsak = behandlingÅrsak,
                )

            // Act & assert
            var exception =
                assertThrows<Feil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDtoTekniskEndring)
                }
            assertThat(exception.message).isEqualTo("Behandling med TEKNISK_ENDRING og årsak $behandlingÅrsak samsvarer ikke.")

            // Arrange
            val opprettBehandlingDtoFørstegangsbehandling =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = behandlingÅrsak,
                )

            // Act & assert
            exception =
                assertThrows {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDtoFørstegangsbehandling)
                }
            assertThat(exception.message).isEqualTo("Behandling med FØRSTEGANGSBEHANDLING og årsak $behandlingÅrsak samsvarer ikke.")
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingType::class,
            names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"],
        )
        fun `skal kaste feil dersom behandlingType er FØRSTEGANGSBEHANDLING eller REVURDERING og behandlingÅrsak er TEKNISK_ENDRING`(
            behandlingType: BehandlingType,
        ) {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = behandlingType,
                    behandlingÅrsak = BehandlingÅrsak.TEKNISK_ENDRING,
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Behandling med $behandlingType og årsak TEKNISK_ENDRING samsvarer ikke.")
        }

        @Test
        fun `skal kaste feil dersom behandlingType er REVURDERING og det ikke finnes noen avsluttede vedtatte behandlinger på fagsaken til søker`() {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.BARNEHAGELISTE,
                )

            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns
                listOf(
                    lagBehandling(
                        fagsak,
                        opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                    ).copy(status = BehandlingStatus.UTREDES),
                )

            // Act & assert
            val exception =
                assertThrows<Feil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }
            assertThat(exception.message).isEqualTo("Kan ikke opprette revurdering på $fagsak uten noen andre behandlinger som er vedtatt.")
        }

        @Test
        fun `skal kaste feil dersom behandlingsårsak er IVERKSETTE_KA_VEDTAK og toggle ikke er skrudd på`() {
            // Arrange
            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.IVERKSETTE_KA_VEDTAK,
                )

            every { unleashNextMedContextService.isEnabled(FeatureToggle.KAN_OPPRETTE_REVURDERING_MED_ÅRSAK_IVERKSETTE_KA_VEDTAK) } returns false

            // Act & assert
            val exception =
                assertThrows<FunksjonellFeil> {
                    opprettBehandlingService.opprettBehandling(opprettBehandlingDto)
                }
            assertThat(exception.melding).isEqualTo("Kan ikke opprette behandling med årsak Iverksette KA-vedtak.")
        }

        @Test
        fun `skal opprette behandling og lagre ned ekstern behandling relasjon`() {
            // Arrange
            val nyEksternBehandlingRelasjon =
                lagNyEksternBehandlingRelasjon(
                    eksternBehandlingId = UUID.randomUUID().toString(),
                    eksternBehandlingFagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )

            val opprettBehandlingDto =
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                    kategori = BehandlingKategori.NASJONAL,
                    nyEksternBehandlingRelasjon = nyEksternBehandlingRelasjon,
                )

            val eksternBehandlingRelasjonSlot = slot<EksternBehandlingRelasjon>()

            every { eksternBehandlingRelasjonService.lagreEksternBehandlingRelasjon(capture(eksternBehandlingRelasjonSlot)) } returnsArgument 0

            // Act
            val opprettetBehandling = opprettBehandlingService.opprettBehandling(opprettBehandlingDto)

            // Assert
            assertThat(opprettetBehandling).isNotNull()
            assertThat(eksternBehandlingRelasjonSlot.captured.id).isEqualTo(0L)
            assertThat(eksternBehandlingRelasjonSlot.captured.internBehandlingId).isEqualTo(behandling.id)
            assertThat(eksternBehandlingRelasjonSlot.captured.eksternBehandlingId).isEqualTo(nyEksternBehandlingRelasjon.eksternBehandlingId)
            assertThat(eksternBehandlingRelasjonSlot.captured.eksternBehandlingFagsystem).isEqualTo(nyEksternBehandlingRelasjon.eksternBehandlingFagsystem)
            assertThat(eksternBehandlingRelasjonSlot.captured.opprettetTid).isNotNull()
        }
    }

    @Nested
    inner class HentBehandling {
        @Test
        fun `skal hente behandling fra behandlingRepository`() {
            // Act
            val hentetBehandling = opprettBehandlingService.hentBehandling(behandling.id)

            // Assert
            assertThat(hentetBehandling).isNotNull()
            verify(exactly = 1) { behandlingRepository.hentBehandling(behandling.id) }
        }
    }
}
