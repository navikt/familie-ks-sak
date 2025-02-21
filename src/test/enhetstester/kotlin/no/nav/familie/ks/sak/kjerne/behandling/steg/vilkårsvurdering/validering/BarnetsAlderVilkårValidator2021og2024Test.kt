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
            barnetsAlderVilkårValidator2024 = BarnetsAlderVilkårValidator2024(),
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
                vilkårLovverkInformasjonForBarn =
                    VilkårLovverkInformasjonForBarn(
                        fødselsdato = person.fødselsdato,
                        adopsjonsdato = null,
                    ),
            )

        // Assert
        Assertions.assertThat(validerBarnetsAlderVilkår).hasSize(1)
        Assertions.assertThat(validerBarnetsAlderVilkår[0]).isEqualTo("Vilkåret for barnets alder må splittes i to perioder fordi den strekker seg over lovendringen 01.08.2024. Henlegg denne behandlingen og opprett en ny behandling. I den nye behandlingen vil splitten dannes automatisk.")
    }
}
