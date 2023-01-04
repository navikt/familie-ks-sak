package no.nav.familie.ks.sak.kjerne.klage

import brukVedtaksdatoFraKlageinstansHvisOversendt
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.FagsystemType
import no.nav.familie.kontrakter.felles.klage.FagsystemVedtak
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.ks.sak.kjerne.klage.dto.OpprettKlageDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val klageClient: KlageClient,
    private val integrasjonClient: IntegrasjonClient,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService

) {

    fun opprettKlage(fagsakId: Long, opprettKlageDto: OpprettKlageDto) {
        val fagsak = fagsakService.hentFagsak(fagsakId)

        opprettKlage(fagsak, opprettKlageDto.kravMottattDato)
    }

    fun opprettKlage(fagsak: Fagsak, kravMottattDato: LocalDate) {
        if (kravMottattDato.isAfter(LocalDate.now())) {
            throw FunksjonellFeil("Kan ikke opprette klage med krav mottatt frem i tid")
        }

        val aktivtFødselsnummer = fagsak.aktør.aktivFødselsnummer()
        val enhetId = integrasjonClient.hentBehandlendeEnhetForPersonIdentMedRelasjoner(aktivtFødselsnummer).enhetId

        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivtFødselsnummer,
                stønadstype = Stønadstype.KONTANTSTØTTE,
                eksternFagsakId = fagsak.id.toString(),
                fagsystem = Fagsystem.KS,
                klageMottatt = kravMottattDato,
                behandlendeEnhet = enhetId
            )
        )
    }

    fun hentKlagebehandlingerPåFagsak(fagsakId: Long): List<KlagebehandlingDto> {
        val klagebehandligerPerFagsak = klageClient.hentKlagebehandlinger(setOf(fagsakId))

        val klagerPåFagsak = klagebehandligerPerFagsak[fagsakId]
            ?: throw Feil("Fikk ikke fagsakId=$fagsakId tilbake fra kallet til klage.")

        return klagerPåFagsak.map { it.brukVedtaksdatoFraKlageinstansHvisOversendt() }
    }

    fun hentFagsystemVedtak(fagsakId: Long): List<FagsystemVedtak> {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val behandlinger = behandlingService.hentFerdigstilteBehandlinger(fagsak)
        val ferdigstilteKsBehandlinger = behandlinger.map { tilFagsystemVedtak(it) }

        // TODO når vi har fått inn tilbakekreving
        val vedtakTilbakekreving = emptyList<FagsystemVedtak>()

        return ferdigstilteKsBehandlinger + vedtakTilbakekreving
    }

    private fun tilFagsystemVedtak(behandling: Behandling): FagsystemVedtak {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandling.id)

        return FagsystemVedtak(
            eksternBehandlingId = behandling.id.toString(),
            behandlingstype = behandling.type.visningsnavn,
            resultat = behandling.resultat.displayName,
            vedtakstidspunkt = vedtak.vedtaksdato ?: error("Mangler vedtakstidspunkt for behandling=${behandling.id}"),
            fagsystemType = FagsystemType.ORDNIÆR
        )
    }
}
