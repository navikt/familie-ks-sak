package no.nav.familie.ks.sak.integrasjon.familieintegrasjon

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggRequest
import no.nav.familie.kontrakter.felles.dokarkiv.LogiskVedleggResponse
import no.nav.familie.kontrakter.felles.dokarkiv.OppdaterJournalpostResponse
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokdist.DistribuerJournalpostRequest
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstidspunkt
import no.nav.familie.kontrakter.felles.dokdist.Distribusjonstype
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.JournalposterForBrukerRequest
import no.nav.familie.kontrakter.felles.kodeverk.KodeverkDto
import no.nav.familie.kontrakter.felles.navkontor.NavKontorEnhet
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveResponseDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveResponse
import no.nav.familie.kontrakter.felles.oppgave.OpprettOppgaveRequest
import no.nav.familie.kontrakter.felles.tilgangskontroll.Tilgang
import no.nav.familie.ks.sak.api.dto.OppdaterJournalpostRequestDto
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.domene.ArbeidsfordelingsEnhet
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteUtenRespons
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
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

        return tilganger.firstOrNull { !it.harTilgang } ?: tilganger.firstOrNull() ?: Tilgang(harTilgang = false)
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

    fun tilordneEnhetForOppgave(oppgaveId: Long, nyEnhet: String): OppgaveResponse {
        val baseUri = URI.create("$integrasjonUri/oppgave/$oppgaveId/enhet/$nyEnhet")
        val uri = UriComponentsBuilder.fromUri(baseUri).queryParam("fjernMappeFraOppgave", true).build()
            .toUri() // fjerner alltid mappe fra Kontantstøtte siden hver enhet har sin mappestruktur

        return kallEksternTjenesteRessurs(
            tjeneste = "oppgave",
            uri = uri,
            formål = "Bytt enhet"
        ) {
            patchForEntity(
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

    fun hentDokumentIJournalpost(dokumentId: String, journalpostId: String): ByteArray {
        val uri = URI.create("$integrasjonUri/journalpost/hentdokument/$journalpostId/$dokumentId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Hent dokument $dokumentId i journalpost $journalpostId"
        ) {
            getForEntity(uri)
        }
    }

    @Cacheable("behandlendeEnhet", cacheManager = "shortCache")
    fun hentBehandlendeEnheter(ident: String): List<ArbeidsfordelingsEnhet> {
        val uri = UriComponentsBuilder
            .fromUri(integrasjonUri)
            .pathSegment("arbeidsfordeling", "enhet", Tema.KON.name)
            .build().toUri()

        return kallEksternTjenesteRessurs(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent behandlende enhet"
        ) {
            postForEntity(uri, mapOf("ident" to ident))
        }
    }

    @Cacheable("enhet", cacheManager = "kodeverkCache")
    fun hentNavKontorEnhet(enhetId: String?): NavKontorEnhet {
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

    fun oppdaterJournalpost(
        request: OppdaterJournalpostRequestDto,
        journalpostId: String
    ): OppdaterJournalpostResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v2/$journalpostId")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Oppdater journalpost"
        ) {
            putForEntity(uri, request)
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

    fun HttpHeaders.medContentTypeJsonUTF8(): HttpHeaders =
        this.apply {
            add("Content-Type", "application/json;charset=UTF-8")
            acceptCharset = listOf(Charsets.UTF_8)
        }

    fun journalførDokument(arkiverDokumentRequest: ArkiverDokumentRequest): ArkiverDokumentResponse {
        val uri = URI.create("$integrasjonUri/arkiv/v4")

        return kallEksternTjenesteRessurs(
            tjeneste = "dokarkiv",
            uri = uri,
            formål = "Journalfør dokument på fagsak ${arkiverDokumentRequest.fagsakId}"
        ) {
            postForEntity(uri, arkiverDokumentRequest)
        }
    }

    @Cacheable("land", cacheManager = "kodeverkCache")
    fun hentLand(landkode: String): String {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/$landkode")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent landkoder for $landkode"
        ) {
            getForEntity(uri)
        }
    }

    @Cacheable("alle-eøs-land", cacheManager = "kodeverkCache")
    fun hentAlleEØSLand(): KodeverkDto {
        val uri = URI.create("$integrasjonUri/kodeverk/landkoder/eea")

        return kallEksternTjenesteRessurs(
            tjeneste = "kodeverk",
            uri = uri,
            formål = "Hent EØS land"
        ) {
            getForEntity(uri)
        }
    }

    fun distribuerBrev(journalpostId: String, distribusjonstype: Distribusjonstype): String {
        val uri = URI.create("$integrasjonUri/dist/v1")

        val journalpostRequest = DistribuerJournalpostRequest(
            journalpostId = journalpostId,
            bestillendeFagsystem = Fagsystem.KONT,
            dokumentProdApp = "FAMILIE_KS_SAK",
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            distribusjonstype = distribusjonstype
        )

        val bestillingId: String = kallEksternTjenesteRessurs(
            tjeneste = "dokdist",
            uri = uri,
            formål = "Distribuer brev"
        ) {
            postForEntity(uri, journalpostRequest, HttpHeaders().medContentTypeJsonUTF8())
        }

        return bestillingId
    }

    @Cacheable("behandlendeEnhetForPersonMedRelasjon", cacheManager = "shortCache")
    fun hentBehandlendeEnhetForPersonIdentMedRelasjoner(ident: String): ArbeidsfordelingsEnhet {
        val uri = UriComponentsBuilder
            .fromUri(integrasjonUri)
            .pathSegment("arbeidsfordeling", "enhet", Tema.KON.name, "med-relasjoner")
            .build().toUri()

        return kallEksternTjenesteRessurs<List<ArbeidsfordelingsEnhet>>(
            tjeneste = "arbeidsfordeling",
            uri = uri,
            formål = "Hent strengeste behandlende enhet for person og alle relasjoner til personen"
        ) {
            postForEntity(uri, PersonIdent(ident))
        }.single()
    }

    companion object {

        const val RETRY_BACKOFF_5000MS = "\${retry.backoff.delay:5000}"
        private const val PATH_TILGANG_PERSON = "tilgang/v2/personer"
        private const val HEADER_NAV_TEMA = "Nav-Tema"
        private val HEADER_NAV_TEMA_KON = Tema.KON.name
    }
}
