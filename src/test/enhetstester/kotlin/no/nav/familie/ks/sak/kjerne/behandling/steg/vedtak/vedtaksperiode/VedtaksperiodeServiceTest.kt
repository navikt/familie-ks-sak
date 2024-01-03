package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagInitieltTilkjentYtelse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeMedBegrunnelserService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class VedtaksperiodeServiceTest {
    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @MockK
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @MockK
    private lateinit var sanityService: SanityService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var utbetalingsperiodeMedBegrunnelserService: UtbetalingsperiodeMedBegrunnelserService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var integrasjonClient: IntegrasjonClient

    @MockK
    private lateinit var refusjonEøsRepository: RefusjonEøsRepository

    @MockK
    private lateinit var kompetanseService: KompetanseService

    @InjectMockKs
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        behandling = lagBehandling()
    }

    @Test
    fun `oppdaterVedtaksperiodeMedFritekster skal sette fritekster på eksisterende vedtaksperiode`() {
        val mocketVedtaksperiode = mockk<VedtaksperiodeMedBegrunnelser>()

        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) } returns mocketVedtaksperiode
        every { vedtaksperiodeHentOgPersisterService.lagre(mocketVedtaksperiode) } returns mocketVedtaksperiode
        every { mocketVedtaksperiode.settFritekster(any()) } returns mockk()
        every { mocketVedtaksperiode.vedtak } returns mockk()

        vedtaksperiodeService.oppdaterVedtaksperiodeMedFritekster(0, listOf("test", "test2"))

        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) }
        verify(exactly = 1) { vedtaksperiodeHentOgPersisterService.lagre(mocketVedtaksperiode) }
        verify(exactly = 1) { mocketVedtaksperiode.settFritekster(any()) }
    }

    @ParameterizedTest
    @EnumSource(
        value = NasjonalEllerFellesBegrunnelse::class,
        names = ["AVSLAG_UREGISTRERT_BARN", "OPPHØR_FULLTIDSPLASS_I_BARNEHAGE"],
    )
    fun `oppdaterVedtaksperiodeMedBegrunnelser skal kaste feil dersom begrunnelse ikke er tillatt for vedtaksperiode type`(
        nasjonalEllerFellesBegrunnelse: NasjonalEllerFellesBegrunnelse,
    ) {
        val vedtaksperiodeMedBegrunnelse =
            VedtaksperiodeMedBegrunnelser(
                id = 0,
                vedtak = Vedtak(id = 0, behandling = behandling),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val mocketPersonOpplysningGrunnlag = mockk<PersonopplysningGrunnlag>()

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns mocketPersonOpplysningGrunnlag
        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) } returns vedtaksperiodeMedBegrunnelse

        val feil =
            assertThrows<Feil> {
                vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(1, listOf(nasjonalEllerFellesBegrunnelse))
            }

        assertThat(
            feil.message,
            Is("Begrunnelsestype ${nasjonalEllerFellesBegrunnelse.begrunnelseType} passer ikke med typen 'UTBETALING' som er satt på perioden."),
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = NasjonalEllerFellesBegrunnelse::class,
        names = ["INNVILGET_IKKE_BARNEHAGE", "INNVILGET_IKKE_BARNEHAGE_ADOPSJON", "INNVILGET_DELTID_BARNEHAGE"],
    )
    fun `oppdaterVedtaksperiodeMedBegrunnelser skal oppdatere vedtaksperioder dersom begrunnelse er tillatt for vedtakstype`(
        nasjonalEllerFellesBegrunnelse: NasjonalEllerFellesBegrunnelse,
    ) {
        val vedtaksperiodeMedBegrunnelse =
            VedtaksperiodeMedBegrunnelser(
                id = 0,
                vedtak = Vedtak(id = 0, behandling = behandling),
                type = Vedtaksperiodetype.UTBETALING,
            )

        val mocketPersonOpplysningGrunnlag = mockk<PersonopplysningGrunnlag>()

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns mocketPersonOpplysningGrunnlag
        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) } returns vedtaksperiodeMedBegrunnelse
        every { vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelse) } returns vedtaksperiodeMedBegrunnelse

        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(1, listOf(nasjonalEllerFellesBegrunnelse))

        verify { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) }
        verify { vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelse) }
    }

    @Test
    fun `oppdaterVedtakMedVedtaksperioder skal slette eksisterende vedtaksperioder for vedtak og lage ny`() {
        behandling.resultat = Behandlingsresultat.FORTSATT_INNVILGET

        val mocketVedtak = mockk<Vedtak>()
        val vedtaksperiodeMedBegrunnelseSlot = slot<VedtaksperiodeMedBegrunnelser>()

        every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(mocketVedtak) } just runs
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(vedtaksperiodeMedBegrunnelseSlot)) } returns mockk()
        every { mocketVedtak.behandling } returns behandling

        vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(mocketVedtak)

        val lagretVedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelseSlot.captured

        assertThat(lagretVedtaksperiodeMedBegrunnelser.fom, Is(nullValue()))
        assertThat(lagretVedtaksperiodeMedBegrunnelser.tom, Is(nullValue()))
        assertThat(lagretVedtaksperiodeMedBegrunnelser.vedtak, Is(mocketVedtak))
        assertThat(lagretVedtaksperiodeMedBegrunnelser.type, Is(Vedtaksperiodetype.FORTSATT_INNVILGET))

        verify { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(mocketVedtak) }
        verify { vedtaksperiodeHentOgPersisterService.lagre(capture(vedtaksperiodeMedBegrunnelseSlot)) }
        verify { mocketVedtak.behandling }
    }

    @Test
    fun `kopierOverVedtaksperioder skal kopiere over vedtaksperioder fra gammel til nytt vedtak`() {
        val gammelVedtak = Vedtak(id = 1, behandling = behandling, aktiv = false)
        val nyttVedtak = Vedtak(id = 2, behandling = behandling, aktiv = true)
        val vedtaksperiodeMedBegrunnelseSlot = slot<VedtaksperiodeMedBegrunnelser>()

        val gammelVedtaksperiodeMedBegrunnelse =
            VedtaksperiodeMedBegrunnelser(
                id = 0,
                vedtak = gammelVedtak,
                fom = LocalDate.of(2020, 12, 12),
                tom = LocalDate.of(2022, 12, 12),
                type = Vedtaksperiodetype.FORTSATT_INNVILGET,
            )

        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(1) } returns
            listOf(
                gammelVedtaksperiodeMedBegrunnelse,
            )
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(vedtaksperiodeMedBegrunnelseSlot)) } returnsArgument 0

        vedtaksperiodeService.kopierOverVedtaksperioder(gammelVedtak, nyttVedtak)

        val nyVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelseSlot.captured

        assertThat(nyVedtaksperiodeMedBegrunnelse.vedtak, Is(nyttVedtak))
        assertThat(nyVedtaksperiodeMedBegrunnelse.type, Is(Vedtaksperiodetype.FORTSATT_INNVILGET))
        assertThat(nyVedtaksperiodeMedBegrunnelse.fom, Is(LocalDate.of(2020, 12, 12)))
        assertThat(nyVedtaksperiodeMedBegrunnelse.tom, Is(LocalDate.of(2022, 12, 12)))

        verify { vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(1) }
    }

    @Test
    fun `hentPersisterteVedtaksperioder skal returnere vedtaksperioder fra vedtaksperiodeHentOgPersisterService`() {
        val vedtak = Vedtak(1, behandling)

        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(1) } returns listOf(mockk(), mockk())

        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        assertThat(vedtaksperioder.size, Is(2))
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner endringstidspunkt for førstegangsbehandling`() {
        assertEquals(TIDENES_MORGEN, vedtaksperiodeService.finnEndringstidspunktForBehandling(behandling, null))
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner endringstidspunkt for revurdering`() {
        val aktør = randomAktør()
        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
                behandling = behandling,
                aktør = aktør,
                stønadFom = YearMonth.now().minusMonths(5),
                stønadTom = YearMonth.now().plusMonths(4),
                sats = 7500,
            )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse, emptyList()))

        val revurdering = lagBehandling()
        val andelTilkjentYtelseForRevurdering =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
                behandling = revurdering,
                aktør = aktør,
                stønadFom = YearMonth.now().minusMonths(3),
                stønadTom = YearMonth.now().plusMonths(4),
                sats = 7500,
            )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                revurdering.id,
            )
        } returns listOf(AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering, emptyList()))

        // Siden periode med fom=YearMonth.now().minusMonths(5), tom=YearMonth.now().minusMonths(4) er opphørt nå
        assertEquals(
            YearMonth.now().minusMonths(5).førsteDagIInneværendeMåned(),
            vedtaksperiodeService.finnEndringstidspunktForBehandling(
                behandling = revurdering,
                sisteVedtattBehandling = behandling,
            ),
        )
    }

    @Test
    fun `finnEndringstidspunktForBehandling finner første endringstidspunkt for revurdering med flere perioder`() {
        val aktør = randomAktør()
        val periode1 = MånedPeriode(YearMonth.now().minusMonths(5), YearMonth.now().minusMonths(3))
        val periode2 = MånedPeriode(YearMonth.now().minusMonths(1), YearMonth.now().plusMonths(4))
        val andelTilkjentYtelse1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
                behandling = behandling,
                aktør = aktør,
                stønadFom = periode1.fom,
                stønadTom = periode1.tom,
                sats = 7500,
            )
        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(behandling),
                behandling = behandling,
                aktør = aktør,
                stønadFom = periode2.fom,
                stønadTom = periode2.tom,
                sats = 7500,
            )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse1, emptyList()),
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelse2, emptyList()),
            )

        val revurdering = lagBehandling()
        val andelTilkjentYtelseForRevurdering1 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
                behandling = revurdering,
                aktør = aktør,
                stønadFom = periode1.fom,
                stønadTom = periode1.tom,
                sats = 7500,
            )
        val andelTilkjentYtelseForRevurdering2 =
            lagAndelTilkjentYtelse(
                tilkjentYtelse = lagInitieltTilkjentYtelse(revurdering),
                behandling = revurdering,
                aktør = aktør,
                stønadFom = periode2.fom,
                stønadTom = periode2.tom,
                sats = 3500,
            )
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService.finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                revurdering.id,
            )
        } returns
            listOf(
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering1, emptyList()),
                AndelTilkjentYtelseMedEndreteUtbetalinger(andelTilkjentYtelseForRevurdering2, emptyList()),
            )

        // endring i beløp på revurdering for periode2
        assertEquals(
            periode2.fom.førsteDagIInneværendeMåned(),
            vedtaksperiodeService.finnEndringstidspunktForBehandling(
                behandling = revurdering,
                sisteVedtattBehandling = behandling,
            ),
        )
    }

    @Test
    fun `finnSisteVedtaksperiodeVisningsdatoForBehandling skal hente siste dato for visning av vedtaksperioder`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barnAktør = randomAktør()
        val barnAktør2 = randomAktør()

        val barnPersonResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnAktør,
            )

        val barnPersonResultat2 =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnAktør2,
            )

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2025, 12, 12),
                    periodeTom = LocalDate.of(2026, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        barnPersonResultat2.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnPersonResultat2,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = barnPersonResultat2,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2025, 12, 12),
                    periodeTom = LocalDate.of(2027, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(barnPersonResultat, barnPersonResultat2)

        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns vilkårsvurdering

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling, Is(LocalDate.of(2027, 11, 30)))

        verify(exactly = 1) { vilkårsvurderingRepository.finnAktivForBehandling(200) }
    }

    @Test
    fun `finnSisteVedtaksperiodeVisningsdatoForBehandling skal returnere null hvis det ikke finnes vilkårsvurdering for behandling`() {
        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns null

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling, Is(nullValue()))

        verify(exactly = 1) { vilkårsvurderingRepository.finnAktivForBehandling(200) }
    }

    @Test
    fun `finnSisteVedtaksperiodeVisningsdatoForBehandling skal returnere null hvis vilkårsvurderingen ikke inneholder noe vilkår som alltid skal vises`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val barnAktør = randomAktør()

        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = barnAktør,
            )

        personResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = personResultat,
                    vilkårType = Vilkår.MEDLEMSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
                VilkårResultat(
                    personResultat = personResultat,
                    vilkårType = Vilkår.MEDLEMSKAP_ANNEN_FORELDER,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2025, 12, 12),
                    periodeTom = LocalDate.of(2026, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        vilkårsvurdering.personResultater = setOf(personResultat)

        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns vilkårsvurdering

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling, Is(nullValue()))

        verify(exactly = 1) { vilkårsvurderingRepository.finnAktivForBehandling(200) }
    }

    @Test
    fun `skal beskrive perioder med eøs refusjoner for behandlinger med avklarte refusjon eøs`() {
        every { personopplysningGrunnlagService.hentSøkersMålform(any()) } returns Målform.NB
        every { integrasjonClient.hentLandkoderISO2() } returns mapOf(Pair("NO", "NORGE"))
        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns emptyList()

        val behandling = lagBehandling(kategori = BehandlingKategori.EØS)

        assertThat(
            vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(
                behandling = behandling,
                avklart = true,
            ),
            Is(nullValue()),
        )

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns
            listOf(
                RefusjonEøs(
                    behandlingId = 1L,
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2022, 1, 1),
                    refusjonsbeløp = 200,
                    land = "NO",
                    refusjonAvklart = true,
                ),
            )

        val perioder = vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = true)

        assertThat(perioder?.size, Is(1))
        assertThat(perioder?.single(), Is("Fra januar 2020 til januar 2022 blir etterbetaling på 200 kroner per måned utbetalt til myndighetene i Norge."))
    }

    @Test
    fun `skal beskrive perioder med eøs refusjoner for behandlinger med uavklarte refusjon eøs`() {
        every { personopplysningGrunnlagService.hentSøkersMålform(any()) } returns Målform.NB
        every { integrasjonClient.hentLandkoderISO2() } returns mapOf(Pair("NO", "NORGE"))
        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns emptyList()

        val behandling = lagBehandling(kategori = BehandlingKategori.EØS)

        assertThat(
            vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(
                behandling = behandling,
                avklart = false,
            ),
            Is(nullValue()),
        )

        every { refusjonEøsRepository.finnRefusjonEøsForBehandling(any()) } returns
            listOf(
                RefusjonEøs(
                    behandlingId = 1L,
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2022, 1, 1),
                    refusjonsbeløp = 200,
                    land = "NO",
                    refusjonAvklart = false,
                ),
            )

        val perioder = vedtaksperiodeService.beskrivPerioderMedRefusjonEøs(behandling = behandling, avklart = false)

        assertThat(perioder?.size, Is(1))
        assertThat(perioder?.single(), Is(("Fra januar 2020 til januar 2022 blir ikke etterbetaling på 200 kroner per måned utbetalt nå siden det er utbetalt barnetrygd i Norge.")))
    }
}
