package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtakFellesfelterSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørMedEndringSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.OpphørtSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.VedtakEndringSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.OpphørMedEndringSammensattKontrollsakDtoUtleder
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.OpphørtSammensattKontrollsakDtoUtleder
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.SammensattKontrollsakBrevDtoUtleder
import no.nav.familie.ks.sak.kjerne.brev.sammensattkontrollsak.VedtakEndringSammensattKontrollsakDtoUtleder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SammensattKontrollsakBrevDtoUtlederTest {
    private val mockedBrevmalService: BrevmalService = mockk()
    private val mockedOpphørtSammensattKontrollsakDtoUtleder: OpphørtSammensattKontrollsakDtoUtleder = mockk()
    private val mockedOpphørMedEndringSammensattKontrollsakDtoUtleder: OpphørMedEndringSammensattKontrollsakDtoUtleder = mockk()
    private val mockedVedtakEndringSammensattKontrollsakDtoUtleder: VedtakEndringSammensattKontrollsakDtoUtleder = mockk()
    private val sammensattKontrollsakBrevDtoUtleder =
        SammensattKontrollsakBrevDtoUtleder(
            brevmalService = mockedBrevmalService,
            opphørtSammensattKontrollsakDtoUtleder = mockedOpphørtSammensattKontrollsakDtoUtleder,
            opphørMedEndringSammensattKontrollsakDtoUtleder = mockedOpphørMedEndringSammensattKontrollsakDtoUtleder,
            vedtakEndringSammensattKontrollsakDtoUtleder = mockedVedtakEndringSammensattKontrollsakDtoUtleder,
        )

    @Test
    fun `skal generere for vedtak opphørt`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        val opphørtSammensattKontrollsakDto =
            OpphørtSammensattKontrollsakDto(
                vedtakFellesfelterSammensattKontrollsakDto = lagVedtakFellesfelterSammensattKontrollsakDto(),
                erFeilutbetalingPåBehandling = false,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns Brevmal.VEDTAK_OPPHØRT

        every {
            mockedOpphørtSammensattKontrollsakDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørtSammensattKontrollsakDto

        // Act
        val brevDto =
            sammensattKontrollsakBrevDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørtSammensattKontrollsakDto)
        verify { mockedOpphørMedEndringSammensattKontrollsakDtoUtleder wasNot called }
        verify { mockedVedtakEndringSammensattKontrollsakDtoUtleder wasNot called }
    }

    @Test
    fun `skal generere for vedtak opphør med endring`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        val opphørMedEndringSammensattKontrollsakDto =
            OpphørMedEndringSammensattKontrollsakDto(
                vedtakFellesfelter = lagVedtakFellesfelterSammensattKontrollsakDto(),
                erFeilutbetalingPåBehandling = false,
                erKlage = false,
            )

        every {
            mockedBrevmalService.hentVedtaksbrevmal(
                behandling = vedtak.behandling,
            )
        } returns Brevmal.VEDTAK_OPPHØR_MED_ENDRING

        every {
            mockedOpphørMedEndringSammensattKontrollsakDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørMedEndringSammensattKontrollsakDto

        // Act
        val brevDto =
            sammensattKontrollsakBrevDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørMedEndringSammensattKontrollsakDto)
        verify { mockedOpphørtSammensattKontrollsakDtoUtleder wasNot called }
        verify { mockedVedtakEndringSammensattKontrollsakDtoUtleder wasNot called }
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

        val vedtakEndringSammensattKontrollsakDto =
            VedtakEndringSammensattKontrollsakDto(
                vedtakFellesfelter = lagVedtakFellesfelterSammensattKontrollsakDto(),
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
            mockedVedtakEndringSammensattKontrollsakDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns vedtakEndringSammensattKontrollsakDto

        // Act
        val brevDto =
            sammensattKontrollsakBrevDtoUtleder.utled(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(vedtakEndringSammensattKontrollsakDto)
        verify { mockedOpphørtSammensattKontrollsakDtoUtleder wasNot called }
        verify { mockedOpphørMedEndringSammensattKontrollsakDtoUtleder wasNot called }
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
    fun `skal kaste feil for brevmaler som ikke er støttet`(brevmal: Brevmal) {
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
                sammensattKontrollsakBrevDtoUtleder.utled(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )
            }
        assertThat(exception.message).isEqualTo(
            "Brevmalen $brevmal er ikke støttet for sammensatte kontrollsaker",
        )
        verify { mockedOpphørtSammensattKontrollsakDtoUtleder wasNot called }
        verify { mockedOpphørMedEndringSammensattKontrollsakDtoUtleder wasNot called }
        verify { mockedVedtakEndringSammensattKontrollsakDtoUtleder wasNot called }
    }
}
