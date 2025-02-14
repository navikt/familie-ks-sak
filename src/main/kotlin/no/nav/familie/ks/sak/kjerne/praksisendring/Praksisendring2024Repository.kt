package no.nav.familie.ks.sak.kjerne.praksisendring

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface Praksisendring2024Repository : JpaRepository<Praksisendring2024, Long> {
    @Query("SELECT pe FROM Praksisendring2024 pe WHERE pe.fagsakId = :fagsakId")
    fun finnPraksisendring2024ForFagsak(fagsakId: Long): List<Praksisendring2024>
}
