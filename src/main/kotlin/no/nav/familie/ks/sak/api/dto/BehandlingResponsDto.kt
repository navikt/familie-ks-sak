package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import java.time.LocalDateTime

data class BehandlingResponsDto(
    val behandlingId: Long,
    val steg: BehandlingSteg,
    val stegTilstand: List<BehandlingStegTilstandResponsDto>,
    val status: BehandlingStatus,
    val resultat: Behandlingsresultat,
    val type: BehandlingType,
    val kategori: BehandlingKategori,
    val årsak: BehandlingÅrsak,
    val opprettetTidspunkt: LocalDateTime,
    val endretAv: String,
    val arbeidsfordelingPåBehandling: ArbeidsfordelingResponsDto
)

data class BehandlingStegTilstandResponsDto(val behandlingSteg: BehandlingSteg, val behandlingStegStatus: BehandlingStegStatus)

data class ArbeidsfordelingResponsDto(
    val behandlendeEnhetId: String,
    val behandlendeEnhetNavn: String,
    val manueltOverstyrt: Boolean = false
)
