package no.nav.familie.ks.sak.integrasjon.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import no.nav.familie.ks.sak.integrasjon.mottak.MottakClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.sikkerhet.SaksbehandlerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InnkommendeJournalføringServiceTest {
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val opprettBehandlingService: OpprettBehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val loggService: LoggService = mockk()
    private val stegService: StegService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val mottakClient: MottakClient = mockk()
    private val saksbehandlerContext: SaksbehandlerContext = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonClient = integrasjonClient,
            fagsakService = fagsakService,
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            journalføringRepository = journalføringRepository,
            loggService = loggService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
            mottakClient = mottakClient,
            saksbehandlerContext = saksbehandlerContext,
        )

    @Test
    fun `skal returnere alle journalposter med harTilgang satt til true for bruker når personer i de digitale søknadene har adressebeskyttelsegradering ugradert`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId = "123"
        val journalposter = listOf(lagJournalpost(personIdent = brukerId, journalpostId = journalpostId))

        every {
            integrasjonClient.hentJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.KON),
                ),
            )
        } returns journalposter

        every { mottakClient.hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId = journalpostId) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT

        every { saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.UGRADERT) } returns true
        // Act
        val journalposterForBruker = innkommendeJournalføringService.hentJournalposterForBruker(brukerId)

        // Assert
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.harTilgang).isTrue
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
    }

    @Test
    fun `skal sette harTilgang til false på journalpost når den er en digital søknad og har personer med adressebeskyttelsegradering dersom saksbehandler ikke har tilgang`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId1 = "123"
        val journalpostId2 = "456"
        val journalposter = listOf(lagJournalpost(personIdent = brukerId, journalpostId = journalpostId1), lagJournalpost(personIdent = brukerId, journalpostId = journalpostId2))

        every {
            integrasjonClient.hentJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.KON),
                ),
            )
        } returns journalposter

        every { mottakClient.hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId = journalpostId1) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { mottakClient.hentStrengesteAdressebeskyttelsegraderingIDigitalSøknad(journalpostId = journalpostId2) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG

        every { saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.UGRADERT) } returns true
        every { saksbehandlerContext.harTilgang(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG) } returns false
        // Act
        val journalposterForBruker = innkommendeJournalføringService.hentJournalposterForBruker(brukerId)

        // Assert
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId1 }.harTilgang).isTrue
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId1 }.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.UGRADERT)
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId2 }.harTilgang).isFalse
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId2 }.adressebeskyttelsegradering).isEqualTo(ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG)
    }

    @Test
    fun `skal returnere alle journalposter med harTilgang satt til true for bruker når søknadene ikke er digitale, da vet vi ingenting om adressebeskyttelsegradering`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId = "123"
        val journalposter = listOf(lagJournalpost(personIdent = brukerId, journalpostId = journalpostId, kanal = "SKAN_NETS"))

        every {
            integrasjonClient.hentJournalposterForBruker(
                JournalposterForBrukerRequest(
                    antall = 1000,
                    brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                    tema = listOf(Tema.KON),
                ),
            )
        } returns journalposter

        // Act
        val journalposterForBruker = innkommendeJournalføringService.hentJournalposterForBruker(brukerId)

        // Assert
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.harTilgang).isTrue
        assertThat(journalposterForBruker.first { it.journalpost.journalpostId === journalpostId }.adressebeskyttelsegradering).isNull()
    }
}
