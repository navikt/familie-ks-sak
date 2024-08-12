package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak.LOVENDRING_2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = AutovedtakLovendringOpphørteSaker.TASK_STEP_TYPE,
    beskrivelse = "Trigger autovedtak av lovendring",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class AutovedtakLovendringOpphørteSaker(
    val autovedtakLovendringService: AutovedtakLovendringService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vilkårsvurderingService: VilkårsvurderingService,
    val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    val vedtakService: VedtakService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val august2024 = YearMonth.of(2024, 8)
    private val førsteJuni2024 = LocalDateTime.of(2024, 6, 1, 0, 0)

    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()

        if (behandlingService.hentBehandlingerPåFagsak(fagsakId).any { it.opprettetÅrsak == LOVENDRING_2024 }) {
            logger.info("Lovendring 2024 allerede kjørt for fagsakId=$fagsakId")
        } else {
            val sisteIverksatteBehandling =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId)
                    ?: throw Feil("Fant ingen aktiv behandling for fagsak $fagsakId")
            val vilkårsvurdering =
                vilkårsvurderingService.finnAktivVilkårsvurdering(sisteIverksatteBehandling.id)
                    ?: throw Feil("Fant ingen vilkårsvurdering for behandling ${sisteIverksatteBehandling.id}")

            validerIngenAndelerEtterJuli20204(sisteIverksatteBehandling)

            validerVedtattEtterJuni2024(sisteIverksatteBehandling)

            validerIkkeFremtidigOpphør(vilkårsvurdering, sisteIverksatteBehandling, fagsakId)

            autovedtakLovendringService.revurderFagsak(fagsakId = fagsakId)
        }
    }

    // Dersom det er andeler etter i august eller senere er ikke behandlingen opphørt
    private fun validerIngenAndelerEtterJuli20204(sisteIverksatteBehandling: Behandling) {
        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(sisteIverksatteBehandling.id)
        if (andeler.any { it.stønadFom >= august2024 }) {
            error("Har andeler i august eller senere på Behandling=${sisteIverksatteBehandling.id}")
        }
    }

    private fun validerVedtattEtterJuni2024(sisteIverksatteBehandling: Behandling) {
        val vedtak = vedtakService.hentAktivVedtakForBehandling(sisteIverksatteBehandling.id)
        if (vedtak.vedtaksdato != null && vedtak.vedtaksdato!! < førsteJuni2024) {
            error("Vedtak=${vedtak.id} har vedtaksdato før juni 2024")
        }
    }

    private fun validerIkkeFremtidigOpphør(
        vilkårsvurdering: Vilkårsvurdering,
        sisteIverksatteBehandling: Behandling,
        fagsakId: Long,
    ) {
        val vilkårResultaterPåBehandling = vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }

        val behandlingHarFremtidigOpphør =
            vilkårResultaterPåBehandling.filter { it.vilkårType == Vilkår.BARNEHAGEPLASS }.any {
                it.søkerHarMeldtFraOmBarnehageplass == true
            }
        if (behandlingHarFremtidigOpphør) error("Siste iverksatte behandling=${sisteIverksatteBehandling.id} på fagsak=$fagsakId har fremtidig opphør.")
    }

    companion object {
        const val TASK_STEP_TYPE = "autovedtakLovendringOpphørteSaker"

        fun opprettTask(
            fagsakId: Long,
        ): Task =
            Task(
                type = TASK_STEP_TYPE,
                payload = fagsakId.toString(),
                properties =
                    Properties().apply {
                        this["fagsakId"] = fagsakId.toString()
                    },
            )
    }
}
