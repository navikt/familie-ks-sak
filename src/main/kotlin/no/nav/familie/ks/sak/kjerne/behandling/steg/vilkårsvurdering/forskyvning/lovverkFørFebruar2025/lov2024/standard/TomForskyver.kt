package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning.lovverkFørFebruar2025.lov2024.standard

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import java.time.LocalDate

fun forskyvTom(
    tomDato: LocalDate?,
): LocalDate? {
    if (tomDato == null) {
        return null
    }
    return tomDato.sisteDagIMåned()
}
