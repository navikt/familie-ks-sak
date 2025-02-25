package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelerOppdatertAbonnent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassKompetanserTilOvergangsordningAndelerService(
    private val tilpassKompetanserService: TilpassKompetanserService,
) : OvergangsordningAndelerOppdatertAbonnent {
    @Transactional
    override fun tilpassKompetanserTilOvergangsordningAndeler(behandlingId: BehandlingId) = tilpassKompetanserService.tilpassKompetanser(behandlingId)
}
