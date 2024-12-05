package no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak

import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.BrevPeriodeService
import no.nav.familie.ks.sak.kjerne.brev.EtterbetalingService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørMedEndringSammensattKontrollsakDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpphørMedEndringSammensattKontrollsakDtoUtleder(
    private val vedtakFellesfelterSammensattKontrollsakDtoUtleder: VedtakFellesfelterSammensattKontrollsakDtoUtleder,
    private val etterbetalingService: EtterbetalingService,
    private val simuleringService: SimuleringService,
    private val brevPeriodeService: BrevPeriodeService,
) {
    private val logger = LoggerFactory.getLogger(OpphørMedEndringSammensattKontrollsakDtoUtleder::class.java)

    fun utled(
        vedtak: Vedtak,
        sammensattKontrollsak: SammensattKontrollsak,
    ): OpphørMedEndringSammensattKontrollsakDto {
        logger.debug("Utleder ${OpphørMedEndringSammensattKontrollsakDto::class.simpleName} for vedtak ${vedtak.id}")
        return OpphørMedEndringSammensattKontrollsakDto(
            vedtakFellesfelter = vedtakFellesfelterSammensattKontrollsakDtoUtleder.utled(vedtak = vedtak, sammensattKontrollsak = sammensattKontrollsak),
            etterbetaling = etterbetalingService.hentEtterbetaling(vedtak = vedtak),
            erFeilutbetalingPåBehandling = simuleringService.erFeilutbetalingPåBehandling(behandlingId = vedtak.behandling.id),
            refusjonEosAvklart = brevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(vedtak = vedtak),
            refusjonEosUavklart = brevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(vedtak = vedtak),
            erKlage = vedtak.behandling.erKlage(),
        )
    }
}
