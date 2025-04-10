package no.nav.familie.ks.sak.statistikk.saksstatistikk

import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.EksternBehandlingRelasjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component
class RelatertBehandlingUtleder(
    @Lazy private val behandlingService: BehandlingService,
    private val eksternBehandlingRelasjonService: EksternBehandlingRelasjonService,
    private val unleashService: UnleashNextMedContextService,
) {
    private val logger: Logger = LoggerFactory.getLogger(RelatertBehandlingUtleder::class.java)

    fun utledRelatertBehandling(behandling: Behandling): RelatertBehandling? {
        if (behandling.erRevurderingKlage() && unleashService.isEnabled(FeatureToggle.SETT_RELATERT_BEHANDLING_FOR_REVURDERING_KLAGE_I_SAKSSTATISTIKK, false)) {
            val eksternKlagebehandlingRelasjon =
                eksternBehandlingRelasjonService.finnEksternBehandlingRelasjon(
                    behandlingId = behandling.id,
                    fagsystem = EksternBehandlingRelasjon.Fagsystem.KLAGE,
                )
            if (eksternKlagebehandlingRelasjon == null) {
                logger.warn("Forventer en ekstern klagebehandling relasjon for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
                return null
            }
            return RelatertBehandling.fraEksternBehandlingRelasjon(eksternKlagebehandlingRelasjon)
        }

        if (behandling.erRevurderingKlage()) {
            return null
        }

        if (behandling.erRevurderingEllerTekniskEndring()) {
            val forrigeVedtatteKontantstøttebehandling = behandlingService.hentForrigeBehandlingSomErVedtatt(behandling)
            if (forrigeVedtatteKontantstøttebehandling == null) {
                logger.warn("Forventer en vedtatt kontantstøttebehandling for fagsak ${behandling.fagsak.id} og behandling ${behandling.id}")
                return null
            }
            return RelatertBehandling.fraKontantstøttebehandling(forrigeVedtatteKontantstøttebehandling)
        }

        return null
    }
}
