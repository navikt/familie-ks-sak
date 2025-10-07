package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import no.nav.familie.ks.sak.api.dto.BarnMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøkerMedOpplysningerDto
import no.nav.familie.ks.sak.api.dto.SøknadDto
import no.nav.familie.ks.sak.api.mapper.SøknadGrunnlagMapper
import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagFagsak
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårResultaterForBarn
import no.nav.familie.ks.sak.data.lagVilkårsvurderingMedSøkersVilkår
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonService
import no.nav.familie.ks.sak.kjerne.adopsjon.AdopsjonValidator
import no.nav.familie.ks.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ks.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.SøknadGrunnlagService
import no.nav.familie.ks.sak.kjerne.behandling.steg.registrersøknad.domene.SøknadGrunnlag
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsAlderVilkårValidator
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsAlderVilkårValidator2021
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsAlderVilkårValidator2021og2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsAlderVilkårValidator2024
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsAlderVilkårValidator2025
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering.BarnetsVilkårValidator
import no.nav.familie.ks.sak.kjerne.beregning.BeregningService
import no.nav.familie.ks.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.PersonopplysningGrunnlagService
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import org.hamcrest.CoreMatchers.`is` as Is

class VilkårsvurderingStegTest {
    private val personopplysningGrunnlagService: PersonopplysningGrunnlagService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val vilkårsvurderingService: VilkårsvurderingService = mockk()
    private val søknadGrunnlagService: SøknadGrunnlagService = mockk()
    private val beregningService: BeregningService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val adopsjonService: AdopsjonService = mockk()

    private val barnetsAlderVilkårValidator2021 = BarnetsAlderVilkårValidator2021()
    private val barnetsAlderVilkårValidator2024 = BarnetsAlderVilkårValidator2024()
    private val barnetsAlderVilkårValidator2025 = BarnetsAlderVilkårValidator2025()
    private val barnetsVilkårValidator: BarnetsVilkårValidator =
        BarnetsVilkårValidator(
            BarnetsAlderVilkårValidator(
                barnetsAlderVilkårValidator2021,
                barnetsAlderVilkårValidator2024,
                BarnetsAlderVilkårValidator2021og2024(
                    barnetsAlderVilkårValidator2021,
                    barnetsAlderVilkårValidator2024,
                ),
                barnetsAlderVilkårValidator2025,
            ),
            adopsjonService,
        )
    private val adopsjonValidator = AdopsjonValidator(adopsjonService)

    private val vilkårsvurderingSteg: VilkårsvurderingSteg =
        VilkårsvurderingSteg(
            personopplysningGrunnlagService,
            behandlingService,
            søknadGrunnlagService,
            vilkårsvurderingService,
            beregningService,
            kompetanseService,
            barnetsVilkårValidator,
            adopsjonValidator,
            adopsjonService,
        )

