package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse

data class VedtakMedTilkjentYtelse(
    val tilkjentYtelse: TilkjentYtelse,
    val vedtak: Vedtak,
    val saksbehandlerId: String,
    val sisteOffsetPerIdent: Map<String, Int> = emptyMap(),
    val sisteOffsetPåFagsak: Int? = null,
    val erSimulering: Boolean
)
