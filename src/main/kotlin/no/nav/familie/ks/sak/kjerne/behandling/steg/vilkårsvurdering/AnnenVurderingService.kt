package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.AnnenVurderingDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.AnnenVurderingRepository
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AnnenVurderingService(private val annenVurderingRepository: AnnenVurderingRepository) {

    fun hent(annenVurderingId: Long): AnnenVurdering = annenVurderingRepository.findById(annenVurderingId)
        .orElseThrow { error("Annen vurdering med id $annenVurderingId finnes ikke i db") }

    @Transactional
    fun endreAnnenVurdering(
        annenVurderingId: Long,
        restAnnenVurdering: AnnenVurderingDto
    ) {
        hent(annenVurderingId = annenVurderingId).let {
            annenVurderingRepository.save(
                it.also {
                    it.resultat = restAnnenVurdering.resultat
                    it.begrunnelse = restAnnenVurdering.begrunnelse
                    it.type = restAnnenVurdering.type
                }
            )
        }
    }
}
