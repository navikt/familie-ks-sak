package no.nav.familie.ks.sak.kjerne.maskinellrevurdering

import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MaskinellRevurderingLovendringService(
    private val stegService: StegService,
    private val opprettBehandlingService: OpprettBehandlingService,
) {
    @Transactional
    fun opprettMaskinellRevurderingOgKjørTilBeslutteVedtak(
        aktør: Aktør,
        kategori: BehandlingKategori,
    ) {
        val behandling =
            opprettBehandlingService.opprettBehandling(
                opprettBehandlingRequest =
                    OpprettBehandlingDto(
                        kategori = kategori,
                        søkersIdent = aktør.aktivFødselsnummer(),
                        behandlingType = BehandlingType.REVURDERING,
                        behandlingÅrsak = BehandlingÅrsak.LOVENDRING,
                    ),
            )
/*
        stegService.utførSteg(
            behandlingId = behandling.id,
            behandlingSteg = BehandlingSteg.REGISTRERE_PERSONGRUNNLAG,
        )
 */
    }
}
