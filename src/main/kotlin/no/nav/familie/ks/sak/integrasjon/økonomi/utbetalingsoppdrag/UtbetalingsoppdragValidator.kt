package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.exception.KONTAKT_TEAMET_SUFFIX
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat

// Validerer at utbetalingsoppdrag ikke inneholder endriner dersom resultat er FORTSATT_INNVILGET.
fun BeregnetUtbetalingsoppdragLongId.valider(
    behandlingsresultat: Behandlingsresultat,
) {
    if (this.utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && behandlingsresultat == Behandlingsresultat.FORTSATT_INNVILGET) {
        throw FunksjonellFeil(
            "Behandling har resultat fortsatt innvilget, men det finnes utbetalingsperioder som ifølge systemet skal endres. $KONTAKT_TEAMET_SUFFIX",
        )
    }
}
