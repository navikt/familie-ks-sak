package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak.SammensattKontrollsak

data class SammensattKontrollsakDto(
    val id: Long,
    val behandlingId: Long,
    val fritekst: String,
)

data class SlettSammensattKontrollsakDto(
    val id: Long,
)

data class OppdaterSammensattKontrollsakDto(
    val id: Long,
    val fritekst: String,
)

data class OpprettSammensattKontrollsakDto(
    val behandlingId: Long,
    val fritekst: String,
)

fun OpprettSammensattKontrollsakDto.tilSammensattKontrollsak(): SammensattKontrollsak =
    SammensattKontrollsak(
        behandlingId = this.behandlingId,
        fritekst = this.fritekst,
    )
