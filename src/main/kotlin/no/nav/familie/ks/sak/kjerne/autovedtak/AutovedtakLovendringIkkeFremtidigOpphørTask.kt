package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ks.sak.kjerne.fagsak.domene.FagsakStatus
import no.nav.familie.ks.sak.statistikk.stønadsstatistikk.PubliserVedtakTask.Companion.TASK_STEP_TYPE
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = AutovedtakLovendringIkkeFremtidigOpphørTask.TASK_STEP_TYPE,
    beskrivelse = "Trigger autovedtak av lovendring",
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
)
class AutovedtakLovendringIkkeFremtidigOpphørTask(
    val autovedtakLovendringService: AutovedtakLovendringService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vilkårsvurderingService: VilkårsvurderingService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val fagsakId = task.payload.toLong()
        val fagsak = fagsakService.hentFagsak(fagsakId)
        if (fagsak.status != FagsakStatus.LØPENDE) {
            throw Feil("Fagsak $fagsakId er ikke løpende")
        }

        if (behandlingService.hentBehandlingerPåFagsak(fagsakId).any { it.opprettetÅrsak == BehandlingÅrsak.LOVENDRING_2024 }) {
            logger.info("Lovendring 2024 allerede kjørt for fagsakId=$fagsakId")
        } else {
            val sisteIverksatteBehandling =
                behandlingService.hentSisteBehandlingSomErIverksatt(fagsakId)
                    ?: throw Feil("Fant ingen aktiv behandling for fagsak $fagsakId")
            val vilkårsvurdering =
                vilkårsvurderingService.finnAktivVilkårsvurdering(sisteIverksatteBehandling.id)
                    ?: throw Feil("Fant ingen vilkårsvurdering for behandling ${sisteIverksatteBehandling.id}")

            val vilkårResultaterPåBehandling = vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }

            val behandlingHarFremtidigOpphør = vilkårResultaterPåBehandling.any { it.søkerHarMeldtFraOmBarnehageplass == true }
            check(behandlingHarFremtidigOpphør) { "Siste iverksatte behandling=${sisteIverksatteBehandling.id} på fagsak=$fagsakId har fremtidig opphør." }

            autovedtakLovendringService.revurderFagsak(fagsakId = fagsakId)
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakLovendringIkkeFremtidigOpphørTask::class.java)
        const val TASK_STEP_TYPE = "autovedtakLovendringIkkeFremtidigOpphør"

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
