package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.ClockProvider
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSkjemaer
import no.nav.familie.ks.sak.common.util.førsteDagINesteMåned
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.tilBarnasSkalIkkeUtbetalesTidslinjer
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndelRepository
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.UtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.tilPerioder
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.utfyltePerioder
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import no.nav.familie.tidslinje.utvidelser.leftJoin
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TilpassKompetanserService(
    kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
    kompetanseEndringsAbonnenter: List<EøsSkjemaEndringAbonnent<Kompetanse>>,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository,
    private val overgangsordningAndelRepository: OvergangsordningAndelRepository,
    private val clockProvider: ClockProvider,
) {
    private val kompetanseSkjemaService = EøsSkjemaService(kompetanseRepository, kompetanseEndringsAbonnenter)

    @Transactional
    fun tilpassKompetanser(behandlingId: BehandlingId) {
        val eksisterendeKompetanser = kompetanseSkjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer = vilkårsvurderingTidslinjeService.hentBarnasRegelverkResultatTidslinjer(behandlingId)
        val endretUtbetalingAndeler = endretUtbetalingAndelRepository.hentEndretUtbetalingerForBehandling(behandlingId.id)
        val overgangsordningAndeler = overgangsordningAndelRepository.hentOvergangsordningAndelerForBehandling(behandlingId.id)

        val annenForelderOmfattetAvNorskLovgivningTidslinje =
            vilkårsvurderingTidslinjeService.hentAnnenForelderOmfattetAvNorskLovgivningTidslinje(behandlingId = behandlingId.id)

        val barnasSkalIkkeUtbetalesTidslinjer = endretUtbetalingAndeler.tilBarnasSkalIkkeUtbetalesTidslinjer()
        val utfylteOvergangsordningAndeler = overgangsordningAndeler.utfyltePerioder()

        val oppdaterteKompetanser =
            tilpassKompetanser(
                eksisterendeKompetanser,
                barnasRegelverkResultatTidslinjer,
                barnasSkalIkkeUtbetalesTidslinjer,
                annenForelderOmfattetAvNorskLovgivningTidslinje,
                utfylteOvergangsordningAndeler,
            ).medBehandlingId(behandlingId)

        kompetanseSkjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, eksisterendeKompetanser, oppdaterteKompetanser)
    }

    private fun tilpassKompetanser(
        eksisterendeKompetanser: Collection<Kompetanse>,
        barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat>>,
        barnasHarEtterbetaling3MånedTidslinjer: Map<Aktør, Tidslinje<Boolean>>,
        annenForelderOmfattetAvNorskLovgivningTidslinje: Tidslinje<Boolean>,
        utfylteOvergangsordningAndeler: List<UtfyltOvergangsordningAndel>,
    ): List<Kompetanse> {
        val barnasEøsRegelverkTidslinjer =
            barnaRegelverkTidslinjer
                .tilBarnasEøsRegelverkTidslinjer()
                .leftJoin(barnasHarEtterbetaling3MånedTidslinjer) { regelverk, harEtterbetaling3Måned ->
                    if (harEtterbetaling3Måned == true) {
                        null
                    } else {
                        regelverk
                    }
                }

        val overgangsordningAndelerTidslinjer =
            utfylteOvergangsordningAndeler
                .groupBy { it.person.aktør }
                .mapValues {
                    it.value
                        .tilPerioder()
                        .tilTidslinje()
                }
        val eksistererEøsRegelverkPåBehandling =
            barnasEøsRegelverkTidslinjer.values.any { it.tilPerioderIkkeNull().isNotEmpty() }

        return eksisterendeKompetanser
            .tilSeparateTidslinjerForBarna()
            .outerJoin(barnasEøsRegelverkTidslinjer, overgangsordningAndelerTidslinjer) { kompetanse, regelverk, overgangsordningAndel ->
                when {
                    regelverk != null -> kompetanse ?: Kompetanse.blankKompetanse
                    eksistererEøsRegelverkPåBehandling && overgangsordningAndel != null -> kompetanse ?: Kompetanse.blankKompetanse
                    else -> null
                }
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
                .forlengTomdatoTilUendeligOmTomErSenereEnn(førsteDagINesteMåned = LocalDate.now(clockProvider.get()).førsteDagINesteMåned())
        }
}

private fun <T> Tidslinje<T>.forlengTomdatoTilUendeligOmTomErSenereEnn(førsteDagINesteMåned: LocalDate): Tidslinje<T & Any> {
    val tom = this.tilPerioderIkkeNull().mapNotNull { it.tom }.maxOrNull()
    return if (tom != null && tom > førsteDagINesteMåned) {
        this
            .tilPerioderIkkeNull()
            .filter { it.fom != null && it.fom!! <= førsteDagINesteMåned }
            .replaceLast { Periode(verdi = it.verdi, fom = it.fom, tom = null) }
            .tilTidslinje()
    } else {
        this.tilPerioderIkkeNull().tilTidslinje()
    }
}

private fun <T> List<T>.replaceLast(replacer: (T) -> T): List<T> {
    if (this.isEmpty()) {
        throw Feil("Kan ikke modifisere på siste element i en tom liste")
    }
    return this.take(this.size - 1) + replacer(this.last())
}
