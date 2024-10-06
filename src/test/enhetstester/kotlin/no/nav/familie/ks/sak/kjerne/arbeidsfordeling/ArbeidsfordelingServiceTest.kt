package no.nav.familie.ks.sak.kjerne.arbeidsfordeling

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.pdl.PersonOpplysningerService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlagRepository
import no.nav.familie.unleash.UnleashService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class ArbeidsfordelingServiceTest {
    private val arbeidsfordelingPåBehandlingRepository: ArbeidsfordelingPåBehandlingRepository = mockk()

    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository = mockk()

    private val integrasjonClient: IntegrasjonClient = mockk()

    private val personOpplysningerService: PersonOpplysningerService = mockk()

    private val oppgaveService: OppgaveService = mockk()

    private val loggService: LoggService = mockk()

    private val personidentService: PersonidentService = mockk()

    private val unleashService: UnleashService = mockk()

    private val tilpassArbeidsfordelingService: TilpassArbeidsfordelingService = mockk()

    private val arbeidsfordelingService: ArbeidsfordelingService =
        ArbeidsfordelingService(
            arbeidsfordelingPåBehandlingRepository = arbeidsfordelingPåBehandlingRepository,
            personopplysningGrunnlagRepository = personopplysningGrunnlagRepository,
            integrasjonClient = integrasjonClient,
            personOpplysningerService = personOpplysningerService,
            oppgaveService = oppgaveService,
            loggService = loggService,
            personidentService = personidentService,
            tilpassArbeidsfordelingService = tilpassArbeidsfordelingService,
            unleashService = unleashService,
        )

    @Test
    fun `finnArbeidsfordelingPåBehandling skal kaste exception dersom behandling ikke har tilknyttet arbeidsfordeling`() {
        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(404) } returns null

        val feil =
            assertThrows<IllegalStateException> { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(404) }

        assertThat(feil.message, Is("Finner ikke tilknyttet arbeidsfordeling på behandling med id 404"))
    }

    @Test
    fun `finnArbeidsfordelingPåBehandling skal returnere ArbeidsfordelingPåBehandling dersom det eksisterer i db`() {
        val mockedArbeidsfordelingPåBehandling = mockk<ArbeidsfordelingPåBehandling>()
        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(200) } returns mockedArbeidsfordelingPåBehandling

        val finnArbeidsfordelingPåBehandling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(200)

        assertThat(finnArbeidsfordelingPåBehandling, Is(mockedArbeidsfordelingPåBehandling))
    }

    @Test
    fun `manueltOppdaterBehandlendeEnhet skal kaste exception dersom behandling ikke har tilknyttet arbeidsfordeling`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL)
        val endreBehandlendeEnhetDto = EndreBehandlendeEnhetDto("testId", "testBegrunnelse")

        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id) } returns null

        val feil =
            assertThrows<IllegalStateException> {
                arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
                    behandling,
                    endreBehandlendeEnhetDto,
                )
            }

        assertThat(feil.message, Is("Finner ikke tilknyttet arbeidsfordeling på behandling med id ${behandling.id}"))
    }

    @Test
    fun `manueltOppdaterBehandlendeEnhet skal kaste lagre ned ny arbeidsfordelingPåBehandling med nye detaljer`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.ÅRLIG_KONTROLL)
        val endreBehandlendeEnhetDto = EndreBehandlendeEnhetDto("testId", "testBegrunnelse")
        val mockedArbeidsfordelingPåBehandling = mockk<ArbeidsfordelingPåBehandling>(relaxed = true)
        val mockedArbeidsfordelingPåBehandlingEtterEndring = mockk<ArbeidsfordelingPåBehandling>(relaxed = true)

        every { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id) } returns mockedArbeidsfordelingPåBehandling
        every { integrasjonClient.hentNavKontorEnhet("testId") } returns
            NavKontorEnhet(
                0,
                "testNavn",
                "testEnhet",
                "testStatus",
            )
        every {
            mockedArbeidsfordelingPåBehandling.copy(
                0,
                0,
                "testId",
                "testNavn",
                true,
            )
        } returns mockedArbeidsfordelingPåBehandlingEtterEndring
        every {
            arbeidsfordelingPåBehandlingRepository.save(mockedArbeidsfordelingPåBehandlingEtterEndring)
        } returns mockedArbeidsfordelingPåBehandlingEtterEndring

        arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(behandling, endreBehandlendeEnhetDto)

        verify(exactly = 1) { arbeidsfordelingPåBehandlingRepository.finnArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { integrasjonClient.hentNavKontorEnhet("testId") }
        verify(exactly = 1) { mockedArbeidsfordelingPåBehandling.copy(0, 0, "testId", "testNavn", true) }
        verify(exactly = 1) { arbeidsfordelingPåBehandlingRepository.save(mockedArbeidsfordelingPåBehandlingEtterEndring) }
    }
}
