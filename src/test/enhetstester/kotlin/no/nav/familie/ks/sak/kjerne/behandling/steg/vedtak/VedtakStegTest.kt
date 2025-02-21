package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStegTilstand
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.prosessering.internal.TaskService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.hamcrest.CoreMatchers.`is` as Is

class VedtakStegTest {
    private val behandlingService = mockk<BehandlingService>()
    private val taskService = mockk<TaskService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>(relaxed = true)
    private val loggService = mockk<LoggService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()

    private val vedtakSteg =
        VedtakSteg(
            behandlingService = behandlingService,
            taskService = taskService,
            totrinnskontrollService = totrinnskontrollService,
            loggService = loggService,
            oppgaveService = oppgaveService,
            vedtakService = vedtakService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

    @Test
    fun `utførSteg skal kaste feil dersom behandlingen er henlagt`() {
        val mocketBehandling = mockk<Behandling>()

        every { mocketBehandling.erHenlagt() } returns true
        every { behandlingService.hentBehandling(200) } returns mocketBehandling

        val feil =
            assertThrows<Feil> {
                vedtakSteg.utførSteg(200)
            }

        assertThat(feil.message, Is("Behandlingen er henlagt og dermed så kan ikke vedtak foreslås."))
    }

    @ParameterizedTest
    @EnumSource(value = BehandlingStegStatus::class, names = ["VENTER", "KLAR"])
    fun `utførSteg skal kaste feil dersom det er flere enn 1 steg i behandlingen som har status VENTER eller KLAR`(
        behandlingStegStatus: BehandlingStegStatus,
    ) {
        val mocketBehandling = mockk<Behandling>()
        val mocketStegTilstand = mockk<BehandlingStegTilstand>()
        val mocketStegTilstand2 = mockk<BehandlingStegTilstand>()

        every { mocketBehandling.erHenlagt() } returns false
        every { mocketStegTilstand.behandlingStegStatus } returns behandlingStegStatus
        every { mocketStegTilstand2.behandlingStegStatus } returns behandlingStegStatus
        every { mocketBehandling.behandlingStegTilstand } returns mutableSetOf(mocketStegTilstand, mocketStegTilstand2)
        every { behandlingService.hentBehandling(200) } returns mocketBehandling

        val feil =
            assertThrows<Feil> {
                vedtakSteg.utførSteg(200)
            }

        assertThat(feil.message, Is("Behandlingen har mer enn ett ikke fullført steg."))
    }

    @Test
    fun `utførSteg lage logg, ny godkjennevedtak task og sette status på behandling til fatter vedtak`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)

        every { behandlingService.hentBehandling(200) } returns behandling
        every { loggService.opprettSendTilBeslutterLogg(behandling.id) } just runs
        every { totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling) } returns mockk()
        every { taskService.save(any()) } returns mockk()
        every { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) } returns emptyList()
        every { vedtakService.hentAktivVedtakForBehandling(any()) } returns mockk(relaxed = true)
        every { vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(any()) } returns mockk(relaxed = true)
        every { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(any()) } returns mockk()

        vedtakSteg.utførSteg(200)

        verify(exactly = 1) { behandlingService.hentBehandling(200) }
        verify(exactly = 1) { loggService.opprettSendTilBeslutterLogg(behandling.id) }
        verify(exactly = 1) { totrinnskontrollService.opprettTotrinnskontrollMedSaksbehandler(behandling) }
        verify(exactly = 1) { taskService.save(any()) }
        verify(exactly = 1) { oppgaveService.hentOppgaverSomIkkeErFerdigstilt(behandling) }
        verify(exactly = 1) { vedtakService.hentAktivVedtakForBehandling(behandling.id) }
        verify(exactly = 1) { vedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(any()) }
        verify(exactly = 1) { vedtakService.oppdaterVedtakMedDatoOgStønadsbrev(behandling) }
    }
}
