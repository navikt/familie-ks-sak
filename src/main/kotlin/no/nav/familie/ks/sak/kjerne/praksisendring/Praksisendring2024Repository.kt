package no.nav.familie.ks.sak.kjerne.praksisendring

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface Praksisendring2024Repository : JpaRepository<Praksisendring2024, Long> {
    @Query("SELECT pe FROM Praksisendring2024 pe WHERE pe.fagsakId = :fagsakId")
    fun finnPraksisendring2024ForFagsak(fagsakId: Long): List<Praksisendring2024>

    fun existsPraksisendring2024ByFagsakIdAndAktør(
        fagsakId: Long,
        aktør: Aktør,
    ): Boolean
}
