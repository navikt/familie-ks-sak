package no.nav.familie.ks.sak.integrasjon.tilbakekreving

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.integrasjon.kallEksternTjeneste
import no.nav.familie.ks.sak.integrasjon.kallEksternTjenesteRessurs
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriUtils
import java.net.URI
import java.util.UUID

@Service
class TilbakekrevingKlient(
    @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
    @Qualifier("jwtBearer") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "tilbakreving") {
    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/finnesApenBehandling/v1")

        val finnesTilbakekrevingBehandlingsresponsDto: FinnesTilbakekrevingBehandlingsresponsDto =
            kallEksternTjenesteRessurs(
                tjeneste = "familie-tilbake",
                uri = uri,
                formål = "Sjekker om en fagsak har åpen tilbakekrevingsbehandling",
            ) { getForEntity(uri) }

        return finnesTilbakekrevingBehandlingsresponsDto.finnesÅpenBehandling
    }

    fun hentForhåndsvisningTilbakekrevingVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val uri = URI.create("$familieTilbakeUri/dokument/forhandsvis-varselbrev")

        return kallEksternTjeneste(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter forhåndsvisning av varselbrev",
        ) {
            postForEntity(
                uri = uri,
                payload = forhåndsvisVarselbrevRequest,
                httpHeaders =
                    HttpHeaders().apply {
                        accept = listOf(MediaType.APPLICATION_PDF)
                    },
            )
        }
    }

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): String {
        val uri = URI.create("$familieTilbakeUri/behandling/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter tilbakekrevingsbehandling",
        ) {
            postForEntity(uri, opprettTilbakekrevingRequest)
        }
    }

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/behandlinger/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsbehandlinger på fagsak",
        ) { getForEntity(uri) }
    }

    fun kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId: Long): KanBehandlingOpprettesManueltRespons {
        val uri =
            URI.create(
                encodePath("$familieTilbakeUri/ytelsestype/${Ytelsestype.KONTANTSTØTTE}/fagsak/$fagsakId/kanBehandlingOpprettesManuelt/v1"),
            )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Sjekker om tilbakekrevingsbehandling kan opprettes manuelt",
        ) { getForEntity(uri) }
    }

    fun opprettTilbakekrevingsbehandlingManuelt(request: OpprettManueltTilbakekrevingRequest): String {
        val uri = URI.create("$familieTilbakeUri/behandling/manuelt/task/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter tilbakekrevingsbehandling manuelt",
        ) { postForEntity(uri, request) }
    }

    fun hentTilbakekrevingsvedtak(fagsakId: Long): List<FagsystemVedtak> {
        val uri = URI.create(encodePath("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/vedtak/v1"))

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsvedtak på fagsak",
        ) { getForEntity(uri) }
    }

    fun oppdaterEnhetPåÅpenBehandling(
        behandlingEksternBrukId: UUID,
        nyEnhetId: String,
    ): String {
        val uri = URI.create("$familieTilbakeUri/baks/portefoljejustering/oppdater-behandlende-enhet")

        val request =
            OppdaterBehandlendeEnhetRequest(
                behandlingEksternBrukId = behandlingEksternBrukId,
                nyEnhet = nyEnhetId,
            )

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppdater enhet på åpen tilbakekrevingsbehandling",
        ) {
            putForEntity(uri, request)
        }
    }

    data class OppdaterBehandlendeEnhetRequest(
        val behandlingEksternBrukId: UUID,
        val nyEnhet: String,
    )
}

fun encodePath(path: String) = UriUtils.encodePath(path, "UTF-8")

data class FinnesTilbakekrevingBehandlingsresponsDto(
    val finnesÅpenBehandling: Boolean,
)
