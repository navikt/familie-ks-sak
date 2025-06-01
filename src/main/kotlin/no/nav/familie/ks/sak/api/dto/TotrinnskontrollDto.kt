package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import java.time.LocalDateTime

data class TotrinnskontrollDto(
    val saksbehandler: String,
    val saksbehandlerId: String,
    val beslutter: String? = null,
    val godkjent: Boolean = false,
    val opprettetTidspunkt: LocalDateTime,
)

fun Totrinnskontroll.tilTotrinnskontrollDto() =
    TotrinnskontrollDto(
        saksbehandler = this.saksbehandler,
        beslutter = this.beslutter,
        godkjent = this.godkjent,
        opprettetTidspunkt = this.opprettetTidspunkt,
        saksbehandlerId = this.saksbehandlerId,
    )
