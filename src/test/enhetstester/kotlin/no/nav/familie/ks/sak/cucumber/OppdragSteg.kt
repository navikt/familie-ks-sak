package no.nav.familie.ks.sak.cucumber

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelMedPeriodeId
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.ks.sak.common.domeneparser.DomeneparserUtil.groupByBehandlingId
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.cucumber.OppdragParser.mapTilkjentYtelse
import no.nav.familie.ks.sak.cucumber.ValideringUtil.assertSjekkBehandlingIder
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragGenerator
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.tilRestUtbetalingsoppdrag
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.filtrerAndelerSomSkalSendesTilOppdrag
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory

@Suppress("ktlint:standard:function-naming")
class OppdragSteg {
    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator()
    private var behandlinger = mapOf<Long, Behandling>()
    private var tilkjenteYtelser = mutableMapOf<Long, TilkjentYtelse>()
    private var beregnetUtbetalingsoppdrag = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var beregnetUtbetalingsoppdragSimulering = mutableMapOf<Long, BeregnetUtbetalingsoppdragLongId>()
    private var kastedeFeil = mutableMapOf<Long, Exception>()

    private val logger = LoggerFactory.getLogger(javaClass)

    @Gitt("følgende tilkjente ytelser")
    fun følgendeTilkjenteYtelser(dataTable: DataTable) {
        genererBehandlinger(dataTable)
        tilkjenteYtelser = mapTilkjentYtelse(dataTable, behandlinger)
        if (tilkjenteYtelser.flatMap { (_, tilkjentYtelse) -> tilkjentYtelse.andelerTilkjentYtelse }.any { it.kildeBehandlingId != null }) {
            error("Kildebehandling skal ikke settes på input, denne settes fra utbetalingsgeneratorn")
        }
    }

