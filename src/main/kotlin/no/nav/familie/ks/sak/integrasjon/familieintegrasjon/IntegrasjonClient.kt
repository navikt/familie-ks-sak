package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.kallEksternTjeneste
import no.nav.familie.ks.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.common.kallEksternTjenesteUtenRespons
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class IntegrasjonClient(
    @Value("\${FAMILIE_INTEGRASJONER_API_URL}") private val integrasjonUri: URI,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "integrasjon") {

    val tilgangPersonUri = UriComponentsBuilder.fromUri(integrasjonUri).pathSegment(PATH_TILGANG_PERSON).build().toUri()

    fun sjekkTilgangTilPersoner(personIdenter: List<String>): Tilgang {
        if (SikkerhetContext.erSystemKontekst()) {
            return Tilgang(true, null)
        }

        val tilganger = kallEksternTjeneste<List<Tilgang>>(
            tjeneste = "tilgangskontroll",
            uri = tilgangPersonUri,
            formål = "Sjekk tilgang til personer"
        ) {
            postForEntity(
                tilgangPersonUri,
                personIdenter,
                HttpHeaders().also {
                    it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_KON)
                }
            )
        }

        return tilganger.firstOrNull { !it.harTilgang } ?: tilganger.first()
    }

    fun ferdigstillOppgave(oppgaveId: Long) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/ferdigstill")

        kallEksternTjenesteUtenRespons(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Ferdigstill oppgave"
        ) {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, "")
        }
    }

    fun fordelOppgave(oppgaveId: Long, saksbehandler: String?): OppgaveResponse {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/fordel")
        val uri = if (saksbehandler == null) {
            baseUri
        } else {
            UriComponentsBuilder.fromUri(baseUri).queryParam("saksbehandler", saksbehandler).build().toUri()
        }

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Fordel oppgave"
        ) {
            postForEntity(
                uri,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun finnOppgaveMedId(oppgaveId: Long): Oppgave {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Finn oppgave med id $oppgaveId"
        ) {
            getForEntity(uri)
        }
    }

    fun hentOppgaver(finnOppgaveRequest: FinnOppgaveRequest): FinnOppgaveResponseDto {
        val uri = URI.create("$integrasjonUri/oppgave/v4")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Hent oppgaver"
        ) {
            postForEntity(
                uri,
                finnOppgaveRequest,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalposterForBruker(journalposterForBrukerRequest: JournalposterForBrukerRequest): List<Journalpost> {
        val uri = URI.create("$integrasjonUri/journalpost")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker"
        ) {
            postForEntity(uri, journalposterForBrukerRequest)
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentJournalpost(journalpostId: String): Journalpost {
        val uri = URI.create("$integrasjonUri/journalpost?journalpostId=$journalpostId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalpost id $journalpostId"
        ) {
            getForEntity(uri)
        }
    }

    fun hentDokumentIJournalpost(dokumentInfoId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent dokument $dokumentInfoId i journalpost $journalpostId"
        ) {
            getForEntity(uri)
        }
    }

    fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders =
        this.apply {
            add("Content-Type", "application/json;charset=UTF-8")
            acceptCharset = listOf(Charsets.UTF_8)
        }

    companion object {

        private val logger = LoggerFactory.getLogger(IntegrasjonClient::class.java)
        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"
        private const val PATH_TILGANG_PERSON = "tilgang/v2/personer"
        private const val HEADER_NAV_TEMA = "Nav-Tema"
        private val HEADER_NAV_TEMA_KON = Tema.KON.name
    }
}
