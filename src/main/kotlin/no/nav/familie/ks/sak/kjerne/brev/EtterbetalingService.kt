package no.nav.familie.ks.sak.kjerne.brev

import no.nav.familie.ks.sak.common.util.formaterBeløp
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class EtterbetalingService(
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val simuleringService: SimuleringService,
) {
    fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? {
        val korrigertEtterbetaling =
            korrigertEtterbetalingService
                .finnAktivtKorrigeringPåBehandling(behandlingId = vedtak.behandling.id)
                ?.beløp
                ?.toBigDecimal()

        val etterbetalingFraSimulering =
            simuleringService.hentEtterbetaling(
                behandlingId = vedtak.behandling.id,
            )

        val etterbetalingsBeløp = korrigertEtterbetaling ?: etterbetalingFraSimulering

        return etterbetalingsBeløp
            .takeIf { it > BigDecimal.ZERO }
            ?.let {
                val formatertBeløp = formaterBeløp(it.toInt())
                Etterbetaling(formatertBeløp)
            }
    }
}
