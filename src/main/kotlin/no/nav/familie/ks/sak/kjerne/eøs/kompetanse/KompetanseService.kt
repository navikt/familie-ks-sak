package no.nav.familie.ks.sak.kjerne.eøs.kompetanse

import no.nav.familie.ks.sak.api.dto.KompetanseDto
import no.nav.familie.ks.sak.api.dto.tilKompetanse
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.leftJoin
import no.nav.familie.ks.sak.common.tidslinje.outerJoin
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrer
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.filtrerIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioderIkkeNull
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSeparateTidslinjerForBarna
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilSkjemaer
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.eøs.felles.EøsSkjemaService
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaRepository
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.medBehandlingId
import no.nav.familie.ks.sak.kjerne.eøs.felles.endringsabonnent.EøsSkjemaEndringAbonnent
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.EndretUtbetalingAndelTidslinjeService
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.RegelverkResultat
import no.nav.familie.ks.sak.kjerne.eøs.vilkårsvurdering.VilkårsvurderingTidslinjeService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class KompetanseService(
    kompetanseRepository: EøsSkjemaRepository<Kompetanse>,
    kompetanseEndringsAbonnenter: List<EøsSkjemaEndringAbonnent<Kompetanse>>,
    private val personidentService: PersonidentService,
    private val vilkårsvurderingTidslinjeService: VilkårsvurderingTidslinjeService,
    private val endretUtbetalingAndelTidslinjeService: EndretUtbetalingAndelTidslinjeService
) {

    private val kompetanseSkjemaService = EøsSkjemaService(kompetanseRepository, kompetanseEndringsAbonnenter)

    fun hentKompetanse(kompetanseId: Long) = kompetanseSkjemaService.hentMedId(kompetanseId)

    fun hentKompetanser(behandlingId: Long) = kompetanseSkjemaService.hentMedBehandlingId(behandlingId)

    @Transactional
    fun oppdaterKompetanse(behandlingId: Long, oppdateresKompetanseDto: KompetanseDto) {
        val barnAktører = oppdateresKompetanseDto.barnIdenter.map { personidentService.hentAktør(it) }
        val oppdateresKompetanse = oppdateresKompetanseDto.tilKompetanse(barnAktører)

        kompetanseSkjemaService.endreSkjemaer(behandlingId, oppdateresKompetanse)
    }

    // Oppretter kompetanse skjema i behandlingsresultat
    // når vilkårer er vurdert etter EØS forordningen i vilkårsvurdering for det første gang
    // Tilpasser kompetanse skjema basert på endringer i vilkårsvurdering deretter
    @Transactional
    fun tilpassKompetanse(behandlingId: Long) {
        val eksisterendeKompetanser = kompetanseSkjemaService.hentMedBehandlingId(behandlingId)
        val barnasRegelverkResultatTidslinjer = hentBarnasRegelverkResultatTidslinjer(behandlingId)
        val barnasHarEtterbetaling3MånedTidslinjer =
            endretUtbetalingAndelTidslinjeService.hentBarnasHarEtterbetaling3MånedTidslinjer(behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(
            eksisterendeKompetanser,
            barnasRegelverkResultatTidslinjer,
            barnasHarEtterbetaling3MånedTidslinjer
        ).medBehandlingId(behandlingId)

        kompetanseSkjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, eksisterendeKompetanser, oppdaterteKompetanser)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long) = kompetanseSkjemaService.slettSkjema(kompetanseId)

    private fun tilpassKompetanserTilRegelverk(
        eksisterendeKompetanser: Collection<Kompetanse>,
        barnaRegelverkTidslinjer: Map<Aktør, Tidslinje<RegelverkResultat>>,
        barnasHarEtterbetaling3MånedTidslinjer: Map<Aktør, Tidslinje<Boolean>>
    ): List<Kompetanse> {
        val barnasEøsRegelverkTidslinjer = barnaRegelverkTidslinjer.tilBarnasEøsRegelverkTidslinjer()
            .leftJoin(barnasHarEtterbetaling3MånedTidslinjer) { regelverk, harEtterbetaling3Måned ->
                when (harEtterbetaling3Måned) {
                    true -> null // ta bort regelverk hvis barnet har etterbetaling 3 måned
                    else -> regelverk
                }
            }

        return eksisterendeKompetanser.tilSeparateTidslinjerForBarna()
            .outerJoin(barnasEøsRegelverkTidslinjer) { kompetanse, regelverk ->
                regelverk?.let { kompetanse ?: Kompetanse.blankKompetanse }
            }.tilSkjemaer()
    }

    private fun hentBarnasRegelverkResultatTidslinjer(behandlingId: Long): Map<Aktør, Tidslinje<RegelverkResultat>> =
        vilkårsvurderingTidslinjeService.lagVilkårsvurderingTidslinjer(behandlingId).barnasTidslinjer()
            .mapValues { (_, tidslinjer) ->
                tidslinjer.regelverkResultatTidslinje
            }

    private fun Map<Aktør, Tidslinje<RegelverkResultat>>.tilBarnasEøsRegelverkTidslinjer() =
        this.mapValues { (_, tidslinjer) ->
            tidslinjer.filtrer { it?.regelverk == Regelverk.EØS_FORORDNINGEN }
                .filtrerIkkeNull()
                .forlengTomdatoTilUendeligOmTomErSenereEnn(LocalDate.now())
        }

    private fun <T> Tidslinje<T>.forlengTomdatoTilUendeligOmTomErSenereEnn(nå: LocalDate): Tidslinje<T & Any> {
        val tom = this.tilPerioderIkkeNull().mapNotNull { it.tom }.maxOrNull()
        return if (tom != null && tom > nå) {
            this.tilPerioderIkkeNull()
                .filter { it.fom != null && it.fom <= nå }
                .replaceLast { Periode(verdi = it.verdi, fom = it.fom, tom = null) }
                .tilTidslinje()
        } else {
            this.tilPerioderIkkeNull().tilTidslinje()
        }
    }

    fun <T> List<T>.replaceLast(replacer: (T) -> T) = this.take(this.size - 1) + replacer(this.last())
}
