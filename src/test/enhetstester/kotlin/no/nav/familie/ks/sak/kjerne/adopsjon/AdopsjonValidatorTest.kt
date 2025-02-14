package no.nav.familie.ks.sak.kjerne.adopsjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ks.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.*
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.lagAutomatiskGenererteVilkårForBarnetsAlder
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AdopsjonValidatorTest {
    private val unleashServiceMock: UnleashNextMedContextService = mockk()
    private val adopsjonValidator = AdopsjonValidator(unleashService = unleashServiceMock)

    @BeforeEach
    fun setup(){
        every { unleashServiceMock.isEnabled(FeatureToggle.STØTTER_ADOPSJON) } returns true
    }

    @Test
    fun `Skal kaste feil om det finnes adopsjon i utdypende vilkårsvurdering, men ikke adopsjonsdato for person`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn2.aktør, adopsjonsdato = barn2.fødselsdato.plusMonths(2)))

        // Act & Assert
        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner) }
    }

    @Test
    fun `Skal kaste feil om det finnes adopsjonsdato for person, men adopsjon er ikke valgt i utdypende vilkårsvurdering`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn.aktør, adopsjonsdato = barn.fødselsdato.plusMonths(2)))

        // Act & Assert
        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner) }
    }

    @Test
    fun `Skal ikke kaste feil om det finnes både adopsjonsdato og adopsjon er valgt i utdypende vilkårsvurdering eller ingen av delene`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn.aktør, adopsjonsdato = barn.fødselsdato.plusMonths(2)))

        // Act & Assert
        assertDoesNotThrow { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner) }
    }

    @Test
    fun `Skal ikke kaste feil hvis toggle er av, selv om det finnes adopsjon i utdypende vilkårsvurdering, men ikke adopsjonsdato for person`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør())
        val vilkårsvurdering =
            lagVilkårsvurdering {
                setOf(
                    lagPersonResultat(vilkårsvurdering = it, aktør = søker.aktør),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = true, fødselsdato = barn.fødselsdato) }),
                    lagPersonResultat(vilkårsvurdering = it, aktør = barn2.aktør, lagVilkårResultater = { personResultat -> lagVilkårResultaterForBarn(personResultat = personResultat, erAdopsjon = false, fødselsdato = barn2.fødselsdato) }),
                )
            }

        val adopsjoner = listOf(Adopsjon(behandlingId = vilkårsvurdering.behandling.id, aktør = barn2.aktør, adopsjonsdato = barn2.fødselsdato.plusMonths(2)))

        every { unleashServiceMock.isEnabled(FeatureToggle.STØTTER_ADOPSJON) } returns false

        // Act & Assert
        assertDoesNotThrow { adopsjonValidator.validerAdopsjonIUtdypendeVilkårsvurderingOgAdopsjonsdato(vilkårsvurdering = vilkårsvurdering, adopsjonerIBehandling = adopsjoner) }
    }

    @Test
    fun `Skal ikke kaste feil hvis man validerer at man kan oppdatere adopsjonsdato med ugyldig tilstand, men toggle for at vi støtter adopsjon er ikke påskrudd`(){
        // Arrange
        every { unleashServiceMock.isEnabled(FeatureToggle.STØTTER_ADOPSJON) } returns false

        // Act & Assert
        assertDoesNotThrow { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNETS_ALDER, utypendeVilkårsvurdering = listOf(UtdypendeVilkårsvurdering.ADOPSJON), nyAdopsjonsdato = null) }
    }

    @Test
    fun `Skal kaste feil hvis man validerer at man kan oppdatere adopsjonsdato på et annet vilkår enn barnets alder`(){
        assertThrows<Feil> { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNEHAGEPLASS, utypendeVilkårsvurdering = emptyList(), nyAdopsjonsdato = null) }
    }

    @Test
    fun `Skal kaste feil hvis man validerer at man kan oppdatere adopsjonsdato uten adopsjon i utdypende, men med adopsjonsdato`(){
        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNETS_ALDER, utypendeVilkårsvurdering = emptyList(), nyAdopsjonsdato = LocalDate.now().minusYears(1)) }
    }

    @Test
    fun `Skal kaste feil hvis man validerer at man kan oppdatere adopsjonsdato med adopsjon i utdypende, men uten adopsjonsdato`(){
        assertThrows<FunksjonellFeil> { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNETS_ALDER, utypendeVilkårsvurdering = listOf(UtdypendeVilkårsvurdering.ADOPSJON), nyAdopsjonsdato = null) }
    }

    @Test
    fun `Skal ikke kaste feil hvis man validerer at man kan oppdatere adopsjonsdato med både adopsjon i utdypende og med adopsjonsdato`(){
        assertDoesNotThrow { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNETS_ALDER, utypendeVilkårsvurdering = listOf(UtdypendeVilkårsvurdering.ADOPSJON), nyAdopsjonsdato = LocalDate.now().minusYears(1)) }
    }

    @Test
    fun `Skal ikke kaste feil hvis man validerer at man kan oppdatere adopsjonsdato med verken adopsjon i utdypende eller med adopsjonsdato`(){
        assertDoesNotThrow { adopsjonValidator.validerAtAdopsjonsdatoKanEndresFraVilkår(vilkårType = Vilkår.BARNETS_ALDER, utypendeVilkårsvurdering = emptyList(), nyAdopsjonsdato = null) }
    }

    private fun lagVilkårResultaterForBarn(
        personResultat: PersonResultat,
        erAdopsjon: Boolean,
        fødselsdato: LocalDate,
    ): Set<VilkårResultat> =
        Vilkår
            .hentVilkårFor(PersonType.BARN)
            .flatMap { vilkår ->
                if (vilkår == Vilkår.BARNETS_ALDER) {
                    lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = personResultat.vilkårsvurdering.behandling.id, fødselsdato = fødselsdato, erAdopsjon = erAdopsjon)
                } else {
                    listOf(
                        lagVilkårResultat(
                            behandlingId = personResultat.vilkårsvurdering.behandling.id,
                            personResultat = personResultat,
                            vilkårType = vilkår,
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(1),
                            periodeTom = LocalDate.now().plusYears(2),
                            begrunnelse = "",
                        ),
                    )
                }
            }.toSet()
}