    @Når("beregner utbetalingsoppdrag")
    fun `beregner utbetalingsoppdrag`() {
        tilkjenteYtelser.values.fold(emptyList<TilkjentYtelse>()) { acc, tilkjentYtelse ->
            val behandlingId = tilkjentYtelse.behandling.id
            try {
                genererUtbetalingsoppdragForSimuleringNy(behandlingId, acc, tilkjentYtelse)
                beregnetUtbetalingsoppdrag[behandlingId] = beregnUtbetalingsoppdrag(acc, tilkjentYtelse)
                oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
                    beregnetUtbetalingsoppdrag[behandlingId]!!,
                    tilkjentYtelse,
                )
            } catch (e: Exception) {
                logger.error("Feilet beregning av oppdrag for behandling=$behandlingId")
                kastedeFeil[behandlingId] = e
            }
            acc + tilkjentYtelse
        }
    }

    private fun genererUtbetalingsoppdragForSimuleringNy(
        behandlingId: Long,
        tilkjenteYtelser: List<TilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
    ) {
        try {
            beregnetUtbetalingsoppdragSimulering[behandlingId] =
                beregnUtbetalingsoppdrag(tilkjenteYtelser, tilkjentYtelse, erSimulering = true)
        } catch (e: Exception) {
            logger.error("Feilet beregning av oppdrag ved simulering for behandling=$behandlingId")
            kastedeFeil[behandlingId] = e
        }
    }

    private fun oppdaterTilkjentYtelseMedUtbetalingsoppdrag(
        beregnetUtbetalingsoppdragLongId: BeregnetUtbetalingsoppdragLongId,
        tilkjentYtelse: TilkjentYtelse,
    ) {
        tilkjentYtelse.andelerTilkjentYtelse.forEach { andel ->
            val andelMedOppdatertOffset = beregnetUtbetalingsoppdragLongId.andeler.find { it.id == andel.id }
            if (andelMedOppdatertOffset != null) {
                andel.periodeOffset = andelMedOppdatertOffset.periodeId
                andel.forrigePeriodeOffset = andelMedOppdatertOffset.forrigePeriodeId
                andel.kildeBehandlingId = andelMedOppdatertOffset.kildeBehandlingId
            }
        }
    }

    private fun beregnUtbetalingsoppdrag(
        acc: List<TilkjentYtelse>,
        tilkjentYtelse: TilkjentYtelse,
        erSimulering: Boolean = false,
    ): BeregnetUtbetalingsoppdragLongId {
        val forrigeTilkjentYtelse = acc.lastOrNull()

        val vedtak = lagVedtak(behandling = tilkjentYtelse.behandling)
        val sisteAndelPerIdent = sisteAndelPerIdent(acc)
        return utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
            saksbehandlerId = "saksbehandlerId",
            vedtak = vedtak,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            sisteAndelPerKjede = sisteAndelPerIdent,
            nyTilkjentYtelse = tilkjentYtelse,
            erSimulering = erSimulering,
        )
    }

    @Så("forvent at en exception kastes for behandling {long}")
    fun `forvent at en exception kastes for behandling`(behandlingId: Long) {
        assertThat(kastedeFeil).isNotEmpty
        assertThat(kastedeFeil[behandlingId]).isNotNull
    }

    @Så("forvent følgende utbetalingsoppdrag")
    fun `forvent følgende utbetalingsoppdrag`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(
            dataTable,
            beregnetUtbetalingsoppdrag
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdrag
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
    }

    @Så("forvent følgende oppdaterte andeler")
    fun `forvent følgende oppdaterte andeler`(dataTable: DataTable) {
        validerForventedeOppdaterteAndeler(dataTable, tilkjenteYtelser)
    }

    private fun validerForventedeOppdaterteAndeler(
        dataTable: DataTable,
        tilkjenteYtelser: MutableMap<Long, TilkjentYtelse>,
    ) {
        val forventedeOppdaterteAndelerPerBehandling =
            OppdragParser.mapForventedeAndelerMedPeriodeId(
                dataTable,
            )

        forventedeOppdaterteAndelerPerBehandling.forEach { (behandlingId, forventedeAndelerMedPeriodeId) ->
            val tilkjentYtelse = tilkjenteYtelser[behandlingId] ?: throw Feil("Mangler TilkjentYtelse for behandling $behandlingId")
            val andelerMedPeriodeId =
                tilkjentYtelse.andelerTilkjentYtelse.filtrerAndelerSomSkalSendesTilOppdrag().map {
                    AndelMedPeriodeId(
                        id = it.id.toString(),
                        periodeId = it.periodeOffset!!,
                        forrigePeriodeId = it.forrigePeriodeOffset,
                        kildeBehandlingId = it.kildeBehandlingId!!.toString(),
                    )
                }
            try {
                assertThat(andelerMedPeriodeId).isEqualTo(forventedeAndelerMedPeriodeId)
            } catch (e: Exception) {
                logger.error("Feilet validering av oppdaterte andeler for behandling $behandlingId", e)
                throw e
            }
        }
    }

    @Så("forvent følgende simulering")
    fun `forvent følgende simulering`(dataTable: DataTable) {
        validerForventetUtbetalingsoppdrag(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
        assertSjekkBehandlingIder(
            dataTable,
            beregnetUtbetalingsoppdragSimulering
                .mapValues { it.value.utbetalingsoppdrag.tilRestUtbetalingsoppdrag() }
                .toMutableMap(),
        )
    }

    private fun validerForventetUtbetalingsoppdrag(
        dataTable: DataTable,
        beregnetUtbetalingsoppdrag: MutableMap<Long, Utbetalingsoppdrag>,
    ) {
        val forventedeUtbetalingsoppdrag =
            OppdragParser.mapForventetUtbetalingsoppdrag(
                dataTable,
            )
        forventedeUtbetalingsoppdrag.forEach { forventetUtbetalingsoppdrag ->
            val behandlingId = forventetUtbetalingsoppdrag.behandlingId
            val utbetalingsoppdrag =
                beregnetUtbetalingsoppdrag[behandlingId]
                    ?: error("Mangler utbetalingsoppdrag for $behandlingId")
            try {
                assertUtbetalingsoppdrag(forventetUtbetalingsoppdrag, utbetalingsoppdrag)
            } catch (e: Throwable) {
                logger.error("Feilet validering av behandling $behandlingId")
                throw e
            }
        }
    }

    private fun genererBehandlinger(dataTable: DataTable) {
        val fagsak = lagFagsak()
        behandlinger =
            dataTable
                .groupByBehandlingId()
                .map { lagBehandling(fagsak = fagsak).copy(id = it.key) }
                .associateBy { it.id }
    }

    private fun sisteAndelPerIdent(tilkjenteYtelser: List<TilkjentYtelse>): Map<IdentOgType, AndelTilkjentYtelse> =
        tilkjenteYtelser
            .flatMap { it.andelerTilkjentYtelse }
            .groupBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }
            .mapValues { it.value.maxBy { it.periodeOffset ?: 0 } }

    private fun assertUtbetalingsoppdrag(
        forventetUtbetalingsoppdrag: ForventetUtbetalingsoppdrag,
        utbetalingsoppdrag: Utbetalingsoppdrag,
        medUtbetalingsperiode: Boolean = true,
    ) {
        assertThat(utbetalingsoppdrag.kodeEndring).isEqualTo(forventetUtbetalingsoppdrag.kodeEndring)
        assertThat(utbetalingsoppdrag.utbetalingsperiode).hasSize(forventetUtbetalingsoppdrag.utbetalingsperiode.size)
        if (medUtbetalingsperiode) {
            forventetUtbetalingsoppdrag.utbetalingsperiode.forEachIndexed { index, forventetUtbetalingsperiode ->
                val utbetalingsperiode = utbetalingsoppdrag.utbetalingsperiode[index]
                try {
                    assertUtbetalingsperiode(utbetalingsperiode, forventetUtbetalingsperiode)
                } catch (e: Throwable) {
                    logger.error("Feilet validering av rad $index for oppdrag=${forventetUtbetalingsoppdrag.behandlingId}")
                    throw e
                }
            }
        }
    }
}

