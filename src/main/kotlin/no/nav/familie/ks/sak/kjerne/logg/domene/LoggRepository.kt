package no.nav.familie.ks.sak.kjerne.logg.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LoggRepository : JpaRepository<Logg, Long> {
    @Query(value = "SELECT l FROM Logg l WHERE l.behandlingId = :behandlingId ORDER BY l.opprettetTidspunkt desc, l.id desc")
    fun hentLoggForBehandling(behandlingId: Long): List<Logg>
}
