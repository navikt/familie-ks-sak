package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.micrometer.core.instrument.Metrics
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeIdLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.filtrerAndelerSomSkalSendesTilOppdrag
import no.nav.familie.ks.sak.kjerne.fagsak.domene.Fagsak
import no.nav.familie.restklient.client.RessursException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class UtbetalingsoppdragService(
    private val oppdragKlient: OppdragKlient,
    private val tilkjentYtelseValideringService: TilkjentYtelseValideringService,
    private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    private val sammeOppdragSendtKonflikt = Metrics.counter("familie.ks.sak.samme.oppdrag.sendt.konflikt")

    fun oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
        vedtak: Vedtak,
        saksbehandlerId: String,
    ): Utbetalingsoppdrag {
        val oppdatertBehandling = vedtak.behandling

        val utbetalingsoppdrag =
            genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak,
                saksbehandlerId,
            ).utbetalingsoppdrag.tilRestUtbetalingsoppdrag()

        tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(behandlingId = vedtak.behandling.id)

        iverksettOppdrag(utbetalingsoppdrag, oppdatertBehandling.id)
        return utbetalingsoppdrag
    }

    private fun iverksettOppdrag(
        utbetalingsoppdrag: Utbetalingsoppdrag,
        behandlingId: Long,
    ) {
        if (utbetalingsoppdrag.utbetalingsperiode.isEmpty()) {
            logger.warn("Iverksetter ikke noe mot oppdrag. Ingen utbetalingsperioder. behandlingId=$behandlingId")
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

    @Transactional
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        vedtak: Vedtak,
        saksbehandlerId: String,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = hentForrigeTilkjentYtelse(vedtak.behandling)
        val nyTilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandlingId = vedtak.behandling.id)

        val sisteAndelPerKjede = hentSisteAndelTilkjentYtelse(vedtak.behandling.fagsak)
        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId = saksbehandlerId,
                vedtak = vedtak,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                nyTilkjentYtelse = nyTilkjentYtelse,
                sisteAndelPerKjede = sisteAndelPerKjede,
                erSimulering = erSimulering,
            )

        beregnetUtbetalingsoppdrag.valider(behandlingsresultat = vedtak.behandling.resultat)

        if (!erSimulering) {
            oppdaterTilkjentYtelse(nyTilkjentYtelse, beregnetUtbetalingsoppdrag)
        }

        return beregnetUtbetalingsoppdrag
    }

    private fun oppdaterTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
    ) {
        secureLogger.info("Oppdaterer TilkjentYtelse med utbetalingsoppdrag og offsets på andeler for behandling ${tilkjentYtelse.behandling.id}")

        oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
            tilkjentYtelse = tilkjentYtelse,
            utbetalingsoppdrag = beregnetUtbetalingsoppdrag.utbetalingsoppdrag,
        )
        oppdaterAndelerMedPeriodeOffset(
            tilkjentYtelse = tilkjentYtelse,
            andelerMedPeriodeId = beregnetUtbetalingsoppdrag.andeler,
        )
        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    private fun hentForrigeTilkjentYtelse(behandling: Behandling): TilkjentYtelse? =
        behandlingService
            .hentSisteBehandlingSomErIverksatt(fagsakId = behandling.fagsak.id)
            ?.let { tilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(behandlingId = it.id) }

    private fun hentSisteAndelTilkjentYtelse(fagsak: Fagsak) =
        andelTilkjentYtelseRepository
            .hentSisteAndelPerIdent(fagsakId = fagsak.id)
            .associateBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }

    private fun utledOpphør(
        utbetalingsoppdrag: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag,
        behandling: Behandling,
    ): Opphør {
        val erRentOpphør =
            utbetalingsoppdrag.utbetalingsperiode.isNotEmpty() && utbetalingsoppdrag.utbetalingsperiode.all { it.opphør != null }
        var opphørsdato: LocalDate? = null
        if (erRentOpphør) {
            opphørsdato = utbetalingsoppdrag.utbetalingsperiode.minOf { it.opphør!!.opphørDatoFom }
        }

        if (behandling.type == BehandlingType.REVURDERING) {
            val opphørPåRevurdering = utbetalingsoppdrag.utbetalingsperiode.filter { it.opphør != null }
            if (opphørPåRevurdering.isNotEmpty()) {
                opphørsdato = opphørPåRevurdering.maxOfOrNull { it.opphør!!.opphørDatoFom }
            }
        }
        return Opphør(erRentOpphør = erRentOpphør, opphørsdato = opphørsdato)
    }

    private fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        tilkjentYtelse: TilkjentYtelse,
        utbetalingsoppdrag: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag,
    ) {
        val opphør = utledOpphør(utbetalingsoppdrag, tilkjentYtelse.behandling)

        tilkjentYtelse.utbetalingsoppdrag = jsonMapper.writeValueAsString(utbetalingsoppdrag)
        tilkjentYtelse.stønadTom = tilkjentYtelse.andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
        tilkjentYtelse.stønadFom =
            if (opphør.erRentOpphør) null else tilkjentYtelse.andelerTilkjentYtelse.minOfOrNull { it.stønadFom }
        tilkjentYtelse.endretDato = LocalDate.now()
        tilkjentYtelse.opphørFom = opphør.opphørsdato?.toYearMonth()
    }

    private fun oppdaterAndelerMedPeriodeOffset(
        tilkjentYtelse: TilkjentYtelse,
        andelerMedPeriodeId: List<AndelMedPeriodeIdLongId>,
    ) {
        val andelerSomSkalSendesTilOppdrag = tilkjentYtelse.andelerTilkjentYtelse.filtrerAndelerSomSkalSendesTilOppdrag()

        if (andelerMedPeriodeId.size != andelerSomSkalSendesTilOppdrag.size) {
            logger.warn("Uventet antall andeler fra utbetalingsgenerator. Se secureLogger for informasjon.")
            secureLogger.warn("Uventet antall andeler fra utbetalingsgenerator. Andeler fra utbetalingsgenerator: $andelerMedPeriodeId, forventede andeler: $andelerSomSkalSendesTilOppdrag")
            throw Feil("Antallet andeler med oppdatert periodeOffset, forrigePeriodeOffset og kildeBehandlingId fra ny generator skal være likt antallet ordinære andeler med kalkulertUtbetalingsbeløp != 0 + antall barn med overgangsordning. Generator gir ${andelerMedPeriodeId.size} andeler men det er ${andelerSomSkalSendesTilOppdrag.size} andeler med kalkulertUtbetalingsbeløp != 0")
        }
        andelerSomSkalSendesTilOppdrag.forEach { andel ->
            val andelMedOffset =
                andelerMedPeriodeId.find { it.id == andel.id }
                    ?: throw Feil("Feil ved oppdaterig av offset på andeler. Finner ikke andel med id ${andel.id} blandt andelene med oppdatert offset fra ny generator. Ny generator returnerer andeler med ider [${andelerMedPeriodeId.map { it.id }}]")
            andel.periodeOffset = andelMedOffset.periodeId
            andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
            andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
        }
    }

    data class Opphør(
        val erRentOpphør: Boolean,
        val opphørsdato: LocalDate?,
    )

    companion object {
        val logger = LoggerFactory.getLogger(UtbetalingsoppdragService::class.java)
        val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")
    }
}
