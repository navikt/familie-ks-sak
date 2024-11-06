package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.leftJoin
import no.nav.familie.ks.sak.common.tidslinje.outerJoin
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSkjemaer
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelerOppdatertAbonnent
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.tilBarnasSkalIkkeUtbetalesTidslinjer
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TilpassKompetanserTilEndretUtbetalingAndelerService(
    kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
    kompetanseEndringsAbonnenter: List<EøsSkjemaEndringAbonnent<Kompetanse>>,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
) : EndretUtbetalingAndelerOppdatertAbonnent {
    private val kompetanseSkjemaService = EøsSkjemaService(kompetanseRepository, kompetanseEndringsAbonnenter)

    @Transactional
    override fun tilpassKompetanserTilEndretUtbetalingAndeler(
        behandlingId: BehandlingId,
        endretUtbetalingAndeler: List<EndretUtbetalingAndel>,
    ) {
        val eksisterendeKompetanser = kompetanseSkjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer = vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)
        val barnasSkalIkkeUtbetalesTidslinjer = endretUtbetalingAndeler.tilBarnasSkalIkkeUtbetalesTidslinjer()

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId.id)

        val oppdaterteKompetanser =
            tilpassKompetanserTilRegelverk(
                eksisterendeKompetanser,
                barnasRegelverkResultatTidslinjer,
                barnasSkalIkkeUtbetalesTidslinjer,
                annenForelderOmfattetAvNorskLovgivningTidslinje,
            ).medBehandlingId(behandlingId)

        kompetanseSkjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, eksisterendeKompetanser, oppdaterteKompetanser)
    }
}

fun VilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId: BehandlingId): Map<Aktør, Tidslinje<RegelverkResultat>> =
    this
        .lagVilkårsvurderingTidslinjer(behandlingId.id)
        .barnasTidslinjer()
        .mapValues { (_, tidslinjer) ->
            tidslinjer.regelverkResultatTidslinje
        }

fun tilpassKompetanserTilRegelverk(
    eksisterendeKompetanser: Collection<Kompetanse>,
    barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat>>,
    barnasHarEtterbetaling3MånedTidslinjer: Map<Aktør, Tidslinje<Boolean>>,
    annenForelderOmfattetAvNorskLovgivningTidslinje: Tidslinje<Boolean>,
): List<Kompetanse> {
    val barnasEøsRegelverkTidslinjer =
        barnaRegelverkTidslinjer
            .tilBarnasEøsRegelverkTidslinjer()
            .leftJoin(barnasHarEtterbetaling3MånedTidslinjer) { regelverk, harEtterbetaling3Måned ->
                when (harEtterbetaling3Måned) {
                    true -> null // ta bort regelverk hvis barnet har etterbetaling 3 måned
                    else -> regelverk
                }
            }

    return eksisterendeKompetanser
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, regelverk ->
            regelverk?.let { kompetanse ?: Kompetanse.blankKompetanse }
        }.mapValues { (_, value) ->
            value.kombinerMed(annenForelderOmfattetAvNorskLovgivningTidslinje) { kompetanse, annenForelderOmfattet ->
                kompetanse?.copy(erAnnenForelderOmfattetAvNorskLovgivning = annenForelderOmfattet ?: false)
            }
        }.tilSkjemaer()
}

private fun Map<Aktør, Tidslinje<RegelverkResultat>>.tilBarnasEøsRegelverkTidslinjer() =
    this.mapValues { (_, tidslinjer) ->
        tidslinjer
            .filtrer { it?.regelverk == Regelverk.EØS_FORORDNINGEN }
            .filtrerIkkeNull()
            .forlengTomdatoTilUendeligOmTomErSenereEnn(førsteDagINesteMåned = LocalDate.now().førsteDagINesteMåned())
    }

private fun <T> Tidslinje<T>.forlengTomdatoTilUendeligOmTomErSenereEnn(førsteDagINesteMåned: LocalDate): Tidslinje<T & Any> {
    val tom = this.tilPerioderIkkeNull().mapNotNull { it.tom }.maxOrNull()
    return if (tom != null && tom > førsteDagINesteMåned) {
        this
            .tilPerioderIkkeNull()
            .filter { it.fom != null && it.fom <= førsteDagINesteMåned }
            .replaceLast { Periode(verdi = it.verdi, fom = it.fom, tom = null) }
            .tilTidslinje()
    } else {
        this.tilPerioderIkkeNull().tilTidslinje()
    }
}

fun <T> List<T>.replaceLast(replacer: (T) -> T): List<T> {
    if (this.isEmpty()) {
        throw Feil("Kan ikke modifisere på siste element i en tom liste")
    }
    return this.take(this.size - 1) + replacer(this.last())
}
