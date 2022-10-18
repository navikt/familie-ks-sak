package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnnenVurderingRepository : JpaRepository<AnnenVurdering, Long> {

    @Query(value = "SELECT b FROM AnnenVurdering b WHERE b.personResultat = :personResultat AND b.type = :type")
    fun findBy(personResultat: PersonResultat, type: AnnenVurderingType): AnnenVurdering?
}
