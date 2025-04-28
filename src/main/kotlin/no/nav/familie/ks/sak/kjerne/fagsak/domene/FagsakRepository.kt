package no.nav.familie.ks.sak.kjerne.fagsak.domene

import jakarta.persistence.LockModeType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FagsakRepository : JpaRepository<Fagsak, Long> {
    @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
    fun save(fagsak: Fagsak): Fagsak

    @Query(value = "SELECT f FROM Fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnFagsak(fagsakId: Long): Fagsak?

    @Query(value = "SELECT f FROM Fagsak f WHERE f.aktør = :aktør and f.arkivert = false")
    fun finnFagsakForAktør(aktør: Aktør): Fagsak?

    @Query(value = "SELECT f from Fagsak f WHERE f.status = 'LØPENDE'  AND f.arkivert = false")
    fun finnLøpendeFagsaker(): List<Fagsak>

    @Query(value = "SELECT f from Fagsak f where f.arkivert = false")
    fun hentFagsakerSomIkkeErArkivert(): List<Fagsak>

    @Modifying
    @Query(
        value = """
                SELECT f1.* FROM fagsak f1
                WHERE f1.id IN (
                    WITH sisteiverksatte AS (
                        SELECT b.fk_fagsak_id AS fagsakid, MAX(b.aktivert_tid) AS aktivert_tid
                        FROM behandling b
                                 INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                                 INNER JOIN fagsak f2 ON f2.id = b.fk_fagsak_id
                        WHERE ty.utbetalingsoppdrag IS NOT NULL
                          AND f2.status = 'LØPENDE'
                          AND f2.arkivert = FALSE
                        GROUP BY b.fk_fagsak_id)
                
                    SELECT silp.fagsakid
                    FROM sisteiverksatte silp
                             INNER JOIN behandling b ON b.fk_fagsak_id = silp.fagsakid
                             INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                    WHERE b.aktivert_tid = silp.aktivert_tid AND ty.stonad_tom < DATE_TRUNC('month', NOW()));
                """,
        nativeQuery = true,
    )
    fun finnFagsakerSomSkalAvsluttes(): List<Fagsak>

    @Query(
        value = """
            SELECT new kotlin.Pair(f.id, f.status)
                FROM Fagsak f
                    JOIN Behandling b ON f.id = b.fagsak.id
                    JOIN PersonopplysningGrunnlag gp ON b.id = gp.behandlingId
                    JOIN Person pp ON gp.id = pp.personopplysningGrunnlag.id
                    JOIN Personident p ON p.aktør.aktørId = pp.aktør.aktørId
                WHERE b.aktiv = TRUE
                    AND p.fødselsnummer = :ident 
        """,
    )
    fun finnFagsakIdOgStatusMedAktivBehandlingForIdent(ident: String): List<Pair<Long, FagsakStatus>>
}
