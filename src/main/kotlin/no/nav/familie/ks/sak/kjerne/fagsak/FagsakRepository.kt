package no.nav.familie.ks.sak.kjerne.fagsak

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import javax.persistence.LockModeType

@Repository
interface FagsakRepository : JpaRepository<Fagsak, Long> {

    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Lock(LockModeType.NONE)
    override fun findById(id: Long): Optional<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnFagsak(fagsakId: Long): Fagsak?

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakerForAktør(aktør: Aktør): List<Fagsak>

    @Lock(LockModeType.NONE)
    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>
}
