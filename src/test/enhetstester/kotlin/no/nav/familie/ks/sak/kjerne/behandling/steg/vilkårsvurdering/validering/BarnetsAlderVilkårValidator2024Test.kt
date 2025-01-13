package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.data.lagDødsfall
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.tidslinje.IkkeNullbarPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class BarnetsAlderVilkårValidator2024Test {
    private val barnetsAlderVilkårValidator2024: BarnetsAlderVilkårValidator2024 = BarnetsAlderVilkårValidator2024()

    @Test
    fun `skal returnere ingen feil når perioder er en tom liste`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = listOf(),
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).isEmpty()
    }

    @Test
    fun `skal returnere feil når tom dato settes til etter august året barnet fyller 6 år for barn som er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi =
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNETS_ALDER,
                            utdypendeVilkårsvurderinger =
                                listOf(
                                    UtdypendeVilkårsvurdering.ADOPSJON,
                                ),
                            resultat = Resultat.OPPFYLT,
                        ),
                    fom = fødselsdato.plusMonths(13),
                    tom = fødselsdato.plusYears(6).withMonth(Month.SEPTEMBER.value),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "Du kan ikke sette en t.o.m dato på barnets alder vilkåret som er etter august året barnet fyller 6 år.",
        )
    }

    @Test
    fun `skal returnere feil om differansen mellom fom og tom er mer enn syv måneder for barn som er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi =
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNETS_ALDER,
                            utdypendeVilkårsvurderinger =
                                listOf(
                                    UtdypendeVilkårsvurdering.ADOPSJON,
                                ),
                            resultat = Resultat.OPPFYLT,
                        ),
                    fom = fødselsdato.plusYears(1),
                    tom = fødselsdato.plusYears(2),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "Differansen mellom f.o.m datoen og t.o.m datoen på barnets alder vilkåret kan ikke være mer enn 7 måneder.",
        )
    }

    @Test
    fun `skal returnere feil om perioden sin fom er ulik måneden barnet blir 13mnd for barn som ikke er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusMonths(15),
                    tom = fødselsdato.plusMonths(19),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "F.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 13 måneder eller 01.08.24 dersom barnet fyller 13 måneder før 01.08.24.",
        )
    }

    @Test
    fun `skal returnere feil dersom tom dato for periode er ulik måneden barnet blir 19mnd og barnet ikke er dødt eller adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusMonths(13),
                    tom = fødselsdato.plusMonths(20),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "T.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 19 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.",
        )
    }

    @Test
    fun `skal returnere feil dersom tom dato for periode er ulik måneden barnet blir 19mnd og barnets dødsdato er ulik periode tom for barn som ikke er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )
        person.apply {
            dødsfall =
                lagDødsfall(
                    person = person,
                    dødsfallDato = fødselsdato.plusMonths(18),
                )
        }

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusMonths(13),
                    tom = fødselsdato.plusMonths(20),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "T.o.m datoen på barnets alder vilkåret må være lik datoen barnet fyller 19 måneder. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.",
        )
    }

    @Test
    fun `skal ikke returne feil når ingen feil blir oppdaget`() {
        // Arrange
        val fødselsdato = LocalDate.of(2023, 10, 17)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )
        person.apply {
            dødsfall =
                lagDødsfall(
                    person = person,
                    dødsfallDato = fødselsdato.plusMonths(18),
                )
        }

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusMonths(13),
                    tom = fødselsdato.plusMonths(18),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2024.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2024 = fødselsdato.plusMonths(13),
                periodeTomBarnetsAlderLov2024 = fødselsdato.plusMonths(19),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).isEmpty()
    }
}