private fun assertUtbetalingsperiode(
    utbetalingsperiode: Utbetalingsperiode,
    forventetUtbetalingsperiode: ForventetUtbetalingsperiode,
) {
    assertThat(utbetalingsperiode.erEndringPåEksisterendePeriode)
        .isEqualTo(forventetUtbetalingsperiode.erEndringPåEksisterendePeriode)
    assertThat(utbetalingsperiode.klassifisering).isEqualTo(forventetUtbetalingsperiode.ytelse.klassifisering)
    assertThat(utbetalingsperiode.periodeId).isEqualTo(forventetUtbetalingsperiode.periodeId)
    assertThat(utbetalingsperiode.forrigePeriodeId).isEqualTo(forventetUtbetalingsperiode.forrigePeriodeId)
    assertThat(utbetalingsperiode.sats.toInt()).isEqualTo(forventetUtbetalingsperiode.sats)
    assertThat(utbetalingsperiode.satsType).isEqualTo(Utbetalingsperiode.SatsType.MND)
    assertThat(utbetalingsperiode.vedtakdatoFom).isEqualTo(forventetUtbetalingsperiode.fom)
    assertThat(utbetalingsperiode.vedtakdatoTom).isEqualTo(forventetUtbetalingsperiode.tom)
    assertThat(utbetalingsperiode.opphør?.opphørDatoFom).isEqualTo(forventetUtbetalingsperiode.opphør)
    forventetUtbetalingsperiode.kildebehandlingId?.let {
        assertThat(utbetalingsperiode.behandlingId).isEqualTo(forventetUtbetalingsperiode.kildebehandlingId)
    }
}
