package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerTidslinjer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import java.math.BigDecimal
import java.time.LocalDate

data class PeriodeResultat(
    val aktør: Aktør,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?,
    val vilkårResultater: Set<PeriodeVilkår>
) {
    fun allePåkrevdeVilkårErOppfylt(personType: PersonType): Boolean =
        vilkårResultater.map { it.vilkårType }.containsAll(Vilkår.hentVilkårFor(personType)) &&
            vilkårResultater.all { it.resultat in listOf(Resultat.OPPFYLT, Resultat.IKKE_AKTUELT) }

    fun overlapper(annetPeriodeResultat: PeriodeResultat): Boolean {
        if (periodeFom == null && annetPeriodeResultat.periodeFom == null) {
            throw FunksjonellFeil(
                melding = "Enten søker eller barn må ha fom dato på vilkårsresultatet",
                frontendFeilmelding = "Du må sette en fom dato på minst et vilkår i vilkårsvurderingen"
            )
        }
        if (periodeTom == null && annetPeriodeResultat.periodeTom == null) {
            throw FunksjonellFeil(
                melding = "Enten søker eller barn må ha tom dato på vilkårsresultatet",
                frontendFeilmelding = "Du må sette en tom dato på minst et vilkår i vilkårsvurderingen"
            )
        }

        return (periodeFom == null || annetPeriodeResultat.periodeTom == null || periodeFom <= annetPeriodeResultat.periodeTom) &&
            (periodeTom == null || annetPeriodeResultat.periodeFom == null || periodeTom >= annetPeriodeResultat.periodeFom)
    }
}

data class PeriodeVilkår(
    val vilkårType: Vilkår,
    val resultat: Resultat,
    var begrunnelse: String,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val antallTimer: BigDecimal? = null,
    val periodeFom: LocalDate?,
    val periodeTom: LocalDate?
)

fun PersonResultat.tilPeriodeResultater(): List<PeriodeResultat> {
    val tidslinjer = this.vilkårResultater.filter { !it.erAvslagUtenPeriode() }
        .map { vilkårResultat ->
            listOf(
                Periode(
                    verdi = vilkårResultat,
                    fom = utledFomFraVilkårResultat(vilkårResultat),
                    tom = utledTomFraVilkårResultat(vilkårResultat)
                )
            ).tilTidslinje()
        }
    val kombinertTidslinjer = tidslinjer.kombinerTidslinjer()
    return kombinertTidslinjer.tilPerioder().map { periode ->
        PeriodeResultat(
            aktør = aktør,
            periodeFom = periode.fom,
            periodeTom = periode.tom,
            vilkårResultater = periode.verdi!!.map { vilkårResultat ->
                PeriodeVilkår(
                    vilkårType = vilkårResultat.vilkårType,
                    resultat = vilkårResultat.resultat,
                    begrunnelse = vilkårResultat.begrunnelse,
                    utdypendeVilkårsvurderinger = vilkårResultat.utdypendeVilkårsvurderinger,
                    antallTimer = vilkårResultat.antallTimer,
                    periodeFom = utledFomFraVilkårResultat(vilkårResultat),
                    periodeTom = utledTomFraVilkårResultat(vilkårResultat)
                )
            }.toSet()
        )
    }
}

fun utledFomFraVilkårResultat(vilkårResultat: VilkårResultat) = when {
    // setter fom null slik at det ikke påvirker beregning
    vilkårResultat.resultat == Resultat.IKKE_AKTUELT -> null
    else -> vilkårResultat.periodeFom?.withDayOfMonth(1)
}

fun utledTomFraVilkårResultat(vilkårResultat: VilkårResultat) = when {
    // setter tom null slik at det ikke påvirker beregning
    vilkårResultat.resultat == Resultat.IKKE_AKTUELT -> null
    else -> vilkårResultat.periodeTom?.sisteDagIMåned()
}
