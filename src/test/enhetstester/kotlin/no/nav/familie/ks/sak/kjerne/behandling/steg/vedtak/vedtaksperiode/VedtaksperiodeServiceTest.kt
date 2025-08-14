package no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagSanityBegrunnelse
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.integrasjon.familieintegrasjon.IntegrasjonClient
import no.nav.familie.ks.sak.integrasjon.sanity.SanityService
import no.nav.familie.ks.sak.integrasjon.sanity.domene.SanityResultat
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøs
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.avslagperiode.AvslagsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksbegrunnelseFritekst
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.utbetalingsperiodeMedBegrunnelser.UtbetalingsperiodeGenerator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.EØSBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.NasjonalEllerFellesBegrunnelse
import no.nav.familie.ks.sak.kjerne.brev.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.forrigebehandling.EndringstidspunktService
import no.nav.familie.ks.sak.kjerne.overgangsordning.OvergangsordningAndelService
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Målform
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.YearMonth

internal class VedtaksperiodeServiceTest {
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val personopplysningGrunnlagService = mockk<PersonopplysningGrunnlagService>()
    private val vedtaksperiodeHentOgPersisterService = mockk<VedtaksperiodeHentOgPersisterService>()
    private val vedtakRepository = mockk<VedtakRepository>()
    private val overgangsordningAndelService = mockk<OvergangsordningAndelService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val sanityService = mockk<SanityService>()
    private val utbetalingsperiodeGenerator = mockk<UtbetalingsperiodeGenerator>()
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService = mockk<AndelerTilkjentYtelseOgEndreteUtbetalingerService>()
    private val integrasjonClient = mockk<IntegrasjonClient>()
    private val refusjonEøsRepository = mockk<RefusjonEøsRepository>()
    private val kompetanseService = mockk<KompetanseService>(relaxed = true)
    private val adopsjonService = mockk<AdopsjonService>()
    private val endringstidspunktService = mockk<EndringstidspunktService>()
    private val opphørsperiodeGenerator = mock<OpphørsperiodeGenerator>()
    private val avslagsperiodeGenerator = mock<AvslagsperiodeGenerator>()

    private val vedtaksperiodeService =
        VedtaksperiodeService(
            behandlingRepository = behandlingRepository,
            personopplysningGrunnlagService = personopplysningGrunnlagService,
            vedtaksperiodeHentOgPersisterService = vedtaksperiodeHentOgPersisterService,
            vedtakRepository = vedtakRepository,
            vilkårsvurderingRepository = vilkårsvurderingRepository,
            overgangsordningAndelService = overgangsordningAndelService,
            sanityService = sanityService,
            utbetalingsperiodeGenerator = utbetalingsperiodeGenerator,
            andelerTilkjentYtelseOgEndreteUtbetalingerService = andelerTilkjentYtelseOgEndreteUtbetalingerService,
            integrasjonClient = integrasjonClient,
            refusjonEøsRepository = refusjonEøsRepository,
            kompetanseService = kompetanseService,
            adopsjonService = adopsjonService,
            endringstidspunktService = endringstidspunktService,
            opphørsperiodeGenerator = opphørsperiodeGenerator,
            avslagsperiodeGenerator = avslagsperiodeGenerator,
        )

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

