package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandling
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.HentFagsystemsbehandlingRespons
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FagsystemsbehandlingService(
    private val behandlingService: BehandlingService,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakService: VedtakService,
    private val tilbakekrevingsbehandlingHentService: TilbakekrevingsbehandlingHentService,
    private val kafkaProducer: KafkaProducer,
) {

    fun hentFagsystemsbehandling(request: HentFagsystemsbehandlingRequest): HentFagsystemsbehandlingRespons {
        logger.info("Henter behandling for behandlingId=${request.eksternId}")
        val behandling = behandlingService.hentBehandling(request.eksternId.toLong())

        return lagHentFagsystembehandlingRespons(request, behandling)
    }

    fun sendFagsystemsbehandlingRespons(
        respons: HentFagsystemsbehandlingRespons,
        key: String,
        behandlingId: String,
    ) {
        kafkaProducer.sendFagsystemsbehandlingRespons(respons, key, behandlingId)
    }

    private fun lagHentFagsystembehandlingRespons(
        request: HentFagsystemsbehandlingRequest,
        behandling: Behandling,
    ): HentFagsystemsbehandlingRespons {
        val behandlingId = behandling.id
        val persongrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandlingId = behandlingId)
        val arbeidsfordeling = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandlingId)
        val aktivVedtaksdato = vedtakService.hentAktivVedtakForBehandling(behandlingId).vedtaksdato?.toLocalDate()
            ?: throw Feil("Finnes ikke vedtaksdato for behandling $behandlingId")
        val faktainfo = Faktainfo(
            revurderingsårsak = behandling.opprettetÅrsak.visningsnavn,
            revurderingsresultat = behandling.resultat.displayName,
            tilbakekrevingsvalg = tilbakekrevingsbehandlingHentService.hentTilbakekreving(behandlingId).valg,
        )

        val hentFagsystemsbehandling = HentFagsystemsbehandling(
            eksternFagsakId = request.eksternFagsakId,
            eksternId = request.eksternId,
            ytelsestype = request.ytelsestype,
            regelverk = behandling.kategori.tilRegelverk(),
            personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
            språkkode = persongrunnlag.søker.målform.tilSpråkkode(),
            enhetId = arbeidsfordeling.behandlendeEnhetId,
            enhetsnavn = arbeidsfordeling.behandlendeEnhetNavn,
            revurderingsvedtaksdato = aktivVedtaksdato,
            faktainfo = faktainfo,
            institusjon = null, // alltid null for Kontantstøtte
        )

        return HentFagsystemsbehandlingRespons(hentFagsystemsbehandling = hentFagsystemsbehandling)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
