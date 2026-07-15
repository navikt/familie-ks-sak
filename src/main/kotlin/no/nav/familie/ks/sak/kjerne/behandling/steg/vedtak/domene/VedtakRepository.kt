package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene

import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface VedtakRepository : JpaRepository<Vedtak, Long> {
    @Query(value = "SELECT v FROM Vedtak v WHERE v.id = :vedtakId")
    fun hentVedtak(vedtakId: Long): Vedtak

    @Query(value = "SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId")
    fun finnVedtakForBehandling(behandlingId: Long): List<Vedtak>

    @Query("SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktivOptional(behandlingId: Long): Vedtak?

    @Query("SELECT v FROM Vedtak v JOIN v.behandling b WHERE b.id = :behandlingId AND v.aktiv = true")
    fun findByBehandlingAndAktiv(behandlingId: Long): Vedtak

    @Query(
        """
        SELECT v.id FROM Vedtak v JOIN v.behandling b
        WHERE v.stønadBrevPdf IS NOT NULL
        AND b.status = :status
        AND v.vedtaksdato < :vedtaksdatoFør
        """,
    )
    fun finnVedtakIderMedStønadBrevPdf(
        status: BehandlingStatus,
        vedtaksdatoFør: LocalDateTime,
        pageable: Pageable,
    ): List<Long>

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE Vedtak v
        SET v.stønadBrevPdf = null
        WHERE v.id IN :vedtakIder
        """,
    )
    fun slettStønadBrevPdfForVedtak(vedtakIder: List<Long>): Int
}
