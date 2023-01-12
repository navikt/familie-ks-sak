package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.stereotype.Service

@Service
class KompetanseSkjemaEndretService(private val tilbakestillBehandlingService: TilbakestillBehandlingService) :
    EøsSkjemaEndringAbonnent<Kompetanse> {

    override fun skjemaerEndret(behandlingId: Long, endretTil: List<Kompetanse>) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}
