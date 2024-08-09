package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingRepository : JpaRepository<Behandling, Long> {
    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun hentBehandling(behandlingId: Long): Behandling

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.arkivert = false AND b.id=:behandlingId AND b.aktiv = true ",
    )
    fun hentAktivBehandling(behandlingId: Long): Behandling

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false")
    fun finnBehandlinger(fagsakId: Long): List<Behandling>

    @Query(value = "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id in :fagsakIder AND f.arkivert = false")
    fun finnBehandlinger(fagsakIder: Set<Long>): List<Behandling>

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false " +
            "AND b.aktiv = true ",
    )
    fun findByFagsakAndAktiv(fagsakId: Long): Behandling?

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND f.arkivert = false AND " +
            "b.aktiv = true AND b.status <> 'AVSLUTTET'",
    )
    fun findByFagsakAndAktivAndOpen(fagsakId: Long): Behandling?

    @Query(
        """SELECT b FROM Behandling b
                           INNER JOIN TilkjentYtelse ty on b.id = ty.behandling.id
                        WHERE b.fagsak.id = :fagsakId AND ty.utbetalingsoppdrag IS NOT NULL""",
    )
    fun finnIverksatteBehandlinger(fagsakId: Long): List<Behandling>

    @Query(
        """
            select b from Behandling b
                            where b.fagsak.id = :fagsakId and b.status = 'IVERKSETTER_VEDTAK'
        """,
    )
    fun finnBehandlingerSomHolderPåÅIverksettes(fagsakId: Long): List<Behandling>

    @Query(
        """select b from Behandling b
                           inner join BehandlingStegTilstand bst on b.id = bst.behandling.id
                        where b.fagsak.id = :fagsakId AND bst.behandlingSteg = 'BESLUTTE_VEDTAK' 
                        AND bst.behandlingStegStatus IN (no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR, 
            no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER)""",
    )
    fun finnBehandlingerSendtTilGodkjenning(fagsakId: Long): List<Behandling>

    @Query("SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.id = :fagsakId AND b.status = 'AVSLUTTET' AND f.arkivert = false")
    fun finnByFagsakAndAvsluttet(fagsakId: Long): List<Behandling>

    @Query(
        value = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.aktivert_tid) AS aktivert_tid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
        SELECT b.id FROM sisteiverksattebehandlingfraløpendefagsak silp JOIN behandling b 
            ON b.fk_fagsak_id = silp.fagsakid 
        WHERE b.aktivert_tid = silp.aktivert_tid""",
        countQuery = """WITH sisteiverksattebehandlingfraløpendefagsak AS (
                            SELECT f.id AS fagsakid, MAX(b.aktivert_tid) AS aktivert_tid
                            FROM behandling b
                                   INNER JOIN fagsak f ON f.id = b.fk_fagsak_id
                                   INNER JOIN tilkjent_ytelse ty ON b.id = ty.fk_behandling_id
                            WHERE f.status = 'LØPENDE'
                              AND ty.utbetalingsoppdrag IS NOT NULL
                              AND f.arkivert = false
                            GROUP BY fagsakid)
                        
                        SELECT count(b.id) FROM sisteiverksattebehandlingfraløpendefagsak silp JOIN behandling b 
                            ON b.fk_fagsak_id = silp.fagsakid 
                        WHERE b.aktivert_tid = silp.aktivert_tid""",
        nativeQuery = true,
    )
    fun finnSisteIverksatteBehandlingFraLøpendeFagsaker(page: Pageable): Page<Long>

    @Query(
        """ SELECT new kotlin.Pair(b.id, p.fødselsnummer) from Behandling b 
                INNER JOIN Fagsak f ON f.id = b.fagsak.id 
                INNER JOIN Aktør a on f.aktør.aktørId = a.aktørId 
                INNER JOIN Personident p on p.aktør.aktørId = a.aktørId 
            where b.id in (:behandlingIder) AND p.aktiv=true AND f.status = 'LØPENDE' """,
    )
    fun finnAktivtFødselsnummerForBehandlinger(behandlingIder: List<Long>): List<Pair<Long, String>>

    @Query(
        value = """
            SELECT b.*
            FROM behandling b
                     INNER JOIN tilkjent_ytelse ty ON ty.fk_behandling_id = b.id
            WHERE ty.stonad_tom > '2024-07-31'
              AND b.soknad_mottatt_dato < '2024-02-02'
              AND EXISTS (SELECT 1
                          FROM vedtak v
                          WHERE v.fk_behandling_id = b.id
                            AND v.vedtaksdato > '2024-02-29')
              AND EXISTS (SELECT 1
                          FROM andel_tilkjent_ytelse aty
                          WHERE aty.fk_behandling_id = b.id)
              AND NOT EXISTS (SELECT 1
                              FROM behandling b2
                                       INNER JOIN vilkar_resultat vr ON vr.fk_behandling_id = b2.id
                              WHERE b2.fk_fagsak_id = b.fk_fagsak_id
                                AND vr.soker_har_meldt_fra_om_barnehageplass = true
                                AND EXTRACT(MONTH FROM vr.periode_tom) = 8
                                AND EXTRACT(YEAR FROM vr.periode_tom) = 2024)
              AND NOT EXISTS (SELECT 1
                              FROM behandling b2
                                       INNER JOIN vilkar_resultat vr ON vr.fk_behandling_id = b2.id
                              WHERE b2.fk_fagsak_id = b.fk_fagsak_id
                                AND EXTRACT(MONTH FROM vr.periode_fom) = 8
                                AND EXTRACT(YEAR FROM vr.periode_fom) = 2024
                                AND vr.resultat = 'IKKE_OPPFYLT')
              AND ty.utbetalingsoppdrag IS NOT NULL
              AND b.status = 'AVSLUTTET';
        """,
        nativeQuery = true,
    )
    fun finnBehandlingerSomSkalMottaInformasjonsbrevOmKontantstøtteLovendringJuli2024(): List<Behandling>

    @Query(
        value = """
            SELECT DISTINCT (f.id)
            FROM fagsak f
                     JOIN behandling b ON f.id = b.fk_fagsak_id
                     JOIN vilkar_resultat vr ON vr.fk_behandling_id = b.id
            WHERE b.aktiv = true
              AND f.status = 'LØPENDE'
              AND NOT EXISTS (
                SELECT 1 FROM behandling b2
                   INNER JOIN vilkar_resultat vr ON vr.fk_behandling_id = b2.id
                WHERE b2.fk_fagsak_id = b.fk_fagsak_id
                AND vr.soker_har_meldt_fra_om_barnehageplass = true
                AND vr.resultat = 'OPPFYLT')
              AND NOT EXISTS (
                SELECT 1 FROM behandling b2
                WHERE b2.fk_fagsak_id = b.fk_fagsak_id
                AND b2.opprettet_aarsak = 'LOVENDRING_2024');
        """,
        nativeQuery = true,
    )
    fun finnBehandlingerSomSkalRekjøresLovendring(): List<Long>

    @Query(
        value = """
            SELECT DISTINCT (f.id)
            FROM fagsak f
                     JOIN behandling b ON f.id = b.fk_fagsak_id
                     JOIN vilkar_resultat vr ON vr.fk_behandling_id = b.id
                     JOIN vedtak v ON v.fk_behandling_id = b.id
            WHERE b.aktiv = true
              AND vr.soker_har_meldt_fra_om_barnehageplass = true
              AND vr.periode_tom > '2024-07-30'
              AND v.vedtaksdato < '2024-07-03 14:00:00'
              AND NOT EXISTS (
                SELECT 1 FROM behandling b2
                WHERE b2.fk_fagsak_id = b.fk_fagsak_id
                AND b2.opprettet_aarsak = 'LOVENDRING_2024'
              );
        """,
        nativeQuery = true,
    )
    fun finnBehandlingerSomSkalRekjøresLovendringForFremtidigOpphør(): List<Long>
}
