package no.nav.familie.ks.sak.kjerne.klage

import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val klagebehandlingHenter: KlagebehandlingHenter,
    private val klagebehandlingOppretter: KlagebehandlingOppretter,
) {
    fun opprettKlage(
        fagsakId: Long,
        klageMottattDato: LocalDate,
    ): UUID = klagebehandlingOppretter.opprettKlage(fagsakId, klageMottattDato)

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottattDato: LocalDate,
    ): UUID = klagebehandlingOppretter.opprettKlage(fagsak, klageMottattDato)

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
