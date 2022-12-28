package no.nav.familie.ks.sak.kjerne.tilbakekreving

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagØkonomiSimuleringMottaker
import no.nav.familie.ks.sak.data.lagØkonomiSimuleringPostering
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.integrasjon.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.simulering.SimuleringService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
internal class TilbakekrevingServiceTest {

    @MockK
    private lateinit var tilbakekrevingKlient: TilbakekrevingKlient

    @MockK
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var simuleringService: SimuleringService

    @InjectMockKs
    private lateinit var tilbakekrevingService: TilbakekrevingService

    private val søker = randomFnr()
    private val behandling = lagBehandling()
    private val vedtak = Vedtak(behandling = behandling)
    private val forhåndsvisVarselbrevRequestSlot = slot<ForhåndsvisVarselbrevRequest>()
    private val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søker
    )
    private val arbeidsfordeling = lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
    private val førsteFeilPostering = lagPostering(
        YearMonth.of(2022, 1),
        YearMonth.of(2022, 1),
        BigDecimal("7500")
    )

    private val andreFeilPostering = lagPostering(
        YearMonth.of(2022, 2),
        YearMonth.of(2022, 2),
        BigDecimal("4500")
    )
    private val tredjeFeilPostering = lagPostering(
        YearMonth.of(2022, 4),
        YearMonth.of(2022, 4),
        BigDecimal("4500")
    )
    private val ytelsePostering = lagPostering(
        YearMonth.of(2022, 5),
        YearMonth.of(2022, 5),
        BigDecimal("4500"),
        PosteringType.YTELSE
    )
    private val økonomiSimuleringMottaker = lagØkonomiSimuleringMottaker(
        behandling = behandling,
        økonomiSimuleringPosteringer = listOf(førsteFeilPostering, andreFeilPostering, tredjeFeilPostering, ytelsePostering)
    )

    @Test
    fun `hentForhåndsvisningTilbakekrevingVarselBrev henter forhåndvisning av varselbrev med feilutbetalte perioder`() {
        every { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) } returns vedtak
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns personopplysningGrunnlag
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) } returns arbeidsfordeling
        every {
            tilbakekrevingKlient.hentForhåndsvisningTilbakekrevingVarselbrev(capture(forhåndsvisVarselbrevRequestSlot))
        } returns ByteArray(10)
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(16500)
        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns listOf(økonomiSimuleringMottaker)

        tilbakekrevingService.hentForhåndsvisningTilbakekrevingVarselBrev(
            behandling.id,
            ForhåndsvisTilbakekrevingVarselbrevDto("fritekst")
        )
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { tilbakekrevingKlient.hentForhåndsvisningTilbakekrevingVarselbrev(any()) }
        verify(exactly = 1) { simuleringService.hentFeilutbetaling(behandling.id) }
        verify(exactly = 1) { simuleringService.hentSimuleringPåBehandling(behandling.id) }

        val request = forhåndsvisVarselbrevRequestSlot.captured
        assertEquals("fritekst", request.varseltekst)
        assertEquals(Ytelsestype.KONTANTSTØTTE, request.ytelsestype)
        assertEquals(arbeidsfordeling.behandlendeEnhetId, request.behandlendeEnhetId)
        assertEquals(arbeidsfordeling.behandlendeEnhetNavn, request.behandlendeEnhetsNavn)
        assertEquals(arbeidsfordeling.behandlendeEnhetNavn, request.behandlendeEnhetsNavn)
        assertEquals(Målform.NB.name, request.språkkode.name)
        assertEquals(Fagsystem.KONT, request.fagsystem)
        assertEquals(behandling.fagsak.id, request.eksternFagsakId.toLong())
        assertEquals(søker, request.ident)
        assertEquals(SikkerhetContext.SYSTEM_NAVN, request.saksbehandlerIdent)
        assertNull(request.institusjon)
        assertNotNull(request.feilutbetaltePerioderDto)
        assertEquals(16500, request.feilutbetaltePerioderDto.sumFeilutbetaling)

        val perioder = request.feilutbetaltePerioderDto.perioder
        assertTrue { perioder.size == 2 }
        assertTrue {
            perioder.any {
                it.fom == YearMonth.of(2022, 1).førsteDagIInneværendeMåned() &&
                    it.tom == YearMonth.of(2022, 2).atEndOfMonth()
            }
        }
        assertTrue {
            perioder.any {
                it.fom == YearMonth.of(2022, 4).førsteDagIInneværendeMåned() &&
                    it.tom == YearMonth.of(2022, 4).atEndOfMonth()
            }
        }
    }

    private fun lagPostering(
        fom: YearMonth,
        tom: YearMonth,
        beløp: BigDecimal,
        posteringType: PosteringType = PosteringType.FEILUTBETALING
    ) = lagØkonomiSimuleringPostering(
        behandling = behandling,
        fom = fom.førsteDagIInneværendeMåned(),
        tom = tom.atEndOfMonth(),
        beløp = beløp,
        forfallsdato = LocalDate.now().plusMonths(5),
        posteringType = posteringType
    )
}
