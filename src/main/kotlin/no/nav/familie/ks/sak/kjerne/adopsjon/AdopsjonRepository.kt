package no.nav.familie.ks.sak.kjerne.adopsjon

import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AdopsjonRepository : JpaRepository<Adopsjon, Long> {
    @Query(value = "SELECT a FROM Adopsjon a WHERE a.behandlingId = :behandlingId")
    fun hentAlleAdopsjonerForBehandling(behandlingId: Long): List<Adopsjon>

    @Query(value = "SELECT a FROM Adopsjon a WHERE a.behandlingId = :behandlingId AND a.aktør = :aktør")
    fun finnAdopsjonForAktørIBehandling(
        behandlingId: Long,
        aktør: Aktør,
    ): Adopsjon?
}
