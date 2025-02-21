package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.ks.infotrygd.feed.VedtakDto
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.infotrygd.KafkaProducer
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.YearMonth

internal class SendVedtakHendelseTilInfotrygdTaskTest {
    private val kafkaProducer = mockk<KafkaProducer>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()

    private val sendVedtakHendelseTilInfotrygdTask = SendVedtakHendelseTilInfotrygdTask(kafkaProducer, andelerTilkjentYtelseOgEndreteUtbetalingerService)

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `doTask skal sende vedtak hendelse til kafka`() {
        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                stønadFom = YearMonth.now().minusMonths(5),
                stønadTom = YearMonth.now().minusMonths(3),
            )
        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                stønadFom = YearMonth.now().minusMonths(6),
                stønadTom = YearMonth.now().minusMonths(2),
            )

        val vedtakDtoSlot = slot<VedtakDto>()

        every { kafkaProducer.sendVedtakHendelseTilInfotrygd(capture(vedtakDtoSlot)) } just runs
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse1, emptyList()),
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse2, emptyList()),
            )

        assertDoesNotThrow {
            sendVedtakHendelseTilInfotrygdTask.doTask(
                SendVedtakHendelseTilInfotrygdTask.opprettTask(
                    fnrStoenadsmottaker = behandling.fagsak.aktør.aktivFødselsnummer(),
                    behandlingId = behandling.id,
                ),
            )
        }
        verify(atLeast = 1) { kafkaProducer.sendVedtakHendelseTilInfotrygd(any()) }

        val sendtData = vedtakDtoSlot.captured
        assertEquals(behandling.fagsak.aktør.aktivFødselsnummer(), sendtData.fnrStoenadsmottaker)
        assertEquals(andelTilkjentYtelse2.stønadFom.førsteDagIInneværendeMåned(), sendtData.datoStartNyKS)
    }
}
