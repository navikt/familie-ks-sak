package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammen
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.math.BigDecimal
import java.time.LocalDate

data class PeriodeResultat(
    val aktør: Aktør,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val vilkårResultater: Set<PeriodeVilkår>,
)

data class PeriodeVilkår(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    var begrunnelse: String,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val antallTimer: BigDecimal? = null,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
)

fun PersonResultat.tilPeriodeResultater(): List<PeriodeResultat> {
    val tidslinjer =
        this.vilkårResultater
            .filter { !it.erAvslagUtenPeriode() }
            .map { vilkårResultat ->
                listOf(
                    Periode(
                        verdi = vilkårResultat,
                        fom = utledFomFraVilkårResultat(vilkårResultat),
                        tom = utledTomFraVilkårResultat(vilkårResultat),
                    ),
                ).tilTidslinje()
            }
    val kombinertTidslinjer = tidslinjer.slåSammen()
    return kombinertTidslinjer.tilPerioder().map { periode ->
        PeriodeResultat(
            aktør = aktør,
            periodeFom = periode.fom,
            periodeTom = periode.tom,
            vilkårResultater =
                periode.verdi!!
                    .map { vilkårResultat ->
                        PeriodeVilkår(
                            vilkårType = vilkårResultat.vilkårType,
                            resultat = vilkårResultat.resultat,
                            begrunnelse = vilkårResultat.begrunnelse,
                            utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
                            antallTimer = vilkårResultat.antallTimer,
                            periodeFom = utledFomFraVilkårResultat(vilkårResultat),
                            periodeTom = utledTomFraVilkårResultat(vilkårResultat),
                        )
                    }.toSet(),
        )
    }
}

fun utledFomFraVilkårResultat(vilkårResultat: VilkårResultat) =
    when (vilkårResultat.resultat) {
        Resultat.IKKE_AKTUELT -> null

        // setter fom null slik at det ikke påvirker beregning
        else -> vilkårResultat.periodeFom?.withDayOfMonth(1)
    }

fun utledTomFraVilkårResultat(vilkårResultat: VilkårResultat) =
    when (vilkårResultat.resultat) {
        Resultat.IKKE_AKTUELT -> null

        // setter tom null slik at det ikke påvirker beregning v
        else -> vilkårResultat.periodeTom?.sisteDagIMåned()
    }
