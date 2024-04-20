package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.common.util.LocalDateProvider
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ks.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personident.PersonidentService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class BehandlingsresultatServiceTest {
    @MockK
    private lateinit var localDateProvider: LocalDateProvider

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var andelerTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @MockK
    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @MockK
    private lateinit var kompetanseService: KompetanseService

    @InjectMockKs
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere tom liste dersom behandlingen ikke er søknad, fødselshendelse eller manuell migrering`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.BARNEHAGELISTE)

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDto = null,
            )

        assertThat(personerFramstiltForKrav, Is(emptyList()))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal bare returnere barn som er folkeregistret og krysset av på søknad`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn1Fnr = randomFnr()
        val mocketAktør = mockk<Aktør>()

        val barnSomErKryssetAvFor =
            BarnMedOpplysningerDto(
                ident = barn1Fnr,
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val barnSomIkkeErKryssetAvFor =
            BarnMedOpplysningerDto(
                ident = randomFnr(),
                navn = "barn2",
                inkludertISøknaden = false,
                erFolkeregistrert = true,
            )

        val barnSomErKryssetAvForMenIkkeFolkeregistrert =
            BarnMedOpplysningerDto(
                ident = randomFnr(),
                navn = "barn3",
                inkludertISøknaden = true,
                erFolkeregistrert = false,
            )

        val søknadDto =
            SøknadDto(
                barnaMedOpplysninger =
                    listOf(
                        barnSomErKryssetAvFor,
                        barnSomIkkeErKryssetAvFor,
                        barnSomErKryssetAvForMenIkkeFolkeregistrert,
                    ),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { personidentService.hentAktør(barn1Fnr) } returns mocketAktør

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDto = søknadDto,
            )

        assertThat(personerFramstiltForKrav.single(), Is(mocketAktør))

        verify(exactly = 1) { personidentService.hentAktør(barn1Fnr) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal ikke returnere duplikater av personer`() {
        val behandling = lagBehandling(opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val barn = lagPerson(aktør = randomAktør())

        val barnSomErKryssetAvFor =
            BarnMedOpplysningerDto(
                ident = barn.aktør.aktivFødselsnummer(),
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val duplikatBarnSomErKryssetAvFor =
            BarnMedOpplysningerDto(
                ident = barn.aktør.aktivFødselsnummer(),
                navn = "barn1",
                inkludertISøknaden = true,
                erFolkeregistrert = true,
            )

        val søknadDto =
            SøknadDto(
                barnaMedOpplysninger = listOf(barnSomErKryssetAvFor, duplikatBarnSomErKryssetAvFor),
                søkerMedOpplysninger = mockk(),
                endringAvOpplysningerBegrunnelse = "",
            )

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(any()) } returns Vilkårsvurdering(behandling = behandling)
        every { personidentService.hentAktør(barn.aktør.aktivFødselsnummer()) } returns barn.aktør

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDto = søknadDto,
            )

        assertThat(personerFramstiltForKrav.size, Is(1))
        assertThat(personerFramstiltForKrav.single(), Is(barn.aktør))
    }
}
