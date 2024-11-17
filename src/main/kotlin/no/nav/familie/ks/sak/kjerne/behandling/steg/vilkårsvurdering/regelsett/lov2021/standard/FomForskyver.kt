package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.regelsett.lov2021.standard

import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import java.time.LocalDate

fun forskyvFom(periodeFom: LocalDate?): LocalDate? {
    if (periodeFom == null) {
        return null
    }
    return periodeFom.plusMonths(1).førsteDagIInneværendeMåned()
}
