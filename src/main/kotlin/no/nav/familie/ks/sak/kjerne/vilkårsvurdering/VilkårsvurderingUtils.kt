package no.nav.familie.ks.sak.kjerne.vilkårsvurdering

import no.nav.familie.ks.sak.api.dto.VilkårResultatDto
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.tidslinje.TidsEnhet
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.TidslinjePeriode
import no.nav.familie.ks.sak.common.tidslinje.Verdi
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilTidslinjePerioderMedLocalDate
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import java.time.Duration
import java.time.LocalDate

/**
 * Funksjon som tar inn endret vilkår og lager nye vilkårresultater til å få plass til den endrede perioden.
 * @param[personResultat] Person med vilkår som eventuelt justeres
 * @param[restVilkårResultat] Det endrede vilkårresultatet
 * @return VilkårResultater før og etter mutering
 */
fun endreVilkårResultat(
    vilkårResultater: List<VilkårResultat>,
    endretVilkårResultatDto: VilkårResultatDto
): List<VilkårResultat> {
    validerAvslagUtenPeriodeMedLøpende(
        vilkårResultater = vilkårResultater,
        endretVilkårResultat = endretVilkårResultatDto
    )

    val endretVilkårResultat =
        endretVilkårResultatDto.tilVilkårResultat(vilkårResultater.single { it.id == endretVilkårResultatDto.id })

    val placeholder: List<VilkårResultat> = vilkårResultater
        .filter { !it.erAvslagUtenPeriode() || it.id == endretVilkårResultatDto.id }
        .flatMap {
            tilpassVilkårForEndretVilkår(
                eksisterendeVilkårResultat = it,
                endretVilkårResultat = endretVilkårResultat!!
            )
        }

    return placeholder
}

fun lagTidslinjeForVilkårResultat(
    innhold: List<VilkårResultat>,
    startDato: LocalDate,
    tidsEnhet: TidsEnhet
): Tidslinje<VilkårResultat> {
    val perioder = innhold.map {
        TidslinjePeriode(
            it,
            Duration.between(
                it.periodeFom?.atStartOfDay() ?: LocalDate.MIN,
                it.periodeTom?.atStartOfDay() ?: LocalDate.MAX
            ).toDaysPart().toInt(),
            it.periodeFom == null || it.periodeTom == null
        )
    }

    return Tidslinje(startDato, perioder, tidsEnhet = tidsEnhet)
}

fun List<VilkårResultat>.tilTidslinje() = lagTidslinjeForVilkårResultat(
    innhold = this,
    startDato = this.minOf { it.periodeFom ?: LocalDate.MIN },
    tidsEnhet = TidsEnhet.DAG
)

/**
 * @param [personResultat] person vilkårresultatet tilhører
 * @param [eksisterendeVilkårResultat] vilkårresultat som skal oppdaters på person
 * @param [endretVilkårResultatDto] oppdatert resultat fra frontend
 */
fun tilpassVilkårForEndretVilkår(
    eksisterendeVilkårResultat: VilkårResultat,
    endretVilkårResultat: VilkårResultat
): List<VilkårResultat> {
    if (eksisterendeVilkårResultat.id == endretVilkårResultat.id) {
        return listOf(endretVilkårResultat)
    }

    if (eksisterendeVilkårResultat.vilkårType != endretVilkårResultat.vilkårType || endretVilkårResultat.erAvslagUtenPeriode()) {
        return listOf(eksisterendeVilkårResultat)
    }

    val eksisterendeVilkårResultatTidslinje = listOf(eksisterendeVilkårResultat).tilTidslinje()
    val endretVilkårResultatTidslinje = listOf(endretVilkårResultat).tilTidslinje()

    return eksisterendeVilkårResultatTidslinje
        .kombinerMed(endretVilkårResultatTidslinje) { eksisterendeVilkår, endretVilkår ->
            if (endretVilkår is Verdi) {
                endretVilkår
            } else {
                eksisterendeVilkår
            }
        }.tilTidslinjePerioderMedLocalDate()
        .mapNotNull {
            val vilkårResultat = it.periodeVerdi.verdi

            val vilkårsdatoErUendret = it.fom == vilkårResultat?.periodeFom &&
                it.tom == vilkårResultat.periodeTom

            if (vilkårsdatoErUendret) {
                vilkårResultat
            } else {
                vilkårResultat?.kopierMedNyPeriode(
                    fom = it.fom,
                    tom = it.tom,
                    behandlingId = endretVilkårResultat.behandlingId
                )
            }
        }
}

fun validerAvslagUtenPeriodeMedLøpende(vilkårResultater: List<VilkårResultat>, endretVilkårResultat: VilkårResultatDto) {
    val filtrerteVilkårResultater =
        vilkårResultater.filter { it.vilkårType == endretVilkårResultat.vilkårType && it.id != endretVilkårResultat.id }

    when {
        // For bor med søker-vilkåret kan avslag og innvilgelse være overlappende, da man kan f.eks. avslå full kontantstøtte, men innvilge delt
        endretVilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER -> return

        endretVilkårResultat.erAvslagUtenPeriode() && filtrerteVilkårResultater.any { it.resultat == Resultat.OPPFYLT && it.harFremtidigTom() } ->
            throw FunksjonellFeil(
                "Finnes løpende oppfylt ved forsøk på å legge til avslag uten periode ",
                "Du kan ikke legge til avslag uten datoer fordi det finnes oppfylt løpende periode på vilkåret."
            )

        endretVilkårResultat.harFremtidigTom() && filtrerteVilkårResultater.any { it.erAvslagUtenPeriode() } ->
            throw FunksjonellFeil(
                "Finnes avslag uten periode ved forsøk på å legge til løpende oppfylt",
                "Du kan ikke legge til løpende periode fordi det er vurdert avslag uten datoer på vilkåret."
            )
    }
}
