package no.nav.familie.ks.sak.kjerne.overgangsordning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OvergangsordningAndelRepository : JpaRepository<OvergangsordningAndel, Long> {
    @Query("SELECT ka FROM OvergangsordningAndel ka WHERE ka.behandlingId = :behandlingId")
    fun hentOvergangsordningAndelerForBehandling(behandlingId: Long): List<OvergangsordningAndel>

    @Query("SELECT ka FROM OvergangsordningAndel ka WHERE ka.id = :id")
    fun finnOvergangsordningAndel(id: Long): OvergangsordningAndel?
}
