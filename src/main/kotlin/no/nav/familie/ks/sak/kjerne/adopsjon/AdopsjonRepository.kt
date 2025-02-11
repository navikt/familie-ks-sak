package no.nav.familie.ks.sak.kjerne.adopsjon

import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.AktørId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AdopsjonRepository : JpaRepository<Adopsjon, Long> {
    @Query(value = "SELECT a FROM Adopsjon a WHERE a.behandlingId = :behandlingId")
    fun finnAlleAdopsjonerForBehandling(behandlingId: Long): List<Adopsjon>

    @Query(value = "SELECT a FROM Adopsjon a WHERE a.behandlingId = :behandlingId AND a.aktør.aktørId = :aktorId")
    fun finnAdopsjonForAktørIBehandling(behandlingId: Long, aktørId: AktørId): Adopsjon?
}