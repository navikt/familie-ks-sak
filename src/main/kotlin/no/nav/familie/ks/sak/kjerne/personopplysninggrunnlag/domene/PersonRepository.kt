package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PersonRepository : JpaRepository<Person, Long> {

    @Query(
        "SELECT p FROM Person p" +
            " WHERE p.aktør = :aktør"
    )
    fun findByAktør(aktør: Aktør): List<Person>
}
