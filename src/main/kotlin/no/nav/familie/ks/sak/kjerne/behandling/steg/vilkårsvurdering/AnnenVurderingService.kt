package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnenVurderingService(private val annenVurderingRepository: AnnenVurderingRepository) {

    fun hentAnnenVurderingThrows(annenVurderingId: Long): AnnenVurdering =
        annenVurderingRepository.findById(annenVurderingId)
            .orElseThrow { FunksjonellFeil("Annen vurdering med id $annenVurderingId finnes ikke i db") }

    @Transactional
    fun endreAnnenVurdering(
        annenVurderingDto: AnnenVurderingDto
    ) {
        hentAnnenVurderingThrows(annenVurderingId = annenVurderingDto.id).let {
            annenVurderingRepository.save(
                it.also {
                    it.resultat = annenVurderingDto.resultat
                    it.begrunnelse = annenVurderingDto.begrunnelse
                    it.type = annenVurderingDto.type
                }
            )
        }
    }
}
