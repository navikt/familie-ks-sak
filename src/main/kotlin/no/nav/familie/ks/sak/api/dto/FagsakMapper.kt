package no.nav.familie.ks.sak.api.dto

import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.ks.sak.integrasjon.pdl.domene.PdlPersonInfo
import no.nav.familie.ks.sak.kjerne.behandling.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.fagsak.Fagsak

object FagsakMapper {

    fun lagFagsakDeltagerResponsDto(
        personInfo: PdlPersonInfo? = null,
        ident: String = "",
        rolle: FagsakDeltagerRolle,
        fagsak: Fagsak? = null,
        adressebeskyttelseGradering: ADRESSEBESKYTTELSEGRADERING? = null,
        harTilgang: Boolean = true
    ): FagsakDeltagerResponsDto = FagsakDeltagerResponsDto(
        navn = personInfo?.navn,
        ident = ident,
        rolle = rolle,
        kjønn = personInfo?.kjønn,
        fagsakId = fagsak?.id,
        fagsakStatus = fagsak?.status,
        adressebeskyttelseGradering = adressebeskyttelseGradering,
        harTilgang = harTilgang
    )

    fun lagMinimalFagsakResponsDto(
        fagsak: Fagsak,
        aktivtBehandling: Behandling? = null,
        behandlinger: List<BehandlingResponsDto> = emptyList()
    ): MinimalFagsakResponsDto =
        MinimalFagsakResponsDto(
            opprettetTidspunkt = fagsak.opprettetTidspunkt,
            id = fagsak.id,
            søkerFødselsnummer = fagsak.aktør.aktivFødselsnummer(),
            status = fagsak.status.name,
            underBehandling = if (aktivtBehandling == null) false else aktivtBehandling.status != BehandlingStatus.AVSLUTTET,
            løpendeKategori = aktivtBehandling?.kategori?.name,
            behandlinger = behandlinger
        )

    fun lagBehandlingResponsDto(behandling: Behandling) = BehandlingResponsDto(
        behandlingId = behandling.id,
        opprettetTidspunkt = behandling.opprettetTidspunkt,
        kategori = behandling.kategori.name,
        aktiv = behandling.aktiv,
        årsak = behandling.opprettetÅrsak.visningsnavn,
        type = behandling.type.visningsnavn,
        status = behandling.status.name,
        resultat = behandling.resultat.displayName,
        vedtaksdato = null // TODO - kommer når vedtak er implementert
    )
}
