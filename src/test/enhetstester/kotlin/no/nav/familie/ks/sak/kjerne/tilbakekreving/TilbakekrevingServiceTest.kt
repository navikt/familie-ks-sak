package no.nav.familie.ks.sak.kjerne.tilbakekreving

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.Brevmottaker
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.KanBehandlingOpprettesManueltRespons
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.ks.sak.api.dto.ForhåndsvisTilbakekrevingVarselbrevDto
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
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
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerDb
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerRepository
import no.nav.familie.ks.sak.kjerne.brev.mottaker.MottakerType
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ks.sak.sikkerhet.SikkerhetContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.stream.Stream

internal class TilbakekrevingServiceTest {
    private val tilbakekrevingKlient = mockk<TilbakekrevingKlient>()
    private val tilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val totrinnskontrollRepository = mockk<TotrinnskontrollRepository>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val simuleringService = mockk<SimuleringService>()
    private val brevmottakerRepository = mockk<BrevmottakerRepository>(relaxed = true)

    private val tilbakekrevingService =
        TilbakekrevingService(
            tilbakekrevingKlient = tilbakekrevingKlient,
            tilbakekrevingRepository = tilbakekrevingRepository,
            vedtakRepository = vedtakRepository,
            totrinnskontrollRepository = totrinnskontrollRepository,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            arbeidsfordelingService = arbeidsfordelingService,
            simuleringService = simuleringService,
            brevmottakerRepository = brevmottakerRepository,
        )

