package no.nav.familie.ks.sak.integrasjon.journalføring

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.journalpost.TilgangsstyrtJournalpost
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.journalføring.domene.JournalføringRepository
import no.nav.familie.ks.sak.integrasjon.lagJournalpost
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSøknadsinfoService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InnkommendeJournalføringServiceTest {
    private val integrasjonClient: IntegrasjonClient = mockk()
    private val fagsakService: FagsakService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val opprettBehandlingService: OpprettBehandlingService = mockk()
    private val journalføringRepository: JournalføringRepository = mockk()
    private val loggService: LoggService = mockk()
    private val behandlingSøknadsinfoService: BehandlingSøknadsinfoService = mockk()
    private val innkommendeJournalføringService: InnkommendeJournalføringService =
        InnkommendeJournalføringService(
            integrasjonClient = integrasjonClient,
            fagsakService = fagsakService,
            opprettBehandlingService = opprettBehandlingService,
            behandlingService = behandlingService,
            journalføringRepository = journalføringRepository,
            loggService = loggService,
            behandlingSøknadsinfoService = behandlingSøknadsinfoService,
        )

    @Test
    fun `skal hente og returnere tilgangsstyrte journalposter`() {
        // Arrange
        val brukerId = "12345678910"
        val journalpostId = "123"
        val journalposter = listOf(lagJournalpost(personIdent = brukerId, journalpostId = journalpostId)).map { TilgangsstyrtJournalpost(it, true) }

        every {
            integrasjonClient.hentTilgangsstyrteJournalposterForBruker(
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
    }
}
