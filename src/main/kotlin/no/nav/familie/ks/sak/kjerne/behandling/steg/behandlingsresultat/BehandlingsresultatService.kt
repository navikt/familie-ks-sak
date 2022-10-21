package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import org.springframework.stereotype.Service

@Service
class BehandlingsresultatService(
    private val behandlingService: BehandlingService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService
) {

    fun utledBehandlingsresultat(behandling: Behandling): Behandlingsresultat {
        val forrigeBehandling = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)

        val andelerMedEndringer = andelerTilkjentYtelseOgEndreteUtbetalingerService
            .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)

        val forrigeAndelerMedEndringer = forrigeBehandling?.let {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(it.id)
        } ?: emptyList()

        return Behandlingsresultat.INNVILGET
    }
}
