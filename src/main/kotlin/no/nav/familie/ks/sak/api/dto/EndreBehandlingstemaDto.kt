package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori

data class EndreBehandlingstemaDto(
    val behandlingKategori: BehandlingKategori,
)