    private val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
    private val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2022, 2, 10))

    private val fagsak = lagFagsak(søker.aktør)

    private val behandling = lagBehandling(id = 1, fagsak = fagsak, opprettetÅrsak = BehandlingÅrsak.SØKNAD)

    @BeforeEach
    fun init() {
        val søknadGrunnlagMock = mockk<SøknadGrunnlag>(relaxed = true)
        mockkObject(SøknadGrunnlagMapper)
        with(SøknadGrunnlagMapper) {
            every { søknadGrunnlagMock.tilSøknadDto() } returns
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
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn.aktør),
                barnasFødselsdatoer = listOf(barn.fødselsdato),
            )

        every { søknadGrunnlagService.finnAktiv(behandling.id) } returns søknadGrunnlagMock
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { beregningService.oppdaterTilkjentYtelsePåBehandlingFraVilkårsvurdering(any(), any(), any()) } just runs
        every { adopsjonService.hentAlleAdopsjonerForBehandling(any()) } returns emptyList()
        every { adopsjonService.finnAdopsjonForAktørIBehandling(any(), any()) } returns null
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis behandlingsårsak er DØDSFALL og det eksisterer vilkår lengre fram i tid enn søkers dødsdato`() {
        val behandling = behandling.copy(opprettetÅrsak = BehandlingÅrsak.DØDSFALL)
        every { behandlingService.hentBehandling(behandling.id) } returns behandling

        val barn = randomAktør()
        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                søkerAktør = søker.aktør,
                søkerDødsDato = LocalDate.of(2020, 12, 12),
                barnasIdenter = listOf(barn.aktivFødselsnummer()),
            )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat)

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        val feil =
            assertThrows<FunksjonellFeil> {
                vilkårsvurderingSteg.utførSteg(behandling.id)
            }

        assertThat(
            feil.message,
            Is(
                "Ved behandlingsårsak \"Dødsfall\" må vilkårene på søker avsluttes senest dagen søker døde, men \"Bosatt i riket\" vilkåret til søker slutter etter søkers død.",
            ),
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste funksjonell feil hvis det er periode overlapp mellom delt bosted og gradert barnehageplass vilkår`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
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
                    antallTimer = BigDecimal(25),
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOR_MED_SØKER,
                    resultat = Resultat.OPPFYLT,
                    utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    periodeFom = LocalDate.of(2020, 12, 12),
                    periodeTom = LocalDate.of(2022, 12, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker

        val feil =
            assertThrows<IllegalStateException> {
                vilkårsvurderingSteg.utførSteg(behandling.id)
            }

        assertThat(
            feil.message,
            Is("Det er lagt inn gradert barnehageplass og delt bosted for samme periode."),
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
            every { søknadGrunnlagMock.tilSøknadDto() } returns
                SøknadDto(
                    søkerMedOpplysninger = SøkerMedOpplysningerDto("søkerIdent"),
                    barnaMedOpplysninger = emptyList(),
                    "begrunnelse",
                )
        }

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søker.aktør.aktivFødselsnummer(),
                søkerAktør = søker.aktør,
            )

        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )

        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat)

        every { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) } returns personopplysningGrunnlag
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { søknadGrunnlagService.finnAktiv(behandling.id) } returns søknadGrunnlagMock

        val feil =
            assertThrows<FunksjonellFeil> {
                vilkårsvurderingSteg.utførSteg(behandling.id)
            }

        assertThat(
            feil.message,
            Is("Ingen barn i personopplysningsgrunnlag ved validering av vilkårsvurdering på behandling ${behandling.id}"),
        )

        assertThat(
            feil.frontendFeilmelding,
            Is("Barn må legges til for å gjennomføre vilkårsvurdering."),
        )

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
    }

    @Test
    fun `utførSteg - skal kaste feil hvis barnehageplass perioder ikke dekker perioder i barnets alder vilkår`() {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
            )
        val søkerPersonResultat = vilkårsvurdering.personResultater.first()

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnFødselsDato = LocalDate.of(2020, 4, 1)
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barnFødselsDato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(
                            LocalDate.of(2021, 4, 1),
                            // stopper før barnets blir 2 år
                            LocalDate.of(2021, 8, 31),
                        ) to BigDecimal(10),
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Det mangler vurdering på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}. " +
                "Hele eller deler av perioden der barnets alder vilkåret er oppfylt er ikke vurdert.",
            exception.message,
        )
    }

    @Test
    fun `utførSteg - skal kaste feil hvis barnehageplass perioder starter etter siste dato i barnets alder vilkår`() {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
            )
        val søkerPersonResultat = vilkårsvurdering.personResultater.first()

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnFødselsDato = LocalDate.of(2020, 4, 1)
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barnFødselsDato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(
                            LocalDate.of(2021, 4, 1),
                            LocalDate.of(2022, 4, 1),
                        ) to null,
                        NullablePeriode(
                            // periode starter etter barnets 2 års dato
                            LocalDate.of(2022, 4, 2),
                            LocalDate.of(2022, 8, 31),
                        ) to BigDecimal(30),
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Du har lagt til en periode på vilkåret ${Vilkår.BARNEHAGEPLASS.beskrivelse}" +
                " som starter etter at barnet har fylt 2 år eller startet på skolen. " +
                "Du må fjerne denne perioden for å kunne fortsette",
            exception.message,
        )
    }

    @Test
    fun `utførSteg - skal kaste feil hvis det finnes mer enn 2 barnehageplass vilkår i en måned`() {
        val vilkårsvurdering =
            lagVilkårsvurderingMedSøkersVilkår(
                søkerAktør = søker.aktør,
                behandling = behandling,
            )
        val søkerPersonResultat = vilkårsvurdering.personResultater.first()

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val barnFødselsDato = LocalDate.of(2020, 4, 1)
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barnFødselsDato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(
                            LocalDate.of(2021, 4, 1),
                            LocalDate.of(2021, 6, 8),
                        ) to null,
                        NullablePeriode(
                            LocalDate.of(2021, 6, 9),
                            LocalDate.of(2021, 6, 15),
                        ) to BigDecimal(30),
                        NullablePeriode(
                            LocalDate.of(2021, 6, 16),
                            LocalDate.of(2022, 4, 30),
                        ) to BigDecimal(16),
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Du har lagt inn flere enn 2 endringer i barnehagevilkåret i samme måned. " +
                "Dette er ikke støttet enda. Ta kontakt med Team BAKS.",
            exception.message,
        )
    }

    @Test
    fun `utførSteg - skal kaste feil når det er blanding av regelverk på vilkårene for barnet`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        // BOSATT I RIKET er vurdert etter både NASJONALE_REGLER og EØS_FORORDNINGEN
        // mens MEDLEMSKAP er vurdert etter kun NASJONALE_REGLER
        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(1987, 7, 31),
                    periodeTom = LocalDate.of(2022, 12, 14),
                    regelverk = Regelverk.NASJONALE_REGLER,
                ),
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(2022, 12, 15),
                    periodeTom = null,
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                ),
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    periodeFom = LocalDate.of(2022, 12, 15),
                    periodeTom = null,
                    regelverk = Regelverk.EØS_FORORDNINGEN,
                ),
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.MEDLEMSKAP,
                    periodeFom = LocalDate.of(1992, 7, 31),
                    periodeTom = LocalDate.of(2022, 12, 14),
                    regelverk = Regelverk.NASJONALE_REGLER,
                ),
                lagVilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.MEDLEMSKAP,
                    periodeFom = LocalDate.of(2022, 12, 15),
                    periodeTom = null,
                    regelverk = Regelverk.NASJONALE_REGLER,
                ),
            ),
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder = listOf(NullablePeriode(fom = barn.fødselsdato.plusYears(1), tom = null) to null),
                regelverk = Regelverk.EØS_FORORDNINGEN,
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurdering

        val exception = assertThrows<FunksjonellFeil> { vilkårsvurderingSteg.utførSteg(behandling.id) }
        assertEquals(
            "Det er forskjellig regelverk for en eller flere perioder for søker eller barna",
            exception.message,
        )
    }

    @Test
    fun `utførSteg - skal validere vilkårsvurderingen`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
        every {
            behandlingService.endreBehandlingstemaPåBehandling(
                any(),
                BehandlingKategori.NASJONAL,
            )
        } returns behandling
        every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns emptyList()

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 0) { kompetanseService.tilpassKompetanse(BehandlingId(behandling.id)) }
    }

    @Test
    fun `utførSteg - skal kaste feil dersom det finnes vilkår som har fom dato satt senere enn inneværende måned`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now().plusMonths(1),
                    periodeTom = LocalDate.now().plusMonths(2),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
        every {
            behandlingService.endreBehandlingstemaPåBehandling(
                any(),
                BehandlingKategori.NASJONAL,
            )
        } returns behandling
        every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns emptyList()

        val feilmelding =
            assertThrows<FunksjonellFeil> {
                vilkårsvurderingSteg.utførSteg(behandling.id)
            }.melding

        assertThat(feilmelding, Is("Vilkår BOSATT_I_RIKET for person født ${søker.fødselsdato} har perioder der f.o.m dato er satt senere enn inneværende måned."))
    }

    @Test
    fun `utførSteg - skal kaste feil dersom det finnes barnehageplass vilkår som har fom dato satt senere enn neste måned`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BARNEHAGEPLASS,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.now().plusMonths(2),
                    periodeTom = LocalDate.now().plusMonths(3),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
        every {
            behandlingService.endreBehandlingstemaPåBehandling(
                any(),
                BehandlingKategori.NASJONAL,
            )
        } returns behandling
        every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns emptyList()

        val feilmelding =
            assertThrows<FunksjonellFeil> {
                vilkårsvurderingSteg.utførSteg(behandling.id)
            }.melding

        assertThat(feilmelding, Is("Vilkår BARNEHAGEPLASS for person født ${søker.fødselsdato} har perioder der f.o.m dato er satt senere enn neste måned."))
    }

    @Test
    fun `utførSteg - skal oppdatere behandlingstema med EØS hvis nåværende behandling inneholder vilkår vurdert etter EØS ordningen`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns null
        every { behandlingService.endreBehandlingstemaPåBehandling(any(), BehandlingKategori.EØS) } returns behandling
        every { kompetanseService.tilpassKompetanse(BehandlingId(behandling.id)) } just runs

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 1) { behandlingService.endreBehandlingstemaPåBehandling(any(), BehandlingKategori.EØS) }
        verify(exactly = 1) { kompetanseService.tilpassKompetanse(BehandlingId(behandling.id)) }
    }

    @Test
    fun `utførSteg - skal oppdatere behandlingstema med EØS hvis forrige behandling inneholder løpende vilkår vurdert etter EØS ordningen`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        val forrigeBehandling = lagBehandling(id = 0, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val forrigeVilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultatIForrigeVilkårsvurdering =
            PersonResultat(vilkårsvurdering = forrigeVilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultatIForrigeVilkårsvurdering =
            PersonResultat(vilkårsvurdering = forrigeVilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultatIForrigeVilkårsvurdering.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultatIForrigeVilkårsvurdering,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2099, 10, 12),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )

        val barnIForrigeVilkårsvurderingFødselsDato = LocalDate.now().minusYears(2)
        val vilkårResultaterForBarnIForrigeVilkårsvurdering =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultatIForrigeVilkårsvurdering,
                barnFødselsdato = barnIForrigeVilkårsvurderingFødselsDato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(
                            barnIForrigeVilkårsvurderingFødselsDato.plusYears(1),
                            barnIForrigeVilkårsvurderingFødselsDato.plusYears(2),
                        ) to null,
                    ),
                behandlingId = forrigeBehandling.id,
            )
        barnPersonResultatIForrigeVilkårsvurdering.setSortedVilkårResultater(
            vilkårResultaterForBarnIForrigeVilkårsvurdering,
        )
        forrigeVilkårsvurderingForSøker.personResultater =
            setOf(søkerPersonResultatIForrigeVilkårsvurdering, barnPersonResultatIForrigeVilkårsvurdering)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(forrigeBehandling.id) } returns forrigeVilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns forrigeBehandling
        every { behandlingService.endreBehandlingstemaPåBehandling(any(), BehandlingKategori.EØS) } returns behandling
        every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns emptyList()

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 1) { behandlingService.endreBehandlingstemaPåBehandling(any(), BehandlingKategori.EØS) }
        // Siden nåværende vilkårvurdering vurderte etter NASJONALE REGLER, oppdaterer vi ikke kompetanse
        verify(exactly = 0) { kompetanseService.tilpassKompetanse(BehandlingId(behandling.id)) }
    }

    @Test
    fun `utførSteg - skal ikke oppdatere behandlingstema med EØS hvis forrige behandling inneholder løpende vilkår vurdert etter EØS ordningen men som er utløpt`() {
        val vilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    vurderesEtter = Regelverk.NASJONALE_REGLER,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2022, 10, 12),
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )
        val vilkårResultaterForBarn =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultat,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(barn.fødselsdato.plusYears(1), barn.fødselsdato.plusYears(2)) to null,
                    ),
                behandlingId = behandling.id,
            )
        barnPersonResultat.setSortedVilkårResultater(vilkårResultaterForBarn)
        vilkårsvurderingForSøker.personResultater = setOf(søkerPersonResultat, barnPersonResultat)

        val forrigeBehandling = lagBehandling(id = 0, opprettetÅrsak = BehandlingÅrsak.SØKNAD)
        val forrigeVilkårsvurderingForSøker = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultatIForrigeVilkårsvurdering =
            PersonResultat(vilkårsvurdering = forrigeVilkårsvurderingForSøker, aktør = søker.aktør)
        val barnPersonResultatIForrigeVilkårsvurdering =
            PersonResultat(vilkårsvurdering = forrigeVilkårsvurderingForSøker, aktør = barn.aktør)

        søkerPersonResultatIForrigeVilkårsvurdering.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultatIForrigeVilkårsvurdering,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2018, 12, 12),
                    periodeTom = LocalDate.of(2019, 10, 12),
                    vurderesEtter = Regelverk.EØS_FORORDNINGEN,
                    begrunnelse = "",
                    behandlingId = behandling.id,
                ),
            ),
        )

        val vilkårResultaterForBarnIForrigeVilkårsvurdering =
            lagVilkårResultaterForBarn(
                personResultat = barnPersonResultatIForrigeVilkårsvurdering,
                barnFødselsdato = barn.fødselsdato,
                barnehageplassPerioder =
                    listOf(
                        NullablePeriode(
                            barn.fødselsdato.plusYears(1),
                            barn.fødselsdato.plusYears(2),
                        ) to null,
                    ),
                behandlingId = forrigeBehandling.id,
            )
        barnPersonResultatIForrigeVilkårsvurdering.setSortedVilkårResultater(
            vilkårResultaterForBarnIForrigeVilkårsvurdering,
        )
        forrigeVilkårsvurderingForSøker.personResultater =
            setOf(søkerPersonResultatIForrigeVilkårsvurdering, barnPersonResultatIForrigeVilkårsvurdering)

        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) } returns vilkårsvurderingForSøker
        every { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(forrigeBehandling.id) } returns forrigeVilkårsvurderingForSøker
        every { behandlingService.hentSisteBehandlingSomErVedtatt(behandling.fagsak.id) } returns forrigeBehandling
        every {
            behandlingService.endreBehandlingstemaPåBehandling(
                any(),
                BehandlingKategori.NASJONAL,
            )
        } returns behandling
        every { kompetanseService.hentKompetanser(BehandlingId(behandling.id)) } returns emptyList()

        vilkårsvurderingSteg.utførSteg(behandling.id)

        verify(exactly = 1) { behandlingService.hentBehandling(behandling.id) }
        verify(exactly = 1) { personopplysningGrunnlagService.hentAktivPersonopplysningGrunnlagThrows(any()) }
        verify(exactly = 1) { vilkårsvurderingService.hentAktivVilkårsvurderingForBehandling(behandling.id) }
        verify(exactly = 1) { søknadGrunnlagService.finnAktiv(behandling.id) }
        verify(exactly = 0) { kompetanseService.tilpassKompetanse(BehandlingId(behandling.id)) }
    }
}
