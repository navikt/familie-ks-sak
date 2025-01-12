package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.logger
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class UtledFagsakSomHarUtbetalingSammeMånedSomBarnehagestart(
    private val fagsakRepository: FagsakRepository,
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
) {

    val relevanteMåneder =
        listOf(
            YearMonth.of(2024, 8),
            YearMonth.of(2024, 9),
            YearMonth.of(2024, 10),
            YearMonth.of(2024, 11),
            YearMonth.of(2024, 12),
        )

    fun utledFagsakerSomHarUtbetalingSammeMånedSomBarnehagestart() {
        // Hent fagsaker som har sist iverksatt behandling som hadde en andel som varte fram til august, september, oktober, november eller desember 2024.
        val fagsaker = fagsakRepository.finnFagsakerSomHarSistIverksattBehandlingMedUtbetalingSomStopperiAugustSeptemberNovemberEllerDesember2024()
        val sistIverksatteBehandlinger = fagsaker.mapNotNull { behandlingService.hentSisteBehandlingSomErIverksatt(it.id) }

        val sistIverksatteBehandlingerMedAndelSomGårUtIAugustSeptemberNovemberEllerDesember2024 =
            sistIverksatteBehandlinger.mapNotNull { behandling ->
                val andelerIBehandling = andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

                Pair(behandling, andelerIBehandling).takeIf { andelerIBehandling.any { it.stønadTom in relevanteMåneder } }
            }.toMap()

        // Sjekk hvilke barn som får utbetaling i august, september, november og desember 2024
        val behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart =
            sistIverksatteBehandlingerMedAndelSomGårUtIAugustSeptemberNovemberEllerDesember2024.filter {
                val behandling = it.key
                val andeler = it.value
                val personOpplysningGrunnlagForBehandling = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
                val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)

                val barnSomFårSisteUtbetalingIDisseMånedeneIBehandling = andeler.filter { it.stønadTom in relevanteMåneder }.map { it.aktør }.distinct()

                barnSomFårSisteUtbetalingIDisseMånedeneIBehandling.any { aktør ->
                    val person = personOpplysningGrunnlagForBehandling.personer.singleOrNull { it.aktør == aktør }
                    val fødselsdato = person?.fødselsdato ?: error("Finner ikke fødselsdato på barn med aktør id ${aktør.aktørId}")
                    val månedÅrBarnEr13Måneder = fødselsdato.plusMonths(13).toYearMonth()
                    val månedÅrSomBarnStarterIFulltidBarnehageplass =
                        vilkårsvurdering?.personResultater
                            ?.filter { person.aktør == it.aktør }
                            ?.flatMap { it.vilkårResultater }
                            ?.filter { it.antallTimer != null && it.antallTimer > BigDecimal(40) }
                            ?.map { it.periodeFom?.toYearMonth() } ?: emptyList()
                    val månedÅrSomAndelSlutterForBarn = andeler.map { it.stønadTom }

                    val barnStarterIFulltidBarnehageSammeMånedSom13Måned = månedÅrBarnEr13Måneder in månedÅrSomBarnStarterIFulltidBarnehageplass

                    barnStarterIFulltidBarnehageSammeMånedSom13Måned && månedÅrSomAndelSlutterForBarn.contains(månedÅrBarnEr13Måneder)
                }
            }

        secureLogger.info("FagsakSomHarUtbetalingSammeMånedSomBarnehagestart count: ${behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart.size}")
        secureLogger.info("FagsakSomHarUtbetalingSammeMånedSomBarnehagestart innhold: ${behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart.map { it.key.fagsak.id }}")
    }
}