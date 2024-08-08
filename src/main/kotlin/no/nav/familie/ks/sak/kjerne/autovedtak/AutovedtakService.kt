package no.nav.familie.ks.sak.kjerne.autovedtak

import no.nav.familie.ks.sak.api.dto.OpprettBehandlingDto
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.OpprettBehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.Beslutning
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.springframework.stereotype.Service

@Service
class AutovedtakService(
    private val stegService: StegService,
    private val behandlingService: BehandlingService,
    private val opprettBehandlingService: OpprettBehandlingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val loggService: LoggService,
    private val vedtakService: VedtakService,
) {
    fun opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
        aktør: Aktør,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak,
    ): Behandling {
        val nyBehandling =
            opprettBehandlingService.opprettBehandling(
                OpprettBehandlingDto(
                    søkersIdent = aktør.aktivFødselsnummer(),
                    behandlingType = behandlingType,
                    behandlingÅrsak = behandlingÅrsak,
                ),
            )

        stegService.utførSteg(behandlingId = nyBehandling.id, behandlingSteg = BehandlingSteg.VILKÅRSVURDERING)
        stegService.utførSteg(behandlingId = nyBehandling.id, behandlingSteg = BehandlingSteg.BEHANDLINGSRESULTAT)

        return behandlingService.hentBehandling(behandlingId = nyBehandling.id)
    }

    fun opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)

        loggService.opprettBeslutningOmVedtakLogg(
            behandling = behandling,
            beslutning = Beslutning.GODKJENT,
            begrunnelse = null,
        )

        return vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling)
    }
}
