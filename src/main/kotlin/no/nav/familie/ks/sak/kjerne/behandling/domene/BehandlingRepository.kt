package no.nav.familie.ks.sak.kjerne.behandling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BehandlingRepository : JpaRepository<Behandling, Long> {

    @Query(value = "SELECT b FROM Behandling b WHERE b.id = :behandlingId")
    fun finnBehandling(behandlingId: Long): Behandling

    @Query(
        "SELECT b FROM Behandling b JOIN b.fagsak f WHERE f.arkivert = false AND b.id=:behandlingId AND b.aktiv = true "
    )
    fun finnAktivBehandling(behandlingId: Long): Behandling

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
}
