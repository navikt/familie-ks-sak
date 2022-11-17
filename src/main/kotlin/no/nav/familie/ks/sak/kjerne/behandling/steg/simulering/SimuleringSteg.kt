package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SimuleringSteg : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.SIMULERING

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        // TODO: håndter eventuell tilbakekreving. Relevant for revurderinger, men ikke i førstegangsbehandling.
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SimuleringSteg::class.java)
    }
}
