package no.nav.familie.ks.sak.kjerne.behandling.steg.simulering

import io.micrometer.core.instrument.Metrics
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.ks.sak.api.dto.SimuleringDto
import no.nav.familie.ks.sak.api.mapper.SimuleringMapper.tilSimuleringDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.config.BehandlerRolle
import no.nav.familie.ks.sak.config.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØknomiSimuleringMottakerRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ks.sak.sikkerhet.TilgangService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import javax.transaction.Transactional

@Service
class SimuleringService(
    private val oppdragKlient: OppdragKlient,
    private val utbetalingsoppdragService: UtbetalingsoppdragService,
    private val beregningService: BeregningService,
    private val øknomiSimuleringMottakerRepository: ØknomiSimuleringMottakerRepository,
    private val tilgangService: TilgangService,
    private val featureToggleService: FeatureToggleService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository
) {
    private val simulert = Metrics.counter("familie.ks.sak.oppdrag.simulert")

    fun hentSimuleringFraFamilieOppdrag(vedtak: Vedtak): DetaljertSimuleringResultat? {
        if (vedtak.behandling.resultat == Behandlingsresultat.FORTSATT_INNVILGET ||
            vedtak.behandling.resultat == Behandlingsresultat.AVSLÅTT ||
            beregningService.innvilgetSøknadUtenUtbetalingsperioderGrunnetEndringsPerioder(behandling = vedtak.behandling)
        ) {
            return null
        }

        /**
         * SOAP integrasjonen støtter ikke full epost som MQ,
         * så vi bruker bare første 8 tegn av saksbehandlers epost for simulering.
         * Denne verdien brukes ikke til noe i simulering.
         */

        val tilkjentYtelse = utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak = vedtak,
            saksbehandlerId = SikkerhetContext.hentSaksbehandler().take(8),
            andelTilkjentYtelseForUtbetalingsoppdragFactory = AndelTilkjentYtelseForSimuleringFactory(),
            erSimulering = true
        )

        val utbetalingsoppdrag =
            objectMapper.readValue(tilkjentYtelse.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)

        // Simulerer ikke mot økonomi når det ikke finnes utbetalingsperioder
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) return null

        simulert.increment()
        return oppdragKlient.hentSimulering(utbetalingsoppdrag)
    }

    @Transactional
    fun lagreSimuleringPåBehandling(
        simuleringMottakere: List<SimuleringMottaker>,
        beahndling: Behandling
    ): List<ØkonomiSimuleringMottaker> {
        val vedtakSimuleringMottakere = simuleringMottakere.map { it.tilBehandlingSimuleringMottaker(beahndling) }
        return øknomiSimuleringMottakerRepository.saveAll(vedtakSimuleringMottakere)
    }

    @Transactional
    fun slettSimuleringPåBehandling(behandlingId: Long) =
        øknomiSimuleringMottakerRepository.deleteByBehandlingId(behandlingId)

    fun hentSimuleringPåBehandling(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        return øknomiSimuleringMottakerRepository.findByBehandlingId(behandlingId)
    }

    fun oppdaterSimuleringPåBehandlingVedBehov(behandlingId: Long): List<ØkonomiSimuleringMottaker> {
        val behandling = behandlingRepository.hentBehandling(behandlingId)
        val behandlingErFerdigBesluttet =
            behandling.status == BehandlingStatus.IVERKSETTER_VEDTAK ||
                behandling.status == BehandlingStatus.AVSLUTTET

        val simulering = hentSimuleringPåBehandling(behandlingId)
        val simuleringDto = simulering.tilSimuleringDto()

        return if (!behandlingErFerdigBesluttet && simuleringErUtdatert(simuleringDto)) {
            oppdaterSimuleringPåBehandling(behandling)
        } else {
            simulering
        }
    }

    private fun simuleringErUtdatert(simulering: SimuleringDto) =
        simulering.tidSimuleringHentet == null ||
            (
                simulering.forfallsdatoNestePeriode != null &&
                    simulering.tidSimuleringHentet < simulering.forfallsdatoNestePeriode &&
                    LocalDate.now() > simulering.forfallsdatoNestePeriode
                )

    @Transactional
    fun oppdaterSimuleringPåBehandling(behandling: Behandling): List<ØkonomiSimuleringMottaker> {
        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktivOptional(behandling.id)
            ?: throw Feil("Fant ikke aktivt vedtak på behandling${behandling.id}")
        tilgangService.validerTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette simulering"
        )

        val simulering: List<SimuleringMottaker> =
            hentSimuleringFraFamilieOppdrag(vedtak = aktivtVedtak)?.simuleringMottaker ?: emptyList()

        slettSimuleringPåBehandling(behandling.id)
        return lagreSimuleringPåBehandling(simulering, behandling)
    }

    fun hentEtterbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return hentEtterbetaling(vedtakSimuleringMottakere)
    }

    fun hentFeilutbetaling(behandlingId: Long): BigDecimal {
        val vedtakSimuleringMottakere = hentSimuleringPåBehandling(behandlingId)
        return hentFeilutbetaling(vedtakSimuleringMottakere)
    }

    fun hentEtterbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal {
        return økonomiSimuleringMottakere.tilSimuleringDto().etterbetaling
    }

    fun hentFeilutbetaling(økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>): BigDecimal {
        return økonomiSimuleringMottakere.tilSimuleringDto().feilutbetaling
    }
}
