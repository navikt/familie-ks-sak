package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.NavIdent
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val klageClient: KlageClient,
    private val integrasjonClient: IntegrasjonClient,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val klagebehandlingHenter: KlagebehandlingHenter,
) {
    fun opprettKlage(
        fagsakId: Long,
        klageMottattDato: LocalDate,
    ): UUID {
        val fagsak = fagsakService.hentFagsak(fagsakId)

        return opprettKlage(fagsak, klageMottattDato)
    }

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottattDato: LocalDate,
    ): UUID {
        if (klageMottattDato.isAfter(LocalDate.now())) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid")
        }

        val aktivtFødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        val enhetsnummer = integrasjonClient.hentBehandlendeEnheterSomNavIdentHarTilgangTil(NavIdent(saksbehandlerIdent)).first().enhetsnummer

        return klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivtFødselsnummer,
                stønadstype = Stønadstype.KONTANTSTØTTE,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.KS,
                klageMottatt = klageMottattDato,
                behandlendeEnhet = enhetsnummer,
                behandlingsårsak = Klagebehandlingsårsak.ORDINÆR,
            ),
        )
    }

    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> = klagebehandlingHenter.hentKlagebehandlingerPåFagsak(fagsakId)

    fun hentForrigeVedtatteKlagebehandling(behandling: Behandling): KlagebehandlingDto? = klagebehandlingHenter.hentForrigeVedtatteKlagebehandling(behandling)

    fun hentFagsystemVedtak(fagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val behandlinger = behandlingService.hentFerdigstilteBehandlinger(fagsak)

        val ferdigstilteKsVedtak = behandlinger.map { tilFagsystemVedtak(it) }
        val vedtakTilbakekreving = tilbakekrevingKlient.hentTilbakekrevingsvedtak(fagsakId)

        return ferdigstilteKsVedtak + vedtakTilbakekreving
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandling.id)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = behandling.resultat.displayName,
            vedtakstidspunkt = vedtak.vedtaksdato ?: throw Feil("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR,
            regelverk = behandling.kategori.tilRegelverk(),
        )
    }
}
