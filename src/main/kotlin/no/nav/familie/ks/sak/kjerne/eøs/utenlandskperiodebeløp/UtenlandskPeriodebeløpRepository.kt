package no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.domene.UtenlandskPeriodebeløp
import org.springframework.data.jpa.repository.Query

interface UtenlandskPeriodebeløpRepository : EøsSkjemaRepository<UtenlandskPeriodebeløp> {
    @Query("SELECT upb FROM UtenlandskPeriodebeløp upb WHERE upb.behandlingId = :behandlingId")
    override fun findByBehandlingId(behandlingId: Long): List<UtenlandskPeriodebeløp>
}
