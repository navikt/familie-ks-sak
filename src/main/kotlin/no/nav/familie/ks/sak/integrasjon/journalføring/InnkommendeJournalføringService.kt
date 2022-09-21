package no.nav.familie.ks.sak.integrasjon.journalføring

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Bruker
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import org.springframework.stereotype.Service

@Service
class InnkommendeJournalføringService(private val integrasjonClient: IntegrasjonClient) {

    fun hentJournalposterForBruker(brukerId: String): List<Journalpost> =
        integrasjonClient.hentJournalposterForBruker(
            JournalposterForBrukerRequest(
                antall = 1000,
                brukerId = Bruker(id = brukerId, type = BrukerIdType.FNR),
                tema = listOf(Tema.KON)
            )
        )

    fun hentDokumentIJournalpost(journalpostId: String, dokumentInfoId: String): ByteArray =
        integrasjonClient.hentDokumentIJournalpost(dokumentInfoId, journalpostId)
}
