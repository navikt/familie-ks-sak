package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.IBegrunnelse
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak

data class SanityBegrunnelseMedEndringsårsakResponseDto(
    val id: IBegrunnelse,
    val navn: String,
    val endringsårsaker: List<Årsak>,
)
