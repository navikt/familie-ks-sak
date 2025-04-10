package no.nav.familie.ks.sak.api.dto

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.NyEksternBehandlingRelasjon
import java.time.LocalDate

data class OpprettBehandlingDto(
    val kategori: BehandlingKategori? = null,
    val søkersIdent: String,
    val behandlingType: BehandlingType,
    val behandlingÅrsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    val saksbehandlerIdent: String? = null,
    val søknadMottattDato: LocalDate? = null,
    val nyEksternBehandlingRelasjon: NyEksternBehandlingRelasjon? = null,
)
