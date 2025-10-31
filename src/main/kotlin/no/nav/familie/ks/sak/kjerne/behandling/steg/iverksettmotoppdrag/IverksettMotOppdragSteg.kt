package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ks.sak.api.dto.BehandlingStegDto
import no.nav.familie.ks.sak.api.dto.IverksettMotOppdragDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.TaskRepositoryWrapper
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.IBehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IverksettMotOppdragSteg(
    private val behandlingService: BehandlingService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val vedtakService: VedtakService,
    private val taskService: TaskRepositoryWrapper,
) : IBehandlingSteg {
    private val iverksattOppdrag = Metrics.counter("familie.ks.sak.oppdrag.iverksatt")

    override fun getBehandlingssteg(): BehandlingSteg = BehandlingSteg.IVERKSETT_MOT_OPPDRAG

    @Transactional
    override fun utførSteg(
        behandlingId: Long,
        behandlingStegDto: BehandlingStegDto,
    ) {
        logger.info("Utfører steg ${getBehandlingssteg().name} for behandling $behandlingId")

        val behandling = behandlingService.hentBehandling(behandlingId)

        tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(behandling)
        validerTotrinnskontrollForBehandling(behandling)

        val vedtak = vedtakService.hentAktivVedtakForBehandling(behandlingId)

        utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtak,
            saksbehandlerId = (behandlingStegDto as IverksettMotOppdragDto).saksbehandlerId,
        )

        iverksattOppdrag.increment()

        // Opprett task for å sende vedtak hendelse til infotrygd
        val sisteBehandlingSomErVedtatt = behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id)
        if (sisteBehandlingSomErVedtatt == null) {
            taskService.save(
                SendVedtakHendelseTilInfotrygdTask.opprettTask(behandling.fagsak.aktør.aktivFødselsnummer(), behandlingId),
            )
        }
    }

    fun validerTotrinnskontrollForBehandling(behandling: Behandling) =
        totrinnskontrollService.hentAktivForBehandling(behandlingId = behandling.id).also {
            if (it.erUgyldig()) {
                throw Feil(
                    message = "Totrinnskontroll($it) er ugyldig ved iverksetting",
                    frontendFeilmelding = "Totrinnskontroll er ugyldig ved iverksetting",
                )
            }

            if (!it.godkjent) {
                throw Feil(
                    message = "Prøver å iverksette et underkjent vedtak",
                    frontendFeilmelding = "",
                )
            }
        }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IverksettMotOppdragSteg::class.java)
    }
}
