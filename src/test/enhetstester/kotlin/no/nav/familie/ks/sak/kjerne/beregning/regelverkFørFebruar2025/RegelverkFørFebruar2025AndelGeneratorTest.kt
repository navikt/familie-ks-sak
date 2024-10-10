package no.nav.familie.ks.sak.kjerne.beregning.regelverkFørFebruar2025

import io.mockk.every
import io.mockk.mockk
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
import no.nav.familie.ks.sak.kjerne.brev.lagVilkårResultater
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegelverkFørFebruar2025AndelGeneratorTest {
    private val regelverkFørFebruar2025AndelGenerator: RegelverkFørFebruar2025AndelGenerator = RegelverkFørFebruar2025AndelGenerator()

    @Test
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som kun treffer regelverk etter august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnRegelverkAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2023, 8, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnRegelverkAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår = lagVilkårResultater(person = barnRegelverkAugust2024, personResultat = barnetsPersonResultat)
        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnRegelverkAugust2024,
                søker = søker,
                tilkjentYtelse = tilkjentYtelse,
                vilkårsvurdering = vilkårsvurdering,
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(barnetsVilkår.first().periodeFom!!.toYearMonth())
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(barnetsVilkår.first().periodeTom!!.toYearMonth())
    }

    @Test
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som kun treffer regelverk før august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnRegelverkAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2022, 7, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnRegelverkAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår = lagVilkårResultater(person = barnRegelverkAugust2024, personResultat = barnetsPersonResultat)
        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnRegelverkAugust2024,
                søker = søker,
                tilkjentYtelse = tilkjentYtelse,
                vilkårsvurdering = vilkårsvurdering,
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(
            barnetsVilkår
                .first()
                .periodeFom!!
                .plusMonths(1)
                .toYearMonth(),
        )
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(
            barnetsVilkår
                .first()
                .periodeTom!!
                .minusMonths(1)
                .toYearMonth(),
        )
    }

    @Test
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som treffer regelverk før og etter august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnRegelverkFørOgEtterAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2022, 9, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnRegelverkFørOgEtterAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår = lagVilkårResultater(person = barnRegelverkFørOgEtterAugust2024, personResultat = barnetsPersonResultat)
        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnRegelverkFørOgEtterAugust2024,
                søker = søker,
                tilkjentYtelse = tilkjentYtelse,
                vilkårsvurdering = vilkårsvurdering,
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(
            barnetsVilkår
                .first()
                .periodeFom!!
                .plusMonths(1)
                .toYearMonth(),
        )
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(barnetsVilkår.first().periodeTom!!.toYearMonth())
    }

    @Test
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse hvor et av søkers vilkår avsluttes før barnets vilkår`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barn = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2023, 8, 8))), personType = PersonType.BARN)
        val forventetAndelTom = barn.fødselsdato.plusMonths(15)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat, overstyrendeVilkårResultater = listOf(lagVilkårResultat(vilkårType = Vilkår.BOSATT_I_RIKET, periodeFom = søker.fødselsdato, periodeTom = forventetAndelTom, personResultat = søkersPersonResultat)))
        val barnetsVilkår = lagVilkårResultater(person = barn, personResultat = barnetsPersonResultat)
        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            regelverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barn,
                søker = søker,
                tilkjentYtelse = tilkjentYtelse,
                vilkårsvurdering = vilkårsvurdering,
            )

        // Assert
        assertThat(andelerTilkjentYtelse).hasSize(1)
        assertThat(andelerTilkjentYtelse[0].stønadFom).isEqualTo(barnetsVilkår.first().periodeFom!!.toYearMonth())
        assertThat(andelerTilkjentYtelse[0].stønadTom).isEqualTo(forventetAndelTom.toYearMonth())
    }
}
