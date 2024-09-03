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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OpprettSammensattKontrollsakBrevDtoServiceTest {
    private val mockedBrevmalService: BrevmalService = mockk()
    private val mockedOpprettOpphørtSammensattKontrollsakDtoService: OpprettOpphørtSammensattKontrollsakDtoService = mockk()
    private val mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService: OpprettOpphørMedEndringSammensattKontrollsakDtoService = mockk()
    private val mockedOpprettVedtakEndringSammensattKontrollsakDtoService: OpprettVedtakEndringSammensattKontrollsakDtoService = mockk()
    private val opprettSammensattKontrollsakBrevDtoService =
        OpprettSammensattKontrollsakBrevDtoService(
            brevmalService = mockedBrevmalService,
            opprettOpphørtSammensattKontrollsakDtoService = mockedOpprettOpphørtSammensattKontrollsakDtoService,
            opprettOpphørMedEndringSammensattKontrollsakDtoService = mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService,
            opprettVedtakEndringSammensattKontrollsakDtoService = mockedOpprettVedtakEndringSammensattKontrollsakDtoService,
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
            mockedOpprettOpphørtSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørtSammensattKontrollsakDto

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørtSammensattKontrollsakDto)
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakDtoService wasNot called }
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
            mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns opphørMedEndringSammensattKontrollsakDto

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(opphørMedEndringSammensattKontrollsakDto)
        verify { mockedOpprettOpphørtSammensattKontrollsakDtoService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakDtoService wasNot called }
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
            mockedOpprettVedtakEndringSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns vedtakEndringSammensattKontrollsakDto

        // Act
        val brevDto =
            opprettSammensattKontrollsakBrevDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(brevDto).isEqualTo(vedtakEndringSammensattKontrollsakDto)
        verify { mockedOpprettOpphørtSammensattKontrollsakDtoService wasNot called }
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService wasNot called }
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
                opprettSammensattKontrollsakBrevDtoService.opprett(
                    vedtak = vedtak,
                    sammensattKontrollsak = sammensattKontrollsak,
                )
            }
        assertThat(exception.message).isEqualTo(
            "Brevmalen $brevmal er ikke støttet for sammensatte kontrollsaker",
        )
        verify { mockedOpprettOpphørtSammensattKontrollsakDtoService wasNot called }
        verify { mockedOpprettOpphørMedEndringSammensattKontrollsakDtoService wasNot called }
        verify { mockedOpprettVedtakEndringSammensattKontrollsakDtoService wasNot called }
    }
}
