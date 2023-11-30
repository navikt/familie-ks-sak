package no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjema

interface EøsSkjemaEndringAbonnent<T : EøsSkjema<T>> {
    fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: List<T>,
    )
}
