package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørMedEndringSammensattKontrollsakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettOpphørMedEndringSammensattKontrollsakDtoService(
    private val opprettVedtakFellesfelterSammensattKontrollsakDtoService: OpprettVedtakFellesfelterSammensattKontrollsakDtoService,
    private val etterbetalingService: EtterbetalingService,
    private val simuleringService: SimuleringService,
    private val brevPeriodeService: BrevPeriodeService,
) {
    private val logger = LoggerFactory.getLogger(OpprettOpphørMedEndringSammensattKontrollsakDtoService::class.java)

    fun opprett(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): OpphørMedEndringSammensattKontrollsakDto {
        logger.debug("Oppretter ${OpphørMedEndringSammensattKontrollsakDto::class.simpleName} for vedtak ${vedtak.id}")
        return OpphørMedEndringSammensattKontrollsakDto(
            vedtakFellesfelter = opprettVedtakFellesfelterSammensattKontrollsakDtoService.opprett(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            etterbetaling = etterbetalingService.hentEtterbetaling(vedtak = vedtak),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
            refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak = vedtak),
            refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak = vedtak),
            erKlage = vedtak.behandling.erKlage(),
        )
    }
}
