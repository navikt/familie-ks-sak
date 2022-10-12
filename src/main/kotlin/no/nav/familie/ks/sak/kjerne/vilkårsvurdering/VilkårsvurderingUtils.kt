package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat

/**
 * Funksjon som tar inn endret vilkår og lager nye vilkårresultater til å få plass til den endrede perioden.
 * @param[personResultat] Person med vilkår som eventuelt justeres
 * @param[restVilkårResultat] Det endrede vilkårresultatet
 * @return VilkårResultater før og etter mutering
 */
fun endreVilkårResultatVedEndringAvPeriode(
    personResultat: PersonResultat,
    vilkårSomEndres: VilkårResultatDto
): Pair<List<VilkårResultat>, List<VilkårResultat>> {

    validerAvslagUtenPeriodeMedLøpende(
        personResultat = personResultat,
        vilkårSomEndres = vilkårSomEndres
    )

    val (vilkårResultaterSomSkalEndres, vilkårResultater) = personResultat.vilkårResultater.partition { !(it.erAvslagUtenPeriode() && it.id != vilkårSomEndres.id) }

    vilkårResultaterSomSkalEndres
        .forEach {
            tilpassVilkårForEndretVilkår(
                personResultat = personResultat,
                vilkårResultat = it,
                endretVilkårResultat = restVilkårResultat
            )
        }

    return Pair(vilkårResultaterSomSkalEndres, personResultat.vilkårResultater.toList())
}

/**
 * @param [personResultat] person vilkårresultatet tilhører
 * @param [vilkårResultat] vilkårresultat som skal oppdaters på person
 * @param [endretVilkårResultat] oppdatert resultat fra frontend
 */
fun tilpassVilkårForEndretVilkår(
    personResultat: PersonResultat,
    vilkårResultat: VilkårResultat,
    endretVilkårResultat: VilkårResultatDto
) {
    val periodePåNyttVilkår: Periode = endretVilkårResultat.toPeriode()

    if (vilkårResultat.id == endretVilkårResultat.id) {
        vilkårResultat.oppdater(endretVilkårResultat)

    } else if (vilkårResultat.vilkårType == endretVilkårResultat.vilkårType && !endretVilkårResultat.erAvslagUtenPeriode()) {
        val periode: Periode = vilkårResultat.toPeriode()

        var nyFom = periodePåNyttVilkår.tom
        if (periodePåNyttVilkår.tom != TIDENES_ENDE) {
            nyFom = periodePåNyttVilkår.tom.plusDays(1)
        }

        val nyTom = periodePåNyttVilkår.fom.minusDays(1)

        when {
            periodePåNyttVilkår.kanErstatte(periode) -> {
                personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
            }

            periodePåNyttVilkår.kanSplitte(periode) -> {
                personResultat.removeVilkårResultat(vilkårResultatId = vilkårResultat.id)
                personResultat.addVilkårResultat(
                    vilkårResultat.kopierMedNyPeriode(
                        fom = periode.fom,
                        tom = nyTom,
                        behandlingId = personResultat.vilkårsvurdering.behandling.id
                    )
                )
                personResultat.addVilkårResultat(
                    vilkårResultat.kopierMedNyPeriode(
                        fom = nyFom,
                        tom = periode.tom,
                        behandlingId = personResultat.vilkårsvurdering.behandling.id
                    )
                )
            }

            periodePåNyttVilkår.kanFlytteFom(periode) -> {
                vilkårResultat.periodeFom = nyFom
                vilkårResultat.erAutomatiskVurdert = false
                vilkårResultat.oppdaterPekerTilBehandling()
            }

            periodePåNyttVilkår.kanFlytteTom(periode) -> {
                vilkårResultat.periodeTom = nyTom
                vilkårResultat.erAutomatiskVurdert = false
                vilkårResultat.oppdaterPekerTilBehandling()
            }
        }
    }
}

fun validerAvslagUtenPeriodeMedLøpende(personResultat: PersonResultat, vilkårSomEndres: VilkårResultatDto) {
    val vilkårResultater =
        personResultat.vilkårResultater.filter { it.vilkårType == vilkårSomEndres.vilkårType && it.id != vilkårSomEndres.id }

    when {
        // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full kontantstøtte, men innvilge delt
        vilkårSomEndres.vilkårType == Vilkår.BOR_MED_SØKER -> return

        vilkårSomEndres.erAvslagUtenPeriode() && vilkårResultater.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
            throw FunksjonellFeil(
                "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret."
            )

        vilkårSomEndres.harFremtidigTom() && vilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret."
            )
    }
}