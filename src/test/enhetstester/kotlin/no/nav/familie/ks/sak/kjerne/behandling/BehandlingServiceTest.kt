package no.nav.familie.ks.sak.kjerne.behandling

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.EndreBehandlendeEnhetDto
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.logg.LoggService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions
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

    @MockK
    private lateinit var loggService: LoggService

    @InjectMockKs
    private lateinit var behandlingService: BehandlingService

    private val søker = randomAktør()
    private val fagsak = lagFagsak(aktør = søker)
    private val behandling = lagBehandling(fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @Test
    fun `lagBehandlingRespons - skal lage BehandlingResponsDto for behandling`() {
        val behandlingResponsDto = behandlingService.lagBehandlingRespons(behandling.id)

        Assertions.assertNotNull(behandlingResponsDto)
        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { arbeidsfordelingService.hentArbeidsfordelingPåBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlag(behandling.id) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurdering(behandling.id) }
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
