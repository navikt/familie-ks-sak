package no.nav.familie.ks.sak.integrasjon.økonomi.internkonsistensavstemming

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag.KodeEndring.ENDR
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode.SatsType.MND
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.oppdrag.UtbetalingsoppdragMedBehandlingOgFagsak
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class InternKonsistensavstemmingServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val oppdragKlient = mockk<OppdragKlient>()
    private val internKonsistensavstemmingService =
        InternKonsistensavstemmingService(
            oppdragKlient = oppdragKlient,
            behandlingService = behandlingService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
            fagsakRepository = mockk(),
            taskService = mockk(),
        )

    @Test
    fun `skal summere overgangsordningandeler i intern konsistensavstemming`() {
        val behandling = lagBehandling()
        val fagsakId = behandling.fagsak.id
        val aktør = behandling.fagsak.aktør

        val ordinæreAndel =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = aktør,
                stønadFom = YearMonth.of(2024, 2),
                stønadTom = YearMonth.of(2024, 8),
                kalkulertUtbetalingsbeløp = 7500,
                ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
            )

        val overgangsordningAndel =
            lagAndelTilkjentYtelse(
                behandling = behandling,
                aktør = aktør,
                stønadFom = YearMonth.of(2024, 9),
                stønadTom = YearMonth.of(2024, 12),
                kalkulertUtbetalingsbeløp = 7500,
                ytelseType = YtelseType.OVERGANGSORDNING,
            )

        val utbetalingsoppdragMedBehandlingOgFagsak =
            UtbetalingsoppdragMedBehandlingOgFagsak(
                fagsakId = fagsakId,
                behandlingId = behandling.id,
                utbetalingsoppdrag =
                    Utbetalingsoppdrag(
                        kodeEndring = ENDR,
                        fagSystem = "KS",
                        saksnummer = "1143961",
                        aktoer = aktør.aktivFødselsnummer(),
                        saksbehandlerId = "VL",
                        avstemmingTidspunkt = LocalDateTime.now(),
                        utbetalingsperiode =
                            listOf(
                                Utbetalingsperiode(
                                    erEndringPåEksisterendePeriode = false,
                                    opphør = null,
                                    periodeId = 1,
                                    forrigePeriodeId = 0,
                                    datoForVedtak = LocalDate.of(2024, 12, 18),
                                    klassifisering = "KS",
                                    vedtakdatoFom = LocalDate.of(2024, 12, 1),
                                    vedtakdatoTom = LocalDate.of(2024, 12, 31),
                                    sats = BigDecimal.valueOf(30000),
                                    satsType = MND,
                                    utbetalesTil = aktør.aktivFødselsnummer(),
                                    behandlingId = behandling.id,
                                    utbetalingsgrad = null,
                                ),
                            ),
                        gOmregning = false,
                    ),
            )

        every { behandlingService.hentSisteBehandlingSomErAvsluttetEllerSendtTilØkonomiPerFagsak(any()) } returns listOf(behandling)
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(any()) } returns listOf(ordinæreAndel, overgangsordningAndel)
        every { oppdragKlient.hentSisteUtbetalingsoppdragForFagsaker(any()) } returns listOf(utbetalingsoppdragMedBehandlingOgFagsak)

        val (andeler, utbetalingsoppdrag) =
            internKonsistensavstemmingService.hentFagsakTilSisteUtbetalingsoppdragOgSisteAndelerMap(fagsakIder = setOf(fagsakId)).getValue(fagsakId)

        assertThat(
            erForskjellMellomAndelerOgOppdrag(
                andeler = andeler,
                utbetalingsoppdrag = utbetalingsoppdrag,
                fagsakId = fagsakId,
            ),
        ).isFalse()
    }
}
