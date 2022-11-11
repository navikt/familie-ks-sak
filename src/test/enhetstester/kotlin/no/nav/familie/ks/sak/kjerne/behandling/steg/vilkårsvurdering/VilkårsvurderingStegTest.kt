package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

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
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.Periode
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultaterForBarn
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.søknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
class VilkårsvurderingStegTest {

    @MockK
    private lateinit var personopplysningGrunnlagService: PersonopplysningGrunnlagService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var beregningService: BeregningService

    @InjectMockKs
    private lateinit var vilkårsvurderingSteg: VilkårsvurderingSteg

    private val søker = randomAktør()
    private val barn = randomAktør()

    private val fagsak = lagFagsak(søker)

    private val behandling = lagBehandling(fagsak, opprettetÅrsak = BehandlingÅrsak.DØDSFALL)

    @BeforeEach
    fun init() {
        val søknadGrunnlagMock = mockk<SøknadGrunnlag>(relaxed = true)
        mockkObject(SøknadGrunnlagMapper)
        with(SøknadGrunnlagMapper) {
            every { søknadGrunnlagMock.tilSøknadDto() } returns SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                barnaMedOpplysninger = listOf(
                    BarnMedOpplysningerDto(ident = "barn1"),
                    BarnMedOpplysningerDto("barn2")
                ),
                "begrunnelse"
            )
        }
        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            søkerAktør = søker,
            barnasIdenter = listOf(barn.aktivFødselsnummer())
        )

        every { søknadGrunnlagService.hentAktiv(behandling.id) } returns søknadGrunnlagMock
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { beregningService.oppdaterTilkjentYtelsePåBehandling(any(), any(), any(), any()) } just runs
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis behandlingsårsak er DØDSFALL og det eksisterer vilkår lengre fram i tid enn søkers dødsdato`() {
        val barn = randomAktør()
        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            søkerAktør = søker,
            søkerDødsDato = LocalDate.of(2020, 12, 12),
            barnasIdenter = listOf(barn.aktivFødselsnummer())
        )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat)

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        val feil = assertThrows<FunksjonellFeil> {
            vilkårsvurderingSteg.utførSteg(behandling.id)
        }

        assertThat(
            feil.message,
            Is("Ved behandlingsårsak \"Dødsfall\" må vilkårene på søker avsluttes senest dagen søker døde, men \"Bosatt i riket\" vilkåret til søker slutter etter søkers død.")
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis det er periode overlapp mellom delt bosted og gradert barnehageplass vilkår`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                    antallTimer = BigDecimal(25)
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        val feil = assertThrows<Feil> {
            vilkårsvurderingSteg.utførSteg(behandling.id)
        }

        assertThat(
            feil.message,
            Is("Det er lagt inn gradert barnehageplass og delt bosted for samme periode.")
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis det ikke er noe barn i personopplysningsgrunnlaget`() {
        val søknadGrunnlagMock = mockk<SøknadGrunnlag>(relaxed = true)

        mockkObject(SøknadGrunnlagMapper)
        with(SøknadGrunnlagMapper) {
            every { søknadGrunnlagMock.tilSøknadDto() } returns SøknadDto(
                søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                barnaMedOpplysninger = emptyList(),
                "begrunnelse"
            )
        }

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søker.aktivFødselsnummer(),
            søkerAktør = søker
        )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat)

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { søknadGrunnlagService.hentAktiv(behandling.id) } returns søknadGrunnlagMock

        val feil = assertThrows<FunksjonellFeil> {
            vilkårsvurderingSteg.utførSteg(behandling.id)
        }

        assertThat(
            feil.message,
            Is("Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}")
        )

        assertThat(
            feil.frontendFeilmelding,
            Is("Barn må legges til for å gjennomføre vilkårsvurdering.")
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.hentAktiv(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste feil hvis barnehageplass perioder ikke dekker perioder i mellom 1 og 2 år vilkår`() {
        val vilkårsvurdering = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )
        val søkerPersonResultat = vilkårsvurdering.personResultater.first()

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn)
        val barnFødselsDato = LocalDate.of(2020, 4, 1)
        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = barnPersonResultat,
            barnFødselsdato = barnFødselsDato,
            barnehageplassPerioder = listOf(
                Periode(
                    LocalDate.of(2021, 4, 1),
                    LocalDate.of(2021, 8, 31) // stopper før barnets blir 2 år
                ) to BigDecimal(10)
            ),
            behandlingId = behandling.id
        )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Det mangler vurdering på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}. " +
                "Hele eller deler av perioden der barnet er mellom 1 og 2 år er ikke vurdert.",
            exception.message
        )
    }

    @Test
    fun `utførSteg - skal kaste feil hvis barnehageplass perioder starter etter siste dato i mellom 1 og 2 år vilkår`() {
        val vilkårsvurdering = lagVilkårsvurderingMedSøkersVilkår(
            søkerAktør = søker,
            behandling = behandling,
            resultat = Resultat.OPPFYLT
        )
        val søkerPersonResultat = vilkårsvurdering.personResultater.first()

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn)
        val barnFødselsDato = LocalDate.of(2020, 4, 1)
        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = barnPersonResultat,
            barnFødselsdato = barnFødselsDato,
            barnehageplassPerioder = listOf(
                Periode(
                    LocalDate.of(2021, 4, 1),
                    LocalDate.of(2022, 4, 1)
                ) to null,
                Periode( // periode starter etter barnets 2 års dato
                    LocalDate.of(2022, 4, 2),
                    LocalDate.of(2022, 8, 31)
                ) to BigDecimal(30)
            ),
            behandlingId = behandling.id
        )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Du har lagt til en periode på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}" +
                " som starter etter at barnet har fylt 2 år eller startet på skolen. " +
                "Du må fjerne denne perioden for å kunne fortsette",
            exception.message
        )
    }

    @Test
    fun `utførSteg - skal validere vilkårsvurderingen`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id
                )
            )
        )
        val barnFødselsDato = LocalDate.now().minusYears(2)
        val vilkårResultaterForBarn = lagVilkårResultaterForBarn(
            personResultat = barnPersonResultat,
            barnFødselsdato = barnFødselsDato,
            barnehageplassPerioder = listOf(Periode(barnFødselsDato.plusYears(1), barnFødselsDato.plusYears(2)) to null),
            behandlingId = behandling.id
        )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.hentAktiv(behandling.id) }
    }
}
