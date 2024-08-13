package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtakFellesfelterSammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpprettOpphørtSammensattKontrollsakServiceTest {
    private val mockedOpprettVedtakFellesfelterSammensattKontrollsakService: OpprettVedtakFellesfelterSammensattKontrollsakService = mockk()
    private val mockedSimuleringService: SimuleringService = mockk()
    private val opprettOpphørtSammensattKontrollsakService: OpprettOpphørtSammensattKontrollsakService =
        OpprettOpphørtSammensattKontrollsakService(
            opprettVedtakFellesfelterSammensattKontrollsakService = mockedOpprettVedtakFellesfelterSammensattKontrollsakService,
            simuleringService = mockedSimuleringService,
        )

    @Test
    fun `skal generere OpphørtSammensattKontrollsak`() {
        // Arrange
        val vedtak = lagVedtak()
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

        every {
            mockedSimuleringService.erFeilutbetalingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns false

        // Act
        val opphørtSammensattKontrollsak =
            opprettOpphørtSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(opphørtSammensattKontrollsak.mal).isEqualTo(Brevmal.VEDTAK_OPPHØRT)
        assertThat(opphørtSammensattKontrollsak.data.delmalData.signaturVedtak.enhet).containsOnly("enhet")
        assertThat(opphørtSammensattKontrollsak.data.delmalData.signaturVedtak.saksbehandler).containsOnly("saksbehandler")
        assertThat(opphørtSammensattKontrollsak.data.delmalData.signaturVedtak.beslutter).containsOnly("beslutter")
        assertThat(opphørtSammensattKontrollsak.data.delmalData.feilutbetaling).isFalse()
        assertThat(opphørtSammensattKontrollsak.data.delmalData.korrigertVedtak).isNull()
        assertThat(opphørtSammensattKontrollsak.data.flettefelter.navn).containsOnly("søkerNavn")
        assertThat(opphørtSammensattKontrollsak.data.flettefelter.fodselsnummer).containsOnly("søkerFødselsnummer")
        assertThat(opphørtSammensattKontrollsak.data.flettefelter.brevOpprettetDato).isNotNull()
        assertThat(opphørtSammensattKontrollsak.data.flettefelter.gjelder).isNull()
        assertThat(opphørtSammensattKontrollsak.data.sammensattKontrollsakFritekst).isEqualTo("sammensattKontrollsakFritekst")
    }
}
