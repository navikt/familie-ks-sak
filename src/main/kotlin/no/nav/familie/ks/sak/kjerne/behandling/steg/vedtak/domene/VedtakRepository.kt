package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedtakRepository : JpaRepository<Vedtak, Long> {
    @Query(value = "SELECT v FROM Vedtak v WHERE v.id = :vedtakId")
    fun hentVedtak(vedtakId: Long): Vedtak

    @Query(value = "SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long): List<Vedtak>

    @Query("SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktivOptional(behandlingId: Long): Vedtak?

    @Query("SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vedtak
}
