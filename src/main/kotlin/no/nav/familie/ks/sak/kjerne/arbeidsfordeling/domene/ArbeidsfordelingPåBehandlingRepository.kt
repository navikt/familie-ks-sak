package no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArbeidsfordelingPåBehandlingRepository : JpaRepository<ArbeidsfordelingPåBehandling, Long> {
    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlingId = :behandlingId")
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling?
}
