package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2024.standard

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import java.time.LocalDate

fun forskyvFom(
    fomDato: LocalDate?,
): LocalDate? {
    if (fomDato == null) {
        return null
    }
    return fomDato.førsteDagIInneværendeMåned()
}
