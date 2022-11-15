package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import java.time.LocalDateTime

data class VedtakDto(
    val aktiv: Boolean,
    val vedtaksdato: LocalDateTime?,
    val vedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelserDto>,
    val id: Long
)

fun Vedtak.tilVedtakDto(
    vedtaksperioderMedBegrunnelser: List<UtvidetVedtaksperiodeMedBegrunnelserDto>,
    skalMinimeres: Boolean
) =
    VedtakDto(
        aktiv = this.aktiv,
        vedtaksdato = this.vedtaksdato,
        id = this.id,
        vedtaksperioderMedBegrunnelser = if (skalMinimeres) {
            vedtaksperioderMedBegrunnelser
                .filter { it.begrunnelser.isNotEmpty() }
                .map { it.copy(gyldigeBegrunnelser = emptyList()) }
        } else {
            vedtaksperioderMedBegrunnelser
        }
    )
