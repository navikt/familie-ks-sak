package no.nav.familie.ks.sak.kjerne.beregning.lovverkFørFebruar2025

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.brev.lagVilkårResultater
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class LovverkFørFebruar2025AndelGeneratorTest {
    private val lovverkFørFebruar2025AndelGenerator: LovverkFørFebruar2025AndelGenerator = LovverkFørFebruar2025AndelGenerator()

    @Test
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som kun treffer lovverk etter august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnLovverkAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2023, 8, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnLovverkAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår =
            lagVilkårResultater(
                person = barnLovverkAugust2024,
                personResultat = barnetsPersonResultat,
                overstyrendeVilkårResultater =
                    listOf(
                        lagVilkårResultat(
                            personResultat = barnetsPersonResultat,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = barnLovverkAugust2024.fødselsdato.plusYears(1),
                            periodeTom = barnLovverkAugust2024.fødselsdato.plusYears(2),
                            behandlingId = 0L,
                            antallTimer = null,
                        ),
                        lagVilkårResultat(
                            personResultat = barnetsPersonResultat,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = barnLovverkAugust2024.fødselsdato.plusYears(2).plusDays(1),
                            periodeTom = null,
                            antallTimer = BigDecimal(40),
                            resultat = Resultat.IKKE_OPPFYLT,
                        ),
                    ),
            )

        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            lovverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnLovverkAugust2024,
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
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som kun treffer lovverk før august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnLovverkAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2022, 7, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnLovverkAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår = lagVilkårResultater(person = barnLovverkAugust2024, personResultat = barnetsPersonResultat)
        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            lovverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnLovverkAugust2024,
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
    fun `skal kombinere og mappe VilkårResultater til andeler tilkjent ytelse for barn som treffer lovverk før og etter august 2024`() {
        // Arrange
        val søker = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(1991, 3, 16))), personType = PersonType.SØKER)
        val barnLovverkFørOgEtterAugust2024 = lagPerson(aktør = randomAktør(fnr = randomFnr(fødselsdato = LocalDate.of(2022, 9, 8))), personType = PersonType.BARN)
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søkersPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = søker.aktør)
        val barnetsPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barnLovverkFørOgEtterAugust2024.aktør)
        val søkersVilkår = lagVilkårResultater(person = søker, personResultat = søkersPersonResultat)
        val barnetsVilkår =
            lagVilkårResultater(
                person = barnLovverkFørOgEtterAugust2024,
                personResultat = barnetsPersonResultat,
                overstyrendeVilkårResultater =
                    listOf(
                        lagVilkårResultat(
                            personResultat = barnetsPersonResultat,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = barnLovverkFørOgEtterAugust2024.fødselsdato.plusYears(1),
                            periodeTom = barnLovverkFørOgEtterAugust2024.fødselsdato.plusYears(2),
                            behandlingId = 0L,
                            antallTimer = null,
                        ),
                        lagVilkårResultat(
                            personResultat = barnetsPersonResultat,
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = barnLovverkFørOgEtterAugust2024.fødselsdato.plusYears(2).plusDays(1),
                            periodeTom = null,
                            antallTimer = BigDecimal(40),
                            resultat = Resultat.IKKE_OPPFYLT,
                        ),
                    ),
            )

        søkersPersonResultat.setSortedVilkårResultater(søkersVilkår.toSet())
        barnetsPersonResultat.setSortedVilkårResultater(barnetsVilkår.toSet())
        vilkårsvurdering.personResultater = setOf(søkersPersonResultat, barnetsPersonResultat)

        val tilkjentYtelse = mockk<TilkjentYtelse>()
        every { tilkjentYtelse.behandling.id } returns 1

        // Act
        val andelerTilkjentYtelse =
            lovverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
                barn = barnLovverkFørOgEtterAugust2024,
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
            lovverkFørFebruar2025AndelGenerator.beregnAndelerForBarn(
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
