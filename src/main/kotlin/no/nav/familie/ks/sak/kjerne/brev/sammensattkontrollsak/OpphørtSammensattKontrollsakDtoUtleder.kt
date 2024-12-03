package no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpphørtSammensattKontrollsakDtoUtleder(
    private val vedtakFellesfelterSammensattKontrollsakDtoUtleder: VedtakFellesfelterSammensattKontrollsakDtoUtleder,
    private val simuleringService: SimuleringService,
) {
    private val logger = LoggerFactory.getLogger(OpphørtSammensattKontrollsakDtoUtleder::class.java)

    fun utled(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): OpphørtSammensattKontrollsakDto {
        logger.debug("Utleder ${OpphørtSammensattKontrollsakDto::class.simpleName} for vedtak ${vedtak.id}")
        return OpphørtSammensattKontrollsakDto(
            vedtakFellesfelterSammensattKontrollsakDto = vedtakFellesfelterSammensattKontrollsakDtoUtleder.utled(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
        )
    }
}
