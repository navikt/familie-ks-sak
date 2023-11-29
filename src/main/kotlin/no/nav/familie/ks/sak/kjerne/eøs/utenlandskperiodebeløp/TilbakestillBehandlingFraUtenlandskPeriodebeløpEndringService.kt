package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.kjerne.behandling.TilbakestillBehandlingService
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.springframework.stereotype.Service

@Service
class TilbakestillBehandlingFraUtenlandskPeriodebeløpEndringService(
    private val tilbakestillBehandlingTilBehandlingsresultatService: TilbakestillBehandlingService,
) : EøsSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    override fun skjemaerEndret(
        behandlingId: Long,
        endretTil: List<UtenlandskPeriodebeløp>,
    ) {
        tilbakestillBehandlingTilBehandlingsresultatService
            .tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }
}
