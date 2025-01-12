package no.nav.familie.ks.sak.internal

import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.secureLogger
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
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
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
) {
    fun utledFagsakerSomHarUtbetalingSammeMånedSomBarnehagestart() {
        // Ved start av fulltidplass barnehage så vil andelen ende. Relevante andeler er andeler som sluttes å utbetales i august - des 2024
        val relevanteSluttMånederForAndel = (8..12).map { YearMonth.of(2024, it) }

        // Hent fagsaker som har sist iverksatt behandling som hadde en andel som varte fram til august - des 2024.
        val fagsaker = fagsakRepository.finnFagsakerSomHarSistIverksattBehandlingMedUtbetalingSomStopperMellomAugOgDes2024()

        // Henter sist iverksatte behandling igjen for fagsaker. Dette bare for å være 100% sikker, slik at vi ikke lener oss for mye på selve SQL spørringen.
        val sistIverksatteBehandlinger = fagsaker.mapNotNull { behandlingService.hentSisteBehandlingSomErIverksatt(it.id) }

        // Av disse behandlingene ønsker vi å filtrere bort de som ikke har andeler som slutter i august-des 2024.
        // Dette skal egentlig allerede gjøres av SQL spørringen, men vi tar en dobbelsjekk til.

        val behandlingerOgRelevanteAndeler =
            sistIverksatteBehandlinger
                .associateWith { behandling ->
                    andelerTilkjentYtelseRepository
                        .finnAndelerTilkjentYtelseForBehandling(behandling.id)
                        .filter { it.stønadTom in relevanteSluttMånederForAndel }
                }.filterValues { it.isNotEmpty() }

        // Av alle behandlinger som har utbetaling som slutter de relevantene månedene, beholder vi bare de som har andel samtidig som barn starter fulltid i barnehage.
        val behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart =
            behandlingerOgRelevanteAndeler.filter { (behandling, andeler) ->

                val personOpplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
                val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
                val barnSomFårSisteUtbetaling = andeler.map { it.aktør }.distinct()

                barnSomFårSisteUtbetaling.any { barn ->
                    val person = personOpplysningGrunnlag.personer.singleOrNull { it.aktør == barn }
                    val fødselsdato = person?.fødselsdato ?: error("Finner ikke fødselsdato på barn med aktør id ${barn.aktørId}")
                    val månedÅrBarnEr13Måneder = fødselsdato.plusMonths(13).toYearMonth()

                    val månederBarnStarterIFulltidBarnehage =
                        vilkårsvurdering
                            ?.personResultater
                            ?.filter { it.aktør == barn }
                            ?.flatMap { it.vilkårResultater }
                            ?.filter { it.antallTimer != null && it.antallTimer > BigDecimal(32) }
                            ?.mapNotNull { it.periodeFom?.toYearMonth() }
                            .orEmpty()

                    val andelSluttMånederForBarn = andeler.filter { it.aktør == barn }.map { it.stønadTom }

                    val barnStarterIFulltidBarnehageplassMånedenBarnBlir13Måned = månedÅrBarnEr13Måneder in månederBarnStarterIFulltidBarnehage
                    val barnFårAndelSammeMånedSomBarnEr13Måned = månedÅrBarnEr13Måneder in andelSluttMånederForBarn

                    barnStarterIFulltidBarnehageplassMånedenBarnBlir13Måned && barnFårAndelSammeMånedSomBarnEr13Måned
                }
            }

        secureLogger.info("FagsakSomHarUtbetalingSammeMånedSomBarnehagestart count: ${behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart.size}")
        secureLogger.info("FagsakSomHarUtbetalingSammeMånedSomBarnehagestart innhold: ${behandlingerMedBarnSomFårBetalingSammeMånedSomBarnehageStart.map { it.key.fagsak.id }}")
    }
}
