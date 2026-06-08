package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import java.time.LocalDateTime

data class VedtakDto(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val id: Long,
)

fun Vedtak.tilVedtakDto() =
    VedtakDto(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
    )
