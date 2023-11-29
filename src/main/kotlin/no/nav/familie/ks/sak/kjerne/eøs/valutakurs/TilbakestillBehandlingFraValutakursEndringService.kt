package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingFraValutakursEndringService(
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingService,
) : EøsSkjemaEndringAbonnent<Valutakurs> {
    override fun skjemaerEndret(
        behandlingId: Long,
        endretTil: List<Valutakurs>,
    ) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}
