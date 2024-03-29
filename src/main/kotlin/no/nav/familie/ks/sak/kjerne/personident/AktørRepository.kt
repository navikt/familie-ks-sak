package no.nav.familie.ks.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AktørRepository : JpaRepository<Aktør, String> {
    @Query("SELECT a FROM Aktør a WHERE a.aktørId = :aktørId")
    fun findByAktørId(aktørId: String): Aktør?
}
