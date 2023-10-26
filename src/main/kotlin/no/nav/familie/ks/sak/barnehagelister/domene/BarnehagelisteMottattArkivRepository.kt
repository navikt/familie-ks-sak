package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BarnehagelisteMottattArkivRepository : JpaRepository<BarnehagelisteMottattArkiv, UUID> {
    fun existsByMeldingId(meldingId: String): Boolean
}
