package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import io.micrometer.core.instrument.Metrics
import jakarta.transaction.Transactional
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.ks.sak.api.dto.SimuleringResponsDto
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.tilRestUtbetalingsoppdrag
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottakerRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class SimuleringService(
    private val oppdragKlient: OppdragKlient,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val beregningService: BeregningService,
    private val øknomiSimuleringMottakerRepository: ØkonomiSimuleringMottakerRepository,
    private val vedtakRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    private val simulert = Metrics.counter("familie.ks.sak.oppdrag.simulert")

    @Transactional
    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingErFerdigBesluttet =
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(simulering.tilSimuleringDto())) {
            oppdaterSimuleringPåBehandling(behandling)
        } else {
            simulering
        }
    }

    fun hentEtterbetaling(behandlingId: Long): BigDecimal {
        val simuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return simuleringMottakere.tilSimuleringDto().etterbetaling
    }

    fun hentFeilutbetaling(behandlingId: Long): BigDecimal {
        val simuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return simuleringMottakere.tilSimuleringDto().feilutbetaling
    }

    fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean = hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> = øknomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)

    fun oppdaterSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        return oppdaterSimuleringPåBehandling(behandling)
    }

    fun oppdaterSimuleringPåBehandling(behandling: Behandling): List<ØkonomiSimuleringMottaker> {
        val aktivtVedtak =
            vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)
                ?: throw Feil("Fant ikke aktivt vedtak på behandling${behandling.id}")

        val simulering: List<SimuleringMottaker> =
            hentSimuleringFraFamilieOppdrag(vedtak = aktivtVedtak)?.simuleringMottaker ?: emptyList()

        slettSimuleringPåBehandling(behandling.id)
        return lagreSimuleringPåBehandling(simulering, behandling)
    }

    private fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {
        if (!beregningService.sjekkOmDetErEndringIUtbetalingFraForrigeBehandlingSendtTilØkonomi(behandling = vedtak.behandling)) {
            return null
        }

        val utbetalingsoppdrag =
            utbetalingsoppdragService
                .genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                    vedtak = vedtak,
                    saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                    erSimulering = true,
                ).utbetalingsoppdrag
                .tilRestUtbetalingsoppdrag()

        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) return null

        simulert.increment()
        return oppdragKlient.hentSimulering(utbetalingsoppdrag)
    }

    private fun simuleringErUtdatert(simulering: SimuleringResponsDto) =
        simulering.tidSimuleringHentet == null ||
            (
                simulering.forfallsdatoNestePeriode != null &&
                    simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode &&
                    LocalDate.now() > simulering.forfallsdatoNestePeriode
            )

    private fun lagreSimuleringPåBehandling(
        simuleringMottakere: List<SimuleringMottaker>,
        behandling: Behandling,
    ): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(behandling) }
        return øknomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    private fun slettSimuleringPåBehandling(behandlingId: Long) = øknomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)
}
