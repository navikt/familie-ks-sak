package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BarnehagelisteMottattRepository : JpaRepository<BarnehagelisteMottatt, UUID> {
    fun existsByMeldingId(meldingId: String): Boolean

    @Query(nativeQuery = true, value = "SELECT id FROM BARNEHAGELISTE_MOTTATT")
    fun findAllIds(): List<String>
}
