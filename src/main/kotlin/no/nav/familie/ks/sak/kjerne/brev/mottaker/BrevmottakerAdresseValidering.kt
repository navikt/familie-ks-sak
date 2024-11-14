package no.nav.familie.ks.sak.kjerne.brev.mottaker

import no.nav.familie.ks.sak.api.dto.BrevmottakerDto
import no.nav.familie.ks.sak.api.dto.harGyldigAdresse

object BrevmottakerAdresseValidering {
    fun erBrevmottakereGyldige(brevmottakere: List<BrevmottakerDto>): Boolean =
        brevmottakere.all {
            it.harGyldigAdresse()
        }
}
