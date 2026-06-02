package no.nav.familie.ks.sak.integrasjon.tilbakekreving

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
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriUtils
import java.net.URI

@Service
class TilbakekrevingKlient(
    @Value("\${FAMILIE_TILBAKE_API_URL}") private val familieTilbakeUri: URI,
    @Qualifier("tilbakekrevingRestClient") private val restClient: RestClient,
) {
    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/finnesApenBehandling/v1")

        val finnesTilbakekrevingBehandlingsresponsDto: FinnesTilbakekrevingBehandlingsresponsDto =
            kallEksternTjenesteRessurs(
                tjeneste = "familie-tilbake",
                uri = uri,
                formål = "Sjekker om en fagsak har åpen tilbakekrevingsbehandling",
            ) {
                restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .body()!!
            }

        return finnesTilbakekrevingBehandlingsresponsDto.finnesÅpenBehandling
    }

    fun hentForhåndsvisningTilbakekrevingVarselbrev(forhåndsvisVarselbrevRequest: ForhåndsvisVarselbrevRequest): ByteArray {
        val uri = URI.create("$familieTilbakeUri/dokument/forhandsvis-varselbrev")

        return kallEksternTjeneste(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter forhåndsvisning av varselbrev",
        ) {
            restClient
                .post()
                .uri(uri)
                .headers { it.accept = listOf(MediaType.APPLICATION_PDF) }
                .body(forhåndsvisVarselbrevRequest)
                .retrieve()
                .body<ByteArray>()!!
        }
    }

    fun opprettTilbakekrevingBehandling(opprettTilbakekrevingRequest: OpprettTilbakekrevingRequest): String {
        val uri = URI.create("$familieTilbakeUri/behandling/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter tilbakekrevingsbehandling",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(opprettTilbakekrevingRequest)
                .retrieve()
                .body()!!
        }
    }

    fun hentTilbakekrevingsbehandlinger(fagsakId: Long): List<Behandling> {
        val uri = URI.create("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/behandlinger/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsbehandlinger på fagsak",
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
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
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
    }

    fun opprettTilbakekrevingsbehandlingManuelt(request: OpprettManueltTilbakekrevingRequest): String {
        val uri = URI.create("$familieTilbakeUri/behandling/manuelt/task/v1")

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Oppretter tilbakekrevingsbehandling manuelt",
        ) {
            restClient
                .post()
                .uri(uri)
                .body(request)
                .retrieve()
                .body()!!
        }
    }

    fun hentTilbakekrevingsvedtak(fagsakId: Long): List<FagsystemVedtak> {
        val uri = URI.create(encodePath("$familieTilbakeUri/fagsystem/${Fagsystem.KONT}/fagsak/$fagsakId/vedtak/v1"))

        return kallEksternTjenesteRessurs(
            tjeneste = "familie-tilbake",
            uri = uri,
            formål = "Henter tilbakekrevingsvedtak på fagsak",
        ) {
            restClient
                .get()
                .uri(uri)
                .retrieve()
                .body()!!
        }
    }
}

fun encodePath(path: String) = UriUtils.encodePath(path, "UTF-8")

data class FinnesTilbakekrevingBehandlingsresponsDto(
    val finnesÅpenBehandling: Boolean,
)
