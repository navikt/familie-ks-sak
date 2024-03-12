package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse

// Validerer at utbetalingsoppdrag ikke inneholder endriner dersom resultat er FORTSATT_INNVILGET og at det i alle andre tilfeller, unntatt for EØS med differanseberegning, alltid skal ligge perioder i utbetalingsoppdraget.
fun BeregnetUtbetalingsoppdragLongId.valider(
    behandlingsresultat: Behandlingsresultat,
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
) {
    if (this.utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET) {
        throw FunksjonellFeil(
            "Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX",
        )
    } else if (this.utbetalingsoppdrag.utbetalingsperiode.isEmpty() &&
        !kanHaNullutbetaling(behandlingskategori, andelerTilkjentYtelse)
    ) {
        throw FunksjonellFeil(
            "Utbetalingsoppdraget inneholder ingen utbetalingsperioder " +
                "og det er grunn til å tro at denne ikke bør simuleres eller iverksettes. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}

private fun kanHaNullutbetaling(
    behandlingskategori: BehandlingKategori,
    andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
) = behandlingskategori == BehandlingKategori.EØS &&
    andelerTilkjentYtelse.any { it.erAndelSomharNullutbetaling() }
