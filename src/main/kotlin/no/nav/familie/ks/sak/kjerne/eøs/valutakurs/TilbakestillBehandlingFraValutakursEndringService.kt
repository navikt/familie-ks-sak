package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingFraValutakursEndringService(
    @Lazy private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingService,
) : EøsSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: List<Valutakurs>,
    ) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}
