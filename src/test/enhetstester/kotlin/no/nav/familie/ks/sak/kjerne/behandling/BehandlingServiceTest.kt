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
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.domene.ArbeidsfordelingPåBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.domene.SøknadGrunnlag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

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
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(any()) } returns
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
    }

    @Test
    fun `lagBehandlingRespons - skal lage BehandlingResponsDto for behandling`() {
        val behandlingResponsDto = behandlingService.lagBehandlingRespons(behandling.id)

        Assertions.assertNotNull(behandlingResponsDto)
        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.finnAktivVilkårsvurdering(behandling.id) }
        verify(exactly = 1) {
            søknadGrunnlagService.finnAktiv(behandling.id)
        }

        Assertions.assertTrue { behandlingResponsDto.personer.isNotEmpty() }
        Assertions.assertEquals(1, behandlingResponsDto.personer.size)
        Assertions.assertNotNull(behandlingResponsDto.søknadsgrunnlag)
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
}
