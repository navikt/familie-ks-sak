package no.nav.familie.ks.sak.internal.kontantstøtteOvergangsordningBrevNovember2024

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.tilYearMonth
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class DistribuerInformasjonsbrevOvergangsordningNovember2024(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val taskService: TaskService,
    private val andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {
    private val logger: Logger =
        LoggerFactory.getLogger(DistribuerInformasjonsbrevOvergangsordningNovember2024::class.java)

    @Transactional
    fun opprettTaskerForÅJournalføreOgSendeUtInformasjonsbrevOvergangsordningNovember2024() {
        val fagsakSomSkalHaInformasjonsbrevOvergangsordningNovember2024 =
            hentAlleFagsakSomSkalHaInformasjonsbrevOvergangsordningNovember2024()

        fagsakSomSkalHaInformasjonsbrevOvergangsordningNovember2024.forEach { fagsakId ->
            logger.info("Oppretter task for å journalføre og distribuere informasjonsbrev om overgangsordning november 2024 på fagsak $fagsakId")
            val task = SendInformasjonsbrevOvergangsordningNov2024Task.lagTask(fagsakId = fagsakId)

            taskService.save(task)
        }
    }

    fun hentAlleFagsakSomSkalHaInformasjonsbrevOvergangsordningNovember2024(): List<Long> {
        val alleFagsakSomHarBehandlingSomErSøktFørFebruar2024OgVedtattEtterFebruar2024 =
            behandlingRepository
                .finnBehandlingerSomErSøktFørFebruar2024OgVedtattEtterFebruar2024()
                .map { it.fagsak.id }
                .distinct()

        val sisteIverksatteBehandlingIHverFagsak =
            alleFagsakSomHarBehandlingSomErSøktFørFebruar2024OgVedtattEtterFebruar2024.mapNotNull {
                behandlingService.hentSisteBehandlingSomErIverksatt(it)
            }

        val sistIverksatteBehandlingerSomErAvgrensetGrunnetBarnetsAlder =
            sisteIverksatteBehandlingIHverFagsak
                .filter { sjekkOmKontantstøttenErAvkortetGrunnetBarnetsAlder(it) }

        return sistIverksatteBehandlingerSomErAvgrensetGrunnetBarnetsAlder.map { it.fagsak.id }
    }

    private fun sjekkOmKontantstøttenErAvkortetGrunnetBarnetsAlder(sistIverksatteBehandling: Behandling): Boolean {
        val andelerTilkjentYtelse = andelerTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sistIverksatteBehandling.id).ifEmpty { return false }

        val vilkårsvurderingIBehandling =
            vilkårsvurderingRepository.finnAktivForBehandling(behandlingId = sistIverksatteBehandling.id) ?: throw Feil(
                "Fant ikke vilkårsvurdering for iverksatt behandling ${sistIverksatteBehandling.id}",
            )

        val barnaOgBarnetsAlderVilkår =
            vilkårsvurderingIBehandling.personResultater
                .filter { !it.erSøkersResultater() }
                .groupBy { it.aktør }
                .mapValues {
                    it.value.flatMap { personResultat ->
                        personResultat.vilkårResultater.filter { vilkårResultat ->
                            vilkårResultat.vilkårType == Vilkår.BARNETS_ALDER
                        }
                    }
                }

        return barnaOgBarnetsAlderVilkår.any { (barn, barnetsAlderVilkår) ->
            val sisteUtbetalingTilBarn =
                andelerTilkjentYtelse.filter { it.aktør == barn }.maxOfOrNull { it.stønadTom } ?: return@any false
            val sisteTomBarnetsAlderTilBarn = barnetsAlderVilkår.maxOfOrNull { it.periodeTom!! } ?: return@any false

            val juli2024 = YearMonth.of(2024, 7)

            val avkortetPåGrunnAvBarnetsAlder = sisteUtbetalingTilBarn == sisteTomBarnetsAlderTilBarn.tilYearMonth()
            val sisteUtbetalingTilBarnErSenereEllerLikJuli2024 = sisteUtbetalingTilBarn >= juli2024

            avkortetPåGrunnAvBarnetsAlder && sisteUtbetalingTilBarnErSenereEllerLikJuli2024
        }
    }
}
