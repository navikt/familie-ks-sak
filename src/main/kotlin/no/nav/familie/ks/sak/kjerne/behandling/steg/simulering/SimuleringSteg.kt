package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.tilbakekreving.TilbakekrevingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SimuleringSteg(
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
    private val tilbakekrevingService: TilbakekrevingService
) : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.SIMULERING

    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId med tilbakekreving")
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (tilbakekrevingService.harÅpenTilbakekrevingsbehandling(behandling.fagsak.id)) {
            logger.info("Det finnes allerede en åpen tilbakekrevingsbehandling for fagsak ${behandling.fagsak.id}")
            return
        }

        val tilbakekrevingRequestDto = behandlingStegDto as TilbakekrevingRequestDto
        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingId)
        validerTilbakekrevingData(tilbakekrevingRequestDto, feilutbetaling)
        tilbakekrevingService.lagreTilbakekreving(tilbakekrevingRequestDto, behandling)
    }

    // denne metoden kalles når frontend ikke sender tilbakekrevingDto,
    // d.v.s for førstegangsbehandling eller behandling som ikke har en feilutbetaling
    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId uten tilbakekreving")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SimuleringSteg::class.java)
    }
}
