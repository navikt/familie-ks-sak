package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetaling
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingÅrsak
import java.time.LocalDateTime

data class KorrigertEtterbetalingRequestDto(
    val årsak: KorrigertEtterbetalingÅrsak,
    val begrunnelse: String?,
    val beløp: Int,
)

data class KorrigertEtterbetalingResponsDto(
    val id: Long,
    val årsak: KorrigertEtterbetalingÅrsak,
    val begrunnelse: String?,
    val opprettetTidspunkt: LocalDateTime,
    val beløp: Int,
    val aktiv: Boolean,
)

fun KorrigertEtterbetaling.tilKorrigertEtterbetalingResponsDto(): KorrigertEtterbetalingResponsDto =
    KorrigertEtterbetalingResponsDto(
        id = id,
        årsak = årsak,
        begrunnelse = begrunnelse,
        opprettetTidspunkt = opprettetTidspunkt,
        beløp = beløp,
        aktiv = aktiv,
    )
