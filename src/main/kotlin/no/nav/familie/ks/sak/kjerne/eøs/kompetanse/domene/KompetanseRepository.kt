package no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene

import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import org.springframework.data.jpa.repository.Query

interface KompetanseRepository : EøsSkjemaRepository<Kompetanse> {

    @Query("SELECT k FROM Kompetanse k WHERE k.behandlingId = :behandlingId")
    override fun findByBehandlingId(behandlingId: Long): List<Kompetanse>
}
