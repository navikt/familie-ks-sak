package no.nav.familie.ks.sak.kjerne.eøs.util

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

val tomKompetanse = Kompetanse(null, null, emptySet())

class KompetanseBuilder(
    startMåned: YearMonth,
    behandlingId: BehandlingId = BehandlingId(1L),
) : SkjemaBuilder<Kompetanse, KompetanseBuilder>(startMåned, behandlingId) {
    fun medKompetanse(
        k: String,
        vararg barn: Person,
        annenForeldersAktivitetsland: String? = null,
        erAnnenForelderOmfattetAvNorskLovgivning: Boolean? = false,
    ) = medSkjema(k, barn.toList()) {
        when (it) {
            '-' -> {
                tomKompetanse.copy(
                    annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                    erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                )
            }

            'S' -> {
                tomKompetanse
                    .copy(
                        resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
                        annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                        erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                    ).fyllUt()
            }

            'P' -> {
                tomKompetanse
                    .copy(
                        resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND,
                        annenForeldersAktivitetsland = annenForeldersAktivitetsland,
                        erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning,
                    ).fyllUt()
            }

            else -> {
                null
            }
        }
    }

    fun byggKompetanser(): List<Kompetanse> = bygg()
}

fun Kompetanse.fyllUt() =
    this.copy(
        erAnnenForelderOmfattetAvNorskLovgivning = erAnnenForelderOmfattetAvNorskLovgivning ?: false,
        resultat = resultat ?: KompetanseResultat.NORGE_ER_SEKUNDÆRLAND,
        annenForeldersAktivitetsland = annenForeldersAktivitetsland ?: "DK",
        barnetsBostedsland = barnetsBostedsland ?: "NO",
        søkersAktivitet = søkersAktivitet,
        annenForeldersAktivitet = annenForeldersAktivitet,
        søkersAktivitetsland = søkersAktivitetsland ?: "SE",
    )
