package no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene

import no.nav.familie.ks.sak.common.exception.Feil
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ArbeidsfordelingPåBehandlingRepository : JpaRepository<ArbeidsfordelingPåBehandling, Long> {
    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlingId = :behandlingId")
    fun finnArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling?

    fun hentArbeidsfordelingPåBehandling(behandlingId: Long): ArbeidsfordelingPåBehandling =
        finnArbeidsfordelingPåBehandling(behandlingId) ?: throw Feil("Finner ikke tilknyttet arbeidsfordelingsenhet på behandling $behandlingId")

    @Query(value = "SELECT apb FROM ArbeidsfordelingPåBehandling apb WHERE apb.behandlendeEnhetId = :enhetId")
    fun hentAlleArbeidsfordelingPåBehandlingMedEnhet(enhetId: String): List<ArbeidsfordelingPåBehandling>
}
