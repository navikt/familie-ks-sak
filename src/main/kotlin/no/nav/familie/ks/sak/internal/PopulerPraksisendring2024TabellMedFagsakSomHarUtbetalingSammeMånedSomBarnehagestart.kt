package no.nav.familie.ks.sak.internal

import jakarta.transaction.Transactional
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024
import no.nav.familie.ks.sak.kjerne.praksisendring.Praksisendring2024Repository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class PopulerPraksisendring2024TabellMedFagsakSomHarUtbetalingSammeMånedSomBarnehagestart(
    private val fagsakRepository: FagsakRepository,
    private val behandlingService: BehandlingService,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService,
    private val praksisendring2024Repository: Praksisendring2024Repository,
) {
    @Transactional
    fun utfør() {
        // Ved start av fulltidplass barnehage så vil andelen ende. Relevante andeler er andeler som sluttes å utbetales i august - des 2024
        val relevanteSluttMånederForAndel = (8..12).map { YearMonth.of(2024, it) }

        // Hent fagsaker som har sist iverksatt behandling som hadde en andel som varte fram til august - des 2024.
        val fagsaker = fagsakRepository.finnFagsakerSomHarSistIverksattBehandlingMedUtbetalingSomStopperMellomAugOgDes2024()

        // Henter sist iverksatte behandling igjen for fagsaker. Dette bare for å være 100% sikker, slik at vi ikke lener oss for mye på selve SQL spørringen.
        val sistIverksatteBehandlinger = fagsaker.mapNotNull { behandlingService.hentSisteBehandlingSomErIverksatt(it.id) }

        // Vi henter behandlingene og andelene. Dersom behandlingen ikke har andeler som slutter i den relevante perioden, hopper vi over.
        val behandlingerOgRelevanteAndeler =
            sistIverksatteBehandlinger
                .associateWith { behandling ->
                    andelerTilkjentYtelseRepository
                        .finnAndelerTilkjentYtelseForBehandling(behandling.id)
                        .filter { it.stønadTom in relevanteSluttMånederForAndel }
                }.filterValues { it.isNotEmpty() }

        // Vi går gjennom alle behandlinger og relevante andeler
        behandlingerOgRelevanteAndeler.forEach { (behandling, andeler) ->

            val personOpplysningGrunnlag = personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id)
            val vilkårsvurdering = vilkårsvurderingRepository.finnAktivForBehandling(behandling.id)
            val barnSomHarAndelSomSlutterIRelevantMåned = andeler.map { it.aktør }.distinct()

            barnSomHarAndelSomSlutterIRelevantMåned.forEach { barn ->
                val person = personOpplysningGrunnlag.personer.singleOrNull { it.aktør == barn }
                val fødselsdato = person?.fødselsdato ?: error("Finner ikke fødselsdato på barn med aktør id ${barn.aktørId}")
                val månedÅrBarnEr13Måneder = fødselsdato.plusMonths(13).toYearMonth()

                val månederBarnStarterIBarnehage =
                    vilkårsvurdering
                        ?.personResultater
                        ?.filter { it.aktør == barn }
                        ?.flatMap { it.vilkårResultater }
                        ?.filter { it.antallTimer != null && it.antallTimer != BigDecimal(0) }
                        ?.mapNotNull { it.periodeFom?.toYearMonth() }
                        .orEmpty()

                val andelSluttMånederForBarn = andeler.filter { it.aktør == barn }.map { it.stønadTom }

                val barnStarterIBarnehageplassMånedenBarnBlir13Måned = månedÅrBarnEr13Måneder in månederBarnStarterIBarnehage
                val barnFårAndelSammeMånedSomBarnEr13Måned = månedÅrBarnEr13Måneder in andelSluttMånederForBarn

                if (barnStarterIBarnehageplassMånedenBarnBlir13Måned && barnFårAndelSammeMånedSomBarnEr13Måned) {
                    val fagsakId = behandling.fagsak.id

                    if (!praksisendring2024Repository.existsPraksisendring2024ByFagsakIdAndAktør(fagsakId, barn)) {
                        praksisendring2024Repository.save(
                            Praksisendring2024(
                                fagsakId = fagsakId,
                                aktør = barn,
                                utbetalingsmåned = månedÅrBarnEr13Måneder,
                            ),
                        )
                    }
                }
            }
        }
    }
}
