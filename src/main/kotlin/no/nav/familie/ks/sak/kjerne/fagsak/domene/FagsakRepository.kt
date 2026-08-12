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
        WITH siste_vedtatte AS (
            -- Siste vedtatte behandling per fagsak
            SELECT DISTINCT ON (b.fk_fagsak_id) b.id, b.fk_fagsak_id
            FROM behandling b
            INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
            WHERE f.status   = 'AVSLUTTET'
              AND f.arkivert  = FALSE
              AND b.status   = 'AVSLUTTET'
              AND b.resultat NOT LIKE 'HENLAGT%'
            ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC
        ),
        siste_utbetaling AS (
            -- En behandling kan ha flere tilkjente ytelser, så vi bruker den som strekker seg lengst
            SELECT ty.fk_behandling_id, MAX(ty.stonad_tom) AS stonad_tom
            FROM tilkjent_ytelse ty
            GROUP BY ty.fk_behandling_id
        )
        -- Fagsaker der siste utbetaling og siste vedtak var for mer enn 1 år siden.
        -- stonad_tom lagres som første dag i måneden, mens fristen løper fra siste dag i måneden.
        SELECT DISTINCT sv.fk_fagsak_id
        FROM   siste_vedtatte sv
        LEFT JOIN siste_utbetaling su ON su.fk_behandling_id = sv.id
        LEFT JOIN vedtak v ON v.fk_behandling_id = sv.id AND v.aktiv = TRUE
        WHERE  GREATEST((su.stonad_tom + INTERVAL '1 month' - INTERVAL '1 day')::date, v.vedtaksdato::date) + INTERVAL '1 year' <= CURRENT_DATE
        """,
        nativeQuery = true,
    )
    fun finnAvsluttedeFagsakerSomSkalLåses(): List<Long>

    @Query(
        value = """
            SELECT new kotlin.Pair(f.id, f.status)
                FROM Fagsak f
                    JOIN Behandling b ON f.id = b.fagsak.id
                    JOIN PersonopplysningGrunnlag gp ON b.id = gp.behandlingId
                    JOIN Person pp ON gp.id = pp.personopplysningGrunnlag.id
                    JOIN Personident pi ON pi.aktør.aktørId = pp.aktør.aktørId
                WHERE b.aktiv = true
                    AND f.arkivert = false
                    AND pi.fødselsnummer = :ident 
        """,
    )
    fun finnFagsakIdOgStatusMedAktivBehandlingForIdent(ident: String): List<Pair<Long, FagsakStatus>>
}
