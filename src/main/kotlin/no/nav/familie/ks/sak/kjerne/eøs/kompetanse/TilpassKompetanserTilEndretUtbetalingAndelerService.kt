package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelerOppdatertAbonnent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassKompetanserTilEndretUtbetalingAndelerService(
    private val tilpassKompetanserService: TilpassKompetanserService,
) : EndretUtbetalingAndelerOppdatertAbonnent {
    @Transactional
    override fun tilpassKompetanserTilEndretUtbetalingAndeler(behandlingId: BehandlingId) = tilpassKompetanserService.tilpassKompetanser(behandlingId)
}
