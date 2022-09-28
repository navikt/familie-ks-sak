package no.nav.familie.ks.sak.kjerne.behandling.steg

import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.RegisterSøknadDto
import no.nav.familie.ks.sak.api.dto.writeValueAsString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RegisterSøknadSteg : IBehandlingSteg {
    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.REGISTRERE_SØKNAD

    override fun utførSteg(behandlingId: Long, behandlingStegDto: BehandlingStegDto) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")
        val registerSøknadDto = behandlingStegDto as RegisterSøknadDto
        secureLogger.info("Data mottatt ${registerSøknadDto.søknad.writeValueAsString()}")
    }

    override fun gjenopptaSteg(behandlingId: Long) {
        logger.info("Gjenopptar steg ${getBehandlingssteg().name} for behandling $behandlingId")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegisterSøknadSteg::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
