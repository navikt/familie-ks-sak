package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

// Brukte @Lazy annotering for å unngå sirkular avhengighet
@Service
class KompetanseSkjemaEndretService(
    @Lazy private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) :
    EøsSkjemaEndringAbonnent<Kompetanse> {
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: List<Kompetanse>,
    ) {
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}
