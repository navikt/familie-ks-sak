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

class BarnetsAlderVilkårValidator2021Test {
    private val barnetsAlderVilkårValidator2021: BarnetsAlderVilkårValidator2021 = BarnetsAlderVilkårValidator2021()

    @Test
    fun `skal returnere ingen feil når perioder er en tom liste`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = listOf(),
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).isEmpty()
    }

    @Test
    fun `skal returnere feil når tom dato settes til etter august året barnet fyller 6 år for barn som er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

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
                    fom = fødselsdato.plusYears(5),
                    tom = fødselsdato.plusYears(6).withMonth(Month.SEPTEMBER.value),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "Du kan ikke sette en t.o.m dato på barnets alder vilkåret som er etter august året barnet fyller 6 år.",
        )
    }

    @Test
    fun `skal returnere feil om differansen mellom fom og tom er mer enn et år en periode for barn som er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

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
                    tom = fødselsdato.plusYears(2).plusDays(1),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "Differansen mellom f.o.m datoen og t.o.m datoen på barnets alder vilkåret kan ikke være mer enn 1 år.",
        )
    }

    @Test
    fun `skal returnere feil om perioden sin fom er ulik måneden barnet blir 1 år for barn som ikke er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusYears(1).plusDays(1),
                    tom = fødselsdato.plusYears(2),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "F.o.m datoen på barnets alder vilkåret må være lik barnets 1 års dag.",
        )
    }

    @Test
    fun `skal returnere feil dersom tom dato for periode er ulik barnets 2 års dag eller 31 juli 2024 og barnet ikke er dødt eller adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusYears(1),
                    tom = fødselsdato.plusYears(2).minusDays(1),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "T.o.m datoen på barnets alder vilkåret må være lik barnets 2 års dag eller 31.07.24 på grunn av lovendring fra og med 01.08.24. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.",
        )
    }

    @Test
    fun `skal returnere feil dersom tom dato for periode er ulik barnets 2 års dag eller 31 juli 2024 og barnets dødsdato er ulik periode tom for barn som ikke er adoptert`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )
        person.apply {
            dødsfall =
                lagDødsfall(
                    person = person,
                    dødsfallDato = fødselsdato.plusYears(2).plusDays(2),
                )
        }

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusYears(1),
                    tom = fødselsdato.plusYears(2).plusDays(1),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).hasSize(1)
        assertThat(validerBarnetsAlderVilkår).contains(
            "T.o.m datoen på barnets alder vilkåret må være lik barnets 2 års dag eller 31.07.24 på grunn av lovendring fra og med 01.08.24. Dersom barnet ikke lever må t.o.m datoen være lik dato for dødsfall.",
        )
    }

    @Test
    fun `skal ikke returne feil når tom-dato er lik dødsfalldato`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )
        person.apply {
            dødsfall =
                lagDødsfall(
                    person = person,
                    dødsfallDato = fødselsdato.plusYears(2).minusDays(10),
                )
        }

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusYears(1),
                    tom = fødselsdato.plusYears(2).minusDays(10),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).isEmpty()
    }

    @Test
    fun `skal ikke returne feil når ingen feil blir oppdaget`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        val perioder =
            listOf(
                IkkeNullbarPeriode(
                    verdi = lagVilkårResultat(vilkårType = Vilkår.BARNETS_ALDER),
                    fom = fødselsdato.plusYears(1),
                    tom = fødselsdato.plusYears(2),
                ),
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021.validerBarnetsAlderVilkår(
                perioder = perioder,
                barn = person,
                periodeFomBarnetsAlderLov2021 = fødselsdato.plusYears(1),
                periodeTomBarnetsAlderLov2021 = fødselsdato.plusYears(2),
            )

        // Assert
        assertThat(validerBarnetsAlderVilkår).isEmpty()
    }
}
