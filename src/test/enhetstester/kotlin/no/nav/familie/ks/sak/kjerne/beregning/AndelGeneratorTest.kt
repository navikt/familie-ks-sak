package no.nav.familie.ks.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025.RegelverkFørFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.beregning.regelverkLovendringFebruar2025.RegelverkLovendringFebruar2025AndelGenerator
import no.nav.familie.ks.sak.kjerne.brev.lagVilkårResultater
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.ks.sak.kjerne.regelverk.Regelverk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class AndelGeneratorTest {
    private val andelGeneratorer = listOf(RegelverkFørFebruar2025AndelGenerator(), RegelverkLovendringFebruar2025AndelGenerator())

    @ParameterizedTest
    @EnumSource(Regelverk::class)
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse`(regelverk: Regelverk) {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barn = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2023, 8, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør))
        val barnetsVilkår = lagVilkårResultater(person = barn, personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør))

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        val andelGenerator = andelGeneratorer.single { it.regelverk == regelverk }

        // Act
        val andelerTilkjentYtelse =
            andelGenerator.kombinerOgLagAndeler(
                barnAktør = barn.aktør,
                tilkjentYtelse = tilkjentYtelse,
                søkersVilkårResultaterForskjøvetTidslinje =
                    listOf(
                        Periode(
                            verdi = søkersVilkår,
                            fom = søkersVilkår.first().periodeFom,
                            tom = søkersVilkår.first().periodeTom,
                        ),
                    ).tilTidslinje(),
                barnetsVilkårResultaterForskjøvetTidslinje =
                    listOf(
                        Periode(
                            verdi = barnetsVilkår,
                            fom = barnetsVilkår.first().periodeFom,
                            tom = barnetsVilkår.first().periodeTom,
                        ),
                    ).tilTidslinje(),
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(barnetsVilkår.first().periodeFom!!.toYearMonth())
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(barnetsVilkår.first().periodeTom!!.toYearMonth())
    }

    @ParameterizedTest
    @EnumSource(Regelverk::class)
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse hvor et av søkers vilkår avsluttes før barnets vilkår`(regelverk: Regelverk) {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barn = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2023, 8, 8))), personType = PersonType.BARN)
        val forventetAndelTom = barn.fødselsdato.plusMonths(15)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør), overstyrendeVilkårResultater = listOf(lagVilkårResultat(vilkårType = Vilkår.LOVLIG_OPPHOLD, periodeFom = søker.fødselsdato, periodeTom = forventetAndelTom)))
        val barnetsVilkår = lagVilkårResultater(person = barn, personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør))

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        val andelGenerator = andelGeneratorer.single { it.regelverk == regelverk }

        // Act
        val andelerTilkjentYtelse =
            andelGenerator.kombinerOgLagAndeler(
                barnAktør = barn.aktør,
                tilkjentYtelse = tilkjentYtelse,
                søkersVilkårResultaterForskjøvetTidslinje =
                    listOf(
                        Periode(
                            verdi = søkersVilkår,
                            fom = søkersVilkår.first().periodeFom,
                            tom = søkersVilkår.filter { it.periodeTom != null }.maxOfOrNull { it.periodeTom!! },
                        ),
                    ).tilTidslinje(),
                barnetsVilkårResultaterForskjøvetTidslinje =
                    listOf(
                        Periode(
                            verdi = barnetsVilkår,
                            fom = barnetsVilkår.first().periodeFom,
                            tom = barnetsVilkår.first().periodeTom,
                        ),
                    ).tilTidslinje(),
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(barnetsVilkår.first().periodeFom!!.toYearMonth())
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(forventetAndelTom.toYearMonth())
    }
}
