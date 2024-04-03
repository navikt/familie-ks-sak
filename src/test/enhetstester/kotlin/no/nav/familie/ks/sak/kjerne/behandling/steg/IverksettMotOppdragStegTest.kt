package no.nav.familie.ks.sak.kjerne.behandling.steg

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.IverksettMotOppdragDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.UtbetalingsoppdragService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.iverksettmotoppdrag.IverksettMotOppdragSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.VedtakService
import no.nav.familie.ks.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class IverksettMotOppdragStegTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var totrinnskontrollService: TotrinnskontrollService

    @MockK
    private lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @MockK
    private lateinit var utbetalingsoppdragService: UtbetalingsoppdragService

    @MockK
    private lateinit var vedtakService: VedtakService

    @MockK
    private lateinit var taskService: TaskService

    @InjectMockKs
    private lateinit var iverksettMotOppdragSteg: IverksettMotOppdragSteg

    val iverksettMotOppdragDto = IverksettMotOppdragDto(200, "test")

    @Test
    fun `utførSteg skal kaste feil dersom totrinnskontroll er ugyldig `() {
        val mocketBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val mocketTotrinnskontroll =
            Totrinnskontroll(
                behandling = mocketBehandling,
                saksbehandler = "SB1",
                beslutter = "SB1",
                saksbehandlerId = "1234",
                godkjent = true,
            )

        every { behandlingService.hentBehandling(any()) } returns mocketBehandling
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(mocketBehandling) } just runs
        every { totrinnskontrollService.hentAktivForBehandling(mocketBehandling.id) } returns mocketTotrinnskontroll

        val feil = assertThrows<Feil> { iverksettMotOppdragSteg.utførSteg(200, iverksettMotOppdragDto) }

        assertThat(feil.message, Is("Totrinnskontroll($mocketTotrinnskontroll) er ugyldig ved iverksetting"))
        assertThat(feil.frontendFeilmelding, Is("Totrinnskontroll er ugyldig ved iverksetting"))
    }

    @Test
    fun `utførSteg skal kaste feil dersom totrinnskontroll ikke er godkjent`() {
        val mocketBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val mocketTotrinnskontroll =
            Totrinnskontroll(
                behandling = mocketBehandling,
                saksbehandler = "Test",
                saksbehandlerId = "1234",
                godkjent = false,
            )

        every { behandlingService.hentBehandling(200) } returns mocketBehandling
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(mocketBehandling) } just runs
        every { totrinnskontrollService.hentAktivForBehandling(mocketBehandling.id) } returns mocketTotrinnskontroll

        val feil = assertThrows<Feil> { iverksettMotOppdragSteg.utførSteg(200, iverksettMotOppdragDto) }

        assertThat(feil.message, Is("Prøver å iverksette et underkjent vedtak"))
    }

    @Test
    fun `utførSteg skal lage utbetalingsoppdrag hvis totrinnskontroll er gyldig og godkjent`() {
        val mocketBehandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val mocketTotrinnskontroll =
            Totrinnskontroll(
                behandling = mocketBehandling,
                saksbehandler = "Test",
                saksbehandlerId = "1234",
                godkjent = true,
            )

        every { behandlingService.hentBehandling(200) } returns mocketBehandling
        every { tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(mocketBehandling) } just runs
        every { totrinnskontrollService.hentAktivForBehandling(any()) } returns mocketTotrinnskontroll
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns mockk()
        every {
            utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any(), any())
        } returns mockk()
        every { behandlingService.hentSisteBehandlingSomErVedtatt(any()) } returns null
        every { taskService.save(any()) } returns mockk()

        iverksettMotOppdragSteg.utførSteg(200, iverksettMotOppdragDto)

        verify(exactly = 1) { behandlingService.hentBehandling(any()) }
        verify(exactly = 1) {
            tilkjentYtelseValideringService.validerAtIngenUtbetalingerOverstiger100Prosent(
                mocketBehandling,
            )
        }
        verify(exactly = 1) { totrinnskontrollService.hentAktivForBehandling(any()) }
        verify(exactly = 1) { vedtakService.hentAktivVedtakForBehandling(any()) }
        verify(exactly = 1) {
            utbetalingsoppdragService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(any(), any())
        }
        verify(exactly = 1) { taskService.save(any()) }
    }
}
