package no.nav.familie.ks.sak.kjerne.beregning.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EndretUtbetalingAndelRepository : JpaRepository<EndretUtbetalingAndel, Long> {

    @Query("SELECT eua FROM EndretUtbetalingAndel eua WHERE eua.behandlingId = :behandlingId")
    fun findByBehandlingId(behandlingId: Long): List<EndretUtbetalingAndel>
}