    private val søker = randomFnr()
    private val behandling = lagBehandling()
    private val vedtak = Vedtak(behandling = behandling, vedtaksdato = LocalDateTime.now())
    private val forhåndsvisVarselbrevRequestSlot = slot<ForhåndsvisVarselbrevRequest>()
    private val personopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker,
        )
    private val arbeidsfordeling = lagArbeidsfordelingPåBehandling(behandlingId = behandling.id)
    private val førsteFeilPostering =
        lagPostering(
            YearMonth.of(2022, 1),
            YearMonth.of(2022, 1),
            BigDecimal("7500"),
        )

    private val andreFeilPostering =
        lagPostering(
            YearMonth.of(2022, 2),
            YearMonth.of(2022, 2),
            BigDecimal("4500"),
        )
    private val tredjeFeilPostering =
        lagPostering(
            YearMonth.of(2022, 4),
            YearMonth.of(2022, 4),
            BigDecimal("4500"),
        )
    private val ytelsePostering =
        lagPostering(
            YearMonth.of(2022, 5),
            YearMonth.of(2022, 5),
            BigDecimal("4500"),
            PosteringType.YTELSE,
        )
    private val økonomiSimuleringMottaker =
        lagØkonomiSimuleringMottaker(
            behandling = behandling,
            økonomiSimuleringPosteringer = listOf(førsteFeilPostering, andreFeilPostering, tredjeFeilPostering, ytelsePostering),
        )

    @BeforeEach
    fun setup() {
        every { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) } returns vedtak
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns personopplysningGrunnlag
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) } returns arbeidsfordeling
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal(16500)
        every { simuleringService.hentSimuleringPåBehandling(behandling.id) } returns listOf(økonomiSimuleringMottaker)
        every { tilbakekrevingKlient.opprettTilbakekrevingBehandling(any()) } returns "100001"
        every { totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) } returns
            Totrinnskontroll(behandling = behandling, saksbehandler = "test", saksbehandlerId = "Z0000")
    }

    @Test
    fun `hentForhåndsvisningTilbakekrevingVarselBrev henter forhåndvisning av varselbrev med feilutbetalte perioder`() {
        every {
            tilbakekrevingKlient.hentForhåndsvisningTilbakekrevingVarselbrev(capture(forhåndsvisVarselbrevRequestSlot))
        } returns ByteArray(10)

        tilbakekrevingService.hentForhåndsvisningTilbakekrevingVarselBrev(
            behandling.id,
            ForhåndsvisTilbakekrevingVarselbrevDto("fritekst"),
        )
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
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
        perioder.harPeriode(YearMonth.of(2022, 1), YearMonth.of(2022, 2))
        perioder.harPeriode(YearMonth.of(2022, 4), YearMonth.of(2022, 4))
    }

    @Test
    fun `sendOpprettTilbakekrevingRequest sender OpprettTilbakekreving request med varsel`() {
        val requestSlot = slot<OpprettTilbakekrevingRequest>()
        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns
            lagTilbakekreving(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, "test")

        tilbakekrevingService.sendOpprettTilbakekrevingRequest(behandling)

        verify(exactly = 1) { tilbakekrevingKlient.opprettTilbakekrevingBehandling(capture(requestSlot)) }
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { simuleringService.hentSimuleringPåBehandling(behandling.id) }
        verify(exactly = 1) { totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) }
        verify(exactly = 1) { tilbakekrevingRepository.findByBehandlingId(behandling.id) }

        val request = requestSlot.captured
        assertEquals(Fagsystem.KONT, request.fagsystem)
        assertEquals(Regelverk.NASJONAL, request.regelverk)
        assertEquals(Ytelsestype.KONTANTSTØTTE, request.ytelsestype)
        assertEquals(behandling.fagsak.id, request.eksternFagsakId.toLong())
        assertEquals(søker, request.personIdent)
        assertEquals(behandling.id, request.eksternId.toLong())
        assertEquals(Behandlingstype.TILBAKEKREVING, request.behandlingstype)
        assertFalse(request.manueltOpprettet)
        assertEquals(Språkkode.NB.name, request.språkkode.name)
        assertEquals(arbeidsfordeling.behandlendeEnhetId, request.enhetId)
        assertEquals(arbeidsfordeling.behandlendeEnhetNavn, request.enhetsnavn)
        assertEquals("Z0000", request.saksbehandlerIdent)
        assertEquals(LocalDate.now(), request.revurderingsvedtaksdato)

        assertNotNull(request.varsel)
        val varsel = request.varsel
        assertEquals("test", varsel!!.varseltekst)
        assertEquals(BigDecimal("16500"), varsel.sumFeilutbetaling)
        varsel.perioder.harPeriode(YearMonth.of(2022, 1), YearMonth.of(2022, 2))
        varsel.perioder.harPeriode(YearMonth.of(2022, 4), YearMonth.of(2022, 4))

        val faktainfo = request.faktainfo
        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, faktainfo.tilbakekrevingsvalg)
        assertEquals(behandling.opprettetÅrsak.visningsnavn, faktainfo.revurderingsårsak)
        assertEquals(behandling.resultat.displayName, faktainfo.revurderingsresultat)
    }

    @Test
    fun `sendOpprettTilbakekrevingRequest sender OpprettTilbakekreving request uten varsel`() {
        val requestSlot = slot<OpprettTilbakekrevingRequest>()
        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns
            lagTilbakekreving(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)

        tilbakekrevingService.sendOpprettTilbakekrevingRequest(behandling)

        verify(exactly = 1) { tilbakekrevingKlient.opprettTilbakekrevingBehandling(capture(requestSlot)) }
        verify(exactly = 1) { vedtakRepository.findByBehandlingAndAktivOptional(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 0) { simuleringService.hentSimuleringPåBehandling(behandling.id) }
        verify(exactly = 1) { totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id) }
        verify(exactly = 1) { tilbakekrevingRepository.findByBehandlingId(behandling.id) }

        val request = requestSlot.captured
        assertEquals(Fagsystem.KONT, request.fagsystem)
        assertEquals(Regelverk.NASJONAL, request.regelverk)
        assertEquals(Ytelsestype.KONTANTSTØTTE, request.ytelsestype)
        assertEquals(behandling.fagsak.id, request.eksternFagsakId.toLong())
        assertEquals(søker, request.personIdent)
        assertEquals(behandling.id, request.eksternId.toLong())
        assertEquals(Behandlingstype.TILBAKEKREVING, request.behandlingstype)
        assertFalse(request.manueltOpprettet)
        assertEquals(Språkkode.NB.name, request.språkkode.name)
        assertEquals(arbeidsfordeling.behandlendeEnhetId, request.enhetId)
        assertEquals(arbeidsfordeling.behandlendeEnhetNavn, request.enhetsnavn)
        assertEquals("Z0000", request.saksbehandlerIdent)
        assertEquals(LocalDate.now(), request.revurderingsvedtaksdato)
        assertNull(request.varsel)

        val faktainfo = request.faktainfo
        assertEquals(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL, faktainfo.tilbakekrevingsvalg)
        assertEquals(behandling.opprettetÅrsak.visningsnavn, faktainfo.revurderingsårsak)
        assertEquals(behandling.resultat.displayName, faktainfo.revurderingsresultat)
    }

    @Test
    fun `opprettTilbakekrevingsbehandlingManuelt skal ikke opprette når kanBehandlingOpprettesManuelt returnerer med false respons`() {
        every { tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(behandling.fagsak.id) } returns
            KanBehandlingOpprettesManueltRespons(kanBehandlingOpprettes = false, melding = "feilmelding")

        val exception =
            assertThrows<FunksjonellFeil> {
                tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(behandling.fagsak.id)
            }
        assertEquals("Tilbakekrevingsbehandling manuelt kan ikke opprettes pga feilmelding", exception.melding)
        assertEquals("feilmelding", exception.frontendFeilmelding)
    }

    @Test
    fun `opprettTilbakekrevingsbehandlingManuelt skal ikke opprette når kanBehandlingOpprettesManuelt ikke returnerer referanse`() {
        every { tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(behandling.fagsak.id) } returns
            KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = true,
                melding = "Det er mulig å opprette behandling manuelt.",
                kravgrunnlagsreferanse = null,
            )

        val exception =
            assertThrows<Feil> {
                tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(behandling.fagsak.id)
            }
        assertEquals(
            "Tilbakekrevingsbehandling kan opprettes, men har ikke kravgrunnlagsreferanse på respons-en",
            exception.message,
        )
    }

    @Test
    fun `opprettTilbakekrevingsbehandlingManuelt skal ikke opprette når kanBehandlingOpprettesManuelt returnerer med feil referanse`() {
        val kravgrunnlagsreferanse = "123"
        every { tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(behandling.fagsak.id) } returns
            KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = true,
                melding = "Det er mulig å opprette behandling manuelt.",
                kravgrunnlagsreferanse = kravgrunnlagsreferanse,
            )
        every { vedtakRepository.findByBehandlingAndAktivOptional(kravgrunnlagsreferanse.toLong()) } returns null

        val exception =
            assertThrows<FunksjonellFeil> {
                tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(behandling.fagsak.id)
            }
        assertEquals(
            "Tilbakekrevingsbehandling kan ikke opprettes. " +
                "Respons inneholder enten en referanse til en ukjent behandling " +
                "eller behandling $kravgrunnlagsreferanse er ikke vedtatt",
            exception.melding,
        )
        assertEquals(
            "Av tekniske årsaker så kan ikke tilbakekrevingsbehandling opprettes. " +
                "Kontakt brukerstøtte for å rapportere feilen",
            exception.frontendFeilmelding,
        )
    }

    @Test
    fun `opprettTilbakekrevingsbehandlingManuelt skal opprette tilbakekrevingsbehandling`() {
        val requestSlot = slot<OpprettManueltTilbakekrevingRequest>()
        every { tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(behandling.fagsak.id) } returns
            KanBehandlingOpprettesManueltRespons(
                kanBehandlingOpprettes = true,
                melding = "Det er mulig å opprette behandling manuelt.",
                kravgrunnlagsreferanse = behandling.id.toString(),
            )
        every { tilbakekrevingKlient.opprettTilbakekrevingsbehandlingManuelt(any()) } returns ""

        tilbakekrevingService.opprettTilbakekrevingsbehandlingManuelt(behandling.fagsak.id)

        verify(exactly = 1) { tilbakekrevingKlient.opprettTilbakekrevingsbehandlingManuelt(capture(requestSlot)) }

        val request = requestSlot.captured
        assertEquals(behandling.id.toString(), request.eksternId)
        assertEquals(behandling.fagsak.id.toString(), request.eksternFagsakId)
        assertEquals(Ytelsestype.KONTANTSTØTTE, request.ytelsestype)
    }

    @ParameterizedTest
    @ArgumentsSource(TestProvider::class)
    fun `lagOpprettTilbakekrevingRequest sender brevmottakere i kall mot familie-tilbake`(arguments: Pair<MottakerType, Vergetype>) {
        val opprettTilbakekrevingRequest = slot<OpprettTilbakekrevingRequest>()
        val brevmottaker =
            BrevmottakerDb(
                behandlingId = behandling.id,
                type = arguments.first,
                navn = "Donald Duck",
                adresselinje1 = "Andebyveien 1",
                postnummer = "0000",
                poststed = "OSLO",
                landkode = "NO",
            )

        every { tilbakekrevingRepository.findByBehandlingId(behandling.id) } returns
            lagTilbakekreving(Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL, "test")
        every { brevmottakerRepository.finnBrevMottakereForBehandling(behandling.id) } returns
            listOf(brevmottaker)

        tilbakekrevingService.sendOpprettTilbakekrevingRequest(behandling)

        verify(exactly = 1) {
            tilbakekrevingKlient.opprettTilbakekrevingBehandling(capture(opprettTilbakekrevingRequest))
        }

        val actualBrevmottaker = opprettTilbakekrevingRequest.captured.manuelleBrevmottakere.single()

        assertBrevmottakerEquals(brevmottaker, actualBrevmottaker)
        assertEquals(arguments.second, actualBrevmottaker.vergetype)
    }

    private fun lagPostering(
        fom: YearMonth,
        tom: YearMonth,
        beløp: BigDecimal,
        posteringType: PosteringType = PosteringType.FEILUTBETALING,
    ) = lagØkonomiSimuleringPostering(
        behandling = behandling,
        fom = fom.førsteDagIInneværendeMåned(),
        tom = tom.atEndOfMonth(),
        beløp = beløp,
        forfallsdato = LocalDate.now().plusMonths(5),
        posteringType = posteringType,
    )

    private fun lagTilbakekreving(
        valg: Tilbakekrevingsvalg,
        varseltekst: String? = null,
    ) = Tilbakekreving(
        behandling = behandling,
        valg = valg,
        varsel = varseltekst,
        begrunnelse = "test begrunnelse",
        tilbakekrevingsbehandlingId = null,
    )

    private fun List<Periode>.harPeriode(
        fom: YearMonth,
        tom: YearMonth,
    ) = assertTrue {
        this.any {
            it.fom == fom.førsteDagIInneværendeMåned() &&
                it.tom == tom.atEndOfMonth()
        }
    }

    private fun assertBrevmottakerEquals(
        expected: BrevmottakerDb,
        actual: Brevmottaker,
    ) {
        assertEquals(expected.navn, actual.navn)
        assertEquals(expected.type.name, actual.type.name)
        assertEquals(expected.adresselinje1, actual.manuellAdresseInfo?.adresselinje1)
        assertEquals(expected.postnummer, actual.manuellAdresseInfo?.postnummer)
        assertEquals(expected.poststed, actual.manuellAdresseInfo?.poststed)
        assertEquals(expected.landkode, actual.manuellAdresseInfo?.landkode)
    }

    private class TestProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
            Stream.of(
                Arguments.of(Pair(MottakerType.FULLMEKTIG, Vergetype.ANNEN_FULLMEKTIG)),
                Arguments.of(Pair(MottakerType.VERGE, Vergetype.VERGE_FOR_VOKSEN)),
                Arguments.of(Pair(MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, null)),
            )
    }
}
