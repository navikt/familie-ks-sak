package no.nav.familie.ks.sak.kjerne.beregning

import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.tilfeldigPerson
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.VilkårLovverkInformasjonForBarn
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.lagAutomatiskGenererteVilkårForBarnetsAlder
import no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025.utledMaksAntallMånederMedUtbetaling
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class MaksAntallMånederMedUtbetalingUtlederKtTest {
    val behandling = lagBehandling()
    val vilkårsvurdering =
        lagVilkårsvurdering(
            søkerAktør = tilfeldigPerson(personType = PersonType.SØKER).aktør,
            behandling = behandling,
            resultat = Resultat.OPPFYLT,
        )

    @ParameterizedTest
    @CsvSource("5, 11", "6, 11", "7, 11", "8, 11")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 11 måneder for barn født før september 2022`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val fødselsdato = LocalDate.of(2022, måned, 1)
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = tilfeldigPerson(fødselsdato = fødselsdato).aktør,
            )
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = fødselsdato, adopsjonsdato = null)

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn, barnetsAlderVilkårResultater)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("9, 10", "10, 9", "11, 8", "12, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7-10 måneder for barn født sept-des 2022`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val fødselsdato = LocalDate.of(2022, måned, 1)
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = tilfeldigPerson(fødselsdato = fødselsdato).aktør,
            )
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = fødselsdato, adopsjonsdato = null)

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn, barnetsAlderVilkårResultater)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("1, 7", "2, 7", "3, 7", "4, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7 måneder dersom barn er født i januar 2023 eller senere`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val fødselsdato = LocalDate.of(2023, måned, 1)
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = tilfeldigPerson(fødselsdato = fødselsdato).aktør,
            )
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = fødselsdato, adopsjonsdato = null)

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn, barnetsAlderVilkårResultater)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }

    @ParameterizedTest
    @CsvSource("8, 7", "9, 7", "10, 7", "11, 7", "12, 7")
    fun `utledMaksAntallMånederMedUtbetaling - skal gi 7 måneder for barn kun truffet av 2024 regelverk`(
        måned: Int,
        forventetMaksAntallMånederMedUtbetaling: Long,
    ) {
        // Arrange
        val fødselsdato = LocalDate.of(2024, måned, 1)
        val vilkårLovverkInformasjonForBarn =
            VilkårLovverkInformasjonForBarn(
                fødselsdato = fødselsdato,
                adopsjonsdato = null,
            )
        val personResultat =
            PersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                aktør = tilfeldigPerson(fødselsdato = fødselsdato).aktør,
            )
        val barnetsAlderVilkårResultater = lagAutomatiskGenererteVilkårForBarnetsAlder(personResultat = personResultat, behandlingId = behandling.id, fødselsdato = fødselsdato, adopsjonsdato = null)

        // Act
        val maksAntallMånederMedUtbetaling = utledMaksAntallMånederMedUtbetaling(vilkårLovverkInformasjonForBarn, barnetsAlderVilkårResultater)

        // Assert
        assertThat(maksAntallMånederMedUtbetaling).isEqualTo(forventetMaksAntallMånederMedUtbetaling)
    }
}
