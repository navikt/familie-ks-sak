package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak

data class OpprettAutovedtakBehandlingPåFagsakDto(
    val fagsakId: Long,
    val behandlingType: BehandlingType,
    val behandlingsÅrsak: BehandlingÅrsak,
)
