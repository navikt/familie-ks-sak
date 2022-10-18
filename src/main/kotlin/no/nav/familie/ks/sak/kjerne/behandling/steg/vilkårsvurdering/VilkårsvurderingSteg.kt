package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class VilkårsvurderingSteg : IBehandlingSteg {

    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.VILKÅRSVURDERING

    override fun utførSteg(behandlingId: Long) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(VilkårsvurderingSteg::class.java)
    }
}
