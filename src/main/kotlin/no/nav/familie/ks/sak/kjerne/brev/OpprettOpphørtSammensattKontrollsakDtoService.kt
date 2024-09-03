package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettOpphørtSammensattKontrollsakDtoService(
    private val opprettVedtakFellesfelterSammensattKontrollsakDtoService: OpprettVedtakFellesfelterSammensattKontrollsakDtoService,
    private val simuleringService: SimuleringService,
) {
    private val logger = LoggerFactory.getLogger(OpprettOpphørtSammensattKontrollsakDtoService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): OpphørtSammensattKontrollsakDto {
        logger.debug("Oppretter ${OpphørtSammensattKontrollsakDto::class.simpleName} for vedtak ${vedtak.id}")
        return OpphørtSammensattKontrollsakDto(
            vedtakFellesfelterSammensattKontrollsakDto = opprettVedtakFellesfelterSammensattKontrollsakDtoService.opprett(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
        )
    }
}
