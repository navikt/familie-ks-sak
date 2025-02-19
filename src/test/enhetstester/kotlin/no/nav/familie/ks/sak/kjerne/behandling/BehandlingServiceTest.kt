package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.called
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.oppgave.OppgaveService
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.feilutbetaltvaluta.FeilutbetaltValutaService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.brev.mottaker.BrevmottakerService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ks.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringstidspunktService
import no.nav.familie.ks.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import no.nav.familie.ks.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ks.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ks.sak.korrigertvedtak.KorrigertVedtakRepository
import no.nav.familie.ks.sak.statistikk.saksstatistikk.SakStatistikkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BehandlingServiceTest {
    private val mockBehandlingRepository = mockk<BehandlingRepository>()
    private val mockArbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val mockSøknadGrunnlagService = mockk<SøknadGrunnlagService>()
    private val mockStatsborgerskapService = mockk<StatsborgerskapService>()
    private val mockPersonopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val mockVilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val mockLoggService = mockk<LoggService>()
    private val mockAndelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val mockAndelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val mockVedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val mockVedtakRepository = mockk<VedtakRepository>()
    private val mockTotrinnskontrollRepository = mockk<TotrinnskontrollRepository>()
    private val mockTilbakekrevingRepository = mockk<TilbakekrevingRepository>()
    private val mockSanityService = mockk<SanityService>()
    private val mockFeilutbetaltValutaService = mockk<FeilutbetaltValutaService>()
    private val mockKompetanseRepository = mockk<KompetanseRepository>()
    private val mockRefusjonEøsService = mockk<RefusjonEøsService>()
    private val mockUtenlandskPeriodebeløpRepository = mockk<UtenlandskPeriodebeløpRepository>()
    private val mockValutakursRepository = mockk<ValutakursRepository>()
    private val mockKorrigertEtterbetalingRepository = mockk<KorrigertEtterbetalingRepository>()
    private val mockBrevmottakerService = mockk<BrevmottakerService>()
    private val mockOvergangsordningAndelService = mockk<OvergangsordningAndelService>()
    private val mockOppgaveService = mockk<OppgaveService>()
    private val mockSakStatistikkService = mockk<SakStatistikkService>()
    private val mockKorrigertVedtakRepository = mockk<KorrigertVedtakRepository>()
    private val mockAdopsjonService = mockk<AdopsjonService>()
    private val mockEndringstidspunktService = mockk<EndringstidspunktService>()

    private val behandlingService =
        BehandlingService(
            behandlingRepository = mockBehandlingRepository,
            arbeidsfordelingService = mockArbeidsfordelingService,
            søknadGrunnlagService = mockSøknadGrunnlagService,
            statsborgerskapService = mockStatsborgerskapService,
            personopplysningGrunnlagService = mockPersonopplysningGrunnlagService,
            vilkårsvurderingService = mockVilkårsvurderingService,
            loggService = mockLoggService,
            andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = mockAndelerTilkjentYtelseOgEndreteUtbetalingerService,
            vedtaksperiodeService = mockVedtaksperiodeService,
            vedtakRepository = mockVedtakRepository,
            totrinnskontrollRepository = mockTotrinnskontrollRepository,
            tilbakekrevingRepository = mockTilbakekrevingRepository,
            sanityService = mockSanityService,
            feilutbetaltValutaService = mockFeilutbetaltValutaService,
            kompetanseRepository = mockKompetanseRepository,
            refusjonEøsService = mockRefusjonEøsService,
            utenlandskPeriodebeløpRepository = mockUtenlandskPeriodebeløpRepository,
            valutakursRepository = mockValutakursRepository,
            korrigertEtterbetalingRepository = mockKorrigertEtterbetalingRepository,
            brevmottakerService = mockBrevmottakerService,
            overgangsordningAndelService = mockOvergangsordningAndelService,
            oppgaveService = mockOppgaveService,
            sakStatistikkService = mockSakStatistikkService,
            korrigertVedtakRepository = mockKorrigertVedtakRepository,
            adopsjonService = mockAdopsjonService,
            endringstidspunktService = mockEndringstidspunktService,
        )

    private val søker = randomAktør()
    private val søkersIdent = søker.personidenter.first { personIdent -> personIdent.aktiv }.fødselsnummer
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søknadsgrunnlagMockK = mockk<SøknadGrunnlag>()

    @BeforeEach
    fun beforeEach() {
        every { mockBehandlingRepository.hentBehandling(any()) } returns behandling
        every { mockBehandlingRepository.finnBehandlinger(any<Long>()) } returns emptyList()
        every { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns
            ArbeidsfordelingPåBehandling(
                behandlingId = behandling.id,
                behandlendeEnhetId = "enhet",
                behandlendeEnhetNavn = "enhetNavn",
            )
        every { mockArbeidsfordelingService.manueltOppdaterBehandlendeEnhet(any(), any()) } just runs
        every { mockStatsborgerskapService.hentLand(any()) } returns "Norge"
        every { mockPersonopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(any()) } returns
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkersIdent,
            )
        every { mockVilkårsvurderingService.finnAktivVilkårsvurdering(any()) } returns null
        every { mockSøknadGrunnlagService.finnAktiv(any()) } returns søknadsgrunnlagMockK
        mockkObject(SøknadGrunnlagMapper)
        with(SøknadGrunnlagMapper) {
            every { søknadsgrunnlagMockK.tilSøknadDto() } returns
                SøknadDto(
                    søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                    barnaMedOpplysninger =
                        listOf(
                            BarnMedOpplysningerDto(ident = "barn1"),
                            BarnMedOpplysningerDto("barn2"),
                        ),
                    "begrunnelse",
                )
        }

        every { mockVedtakRepository.findByBehandlingAndAktivOptional(any()) } returns Vedtak(behandling = behandling)

        every { mockVedtaksperiodeService.hentUtvidetVedtaksperioderMedBegrunnelser(any()) } returns emptyList()
        every { mockEndringstidspunktService.finnEndringstidspunktForBehandling(any()) } returns TIDENES_MORGEN

        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(
                lagAndelTilkjentYtelse(behandling = behandling),
            )
        every {
            mockAndelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns emptyList()
        every {
            mockAndelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerTilkjentYtelse(behandling.id)
        } returns emptyList()
        every { mockTotrinnskontrollRepository.findByBehandlingAndAktiv(any()) } returns mockk(relaxed = true)
        every { mockTilbakekrevingRepository.findByBehandlingId(any()) } returns null
        every { mockVedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(any()) } returns null
        every { mockFeilutbetaltValutaService.hentAlleFeilutbetaltValutaForBehandling(any()) } returns emptyList()
        every { mockSanityService.hentSanityBegrunnelser() } returns emptyList()
        every { mockKompetanseRepository.findByBehandlingId(any()) } returns emptyList()
        every { mockRefusjonEøsService.hentRefusjonEøsPerioder(any()) } returns emptyList()
        every { mockUtenlandskPeriodebeløpRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { mockValutakursRepository.findByBehandlingId(behandling.id) } returns emptyList()
        every { mockKorrigertEtterbetalingRepository.finnAktivtKorrigeringPåBehandling(behandling.id) } returns null
        every { mockKorrigertVedtakRepository.finnAktivtKorrigertVedtakPåBehandling(behandling.id) } returns null
        every { mockBrevmottakerService.hentBrevmottakere(any()) } returns emptyList()
        every { mockOvergangsordningAndelService.hentOvergangsordningAndeler(any()) } returns emptyList()
        every { mockAdopsjonService.hentAlleAdopsjonerForBehandling(any()) } returns emptyList()
    }

    @Test
    fun `lagBehandlingRespons - skal lage BehandlingResponsDto for behandling`() {
        val behandlingResponsDto = behandlingService.lagBehandlingRespons(behandling.id)

        assertNotNull(behandlingResponsDto)
        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { mockArbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { mockPersonopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandling.id) }
        verify(exactly = 1) { mockVilkårsvurderingService.finnAktivVilkårsvurdering(behandling.id) }
        verify(exactly = 1) { mockSøknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 1) { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) }
        verify(exactly = 1) { mockEndringstidspunktService.finnEndringstidspunktForBehandling(behandling) }
        verify(exactly = 1) { mockTilbakekrevingRepository.findByBehandlingId(behandling.id) }
        verify(exactly = 1) { mockVedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(behandling.id) }
        verify(exactly = 1) { mockKompetanseRepository.findByBehandlingId(behandling.id) }
        verify(exactly = 1) { mockBrevmottakerService.hentBrevmottakere(behandling.id) }
        verify(exactly = 1) { mockOvergangsordningAndelService.hentOvergangsordningAndeler(behandling.id) }

        assertTrue { behandlingResponsDto.personer.isNotEmpty() }
        assertEquals(1, behandlingResponsDto.personer.size)
        assertNotNull(behandlingResponsDto.søknadsgrunnlag)
        assertTrue { behandlingResponsDto.personerMedAndelerTilkjentYtelse.isNotEmpty() }
        assertNull(behandlingResponsDto.endringstidspunkt)
    }

    @Test
    fun `oppdaterBehandlendeEnhet - skal oppdatere behandlende enhet tilknyttet behandling ved hjelp av ArbeidsfordelingService`() {
        // Arrange
        val endreBehandlendeEnhetDto = EndreBehandlendeEnhetDto("nyEnhetId", "begrunnelse")

        // Act
        behandlingService.oppdaterBehandlendeEnhet(behandling.id, endreBehandlendeEnhetDto)

        // Assert
        verify(exactly = 1) {
            mockArbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
                behandling,
                endreBehandlendeEnhetDto,
            )
        }
    }

    @Test
    fun `oppdaterBehandlingsresultat skal oppdatere behandlingsresultat til INNVILGET og lage historikk på det`() {
        // Arrange
        every {
            mockLoggService.opprettVilkårsvurderingLogg(
                behandling = behandling,
                behandlingsForrigeResultat = Behandlingsresultat.IKKE_VURDERT,
                behandlingsNyResultat = Behandlingsresultat.INNVILGET,
            )
        } just runs
        every { mockBehandlingRepository.save(any()) } returns behandling.copy(resultat = Behandlingsresultat.INNVILGET)

        // Act && Assert
        val oppdatertBehandling =
            assertDoesNotThrow {
                behandlingService.oppdaterBehandlingsresultat(behandling.id, Behandlingsresultat.INNVILGET)
            }
        verify(exactly = 1) { mockLoggService.opprettVilkårsvurderingLogg(any(), any(), any()) }
        assertEquals(Behandlingsresultat.INNVILGET, oppdatertBehandling.resultat)
    }

    @Test
    fun `endreBehandlingstemaPåBehandling skal oppdatere kategori og oppgaver, opprette logg, og sende sakstatistikk hvis overstyrt kategori er annerledes`() {
        // Arrange
        every {
            mockLoggService.opprettEndretBehandlingstemaLogg(
                any(),
                BehandlingKategori.NASJONAL,
                BehandlingKategori.EØS,
            )
        } returns mockk()

        every { mockBehandlingRepository.hentBehandling(behandling.id) } returns behandling
        every { mockBehandlingRepository.save(behandling) } returns behandling
        every { mockOppgaveService.oppdaterBehandlingstypePåOppgaverFraBehandling(behandling) } just runs
        every { mockSakStatistikkService.sendMeldingOmEndringAvBehandlingkategori(behandling.id, BehandlingKategori.EØS) } just runs

        // Act
        val oppdatertBehandling =
            behandlingService.endreBehandlingstemaPåBehandling(behandlingId = behandling.id, BehandlingKategori.EØS)

        // Assert
        assertEquals(oppdatertBehandling.kategori, BehandlingKategori.EØS)

        verify(exactly = 1) { mockBehandlingRepository.hentBehandling(behandling.id) }
        verify(exactly = 1) {
            mockLoggService.opprettEndretBehandlingstemaLogg(
                any(),
                BehandlingKategori.NASJONAL,
                BehandlingKategori.EØS,
            )
        }
        verify(exactly = 1) { mockBehandlingRepository.save(behandling) }
        verify(exactly = 1) { mockOppgaveService.oppdaterBehandlingstypePåOppgaverFraBehandling(behandling) }
        verify(exactly = 1) { mockSakStatistikkService.sendMeldingOmEndringAvBehandlingkategori(behandling.id, BehandlingKategori.EØS) }
    }

    @Test
    fun `endreBehandlingstemaPåBehandling skal ikke oppdatere kategori og opprette logg hvis overstyrt kategori er lik`() {
        // Arrange
        every { mockBehandlingRepository.hentBehandling(behandling.id) } returns behandling

        // Act
        val oppdatertBehandling =
            behandlingService.endreBehandlingstemaPåBehandling(
                behandlingId = behandling.id,
                BehandlingKategori.NASJONAL,
            )

        // Assert
        assertEquals(oppdatertBehandling.kategori, BehandlingKategori.NASJONAL)

        verify(exactly = 1) { mockBehandlingRepository.hentBehandling(behandling.id) }
        verify { mockLoggService wasNot called }
        verify(exactly = 0) { mockBehandlingRepository.save(behandling) }
    }
}
