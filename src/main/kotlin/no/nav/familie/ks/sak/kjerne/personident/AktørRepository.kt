package no.nav.familie.ks.sak.kjerne.personident

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface AktørRepository : JpaRepository<Aktør, String> {
    @Query("SELECT a FROM Aktør a WHERE a.aktørId = :aktørId")
    fun findByAktørId(aktørId: String): Aktør?

    /*
Patchemetodene bør kun brukes ved absolutt nødvendighet. Som ved merge av identer. Patchingen bruker sql og ikke JPA
for å fortsatt beholde jpa metodene som immutable og ikke oppdaterbar.

Denne er avhengig av at cascade on update queryene i V26__aktoerId_splitt_update_cascade.sql er lagt til i databasen
     */
    @Modifying
    @Query("update aktoer set aktoer_id = :nyAktørId where aktoer_id = :gammelAktørId", nativeQuery = true)
    fun patchAktørMedNyAktørId(
        gammelAktørId: String,
        nyAktørId: String,
    )
}
