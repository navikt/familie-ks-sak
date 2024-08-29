package no.nav.familie.ks.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.data.lagSammensattKontrollsak
import no.nav.familie.ks.sak.data.lagVedtak
import no.nav.familie.ks.sak.data.lagVedtakFellesfelterSammensattKontrollsakDto
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsAvklart
import no.nav.familie.ks.sak.kjerne.brev.domene.maler.RefusjonEøsUavklart
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpprettVedtakEndringSammensattKontrollsakDtoServiceTest {
    private val mockedSøkersMeldepliktService: SøkersMeldepliktService = mockk()
    private val mockedOpprettVedtakFellesfelterSammensattKontrollsakDtoService: OpprettVedtakFellesfelterSammensattKontrollsakDtoService = mockk()
    private val mockedEtterbetalingService: EtterbetalingService = mockk()
    private val mockedSimuleringService: SimuleringService = mockk()
    private val mockedVedtaksperiodeService: VedtaksperiodeService = mockk()
    private val mockedFeilutbetaltValutaService: FeilutbetaltValutaService = mockk()
    private val mockedBrevPeriodeService: BrevPeriodeService = mockk()
    private val opprettVedtakEndringSammensattKontrollsakDtoService: OpprettVedtakEndringSammensattKontrollsakDtoService =
        OpprettVedtakEndringSammensattKontrollsakDtoService(
            søkersMeldepliktService = mockedSøkersMeldepliktService,
            opprettVedtakFellesfelterSammensattKontrollsakDtoService = mockedOpprettVedtakFellesfelterSammensattKontrollsakDtoService,
            etterbetalingService = mockedEtterbetalingService,
            simuleringService = mockedSimuleringService,
            vedtaksperiodeService = mockedVedtaksperiodeService,
            feilutbetaltValutaService = mockedFeilutbetaltValutaService,
            brevPeriodeService = mockedBrevPeriodeService,
        )

    @Test
    fun `skal opprette VedtakEndringSammensattKontrollsak hvor søker har meldt fra om barnehageplass og det er feilutbetalt valuta periode`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedSøkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                vedtak = vedtak
            )
        } returns true

        every {
            mockedSøkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                vedtak = vedtak,
            )
        } returns false

        every {
            mockedOpprettVedtakFellesfelterSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns lagVedtakFellesfelterSammensattKontrollsakDto()

        val etterbetaling = Etterbetaling("1000")

        every {
            mockedEtterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )
        } returns etterbetaling

        every {
            mockedSimuleringService.erFeilutbetalingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns false

        every {
            mockedVedtaksperiodeService.skalHaÅrligKontroll(
                vedtak = vedtak,
            )
        } returns false

        every {
            mockedFeilutbetaltValutaService.beskrivPerioderMedFeilutbetaltValuta(
                behandlingId = vedtak.behandling.id,
            )
        } returns setOf("22. august 2024")

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
        val vedtakEndringSammensattKontrollsak =
            opprettVedtakEndringSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(vedtakEndringSammensattKontrollsak.mal).isEqualTo(Brevmal.VEDTAK_ENDRING)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.enhet).containsOnly("enhet")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.saksbehandler).containsOnly("saksbehandler")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.beslutter).containsOnly("beslutter")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.etterbetaling).isEqualTo(etterbetaling)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.feilutbetaling).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.klage).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.korrigertVedtak).isNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.informasjonOmAarligKontroll).isFalse()
        assertThat(
            vedtakEndringSammensattKontrollsak.data.delmalData.forMyeUtbetaltKontantstotte
                ?.perioderMedForMyeUtbetalt,
        ).containsOnly("22. august 2024")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.refusjonEosAvklart).isEqualTo(refusjonEøsAvklart)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.refusjonEosUavklart).isEqualTo(refusjonEøsUavklart)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaMeldeFraOmEndringerEosSelvstendigRett).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaMeldeFraOmEndringer).isTrue()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.navn).containsOnly("søkerNavn")
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.fodselsnummer).containsOnly("søkerFødselsnummer")
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.brevOpprettetDato).isNotNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.gjelder).isNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.sammensattKontrollsakFritekst).isEqualTo("sammensattKontrollsakFritekst")
    }

    @Test
    fun `skal opprette VedtakEndringSammensattKontrollsak hvor søker olle har meldt fra om barnehageplass og det ikke er feilutbetalt valuta periode`() {
        // Arrange
        val vedtak = lagVedtak()

        val sammensattKontrollsak =
            lagSammensattKontrollsak(
                behandlingId = vedtak.behandling.id,
            )

        every {
            mockedSøkersMeldepliktService.harSøkerMeldtFraOmBarnehagePlass(
                vedtak = vedtak
            )
        } returns false

        every {
            mockedSøkersMeldepliktService.skalSøkerMeldeFraOmEndringerEøsSelvstendigRett(
                vedtak = vedtak,
            )
        } returns false

        every {
            mockedOpprettVedtakFellesfelterSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )
        } returns lagVedtakFellesfelterSammensattKontrollsakDto()

        val etterbetaling = Etterbetaling("1000")

        every {
            mockedEtterbetalingService.hentEtterbetaling(
                vedtak = vedtak,
            )
        } returns etterbetaling

        every {
            mockedSimuleringService.erFeilutbetalingPåBehandling(
                behandlingId = vedtak.behandling.id,
            )
        } returns false

        every {
            mockedVedtaksperiodeService.skalHaÅrligKontroll(
                vedtak = vedtak,
            )
        } returns false

        every {
            mockedFeilutbetaltValutaService.beskrivPerioderMedFeilutbetaltValuta(
                behandlingId = vedtak.behandling.id,
            )
        } returns null

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
        val vedtakEndringSammensattKontrollsak =
            opprettVedtakEndringSammensattKontrollsakDtoService.opprett(
                vedtak = vedtak,
                sammensattKontrollsak = sammensattKontrollsak,
            )

        // Assert
        assertThat(vedtakEndringSammensattKontrollsak.mal).isEqualTo(Brevmal.VEDTAK_ENDRING)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.enhet).containsOnly("enhet")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.saksbehandler).containsOnly("saksbehandler")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.signaturVedtak.beslutter).containsOnly("beslutter")
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.etterbetaling).isEqualTo(etterbetaling)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.feilutbetaling).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.klage).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.korrigertVedtak).isNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.informasjonOmAarligKontroll).isFalse()
        assertThat(
            vedtakEndringSammensattKontrollsak.data.delmalData.forMyeUtbetaltKontantstotte
                ?.perioderMedForMyeUtbetalt,
        ).isNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.refusjonEosAvklart).isEqualTo(refusjonEøsAvklart)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.refusjonEosUavklart).isEqualTo(refusjonEøsUavklart)
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaMeldeFraOmEndringerEosSelvstendigRett).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaMeldeFraOmEndringer).isFalse()
        assertThat(vedtakEndringSammensattKontrollsak.data.delmalData.duMaaGiNavBeskjedHvisBarnetDittFaarTildeltBarnehageplass).isTrue()
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.navn).containsOnly("søkerNavn")
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.fodselsnummer).containsOnly("søkerFødselsnummer")
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.brevOpprettetDato).isNotNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.flettefelter.gjelder).isNull()
        assertThat(vedtakEndringSammensattKontrollsak.data.sammensattKontrollsakFritekst).isEqualTo("sammensattKontrollsakFritekst")
    }
}
