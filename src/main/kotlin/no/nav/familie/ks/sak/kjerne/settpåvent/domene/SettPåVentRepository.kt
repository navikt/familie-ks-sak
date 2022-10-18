package no.nav.familie.ks.sak.kjerne.settpåvent.domene

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

@Service
interface SettPåVentRepository : JpaRepository<SettPåVent, Long> {
    fun findByBehandlingIdAndAktiv(behandlingId: Long, aktiv: Boolean): SettPåVent?

    fun findByAktivTrue(): List<SettPåVent>
}
