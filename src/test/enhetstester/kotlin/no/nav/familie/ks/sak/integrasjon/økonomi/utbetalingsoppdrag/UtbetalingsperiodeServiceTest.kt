package no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitiellTilkjentYtelse
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.domene.filtrerAndelerSomSkalSendesTilOppdrag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class UtbetalingsperiodeServiceTest {
    private val oppdragKlient = mockk<OppdragKlient>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private val utbetalingsoppdragGenerator = UtbetalingsoppdragGenerator()
    private val utbetalingsoppdragService =
        UtbetalingsoppdragService(
            oppdragKlient = oppdragKlient,
            tilkjentYtelseValideringService = tilkjentYtelseValideringService,
            utbetalingsoppdragGenerator = utbetalingsoppdragGenerator,
            behandlingService = behandlingService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal ikke iverksette mot oppdrag hvis det ikke finnes utbetalingsperioder`() {
        every { tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(any()) } just runs
        every { oppdragKlient.iverksettOppdrag(any()) } returns ""

        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            emptySet(),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtak,
            "abc123",
        )

        verify(exactly = 0) { oppdragKlient.iverksettOppdrag(any()) }
    }

    @Test
    fun `oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett - skal iverksette mot oppdrag hvis det finnes utbetalingsperioder`() {
        every { tilkjentYtelseValideringService.validerIngenAndelerTilkjentYtelseMedSammeOffsetIBehandling(any()) } just runs
        every { oppdragKlient.iverksettOppdrag(any()) } returns ""

        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(
            vedtak = vedtak,
            "abc123",
        )

        verify(exactly = 1) { oppdragKlient.iverksettOppdrag(any()) }
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset når det ikke finnes en forrige behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 3,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets =
                listOf(
                    Pair(0L, null),
                    Pair(1L, 0L),
                    Pair(2L, 1L),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset når det finnes en forrige behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 300,
                    person = person,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0,
                    forrigeperiodeIdOffset = null,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2,
                    forrigeperiodeIdOffset = 1,
                ),
            ),
        )

        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 1,
            forventetAntallUtbetalingsperioder = 1,
            forventedeOffsets =
                listOf(
                    Pair(3L, 2L),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset for 2 personer`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 5,
            forventetAntallUtbetalingsperioder = 5,
            forventedeOffsets =
                listOf(
                    Pair(0L, null),
                    Pair(1L, 0L),
                    Pair(2L, 1L),
                    Pair(3L, null),
                    Pair(4L, 3L),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset for 2 personer og tidligere behandling`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2L,
                    forrigeperiodeIdOffset = 1L,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = barn,
                    periodeIdOffset = 3L,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                    periodeIdOffset = 4L,
                    forrigeperiodeIdOffset = 3L,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 2,
            forventedeOffsets =
                listOf(
                    Pair(5L, 2L),
                    Pair(6L, 4L),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag og oppdatere andeler med offset for 2 personer og tidligere behandling for overgangsordning`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 10),
                    beløp = 350,
                    person = barn,
                ),
                lagAndelTilkjentYtelse(
                    id = 8,
                    fom = YearMonth.of(2024, 11),
                    tom = YearMonth.of(2025, 2),
                    beløp = 350,
                    person = barn,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2024, 4),
                    tom = YearMonth.of(2024, 8),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 10),
                    beløp = 250,
                    person = barn,
                    periodeIdOffset = 2L,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2024, 11),
                    tom = YearMonth.of(2024, 11),
                    beløp = 350,
                    person = barn,
                    periodeIdOffset = 3L,
                    forrigeperiodeIdOffset = 2L,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
                lagAndelTilkjentYtelse(
                    id = 5,
                    fom = YearMonth.of(2024, 12),
                    tom = YearMonth.of(2025, 2),
                    beløp = 250,
                    person = barn,
                    periodeIdOffset = null, // Bare første overgangsordningandel per person får offset
                    forrigeperiodeIdOffset = null,
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                "abc123",
            )

        val lagredeAndeler = tilkjentYtelseSlot.captured.andelerTilkjentYtelse

        verify(exactly = 1) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = lagredeAndeler,
            forventetAntallAndeler = 3,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets =
                listOf(
                    Pair(4L, 1L),
                    Pair(5L, 3L),
                    Pair(6L, 5L),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - skal generere nytt utbetalingsoppdrag for simulering med en eksisterende kjede og en ny kjede`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 6,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 7,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 3),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 4),
                    tom = YearMonth.of(2023, 5),
                    beløp = 350,
                    person = person,
                    periodeIdOffset = 1L,
                    forrigeperiodeIdOffset = 0L,
                ),
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 6),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                    periodeIdOffset = 2L,
                    forrigeperiodeIdOffset = 1L,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = "abc123",
                erSimulering = true,
            )

        verify(exactly = 0) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = emptySet(),
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 3,
            forventedeOffsets =
                listOf(
                    Pair(3L, 2L),
                    Pair(4L, null),
                ),
        )
    }

    @Test
    fun `genererUtbetalingsoppdrag - revurdering hvor 0-utbetaling går til betaling skal ikke opprette noe opphør ved simulering`() {
        val vedtak = lagVedtak()
        val tilkjentYtelse = lagInitiellTilkjentYtelse(vedtak.behandling)
        val person = tilfeldigPerson()
        val barn = tilfeldigPerson()
        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 3,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 250,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 4,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 350,
                    person = barn,
                ),
            ),
        )

        val forrigeBehandling = lagBehandling()
        val forrigeTilkjentYtelse = lagInitiellTilkjentYtelse(forrigeBehandling)
        forrigeTilkjentYtelse.andelerTilkjentYtelse.addAll(
            mutableSetOf(
                lagAndelTilkjentYtelse(
                    id = 1,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 0,
                    person = person,
                ),
                lagAndelTilkjentYtelse(
                    id = 2,
                    fom = YearMonth.of(2023, 1),
                    tom = YearMonth.of(2023, 8),
                    beløp = 0,
                    person = barn,
                ),
            ),
        )
        val tilkjentYtelseSlot = slot<TilkjentYtelse>()
        setUpMocks(
            behandling = vedtak.behandling,
            tilkjentYtelse = tilkjentYtelse,
            tilkjentYtelseSlot = tilkjentYtelseSlot,
            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
        )

        val beregnetUtbetalingsoppdrag =
            utbetalingsoppdragService.genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
                vedtak = vedtak,
                saksbehandlerId = "abc123",
                erSimulering = true,
            )

        verify(exactly = 0) { tilkjentYtelseRepository.save(any()) }

        validerBeregnetUtbetalingsoppdragOgAndeler(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            andelerTilkjentYtelse = emptySet(),
            forventetAntallAndeler = 2,
            forventetAntallUtbetalingsperioder = 2,
            forventedeOffsets =
                listOf(
                    Pair(0L, null),
                    Pair(1L, null),
                ),
        )
    }

    private fun validerBeregnetUtbetalingsoppdragOgAndeler(
        beregnetUtbetalingsoppdrag: BeregnetUtbetalingsoppdragLongId,
        andelerTilkjentYtelse: Set<AndelTilkjentYtelse>,
        forventedeOffsets: List<Pair<Long?, Long?>>,
        forventetAntallAndeler: Int,
        forventetAntallUtbetalingsperioder: Int,
    ) {
        assertThat(beregnetUtbetalingsoppdrag.utbetalingsoppdrag).isNotNull
        assertThat(beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode.size)
            .`as`("Feil antall beregnede utbetalingsperioder: ${beregnetUtbetalingsoppdrag.utbetalingsoppdrag.utbetalingsperiode}")
            .isEqualTo(forventetAntallUtbetalingsperioder)

        assertThat(beregnetUtbetalingsoppdrag.andeler).isNotEmpty
        assertThat(beregnetUtbetalingsoppdrag.andeler.size).isEqualTo(forventetAntallAndeler)

        if (andelerTilkjentYtelse.isNotEmpty()) {
            assertThat(andelerTilkjentYtelse.size).isEqualTo(forventetAntallAndeler)
            assertThat(
                andelerTilkjentYtelse.map {
                    Pair(
                        it.periodeOffset,
                        it.forrigePeriodeOffset,
                    )
                },
            ).isEqualTo(
                forventedeOffsets,
            )
        } else {
            assertThat(
                beregnetUtbetalingsoppdrag.andeler.map {
                    Pair(
                        it.periodeId,
                        it.forrigePeriodeId,
                    )
                },
            ).isEqualTo(
                forventedeOffsets,
            )
        }
    }

    private fun setUpMocks(
        behandling: Behandling,
        tilkjentYtelse: TilkjentYtelse,
        tilkjentYtelseSlot: CapturingSlot<TilkjentYtelse>,
        forrigeTilkjentYtelse: TilkjentYtelse? = null,
    ) {
        if (forrigeTilkjentYtelse == null) {
            every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns null
            every { andelTilkjentYtelseRepository.hentSisteAndelPerIdent(behandling.fagsak.id) } returns emptyList()
        } else {
            every { behandlingService.hentSisteBehandlingSomErIverksatt(behandling.fagsak.id) } returns forrigeTilkjentYtelse.behandling

            every { tilkjentYtelseRepository.finnByBehandlingAndHasUtbetalingsoppdrag(forrigeTilkjentYtelse.behandling.id) } returns forrigeTilkjentYtelse

            every { andelTilkjentYtelseRepository.hentSisteAndelPerIdent(behandling.fagsak.id) } returns
                forrigeTilkjentYtelse.andelerTilkjentYtelse
                    .filtrerAndelerSomSkalSendesTilOppdrag()
                    .groupBy { it.aktør.aktivFødselsnummer() }
                    .mapValues { it.value.maxBy { it.periodeOffset!! } }
                    .values
                    .toList()
        }

        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(behandling.id) } returns tilkjentYtelse

        every { behandlingService.hentBehandlingerPåFagsak(behandling.fagsak.id) } returns listOf(behandling)

        every { tilkjentYtelseRepository.save(capture(tilkjentYtelseSlot)) } returns mockk()
    }
}
