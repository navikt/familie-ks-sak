package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsresultatstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsstatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import java.time.LocalDateTime
import java.util.UUID

data class ForhåndsvisTilbakekrevingVarselbrevDto(
    val fritekst: String,
)

data class FagsakIdDto(
    val fagsakId: Long,
)

data class TilbakekrevingsbehandlingResponsDto(
    val behandlingId: UUID,
    val opprettetTidspunkt: LocalDateTime,
    val aktiv: Boolean,
    val årsak: Behandlingsårsakstype?,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val resultat: Behandlingsresultatstype?,
    val vedtaksdato: LocalDateTime?,
)

fun Behandling.tilTilbakekrevingsbehandlingResponsDto() =
    TilbakekrevingsbehandlingResponsDto(
        behandlingId = this.behandlingId,
        opprettetTidspunkt = this.opprettetTidspunkt,
        aktiv = this.aktiv,
        årsak = this.årsak,
        type = this.type,
        status = this.status,
        resultat = this.resultat,
        vedtaksdato = this.vedtaksdato,
    )
