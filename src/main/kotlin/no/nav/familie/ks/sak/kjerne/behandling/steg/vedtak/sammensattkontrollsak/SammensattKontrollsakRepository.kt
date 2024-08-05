package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.sammensattkontrollsak

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SammensattKontrollsakRepository : JpaRepository<SammensattKontrollsak, Long> {
    @Query(value = "SELECT sk FROM SammensattKontrollsak sk WHERE sk.behandlingId = :behandlingId")
    fun finnSammensattKontrollsakForBehandling(behandlingId: Long): SammensattKontrollsak?

    @Query(value = "SELECT sk FROM SammensattKontrollsak sk WHERE sk.id = :sammensattKontrollsakId")
    fun finnSammensattKontrollsak(sammensattKontrollsakId: Long): SammensattKontrollsak?
}
