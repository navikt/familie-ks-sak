package no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KompensasjonAndelRepository : JpaRepository<KompensasjonAndel, Long> {
    @Query("SELECT ka FROM KompensasjonAndel ka WHERE ka.behandlingId = :behandlingId")
    fun hentKompensasjonAndelerForBehandling(behandlingId: Long): List<KompensasjonAndel>

    @Query("SELECT ka FROM KompensasjonAndel ka WHERE ka.id = :id")
    fun finnKompensasjonAndel(id: Long): KompensasjonAndel?
}
