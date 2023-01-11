package no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent

import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjema

interface EøsSkjemaEndringAbonnent<T : EøsSkjema<T>> {
    fun skjemaerEndret(behandlingId: Long, endretTil: List<T>)
}
