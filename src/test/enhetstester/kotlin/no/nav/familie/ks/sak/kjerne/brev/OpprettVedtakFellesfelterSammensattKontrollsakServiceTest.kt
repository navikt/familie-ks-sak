package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagKorrigertVedtak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettVedtakFellesfelterSammensattKontrollsakServiceTest {
    private val mockedOpprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService = mockk()
    private val mockedKorrigertVedtakService: KorrigertVedtakService = mockk()

    private val opprettVedtakFellesfelterSammensattKontrollsakService: OpprettVedtakFellesfelterSammensattKontrollsakService =
        OpprettVedtakFellesfelterSammensattKontrollsakService(
            opprettGrunnlagOgSignaturDataService = mockedOpprettGrunnlagOgSignaturDataService,
            korrigertVedtakService = mockedKorrigertVedtakService,
        )

    @Test
    fun `skal generere fellesfelter for sammensatt kontroll vedtak`() {
        // Arrange
        val vedtaksdato = LocalDate.of(2024, 8, 22)

        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
                fritekst = "fritekst",
            )

        val korrigertVedtak =
            lagKorrigertVedtak(
                behandling = vedtak.behandling,
                vedtaksdato = vedtaksdato,
            )

        val grunnlagOgSignaturData =
            GrunnlagOgSignaturData(
                grunnlag =
                    lagPersonopplysningGrunnlag(
                        søkerPersonIdent = "01014700311",
                        behandlingId = vedtak.behandling.id,
                        søkerNavn = "søkerNavn",
                    ),
                saksbehandler = "saksbehandler",
                beslutter = "beslutter",
                enhet = "enhet",
            )

        every {
            mockedOpprettGrunnlagOgSignaturDataService.opprett(vedtak)
        } returns grunnlagOgSignaturData

        every {
            mockedKorrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)
        } returns korrigertVedtak

        // Act
        val vedtakFellesfelterSammensattKontrollsak =
            opprettVedtakFellesfelterSammensattKontrollsakService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(vedtakFellesfelterSammensattKontrollsak.enhet).isEqualTo("enhet")
        assertThat(vedtakFellesfelterSammensattKontrollsak.saksbehandler).isEqualTo("saksbehandler")
        assertThat(vedtakFellesfelterSammensattKontrollsak.beslutter).isEqualTo("beslutter")
        assertThat(vedtakFellesfelterSammensattKontrollsak.søkerNavn).isEqualTo("søkerNavn")
        assertThat(vedtakFellesfelterSammensattKontrollsak.søkerFødselsnummer).isEqualTo("01014700311")
        assertThat(vedtakFellesfelterSammensattKontrollsak.korrigertVedtakData?.datoKorrigertVedtak).containsOnly("22. august 2024")
        assertThat(vedtakFellesfelterSammensattKontrollsak.sammensattKontrollsakFritekst).isEqualTo("fritekst")
    }
}
