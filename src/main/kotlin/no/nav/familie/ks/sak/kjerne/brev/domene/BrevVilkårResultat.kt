package no.nav.familie.ks.sak.kjerne.brev.domene

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.common.util.TIDENES_ENDE
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import java.time.LocalDate

data class BrevVilkårResultat(
    val vilkårType: Vilkår,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val resultat: Resultat,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val erEksplisittAvslagPåSøknad: Boolean?
) {

    fun toPeriode(): Periode = lagOgValiderPeriodeFraVilkår(
        this.periodeFom,
        this.periodeTom,
        this.erEksplisittAvslagPåSøknad
    )
}

fun VilkårResultat.tilBrevVilkårResultat() =
    BrevVilkårResultat(
        vilkårType = this.vilkårType,
        periodeFom = this.periodeFom,
        periodeTom = this.periodeTom,
        resultat = this.resultat,
        utdypendeVilkårsvurderinger = this.utdypendeVilkårsvurderinger,
        erEksplisittAvslagPåSøknad = this.erEksplisittAvslagPåSøknad
    )

fun lagOgValiderPeriodeFraVilkår(
    periodeFom: LocalDate?,
    periodeTom: LocalDate?,
    erEksplisittAvslagPåSøknad: Boolean? = null
): Periode {
    return when {
        periodeFom !== null -> {
            Periode(
                fom = periodeFom,
                tom = periodeTom ?: TIDENES_ENDE
            )
        }

        erEksplisittAvslagPåSøknad == true && periodeTom == null -> {
            Periode(
                fom = TIDENES_MORGEN,
                tom = TIDENES_ENDE
            )
        }

        else -> {
            throw FunksjonellFeil("Ugyldig periode. Periode må ha t.o.m.-dato eller være et avslag uten datoer.")
        }
    }
}