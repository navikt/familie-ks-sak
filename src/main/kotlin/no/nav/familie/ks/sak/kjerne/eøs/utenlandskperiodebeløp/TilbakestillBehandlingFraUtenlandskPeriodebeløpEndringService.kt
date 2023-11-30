package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(
    @Lazy private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingService,
) : EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: List<UtenlandskPeriodebeløp>,
    ) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId.id)
    }
}
