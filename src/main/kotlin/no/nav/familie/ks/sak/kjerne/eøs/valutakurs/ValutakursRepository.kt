package no.nav.familie.ks.sak.kjerne.eøs.valutakurs

import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene.Valutakurs
import org.springframework.data.jpa.repository.Query

interface ValutakursRepository : EøsSkjemaRepository<Valutakurs> {
    @Query("SELECT vk FROM Valutakurs vk WHERE vk.behandlingId = :behandlingId")
    override fun findByBehandlingId(behandlingId: Long): List<Valutakurs>
}
