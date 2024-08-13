package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtakFellesfelterSammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørMedEndringSammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsak
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VedtakEndringSammensattKontrollsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OpprettSammensattKontrollsakBrevDtoServiceTest {
    private val mockedBrevmalService: BrevmalService = mockk()
    private val mockedOpprettOpphørtSammensattKontrollsakService: OpprettOpphørtSammensattKontrollsakService = mockk()
    private val mockedOpprettOpphørMedEndringSammensattKontrollsakService: OpprettOpphørMedEndringSammensattKontrollsakService = mockk()
    private val mockedOpprettVedtakEndringSammensattKontrollsakService: OpprettVedtakEndringSammensattKontrollsakService = mockk()
    private val opprettSammensattKontrollsakBrevDtoService =
        OpprettSammensattKontrollsakBrevDtoService(
            brevmalService = mockedBrevmalService,
            opprettOpphørtSammensattKontrollsakService = mockedOpprettOpphørtSammensattKontrollsakService,
            opprettOpphørMedEndringSammensattKontrollsakService = mockedOpprettOpphørMedEndringSammensattKontrollsakService,
            opprettVedtakEndringSammensattKontrollsakService = mockedOpprettVedtakEndringSammensattKontrollsakService,
        )

    @Test
    fun `skal generere for vedtak opphørt`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        val opphørtSammensattKontrollsak =
            OpphørtSammensattKontrollsak(
                vedtakFellesfelterSammensattKontrollsak = lagVedtakFellesfelterSammensattKontrollsak(),
                erFeilutbetalingPåBehandling = false,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns Brevmal.VEDTAK_OPPHØRT

        every {
            mockedOpprettOpphørtSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørtSammensattKontrollsak

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørtSammensattKontrollsak)
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakService wasNot called }
    }

    @Test
    fun `skal generere for vedtak opphør med endring`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        val opphørMedEndringSammensattKontrollsak =
            OpphørMedEndringSammensattKontrollsak(
                vedtakFellesfelter = lagVedtakFellesfelterSammensattKontrollsak(),
                erFeilutbetalingPåBehandling = false,
                erKlage = false,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns Brevmal.VEDTAK_OPPHØR_MED_ENDRING

        every {
            mockedOpprettOpphørMedEndringSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørMedEndringSammensattKontrollsak

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørMedEndringSammensattKontrollsak)
        verify { mockedOpprettOpphørtSammensattKontrollsakService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakService wasNot called }
    }

    @Test
    fun `skal generere for vedtak endring`() {
        // Arrange
        val vedtak =
            lagVedtak(
                behandling =
                    lagBehandling(
                        type = BehandlingType.REVURDERING,
                        resultat = Behandlingsresultat.INNVILGET_OG_ENDRET,
                    ),
            )

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        val vedtakEndringSammensattKontrollsak =
            VedtakEndringSammensattKontrollsak(
                vedtakFellesfelter = lagVedtakFellesfelterSammensattKontrollsak(),
                erFeilutbetalingPåBehandling = false,
                erKlage = false,
                informasjonOmAarligKontroll = false,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns Brevmal.VEDTAK_ENDRING

        every {
            mockedOpprettVedtakEndringSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns vedtakEndringSammensattKontrollsak

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(vedtakEndringSammensattKontrollsak)
        verify { mockedOpprettOpphørtSammensattKontrollsakService wasNot called }
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakService wasNot called }
    }

    @ParameterizedTest
    @EnumSource(
        value = Brevmal::class,
        names = [
            "VEDTAK_OPPHØRT",
            "VEDTAK_OPPHØR_MED_ENDRING",
            "VEDTAK_ENDRING",
        ],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `skal kaste feil for brevmaler som ikke er søtttet`(brevmal: Brevmal) {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns brevmal

        // Act & assert
        val exception =
            assertThrows<Feil> {
                opprettSammensattKontrollsakBrevDtoService.opprett(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )
            }
        assertThat(exception.message).isEqualTo(
            "Brevmalen $brevmal er ikke støttet for sammensatte kontrollsaker",
        )
        verify { mockedOpprettOpphørtSammensattKontrollsakService wasNot called }
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakService wasNot called }
    }
}
