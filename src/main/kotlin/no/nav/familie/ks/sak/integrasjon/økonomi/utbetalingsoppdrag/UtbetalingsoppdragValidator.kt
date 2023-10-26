package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import java.math.BigDecimal
import java.time.LocalDate

// valideringsmetode med logikk for å slippe nullutbetaling saker
fun Utbetalingsoppdrag.valider(
    behandlingsresultat: Behandlingsresultat,
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) {
    if (this.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET) {
        throw FunksjonellFeil(
            "Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX",
        )
    } else if (this.utbetalingsperiode.isEmpty() &&
        !kanHaNullutbetaling(behandlingskategori, andelerTilkjentYtelse)
    ) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}

fun Utbetalingsoppdrag.validerOpphørsoppdrag() {
    if (this.harLøpendeUtbetaling()) {
        error("Generert utbetalingsoppdrag for opphør inneholder oppdragsperioder med løpende utbetaling.")
    }

    if (this.utbetalingsperiode.isNotEmpty() && this.utbetalingsperiode.none { it.opphør != null }) {
        error("Generert utbetalingsoppdrag for opphør mangler opphørsperioder.")
    }
}

fun Utbetalingsoppdrag.harLøpendeUtbetaling() =
    this.utbetalingsperiode.any {
        it.opphør == null &&
            it.sats > BigDecimal.ZERO &&
            it.vedtakdatoTom > LocalDate.now().sisteDagIMåned()
    }

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
) = behandlingskategori == BehandlingKategori.EØS &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetaling() }
