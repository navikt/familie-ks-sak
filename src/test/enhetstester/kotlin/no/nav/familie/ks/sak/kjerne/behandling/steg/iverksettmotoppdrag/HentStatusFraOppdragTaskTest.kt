package no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagTilkjentYtelse
import no.nav.familie.ks.sak.data.lagUtbetalingsperiode
import no.nav.familie.ks.sak.integrasjon.oppdrag.OppdragKlient
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.FAGSYSTEM
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.StegService
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.familie.prosessering.internal.TaskService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class HentStatusFraOppdragTaskTest {
    private val oppdragKlient = mockk<OppdragKlient>()
    private val taskService = mockk<TaskService>()
    private val stegService = mockk<StegService>()
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()

    private val hentStatusFraOppdragTask =
        HentStatusFraOppdragTask(
            oppdragKlient = oppdragKlient,
            taskService = taskService,
            stegService = stegService,
            tilkjentYtelseRepository = tilkjentYtelseRepository,
        )

    private val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @BeforeEach
    fun setup() {
        every { taskService.save(any()) } returns mockk()

        // utbetalingsoppdrag med periode
        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(any()) } returns
            lagTilkjentYtelse(
                behandling = behandling,
                utbetalingsoppdrag =
                    Utbetalingsoppdrag(
                        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                        fagSystem = FAGSYSTEM,
                        saksnummer = "",
                        aktoer = UUID.randomUUID().toString(),
                        saksbehandlerId = "",
                        avstemmingTidspunkt = LocalDateTime.now(),
                        utbetalingsperiode = listOf(lagUtbetalingsperiode()),
                    ),
            )
    }

    @Test
    fun `doTask skal kaste feil når oppdrag returnerer med status LAGT_PÅ_KØ`() {
        every { oppdragKlient.hentStatus(any()) } returns OppdragStatus.LAGT_PÅ_KØ

        val exception = assertThrows<RekjørSenereException> { hentStatusFraOppdragTask.doTask(lagTask()) }
        assertEquals("Mottok ${OppdragStatus.LAGT_PÅ_KØ.name} fra oppdrag.", exception.årsak)
        assertNotNull(exception.triggerTid)
    }

    @Test
    fun `doTask skal sette task til manuell oppfølging når oppdrag returnerer med KVITTERT_TEKNISK_FEIL`() {
        every { oppdragKlient.hentStatus(any()) } returns OppdragStatus.KVITTERT_TEKNISK_FEIL
        val taskSlot = slot<Task>()
        assertDoesNotThrow { hentStatusFraOppdragTask.doTask(lagTask()) }

        verify(exactly = 1) { taskService.save(capture(taskSlot)) }
        assertEquals(Status.MANUELL_OPPFØLGING, taskSlot.captured.status)
    }

    @Test
    fun `doTask skal utføre task når oppdrag returnerer med KVITTERT_OK`() {
        every { oppdragKlient.hentStatus(any()) } returns OppdragStatus.KVITTERT_OK
        every { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) } just runs

        assertDoesNotThrow { hentStatusFraOppdragTask.doTask(lagTask()) }
        verify(exactly = 1) { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) }
    }

    @Test
    fun `doTask skal ikke hente status fra oppdrag hvis utbetalingsperiode er en tom liste`() {
        every { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) } just runs
        every { tilkjentYtelseRepository.hentTilkjentYtelseForBehandling(any()) } returns
            lagTilkjentYtelse(
                behandling = behandling,
                utbetalingsoppdrag =
                    Utbetalingsoppdrag(
                        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
                        fagSystem = FAGSYSTEM,
                        saksnummer = "",
                        aktoer = UUID.randomUUID().toString(),
                        saksbehandlerId = "",
                        avstemmingTidspunkt = LocalDateTime.now(),
                        utbetalingsperiode = emptyList(),
                    ),
            )

        assertDoesNotThrow { hentStatusFraOppdragTask.doTask(lagTask()) }
        verify(exactly = 1) { stegService.utførStegEtterIverksettelseAutomatisk(behandling.id) }
        verify(exactly = 0) { oppdragKlient.hentStatus(any()) }
    }

    private fun lagTask() =
        Task(
            type = HentStatusFraOppdragTask.TASK_STEP_TYPE,
            payload =
                objectMapper.writeValueAsString(
                    HentStatusFraOppdragDto(
                        fagsystem = FAGSYSTEM,
                        personIdent = behandling.fagsak.aktør.aktivFødselsnummer(),
                        behandlingsId = behandling.id,
                        vedtaksId = 1L,
                    ),
                ),
        )
}
