package no.nav.familie.ks.sak.kjerne.eøs

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursService
import org.springframework.stereotype.Service

@Service
class EøsSkjemaerForNyBehandlingService(
    private val kompetanseService: KompetanseService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val valutakursService: ValutakursService,
) {
    fun kopierEøsSkjemaer(
        behandlingId: BehandlingId,
        forrigeBehandlingSomErVedtattId: BehandlingId,
    ) {
        kompetanseService.kopierOgErstattKompetanser(
            fraBehandlingId = forrigeBehandlingSomErVedtattId,
            tilBehandlingId = behandlingId,
        )
        utenlandskPeriodebeløpService.kopierOgErstattUtenlandskPeriodebeløp(
            fraBehandlingId = forrigeBehandlingSomErVedtattId,
            tilBehandlingId = behandlingId,
        )
        valutakursService.kopierOgErstattValutakurser(
            fraBehandlingId = forrigeBehandlingSomErVedtattId,
            tilBehandlingId = behandlingId,
        )
    }
}
