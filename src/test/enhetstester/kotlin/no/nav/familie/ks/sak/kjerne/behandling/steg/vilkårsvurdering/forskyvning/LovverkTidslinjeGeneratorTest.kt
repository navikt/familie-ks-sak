package no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.forskyvning

import no.nav.familie.ks.sak.common.exception.Feil
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIMåned
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.lovverk.Lovverk
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonType
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class LovverkTidslinjeGeneratorTest {
    @Test
    fun `skal generere lovverk-tidslinje for ett barn`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2021, 5, 10))

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn1.aktør),
                barnasFødselsdatoer = listOf(barn1.fødselsdato),
            )

        val vilkårResultaterPerBarn =
            listOf(barn1)
                .associate { barn ->
                    barn.aktør to
                        Vilkår
                            .hentVilkårFor(barn.type)
                            .associateWith { vilkår ->
                                val (fom, tom) =
                                    if (vilkår == Vilkår.BARNETS_ALDER) {
                                        barn.fødselsdato.plusMonths(12) to barn.fødselsdato.plusMonths(20)
                                    } else {
                                        barn.fødselsdato to null
                                    }
                                listOf(
                                    Periode(
                                        verdi =
                                            lagVilkårResultat(
                                                vilkårType = vilkår,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = fom,
                                                periodeTom = tom,
                                            ),
                                        fom = fom.førsteDagIInneværendeMåned(),
                                        tom = tom?.sisteDagIMåned(),
                                    ),
                                )
                            }
                }

        // Act
        val lovverkTidslinje =
            LovverkTidslinjeGenerator.generer(
                barnasForskjøvedeVilkårResultater = vilkårResultaterPerBarn,
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )

        // Assert
        val lovverkPerioder = lovverkTidslinje.tilPerioderIkkeNull()
        assertThat(lovverkPerioder).hasSize(1)
        assertThat(lovverkPerioder.single().verdi).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
        assertThat(lovverkPerioder.single().fom).isNull()
        assertThat(lovverkPerioder.single().tom).isNull()
    }

    @Test
    fun `skal generere lovverk-tidslinje for barn på likt lovverk`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2021, 5, 10))
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 1, 10))

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn1.aktør, barn2.aktør),
                barnasFødselsdatoer = listOf(barn1.fødselsdato, barn2.fødselsdato),
            )

        val vilkårResultaterPerBarn =
            listOf(barn1, barn2)
                .associate { barn ->
                    barn.aktør to
                        Vilkår
                            .hentVilkårFor(barn.type)
                            .associateWith { vilkår ->
                                val (fom, tom) =
                                    if (vilkår == Vilkår.BARNETS_ALDER) {
                                        barn.fødselsdato.plusMonths(12) to barn.fødselsdato.plusMonths(20)
                                    } else {
                                        barn.fødselsdato to null
                                    }
                                listOf(
                                    Periode(
                                        verdi =
                                            lagVilkårResultat(
                                                vilkårType = vilkår,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = fom,
                                                periodeTom = tom,
                                            ),
                                        fom = fom.førsteDagIInneværendeMåned(),
                                        tom = tom?.sisteDagIMåned(),
                                    ),
                                )
                            }
                }

        // Act
        val lovverkTidslinje =
            LovverkTidslinjeGenerator.generer(
                barnasForskjøvedeVilkårResultater = vilkårResultaterPerBarn,
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )

        // Assert
        val lovverkPerioder = lovverkTidslinje.tilPerioderIkkeNull()
        assertThat(lovverkPerioder).hasSize(1)
        assertThat(lovverkPerioder.single().verdi).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
        assertThat(lovverkPerioder.single().fom).isNull()
        assertThat(lovverkPerioder.single().tom).isNull()
    }

    @Test
    fun `skal generere lovverk-tidslinje for barn på likt lovverk som overlapper`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2021, 5, 10))
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2022, 3, 10))

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn1.aktør, barn2.aktør),
                barnasFødselsdatoer = listOf(barn1.fødselsdato, barn2.fødselsdato),
            )

        val vilkårResultaterPerBarn =
            listOf(barn1, barn2)
                .associate { barn ->
                    barn.aktør to
                        Vilkår
                            .hentVilkårFor(barn.type)
                            .associateWith { vilkår ->
                                val (fom, tom) =
                                    if (vilkår == Vilkår.BARNETS_ALDER) {
                                        barn.fødselsdato.plusMonths(12) to barn.fødselsdato.plusMonths(20)
                                    } else {
                                        barn.fødselsdato to null
                                    }
                                listOf(
                                    Periode(
                                        verdi =
                                            lagVilkårResultat(
                                                vilkårType = vilkår,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = fom,
                                                periodeTom = tom,
                                            ),
                                        fom = fom.førsteDagIInneværendeMåned(),
                                        tom = tom?.sisteDagIMåned(),
                                    ),
                                )
                            }
                }

        // Act
        val lovverkTidslinje =
            LovverkTidslinjeGenerator.generer(
                barnasForskjøvedeVilkårResultater = vilkårResultaterPerBarn,
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )

        // Assert
        val lovverkPerioder = lovverkTidslinje.tilPerioderIkkeNull()
        assertThat(lovverkPerioder).hasSize(1)
        assertThat(lovverkPerioder.single().verdi).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
        assertThat(lovverkPerioder.single().fom).isNull()
        assertThat(lovverkPerioder.single().tom).isNull()
    }

    @Test
    fun `skal generere lovverk-tidslinje for barn på ulike lovverk`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2021, 5, 10))
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2024, 1, 10))

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn1.aktør, barn2.aktør),
                barnasFødselsdatoer = listOf(barn1.fødselsdato, barn2.fødselsdato),
            )

        val vilkårResultaterPerBarn =
            listOf(barn1, barn2)
                .associate { barn ->
                    barn.aktør to
                        Vilkår
                            .hentVilkårFor(barn.type)
                            .associateWith { vilkår ->
                                val (fom, tom) =
                                    if (vilkår == Vilkår.BARNETS_ALDER) {
                                        barn.fødselsdato.plusMonths(12) to barn.fødselsdato.plusMonths(20)
                                    } else {
                                        barn.fødselsdato to null
                                    }
                                listOf(
                                    Periode(
                                        verdi =
                                            lagVilkårResultat(
                                                vilkårType = vilkår,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = fom,
                                                periodeTom = tom,
                                            ),
                                        fom = fom.førsteDagIInneværendeMåned(),
                                        tom = tom?.sisteDagIMåned(),
                                    ),
                                )
                            }
                }

        // Act
        val lovverkTidslinje =
            LovverkTidslinjeGenerator.generer(
                barnasForskjøvedeVilkårResultater = vilkårResultaterPerBarn,
                personopplysningGrunnlag = personopplysningGrunnlag,
                adopsjonerIBehandling = emptyList(),
            )

        // Assert
        val lovverkPerioder = lovverkTidslinje.tilPerioderIkkeNull()
        assertThat(lovverkPerioder).hasSize(2)
        assertThat(lovverkPerioder.first().verdi).isEqualTo(Lovverk.FØR_LOVENDRING_2025)
        assertThat(lovverkPerioder.first().fom).isNull()
        assertThat(lovverkPerioder.first().tom).isEqualTo(
            barn2.fødselsdato
                .plusYears(1)
                .førsteDagIInneværendeMåned()
                .minusDays(1),
        )
        assertThat(lovverkPerioder.last().verdi).isEqualTo(Lovverk.LOVENDRING_FEBRUAR_2025)
        assertThat(lovverkPerioder.last().fom).isEqualTo(
            barn2.fødselsdato
                .plusYears(1)
                .førsteDagIInneværendeMåned(),
        )
        assertThat(lovverkPerioder.last().tom).isNull()
    }

    @Test
    fun `skal kaste feil dersom barn på ulike lovverk overlapper`() {
        // Arrange
        val søker = lagPerson(personType = PersonType.SØKER, aktør = randomAktør())
        val barn1 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2023, 5, 10))
        val barn2 = lagPerson(personType = PersonType.BARN, aktør = randomAktør(), fødselsdato = LocalDate.of(2024, 1, 10))

        val personopplysningGrunnlag =
            lagPersonopplysningGrunnlag(
                søkerAktør = søker.aktør,
                barnAktør = listOf(barn1.aktør, barn2.aktør),
                barnasFødselsdatoer = listOf(barn1.fødselsdato, barn2.fødselsdato),
            )

        val vilkårResultaterPerBarn =
            listOf(barn1, barn2)
                .associate { barn ->
                    barn.aktør to
                        Vilkår
                            .hentVilkårFor(barn.type)
                            .associateWith { vilkår ->
                                val (fom, tom) =
                                    if (vilkår == Vilkår.BARNETS_ALDER) {
                                        barn.fødselsdato.plusMonths(12) to barn.fødselsdato.plusMonths(20)
                                    } else {
                                        barn.fødselsdato to null
                                    }
                                listOf(
                                    Periode(
                                        verdi =
                                            lagVilkårResultat(
                                                vilkårType = vilkår,
                                                resultat = Resultat.OPPFYLT,
                                                periodeFom = fom,
                                                periodeTom = tom,
                                            ),
                                        fom = fom.førsteDagIInneværendeMåned(),
                                        tom = tom?.sisteDagIMåned(),
                                    ),
                                )
                            }
                }

        // Act & Assert
        val feil =
            assertThrows<Feil> {
                LovverkTidslinjeGenerator.generer(
                    barnasForskjøvedeVilkårResultater = vilkårResultaterPerBarn,
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    adopsjonerIBehandling = emptyList(),
                )
            }
        assertThat(feil.message).isEqualTo("Støtter ikke overlappende lovverk")
    }
}
