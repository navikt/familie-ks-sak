package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostRequest
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.common.kallEksternTjeneste
import no.nav.familie.ks.sak.common.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.common.kallEksternTjenesteUtenRespons
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsfordelingsenhet
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.Arbeidsforhold
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.ArbeidsforholdRequest
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate

const val DEFAULT_JOURNALFØRENDE_ENHET = "9999"

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
                    it.set(HEADER_NAV_TEMA, HEADER_NAV_TEMA_BAR)
                }
            )
        }

        return tilganger.firstOrNull { !it.harTilgang } ?: tilganger.first()
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    @Cacheable("behandlendeEnhet", cacheManager = "shortCache")
    fun hentBehandlendeEnhet(ident: String): List<Arbeidsfordelingsenhet> {
        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
            .pathSegment("arbeidsfordeling", "enhet", "BAR")
            .build().toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent behandlende enhet"
        ) {
            postForEntity(uri, mapOf("ident" to ident))
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delayExpression = RETRY_BACKOFF_5000MS)
    )
    fun hentArbeidsforhold(ident: String, ansettelsesperiodeFom: LocalDate): List<Arbeidsforhold> {
        val uri = UriComponentsBuilder.fromUri(integrasjonUri)
            .pathSegment("aareg", "arbeidsforhold")
            .build().toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "aareg",
            uri = uri,
            formål = "Hent arbeidsforhold"
        ) {
            postForEntity(uri, ArbeidsforholdRequest(ident, ansettelsesperiodeFom))
        }
    }

    fun distribuerBrev(journalpostId: String, distribusjonstype: Distribusjonstype): String {
        val uri = URI.create("$integrasjonUri/dist/v1")

        val resultat: String = kallEksternTjenesteRessurs(
            tjeneste = "dokdist",
            uri = uri,
            formål = "Distribuer brev"
        ) {
            val journalpostRequest = DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = Fagsystem.BA,
                dokumentProdApp = "FAMILIE_BA_SAK",
                distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
                distribusjonstype = distribusjonstype
            )
            postForEntity(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
        }

        if (resultat.isBlank()) error("BestillingsId fra integrasjonstjenesten mot dokdist er tom")
        return resultat
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

    fun oppdaterOppgave(oppgaveId: Long, oppdatertOppgave: Oppgave) {
        val uri = URI.create("$integrasjonUri/oppgave/$oppgaveId/oppdater")

        kallEksternTjenesteUtenRespons(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Oppdater oppgave"
        ) {
            patchForEntity<Ressurs<OppgaveResponse>>(uri, oppdatertOppgave)
        }
    }

    @Cacheable("enhet", cacheManager = "kodeverkCache")
    fun hentEnhet(enhetId: String?): NavKontorEnhet {
        val uri = URI.create("$integrasjonUri/arbeidsfordeling/nav-kontor/$enhetId")

        return kallEksternTjenesteRessurs(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent nav kontor for enhet $enhetId"
        ) {
            getForEntity(uri)
        }
    }

    fun opprettOppgave(opprettOppgave: OpprettOppgaveRequest): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/opprett")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Opprett oppgave"
        ) {
            postForEntity(
                uri,
                opprettOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            )
        }
    }

    fun patchOppgave(patchOppgave: Oppgave): OppgaveResponse {
        val uri = URI.create("$integrasjonUri/oppgave/${patchOppgave.id}/oppdater")

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Patch oppgave"
        ) {
            patchForEntity(
                uri,
                patchOppgave,
                HttpHeaders().medContentTypeJsonUTF8()
            )
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

    fun ferdigstillJournalpost(journalpostId: String, journalførendeEnhet: String) {
        val uri =
            URI.create("$integrasjonUri/arkiv/v2/$journalpostId/ferdigstill?journalfoerendeEnhet=$journalførendeEnhet")

        kallEksternTjenesteUtenRespons(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent journalposter for bruker"
        ) {
            putForEntity<Ressurs<Any>>(uri, "")
        }
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Oppdater journalpost"
        ) {
            putForEntity(uri, request)
        }
    }

    fun leggTilLogiskVedlegg(request: LogiskVedleggRequest, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Legg til logisk vedlegg på dokument $dokumentinfoId"
        ) {
            postForEntity(uri, request)
        }
    }

    fun slettLogiskVedlegg(logiskVedleggId: String, dokumentinfoId: String): LogiskVedleggResponse {
        val uri = URI.create("$integrasjonUri/arkiv/dokument/$dokumentinfoId/logiskVedlegg/$logiskVedleggId")
        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Slett logisk vedlegg på dokument $dokumentinfoId"
        ) {
            deleteForEntity(uri)
        }
    }

    fun hentDokument(dokumentInfoId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentInfoId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent dokument $dokumentInfoId"
        ) {
            getForEntity(uri)
        }
    }

    fun journalførDokument(
        arkiverDokumentRequest: ArkiverDokumentRequest
    ): ArkiverDokumentResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v4")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Journalfør dokument på fagsak ${arkiverDokumentRequest.fagsakId}"
        ) {
            postForEntity(uri, arkiverDokumentRequest)
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
        private val HEADER_NAV_TEMA_BAR = Tema.KON.name
    }
}
