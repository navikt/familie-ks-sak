package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtakFellesfelterSammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpprettOpphørMedEndringSammensattKontrollsakServiceTest {
    private val mockedOpprettVedtakFellesfelterSammensattKontrollsakService: OpprettVedtakFellesfelterSammensattKontrollsakService = mockk()
    private val mockedEtterbetalingService: EtterbetalingService = mockk()
    private val mockedSimuleringService: SimuleringService = mockk()
    private val mockedBrevPeriodeService: BrevPeriodeService = mockk()
    private val opprettOpphørMedEndringSammensattKontrollsakService: OpprettOpphørMedEndringSammensattKontrollsakService =
        OpprettOpphørMedEndringSammensattKontrollsakService(
            opprettVedtakFellesfelterSammensattKontrollsakService = mockedOpprettVedtakFellesfelterSammensattKontrollsakService,
            etterbetalingService = mockedEtterbetalingService,
            simuleringService = mockedSimuleringService,
            brevPeriodeService = mockedBrevPeriodeService,
        )

    @Test
    fun `skal generere OpphørMedEndringSammensattKontrollsak for klage behandling`() {
        // Arrange
        val vedtak =
            lagVedtak(
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.KLAGE,
                ),
            )
        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedOpprettVedtakFellesfelterSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns lagVedtakFellesfelterSammensattKontrollsak()

        val etterbetaling = Etterbetaling("100")

        every {
            mockedEtterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )
        } returns etterbetaling

        every {
            mockedSimuleringService.erFeilutbetalingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns true

        val refusjonEøsAvklart =
            RefusjonEøsAvklart(
                setOf(
                    "beskrivelse1",
                ),
            )

        every {
            mockedBrevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(
                vedtak = vedtak,
            )
        } returns refusjonEøsAvklart

        val refusjonEøsUavklart =
            RefusjonEøsUavklart(
                setOf(
                    "beskrivelse2",
                ),
            )

        every {
            mockedBrevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(
                vedtak = vedtak,
            )
        } returns refusjonEøsUavklart

        // Act
        val opphørMedEndringSammensattKontrollsak =
            opprettOpphørMedEndringSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(opphørMedEndringSammensattKontrollsak.mal).isEqualTo(Brevmal.VEDTAK_OPPHØR_MED_ENDRING)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.enhet).containsOnly("enhet")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.saksbehandler).containsOnly("saksbehandler")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.beslutter).containsOnly("beslutter")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.feilutbetaling).isTrue()
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.etterbetaling).isEqualTo(etterbetaling)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.korrigertVedtak).isNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.refusjonEosAvklart).isEqualTo(refusjonEøsAvklart)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.refusjonEosUavklart).isEqualTo(refusjonEøsUavklart)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.klage).isTrue()
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.navn).containsOnly("søkerNavn")
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.fodselsnummer).containsOnly("søkerFødselsnummer")
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.brevOpprettetDato).isNotNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.gjelder).isNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.sammensattKontrollsakFritekst).isEqualTo("sammensattKontrollsakFritekst")
    }

    @Test
    fun `skal generere OpphørMedEndringSammensattKontrollsak for behandling som ikke er klage`() {
        // Arrange
        val vedtak =
            lagVedtak(
                lagBehandling(
                    opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                ),
            )
        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedOpprettVedtakFellesfelterSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns lagVedtakFellesfelterSammensattKontrollsak()

        val etterbetaling = Etterbetaling("100")

        every {
            mockedEtterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )
        } returns etterbetaling

        every {
            mockedSimuleringService.erFeilutbetalingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns true

        val refusjonEøsAvklart =
            RefusjonEøsAvklart(
                setOf(
                    "beskrivelse1",
                ),
            )

        every {
            mockedBrevPeriodeService.beskrivPerioderMedAvklartRefusjonEøs(
                vedtak = vedtak,
            )
        } returns refusjonEøsAvklart

        val refusjonEøsUavklart =
            RefusjonEøsUavklart(
                setOf(
                    "beskrivelse2",
                ),
            )

        every {
            mockedBrevPeriodeService.beskrivPerioderMedUavklartRefusjonEøs(
                vedtak = vedtak,
            )
        } returns refusjonEøsUavklart

        // Act
        val opphørMedEndringSammensattKontrollsak =
            opprettOpphørMedEndringSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(opphørMedEndringSammensattKontrollsak.mal).isEqualTo(Brevmal.VEDTAK_OPPHØR_MED_ENDRING)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.enhet).containsOnly("enhet")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.saksbehandler).containsOnly("saksbehandler")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.signaturVedtak.beslutter).containsOnly("beslutter")
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.feilutbetaling).isTrue()
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.etterbetaling).isEqualTo(etterbetaling)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.korrigertVedtak).isNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.refusjonEosAvklart).isEqualTo(refusjonEøsAvklart)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.refusjonEosUavklart).isEqualTo(refusjonEøsUavklart)
        assertThat(opphørMedEndringSammensattKontrollsak.data.delmalData.klage).isFalse()
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.navn).containsOnly("søkerNavn")
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.fodselsnummer).containsOnly("søkerFødselsnummer")
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.brevOpprettetDato).isNotNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.flettefelter.gjelder).isNull()
        assertThat(opphørMedEndringSammensattKontrollsak.data.sammensattKontrollsakFritekst).isEqualTo("sammensattKontrollsakFritekst")
    }
}
