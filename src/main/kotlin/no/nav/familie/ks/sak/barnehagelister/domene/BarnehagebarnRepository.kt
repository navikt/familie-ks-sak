package no.nav.familie.ks.sak.barnehagelister.domene

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BarnehagebarnRepository : JpaRepository<Barnehagebarn, UUID> { // , JpaSpecificationExecutor<Barnehagebarn>
    fun findByIdent(ident: String): Barnehagebarn
}
