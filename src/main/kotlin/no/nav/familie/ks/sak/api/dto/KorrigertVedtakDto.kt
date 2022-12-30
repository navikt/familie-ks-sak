package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtak
import java.time.LocalDate

data class KorrigertVedtakDto(
    val vedtaksdato: LocalDate,
    val begrunnelse: String?
)

fun KorrigertVedtakDto.tilKorrigertVedtak(behandling: Behandling) =
    KorrigertVedtak(vedtaksdato = vedtaksdato, begrunnelse = begrunnelse, behandling = behandling, aktiv = true)
