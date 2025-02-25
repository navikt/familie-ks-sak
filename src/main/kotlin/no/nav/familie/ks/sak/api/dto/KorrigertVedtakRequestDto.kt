package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import java.time.LocalDate

data class KorrigertVedtakRequestDto(
    val vedtaksdato: LocalDate,
    val begrunnelse: String?,
)

class KorrigertVedtakResponsDto(
    val id: Long,
    val vedtaksdato: LocalDate?,
    val begrunnelse: String?,
    val aktiv: Boolean,
)

fun KorrigertVedtakRequestDto.tilKorrigertVedtak(behandling: Behandling) = KorrigertVedtak(vedtaksdato = vedtaksdato, begrunnelse = begrunnelse, behandling = behandling, aktiv = true)

fun KorrigertVedtak.tilKorrigertVedtakResponsDto(): KorrigertVedtakResponsDto =
    KorrigertVedtakResponsDto(
        id = id,
        vedtaksdato = vedtaksdato,
        begrunnelse = begrunnelse,
        aktiv = aktiv,
    )
