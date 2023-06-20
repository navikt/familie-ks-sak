package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BarnehagelisteMottattRepository : JpaRepository<BarnehagelisteMottatt, UUID> {
    fun existsByMeldingId(meldingId: String): Boolean
}
