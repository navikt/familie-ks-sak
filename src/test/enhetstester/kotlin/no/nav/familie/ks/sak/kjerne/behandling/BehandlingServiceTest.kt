package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
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
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.Vedtak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.domene.VedtakRepository
import no.nav.familie.ks.sak.kjerne.behandling.steg.vedtak.vedtaksperiode.domene.VedtaksperiodeService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.beregning.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.StatsborgerskapService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BehandlingServiceTest {

    @MockK
    private lateinit var behandlingRepository: BehandlingRepository

    @MockK
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var statsborgerskapService: StatsborgerskapService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var loggService: LoggService

    @MockK
    private lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var vedtaksperiodeService: VedtaksperiodeService

    @MockK
    private lateinit var vedtakRepository: VedtakRepository

    @InjectMockKs
    private lateinit var behandlingService: BehandlingService

    private val søker = randomAktør()
    private val søkersIdent = søker.personidenter.first { personIdent -> personIdent.aktiv }.fødselsnummer
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
    private val søknadsgrunnlagMockK = mockk<SøknadGrunnlag>()

    @BeforeEach
    fun beforeEach() {
        every { behandlingRepository.hentBehandling(any()) } returns behandling
        every { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(any()) } returns ArbeidsfordelingPåBehandling(
            behandlingId = behandling.id,
            behandlendeEnhetId = "enhet",
            behandlendeEnhetNavn = "enhetNavn"
        )
        every { arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(any(), any()) } just runs
        every { statsborgerskapService.hentLand(any()) } returns "Norge"
        every { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(any()) } returns
            lagPersonopplysningGrunnlag(behandlingId = behandling.id, søkerPersonIdent = søkersIdent)
        every { vilkårsvurderingService.finnAktivVilkårsvurdering(any()) } returns null
        every { søknadGrunnlagService.finnAktiv(any()) } returns søknadsgrunnlagMockK
        mockkObject(SøknadGrunnlagMapper)
        with(SøknadGrunnlagMapper) {
            every { søknadsgrunnlagMockK.tilSøknadDto() } returns SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                barnaMedOpplysninger = listOf(
                    BarnMedOpplysningerDto(ident = "barn1"),
                    BarnMedOpplysningerDto("barn2")
                ),
                "begrunnelse"
            )
        }

        every { vedtakRepository.findByBehandlingAndAktivOptional(any()) } returns Vedtak(behandling = behandling)

        every { vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(any()) } returns emptyList()

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) } returns
            listOf(lagAndelTilkjentYtelse(behandling = behandling))
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(behandling.id)
        } returns emptyList()
        every {
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnEndreteUtbetalingerMedAndelerIHenholdTilVilkårsvurdering(behandling.id)
        } returns emptyList()
    }

    @Test
    fun `lagBehandlingRespons - skal lage BehandlingResponsDto for behandling`() {
        val behandlingResponsDto = behandlingService.lagBehandlingRespons(behandling.id)

        assertNotNull(behandlingResponsDto)
        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.finnAktivPersonopplysningGrunnlag(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.finnAktivVilkårsvurdering(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 1) { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id) }

        assertTrue { behandlingResponsDto.personer.isNotEmpty() }
        assertEquals(1, behandlingResponsDto.personer.size)
        assertNotNull(behandlingResponsDto.søknadsgrunnlag)
        assertTrue { behandlingResponsDto.personerMedAndelerTilkjentYtelse.isNotEmpty() }
    }

    @Test
    fun `oppdaterBehandlendeEnhet - skal oppdatere behandlende enhet tilknyttet behandling ved hjelp av ArbeidsfordelingService`() {
        val endreBehandlendeEnhetDto = EndreBehandlendeEnhetDto("nyEnhetId", "begrunnelse")
        behandlingService.oppdaterBehandlendeEnhet(behandling.id, endreBehandlendeEnhetDto)

        verify(exactly = 1) {
            arbeidsfordelingService.manueltOppdaterBehandlendeEnhet(
                behandling,
                endreBehandlendeEnhetDto
            )
        }
    }

    @Test
    fun `oppdaterBehandlingsresultat skal oppdatere behandlingsresultat til INNVILGET og lage historikk på det`() {
        every {
            loggService.opprettVilkårsvurderingLogg(
                behandling = behandling,
                behandlingsForrigeResultat = Behandlingsresultat.IKKE_VURDERT,
                behandlingsNyResultat = Behandlingsresultat.INNVILGET
            )
        } just runs
        every { behandlingRepository.save(any()) } returns behandling.copy(resultat = Behandlingsresultat.INNVILGET)

        val oppdatertBehandling = assertDoesNotThrow {
            behandlingService.oppdaterBehandlingsresultat(behandling.id, Behandlingsresultat.INNVILGET)
        }
        verify(exactly = 1) { loggService.opprettVilkårsvurderingLogg(any(), any(), any()) }
        assertEquals(Behandlingsresultat.INNVILGET, oppdatertBehandling.resultat)
    }

    @Test
    fun `oppdaterStatusPåBehandling skal sette oppdatert status på behandling`() {
        every { behandlingRepository.hentBehandling(behandling.id) } returns behandling
        every { behandlingRepository.save(any()) } returnsArgument 0

        assertEquals(behandling.status, BehandlingStatus.UTREDES)

        val oppdatertBehandling =
            behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FATTER_VEDTAK)

        assertEquals(oppdatertBehandling.status, BehandlingStatus.FATTER_VEDTAK)

        verify(exactly = 1) { behandlingRepository.hentBehandling(oppdatertBehandling.id) }
        verify(exactly = 1) { behandlingRepository.save(any()) }
    }
}
