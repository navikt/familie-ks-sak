package no.nav.familie.ks.sak.kjerne.brev.mottaker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrevmottakerRepository : JpaRepository<BrevmottakerDb, Long> {
    @Query(value = "SELECT b FROM Brevmottaker b WHERE b.behandlingId = :behandlingId")
    fun finnBrevMottakereForBehandling(behandlingId: Long): List<BrevmottakerDb>
}