        every { sanityService.hentSanityBegrunnelser() } returns emptyList()
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns mocketPersonOpplysningGrunnlag
        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) } returns vedtaksperiodeMedBegrunnelse

        val feil =
            assertThrows<Feil> {
                vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(1, listOf(nasjonalEllerFellesBegrunnelse))
            }

        assertThat(
            feil.message,
        ).isEqualTo("Begrunnelsestype ${nasjonalEllerFellesBegrunnelse.begrunnelseType} passer ikke med typen 'UTBETALING' som er satt på perioden.")
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

        every { sanityService.hentSanityBegrunnelser() } returns emptyList()
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) } returns mocketPersonOpplysningGrunnlag
        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) } returns vedtaksperiodeMedBegrunnelse
        every { vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelse) } returns vedtaksperiodeMedBegrunnelse

        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(1, listOf(nasjonalEllerFellesBegrunnelse))

        verify { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(behandling.id) }
        verify { vedtaksperiodeHentOgPersisterService.hentVedtaksperiodeThrows(any()) }
        verify { vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelse) }
    }

    @Nested
    inner class StøtterFritekst {
        @Test
        fun `støtterFritekst skal gi false dersom vedtaksperioden har type UTBETALING og ingen av begrunnelsene støtter fritekst`() {
            val vedtaksperiodeMedBegrunnelse =
                VedtaksperiodeMedBegrunnelser(
                    id = 0,
                    vedtak = Vedtak(id = 0, behandling = behandling),
                    type = Vedtaksperiodetype.UTBETALING,
                ).also {
                    it.fritekster.add(VedtaksbegrunnelseFritekst(id = 0, vedtaksperiodeMedBegrunnelser = it, fritekst = "Dette er en fritekst"))
                    it.begrunnelser.addAll(listOf(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.tilVedtaksbegrunnelse(it), NasjonalEllerFellesBegrunnelse.INNVILGET_DELTID_BARNEHAGE.tilVedtaksbegrunnelse(it)))
                }

            val sanityBegrunnelser =
                listOf(
                    lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn, støtterFritekst = false, SanityResultat.INNVILGET),
                    lagSanityBegrunnelse(apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.sanityApiNavn, støtterFritekst = false, resultat = SanityResultat.INNVILGET),
                )

            assertThat(vedtaksperiodeMedBegrunnelse.støtterFritekst(sanityBegrunnelser, false)).isFalse
        }

        @Test
        fun `støtterFritekst skal gi true dersom vedtaksperioden har type UTBETALING og en av begrunnelsene er av typen REDUKSJON men ikke REDUKSJON_SATSENDRING`() {
            val vedtaksperiodeMedBegrunnelse =
                VedtaksperiodeMedBegrunnelser(
                    id = 0,
                    vedtak = Vedtak(id = 0, behandling = behandling),
                    type = Vedtaksperiodetype.UTBETALING,
                ).also {
                    it.fritekster.add(VedtaksbegrunnelseFritekst(id = 0, vedtaksperiodeMedBegrunnelser = it, fritekst = "Dette er en fritekst"))
                    it.begrunnelser.addAll(listOf(NasjonalEllerFellesBegrunnelse.REDUKSJON_BARN_DOD.tilVedtaksbegrunnelse(it)))
                }

            val sanityBegrunnelser =
                listOf(
                    lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.REDUKSJON_BARN_DOD.sanityApiNavn, støtterFritekst = false, resultat = SanityResultat.REDUKSJON),
                )

            assertThat(vedtaksperiodeMedBegrunnelse.støtterFritekst(sanityBegrunnelser, false)).isTrue
        }

        @Test
        fun `støtterFritekst skal gi true dersom vedtaksperioden har type UTBETALING og minst en av begrunnelsene støtter fritekst`() {
            val vedtaksperiodeMedBegrunnelse =
                VedtaksperiodeMedBegrunnelser(
                    id = 0,
                    vedtak = Vedtak(id = 0, behandling = behandling),
                    type = Vedtaksperiodetype.UTBETALING,
                ).also {
                    it.fritekster.add(VedtaksbegrunnelseFritekst(id = 0, vedtaksperiodeMedBegrunnelser = it, fritekst = "Dette er en fritekst"))
                    it.begrunnelser.add(NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.tilVedtaksbegrunnelse(it))
                    it.eøsBegrunnelser.add(EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.tilVedtaksbegrunnelse(it))
                }

            val sanityBegrunnelser =
                listOf(
                    lagSanityBegrunnelse(apiNavn = NasjonalEllerFellesBegrunnelse.INNVILGET_IKKE_BARNEHAGE.sanityApiNavn, støtterFritekst = false, resultat = SanityResultat.INNVILGET),
                    lagSanityBegrunnelse(apiNavn = EØSBegrunnelse.INNVILGET_PRIMÆRLAND_STANDARD.sanityApiNavn, støtterFritekst = true, resultat = SanityResultat.INNVILGET),
                )

            assertThat(vedtaksperiodeMedBegrunnelse.støtterFritekst(sanityBegrunnelser, false)).isTrue
        }

        @ParameterizedTest
        @EnumSource(value = Vedtaksperiodetype::class, names = ["OPPHØR", "AVSLAG", "FORTSATT_INNVILGET"])
        fun `støtterFritekst skal gi true dersom vedtaksperioden er ulik UTBETALING selv om ingen av begrunnelsene støtter fritekst`(vedtaksperiodetype: Vedtaksperiodetype) {
            val vedtaksperiodeMedBegrunnelse =
                VedtaksperiodeMedBegrunnelser(
                    id = 0,
                    vedtak = Vedtak(id = 0, behandling = behandling),
                    type = vedtaksperiodetype,
                ).also {
                    it.fritekster.add(VedtaksbegrunnelseFritekst(id = 0, vedtaksperiodeMedBegrunnelser = it, fritekst = "Dette er en fritekst"))
                }

            assertThat(vedtaksperiodeMedBegrunnelse.støtterFritekst(emptyList(), false)).isTrue
        }

        @ParameterizedTest
        @EnumSource(value = Vedtaksperiodetype::class)
        fun `skal returnere true dersom parameter alleBegrunnelserStøtterFritekster er satt til true uavhengig av vedtaksperiodetype`(vedtaksperiodetype: Vedtaksperiodetype) {
            // Arrange
            val vedtaksperiodeMedBegrunnelse =
                VedtaksperiodeMedBegrunnelser(
                    id = 0,
                    vedtak = Vedtak(id = 0, behandling = behandling),
                    type = vedtaksperiodetype,
                )
            // Act
            val støtterFritekst = vedtaksperiodeMedBegrunnelse.støtterFritekst(sanityBegrunnelser = emptyList(), alleBegrunnelserStøtterFritekst = true)

            // Assert
            assertThat(støtterFritekst).isTrue
        }
    }

    @Test
    fun `oppdaterVedtakMedVedtaksperioder skal slette eksisterende vedtaksperioder for vedtak og lage ny`() {
        behandling.resultat = Behandlingsresultat.FORTSATT_INNVILGET

        val mocketVedtak = mockk<Vedtak>()
        val vedtaksperiodeMedBegrunnelseSlot = slot<List<VedtaksperiodeMedBegrunnelser>>()

        every { vedtaksperiodeHentOgPersisterService.slettVedtaksperioderFor(mocketVedtak) } just runs
        every { vedtaksperiodeHentOgPersisterService.lagre(capture(vedtaksperiodeMedBegrunnelseSlot)) } returns mockk()
        every { mocketVedtak.behandling } returns behandling

        vedtaksperiodeService.oppdaterVedtakMedVedtaksperioder(mocketVedtak)

        val lagretVedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelseSlot.captured.single()

        assertThat(lagretVedtaksperiodeMedBegrunnelser.fom).isNull()
        assertThat(lagretVedtaksperiodeMedBegrunnelser.tom).isNull()
        assertThat(lagretVedtaksperiodeMedBegrunnelser.vedtak).isEqualTo(mocketVedtak)
        assertThat(lagretVedtaksperiodeMedBegrunnelser.type).isEqualTo(Vedtaksperiodetype.FORTSATT_INNVILGET)

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

        every { sanityService.hentSanityBegrunnelser() } returns emptyList()

        vedtaksperiodeService.kopierOverVedtaksperioder(gammelVedtak, nyttVedtak)

        val nyVedtaksperiodeMedBegrunnelse = vedtaksperiodeMedBegrunnelseSlot.captured

        assertThat(nyVedtaksperiodeMedBegrunnelse.vedtak).isEqualTo(nyttVedtak)
        assertThat(nyVedtaksperiodeMedBegrunnelse.type).isEqualTo(Vedtaksperiodetype.FORTSATT_INNVILGET)
        assertThat(nyVedtaksperiodeMedBegrunnelse.fom).isEqualTo(LocalDate.of(2020, 12, 12))
        assertThat(nyVedtaksperiodeMedBegrunnelse.tom).isEqualTo(LocalDate.of(2022, 12, 12))

        verify { vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(1) }
    }

    @Test
    fun `hentPersisterteVedtaksperioder skal returnere vedtaksperioder fra vedtaksperiodeHentOgPersisterService`() {
        val vedtak = Vedtak(1, behandling)

        every { vedtaksperiodeHentOgPersisterService.hentVedtaksperioderFor(1) } returns listOf(mockk(), mockk())

        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        assertThat(vedtaksperioder.size).isEqualTo(2)
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
        every { overgangsordningAndelService.hentOvergangsordningAndeler(200) } returns emptyList()

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling).isEqualTo(LocalDate.of(2027, 12, 12))

        verify(exactly = 1) { vilkårsvurderingRepository.finnAktivForBehandling(200) }
    }

    @Test
    fun `finnSisteVedtaksperiodeVisningsdatoForBehandling skal returnere null hvis det ikke finnes vilkårsvurdering for behandling`() {
        every { vilkårsvurderingRepository.finnAktivForBehandling(200) } returns null
        every { overgangsordningAndelService.hentOvergangsordningAndeler(200) } returns emptyList()

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling).isNull()

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
        every { overgangsordningAndelService.hentOvergangsordningAndeler(200) } returns emptyList()

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling).isNull()

        verify(exactly = 1) { vilkårsvurderingRepository.finnAktivForBehandling(200) }
    }

    @Test
    fun `finnSisteVedtaksperiodeVisningsdatoForBehandling skal returnere siste tom i overgangsordning andeler dersom det er senere enn vilkårsvurderingen`() {
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
        val overgangsordningsAndeler = listOf(OvergangsordningAndel(behandlingId = 200, fom = YearMonth.of(2025, 3), tom = YearMonth.of(2025, 3)))
        every { overgangsordningAndelService.hentOvergangsordningAndeler(200) } returns overgangsordningsAndeler

        val finnSisteVedtaksperiodeVisningsdatoForBehandling =
            vedtaksperiodeService.finnSisteVedtaksperiodeVisningsdatoForBehandling(200)

        assertThat(finnSisteVedtaksperiodeVisningsdatoForBehandling).isEqualTo(LocalDate.of(2025, 3, 31))

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
        ).isNull()

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

        assertThat(perioder?.size).isEqualTo(1)
        assertThat(perioder?.single()).isEqualTo("Fra januar 2020 til januar 2022 blir etterbetaling på 200 kroner per måned utbetalt til myndighetene i Norge.")
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
        ).isNull()

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

        assertThat(perioder?.size).isEqualTo(1)
        assertThat(perioder?.single()).isEqualTo("Fra januar 2020 til januar 2022 blir ikke etterbetaling på 200 kroner per måned utbetalt nå siden det er utbetalt kontantstøtte i Norge.")
    }
}
