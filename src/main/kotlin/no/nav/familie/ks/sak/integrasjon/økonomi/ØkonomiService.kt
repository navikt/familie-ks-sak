package no.nav.familie.ks.sak.integrasjon.økonomi

import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.config.FeatureToggleService
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class ØkonomiService(
    private val oppdragKlient: OppdragKlient,
    private val beregningService: BeregningService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val behandlingService: BehandlingService,
    private val featureToggleService: FeatureToggleService,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService

) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ba.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling
        val utbetalingsoppdrag = genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
            vedtak,
            saksbehandlerId,
            andelTilkjentYtelseForUtbetalingsoppdragFactory
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(oppdatertBehandling, utbetalingsoppdrag)

        tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId = vedtak.behandling.id)
        iverksettOppdrag(utbetalingsoppdrag, oppdatertBehandling.id)
        return utbetalingsoppdrag
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag, behandlingId: Long) {
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) {
            UtbetalingsoppdragService.logger.warn(
                "Iverksetter ikke noe mot oppdrag. " +
                    "Ingen utbetalingsperioder for behandlingId=$behandlingId"
            )
            return
        }
        try {
            oppdragKlient.iverksettOppdrag(utbetalingsoppdrag)
        } catch (exception: Exception) {
            if (exception is RessursException &&
                exception.httpStatus == HttpStatus.CONFLICT
            ) {
                sammeOppdragSendtKonflikt.increment()
                logger.info("Bypasset feil med HttpKode 409 ved iverksetting mot økonomi for fagsak ${utbetalingsoppdrag.saksnummer}")
                return
            } else {
                throw exception
            }
        }
    }

    fun hentStatus(oppdragId: OppdragId, behandlingId: Long): OppdragStatus {
        val andelerTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId)
        if (andelerTilkjentYtelse.any { it.erAndelSomSkalSendesTilOppdrag() }) {
            return oppdragKlient.hentStatus(oppdragId)
        }
        return OppdragStatus.KVITTERT_OK // sendte ikke data til økonomi
    }

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
        erSimulering: Boolean = false,
        skalValideres: Boolean = true
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling
        val oppdatertTilstand =
            beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(behandlingId = oppdatertBehandling.id)
                .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

        val oppdaterteKjeder = kjedeinndelteAndeler(oppdatertTilstand)

        val erFørsteIverksatteBehandlingPåFagsak =
            beregningService.hentTilkjentYtelseForBehandlingerIverksattMotØkonomi(fagsakId = oppdatertBehandling.fagsak.id)
                .isEmpty()

        val utbetalingsoppdrag = if (erFørsteIverksatteBehandlingPåFagsak) {
            utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering
            )
        } else {
            val forrigeBehandling =
                behandlingHentOgPersisterService.hentForrigeBehandlingSomErIverksatt(behandling = oppdatertBehandling)
                    ?: error("Finner ikke forrige behandling ved oppdatering av tilkjent ytelse og iverksetting av vedtak")

            val forrigeTilstand =
                beregningService.hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(forrigeBehandling.id)
                    .pakkInnForUtbetaling(andelTilkjentYtelseForUtbetalingsoppdragFactory)

            val forrigeKjeder = kjedeinndelteAndeler(forrigeTilstand)

            val sisteOffsetPerIdent = beregningService.hentSisteOffsetPerIdent(
                forrigeBehandling.fagsak.id,
                andelTilkjentYtelseForUtbetalingsoppdragFactory
            )

            val sisteOffsetPåFagsak = beregningService.hentSisteOffsetPåFagsak(behandling = oppdatertBehandling)

            if (oppdatertTilstand.isNotEmpty()) {
                oppdaterBeståendeAndelerMedOffset(oppdaterteKjeder = oppdaterteKjeder, forrigeKjeder = forrigeKjeder)
                val tilkjentYtelseMedOppdaterteAndeler = oppdatertTilstand.first().tilkjentYtelse
                beregningService.lagreTilkjentYtelseMedOppdaterteAndeler(tilkjentYtelseMedOppdaterteAndeler)
            }

            val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                erFørsteBehandlingPåFagsak = erFørsteIverksatteBehandlingPåFagsak,
                forrigeKjeder = forrigeKjeder,
                sisteOffsetPerIdent = sisteOffsetPerIdent,
                sisteOffsetPåFagsak = sisteOffsetPåFagsak,
                oppdaterteKjeder = oppdaterteKjeder,
                erSimulering = erSimulering,
                endretMigreringsDato = beregnOmMigreringsDatoErEndret(
                    vedtak.behandling,
                    forrigeTilstand.minByOrNull { it.stønadFom }?.stønadFom
                )
            )

            if (!erSimulering && (
                oppdatertBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT || behandlingHentOgPersisterService.hent(
                        oppdatertBehandling.id
                    ).resultat == Behandlingsresultat.OPPHØRT
                )
            ) {
                utbetalingsoppdrag.validerOpphørsoppdrag()
            }

            utbetalingsoppdrag
        }

        return utbetalingsoppdrag.also {
            if (skalValideres) {
                if (featureToggleService.isEnabled(
                        FeatureToggleConfig.KAN_GENERERE_UTBETALINGSOPPDRAG_NY_VALIDERING,
                        false
                    )
                ) {
                    it.valider(
                        behandlingsresultat = vedtak.behandling.resultat,
                        behandlingskategori = vedtak.behandling.kategori,
                        andelerTilkjentYtelse = beregningService.hentAndelerTilkjentYtelseForBehandling(vedtak.behandling.id),
                        erEndreMigreringsdatoBehandling = vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                    )
                } else {
                    it.valider(
                        behandlingsresultat = vedtak.behandling.resultat,
                        erEndreMigreringsdatoBehandling = vedtak.behandling.opprettetÅrsak == BehandlingÅrsak.ENDRE_MIGRERINGSDATO
                    )
                }
            }
        }
    }

    private fun beregnOmMigreringsDatoErEndret(behandling: Behandling, forrigeTilstandFraDato: YearMonth?): YearMonth? {
        val erMigrertSak =
            behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato = behandlingService.hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
            ?.toYearMonth()
            ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null &&
            nyttTilstandFraDato != null &&
            forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)
        ) {
            nyttTilstandFraDato
        } else {
            null
        }
    }

    companion object {

        val logger = LoggerFactory.getLogger(ØkonomiService::class.java)
    }
}

fun Utbetalingsoppdrag.harLøpendeUtbetaling() =
    this.utbetalingsperiode.any {
        it.opphør == null &&
            it.sats > BigDecimal.ZERO &&
            it.vedtakdatoTom > LocalDate.now().sisteDagIMåned()
    }
