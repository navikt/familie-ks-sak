package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

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
                        where b.fagsak.id = :fagsakId AND bst.behandlingSteg = 'BESLUTTE_VEDTAK' AND bst.behandlingStegStatus IN (no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.KLAR, no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus.VENTER)"""
    )
    fun finnBehandlingerSentTilGodkjenning(fagsakId: Long): List<Behandling>
}
