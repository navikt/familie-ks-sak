package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VilkårsvurderingRepository : JpaRepository<Vilkårsvurdering, Long> {
    @Query("SELECT v FROM Vilkårsvurdering v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun finnAktivForBehandling(behandlingId: Long): Vilkårsvurdering?
}
