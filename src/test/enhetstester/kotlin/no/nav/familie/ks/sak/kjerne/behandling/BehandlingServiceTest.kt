package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.oppgave.OpprettOppgaveTask
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.vedtak.VedtakService
import no.nav.familie.prosessering.domene.TaskRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class BehandlingServiceTest {
    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var stegService: StegService

    @MockK
    private lateinit var fagsakRepository: FagsakRepository

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var taskRepository: TaskRepository

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @InjectMockKs
    private lateinit var behandlingService: BehandlingService

    private val søker = randomAktør()
    private val søkersIdent = søker.personidenter.first { personIdent -> personIdent.aktiv }.fødselsnummer
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(
        fagsak,
        opprettetÅrsak = BehandlingÅrsak.SØKNAD
    )

    @BeforeEach
    fun beforeEach() {
        every { personidentService.hentAktør(any()) } returns søker
        every { fagsakRepository.finnFagsakForAktør(søker) } returns fagsak
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns behandling.copy(
            status = BehandlingStatus.AVSLUTTET,
            aktiv = true
        )
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns emptyList()
        every { behandlingRepository.saveAndFlush(any()) } returns behandling
        every { behandlingRepository.save(any()) } returns behandling
        every { arbeidsfordelingService.fastsettBehandledeEnhet(any(), any()) } just runs
        every { arbeidsfordelingService.finnArbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetId = "enhet",
            behandlendeEnhetNavn = "enhetNavn"
        )
        every { arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(any(), any()) } just runs
        every { vedtakService.opprettOgInitierNyttVedtakForBehandling(any()) } just runs
        every { loggService.opprettBehandlingLogg(any()) } just runs
        every { taskRepository.save(any()) } returns OpprettOppgaveTask.opprettTask(
            behandling.id,
            Oppgavetype.BehandleSak,
            LocalDate.now()
        )
        every { stegService.utførSteg(any(), any()) } returns Unit
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(any()) } returns
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søkersIdent)
    }

    @Test
    fun `opprettBehandling - skal opprette behandling når det finnes en fagsak tilknyttet søker og det ikke allerede eksisterer en aktiv behandling som ikke er avsluttet`() {
        val opprettetBehandling = behandlingService.opprettBehandling(
            OpprettBehandlingDto(
                søkersIdent = søkersIdent,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                kategori = BehandlingKategori.NASJONAL
            )
        )

        // Validerer at tidligere aktiv behandling blir satt til inaktiv
        verify {
            behandlingRepository.saveAndFlush(
                withArg {
                    assertFalse(it.aktiv)
                }
            )
        }

        // Validerer at ny behandling blir satt til aktiv og inneholder forventede felter
        verify {
            behandlingRepository.save(
                withArg {
                    assertTrue(it.aktiv)
                    assertEquals(BehandlingÅrsak.SØKNAD, it.opprettetÅrsak)
                    assertEquals(BehandlingType.FØRSTEGANGSBEHANDLING, it.type)
                    assertEquals(BehandlingKategori.NASJONAL, it.kategori)
                    assertEquals(BehandlingSteg.REGISTRERE_PERSONGRUNNLAG, it.steg)
                    assertEquals(fagsak, it.fagsak)
                }
            )
        }

        // Validerer at "BehandleSak"-oppgave blir opprettet
        verify(exactly = 1) { taskRepository.save(any()) }

        assertNotNull(opprettetBehandling)
    }

    @Test
    fun `opprettBehandling - skal kaste feil dersom det ikke finnes noen fagsak tilknyttet søker`() {
        every { fagsakRepository.finnFagsakForAktør(søker) } returns null

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL
                )
            )
        }

        assertEquals("Kan ikke lage behandling på person uten tilknyttet fagsak.", funksjonellFeil.melding)
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["OPPRETTET", "UTREDES", "FATTER_VEDTAK", "IVERKSETTER_VEDTAK"]
    )
    fun `opprettBehandling - skal kaste feil dersom det allerede finnes en aktiv behandling som ikke er avsluttet`(
        behandlingStatus: BehandlingStatus
    ) {
        every { behandlingRepository.findByFagsakAndAktiv(fagsak.id) } returns behandling.copy(
            aktiv = true,
            status = behandlingStatus
        )

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL
                )
            )
        }

        assertEquals(
            "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
            funksjonellFeil.melding
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingType::class,
        names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"]
    )
    fun `opprettBehandling - skal kaste feil dersom behandlingkategori ikke er satt og behandlingstype er FØRSTEGANGSBEHANDLING eller REVURDERING og behandlingsårsak er SØKNAD`(
        behandlingType: BehandlingType
    ) {
        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = behandlingType
                )
            )
        }
        assertEquals(
            "Behandling med type ${behandlingType.visningsnavn} og årsak Søknad krever behandlingskategori",
            funksjonellFeil.melding
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["SØKNAD", "BARNEHAGELISTE"]
    )
    fun `opprettBehandling - skal kaste feil dersom behandlingType er TEKNISK_ENDRING og behandlingÅrsak ikke samsvarer`(
        behandlingÅrsak: BehandlingÅrsak
    ) {
        val funksjonellFeil = assertThrows<Feil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    behandlingÅrsak = behandlingÅrsak
                )
            )
        }
        assertEquals(
            "Behandling med TEKNISK_ENDRING og årsak $behandlingÅrsak samsvarer ikke.",
            funksjonellFeil.message
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingÅrsak::class,
        names = ["ÅRLIG_KONTROLL", "DØDSFALL", "NYE_OPPLYSNINGER", "KLAGE", "KORREKSJON_VEDTAKSBREV", "SATSENDRING"]
    )
    fun `opprettBehandling - skal kaste feil dersom behandlingType er TEKNISK_ENDRING eller FØRSTEGANGSBEHANDLING og behandlingÅrsak ikke samsvarer`(
        behandlingÅrsak: BehandlingÅrsak
    ) {
        var funksjonellFeil = assertThrows<Feil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.TEKNISK_ENDRING,
                    behandlingÅrsak = behandlingÅrsak
                )
            )
        }
        assertEquals(
            "Behandling med TEKNISK_ENDRING og årsak $behandlingÅrsak samsvarer ikke.",
            funksjonellFeil.message
        )

        funksjonellFeil = assertThrows<Feil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    behandlingÅrsak = behandlingÅrsak
                )
            )
        }
        assertEquals(
            "Behandling med FØRSTEGANGSBEHANDLING og årsak $behandlingÅrsak samsvarer ikke.",
            funksjonellFeil.message
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = BehandlingType::class,
        names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"]
    )
    fun `opprettBehandling - skal kaste feil dersom behandlingType er FØRSTEGANGSBEHANDLING eller REVURDERING og behandlingÅrsak er TEKNISK_ENDRING`(
        behandlingType: BehandlingType
    ) {
        val funksjonellFeil = assertThrows<Feil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = behandlingType,
                    behandlingÅrsak = BehandlingÅrsak.TEKNISK_ENDRING
                )
            )
        }
        assertEquals(
            "Behandling med $behandlingType og årsak TEKNISK_ENDRING samsvarer ikke.",
            funksjonellFeil.message
        )
    }

    @Test
    fun `opprettBehandling - skal kaste feil dersom behandlingType er REVURDERING og det ikke finnes noen avsluttede vedtatte behandlinger på fagsaken til søker`() {
        every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(
            lagBehandling(
                fagsak,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD
            ).copy(status = BehandlingStatus.UTREDES)
        )
        val funksjonellFeil = assertThrows<Feil> {
            behandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = søkersIdent,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.BARNEHAGELISTE
                )
            )
        }
        assertEquals(
            "Kan ikke opprette revurdering på $fagsak uten noen andre behandlinger som er vedtatt.",
            funksjonellFeil.message
        )
    }

    @Test
    fun `hentBehandling - skal hente behandling fra behandlingRepository`() {
        val hentetBehandling = behandlingService.hentBehandling(behandling.id)

        assertNotNull(hentetBehandling)
        verify(exactly = 1) { behandlingRepository.hentBehandling(behandling.id) }
    }

    @Test
    fun `lagBehandlingRespons - skal lage BehandlingResponsDto for behandling`() {
        val behandlingResponsDto = behandlingService.lagBehandlingRespons(behandling.id)

        assertNotNull(behandlingResponsDto)
        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.finnArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(behandling.id) }

        assertNotNull(behandlingResponsDto.personer)
        assertEquals(1, behandlingResponsDto.personer.size)
    }

    @Test
    fun `oppdaterBehandlendeEnhet - skal oppdatere behandlende enhet tilknyttet behandling ved hjelp av ArbeidsfordelingService`() {
        val endreBehandlendeEnhetDto = EndreBehandlendeEnhetDto("nyEnhetId", "begrunnelse")
        behandlingService.oppdaterBehandlendeEnhet(behandling.id, endreBehandlendeEnhetDto)

        verify(exactly = 1) {
            arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
                behandling,
                endreBehandlendeEnhetDto
            )
        }
    }
}
