package no.nav.familie.ks.sak.korrigertvedtak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface KorrigertVedtakRepository : JpaRepository<KorrigertVedtak, Long> {

    @Query("SELECT kv FROM KorrigertVedtak kv JOIN kv.behandling b WHERE b.id = :behandlingId AND kv.aktiv = true")
    fun finnAktivtKorrigertVedtakPåBehandling(behandlingId: Long): KorrigertVedtak?
}
