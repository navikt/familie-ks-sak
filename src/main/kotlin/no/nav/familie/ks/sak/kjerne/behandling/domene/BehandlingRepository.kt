package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigInteger

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun hentBehandling(behandlingId: Long): Behandling

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.arkivert = false AND b.id=:behandlingId AND b.aktiv = true "
    )
    fun hentAktivBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query(
        """SELECT b FROM Behandling b
                           INNER JOIN ArbeidsfordelingPåBehandling apb on b.id = apb.behandlingId
                        WHERE b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL"""
    )
    fun finnBehandlingerUnderEnhet(enhetId: Long): List<Behandling>

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false " +
                "AND b.aktiv = true "
    )
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false AND " +
                "b.aktiv = true AND b.status <> 'AVSLUTTET'"
    )
    fun findByFagsakAndAktivAndOpen(fagsakId: Long): Behandling?

    @Query(
        """SELECT b FROM Behandling b
                           INNER JOIN TilkjentYtelse ty on b.id = ty.behandling.id
                        WHERE b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL"""
    )
    fun finnIverksatteBehandlinger(fagsakId: Long): List<Behandling>

    @Query(
        """
            select b from Behandling b
                            where b.fagsak.id = :fagsakId and b.status = 'IVERKSETTER_VEDTAK'
        """
    )
    fun finnBehandlingerSomHolderPåÅIverksettes(fagsakId: Long): List<Behandling>

    @Query(
        """select b from Behandling b
                           inner join BehandlingStegTilstand bst on b.id = bst.behandling.id
                        where b.fagsak.id = :fagsakId AND bst.behandlingSteg = 'BESLUTTE_VEDTAK' 
                        AND bst.behandlingStegStatus IN (no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR, 
            no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER)"""
    )
    fun finnBehandlingerSendtTilGodkjenning(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.opprettet_tid) AS opprettet_tid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
        SELECT b.id FROM sisteiverksattebehandlingfraløpendefagsak silp JOIN behandling b 
            ON b.fk_fagsak_id = silp.fagsakid 
        WHERE b.opprettet_tid = silp.opprettet_tid""",

        countQuery = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.opprettet_tid) AS opprettet_tid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
                        SELECT count(b.id) FROM sisteiverksattebehandlingfraløpendefagsak silp JOIN behandling b 
                            ON b.fk_fagsak_id = silp.fagsakid 
                        WHERE b.opprettet_tid = silp.opprettet_tid""",

        nativeQuery = true
    )
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(page: Pageable): Page<BigInteger>

    @Query(
        """ SELECT new kotlin.Pair(b.id, p.fødselsnummer) from Behandling b 
                INNER JOIN Fagsak f ON f.id = b.fagsak.id 
                INNER JOIN Aktør a on f.aktør.aktørId = a.aktørId 
                INNER JOIN Personident p on p.aktør.aktørId = a.aktørId 
            where b.id in (:behandlingIder) AND p.aktiv=true AND f.status = 'LØPENDE' """
    )
    fun finnAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): List<Pair<Long, String>>
}
