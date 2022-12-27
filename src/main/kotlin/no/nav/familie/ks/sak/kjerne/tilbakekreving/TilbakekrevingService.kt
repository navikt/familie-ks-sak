package no.nav.familie.ks.sak.kjerne.tilbakekreving

import no.nav.familie.ks.sak.api.dto.TilbakekrevingRequestDto
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
    private val tilbakekrevingKlient: TilbakekrevingKlient,
    private val tilbakekrevingRepository: TilbakekrevingRepository
) {

    fun harÅpenTilbakekrevingsbehandling(fagsakId: Long): Boolean =
        tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)

    @Transactional
    fun lagreTilbakekreving(tilbakekrevingRequestDto: TilbakekrevingRequestDto, behandling: Behandling): Tilbakekreving? {
        val eksisterendeTilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
        val tilbakekreving = Tilbakekreving(
            begrunnelse = tilbakekrevingRequestDto.begrunnelse,
            behandling = behandling,
            valg = tilbakekrevingRequestDto.valg,
            varsel = tilbakekrevingRequestDto.varsel,
            tilbakekrevingsbehandlingId = eksisterendeTilbakekreving?.tilbakekrevingsbehandlingId
        )

        eksisterendeTilbakekreving?.let { tilbakekrevingRepository.deleteById(it.id) }
        return tilbakekrevingRepository.save(tilbakekreving)
    }
}
