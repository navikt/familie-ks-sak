package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.validering

import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BarnetsAlderVilkårValidator2021og2024Test {
    private val barnetsAlderVilkårValidator2021og2024: BarnetsAlderVilkårValidator2021og2024 =
        BarnetsAlderVilkårValidator2021og2024(
            barnetsAlderVilkårValidator2021 = BarnetsAlderVilkårValidator2021(),
            barnetsAlderVilkårValidator2024 = BarnetsAlderVilkårValidator2024()
        )

    @Test
    fun `skal kaste feil dersom perioder ikke matcher`() {
        // Arrange
        val fødselsdato = LocalDate.of(2022, 7, 31)

        val person =
            lagPerson(
                aktør = randomAktør(),
                fødselsdato = fødselsdato,
            )

        // Act
        val validerBarnetsAlderVilkår =
            barnetsAlderVilkårValidator2021og2024.validerBarnetsAlderVilkår(
                perioder = listOf(),
                barn = person,
                vilkårLovverkInformasjonForBarn = VilkårLovverkInformasjonForBarn(person.fødselsdato),
                behandlingSkalFølgeNyeLovendringer2024 = true,
            )

        // Assert
        Assertions.assertThat(validerBarnetsAlderVilkår).hasSize(1)
        Assertions.assertThat(validerBarnetsAlderVilkår[0]).isEqualTo("Barnets alder vilkåret må splittes i to perioder fordi barnet fyller 1 år før og 19 måneder etter 01.08.24. Periodene må være som følgende: [2023-07-31 - 2024-07-31, 2024-08-01 - 2024-02-29]")

    }

}
