package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsak
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettOpphørtSammensattKontrollsakService(
    private val opprettVedtakFellesfelterSammensattKontrollsakService: OpprettVedtakFellesfelterSammensattKontrollsakService,
    private val simuleringService: SimuleringService,
) {
    private val logger = LoggerFactory.getLogger(OpprettOpphørtSammensattKontrollsakService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): OpphørtSammensattKontrollsak {
        logger.debug("Oppretter ${OpphørtSammensattKontrollsak::class.simpleName} for vedtak ${vedtak.id}")
        return OpphørtSammensattKontrollsak(
            vedtakFellesfelterSammensattKontrollsak = opprettVedtakFellesfelterSammensattKontrollsakService.opprett(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
        )
    }
}
