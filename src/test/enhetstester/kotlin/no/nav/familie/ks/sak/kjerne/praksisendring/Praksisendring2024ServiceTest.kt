package no.nav.familie.ks.sak.kjerne.praksisendring

import aug
import io.mockk.every
import io.mockk.mockk
import jan
import jul
import no.nav.familie.ks.sak.common.util.NullablePeriode
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagInitiellTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagPersonopplysningGrunnlag
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårResultaterForBarn
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Resultat
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.PersonopplysningGrunnlag
import okt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class Praksisendring2024ServiceTest {
    private val mockPraksisendring2024Repository = mockk<Praksisendring2024Repository>()
    private val praksisendring2024Service = Praksisendring2024Service(mockPraksisendring2024Repository)

    @ParameterizedTest
    @ValueSource(ints = [8, 9, 10, 11, 12])
    fun `skal generere andel for barn som blir 13 måneder og som tidligere har fått utbetalt i aug-des 2024`(måned: Int) {
        // Arrange
        val utbetalingsmåned = YearMonth.of(2024, måned)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato))
        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns
            listOf(
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn,
                    utbetalingsmåned = fødselsdato.plusMonths(13).toYearMonth(),
                ),
            )

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).hasSize(1)
        with(andelerForPraksisendring2024.first()) {
            assertThat(aktør).isEqualTo(barn)
            assertThat(stønadFom).isEqualTo(utbetalingsmåned)
            assertThat(stønadTom).isEqualTo(utbetalingsmåned)
            assertThat(kalkulertUtbetalingsbeløp).isEqualTo(7500)
            assertThat(nasjonaltPeriodebeløp).isEqualTo(7500)
            assertThat(type).isEqualTo(YtelseType.PRAKSISENDRING_2024)
            assertThat(sats).isEqualTo(7500)
            assertThat(prosent).isEqualTo(BigDecimal(100))
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [8, 9, 10, 11, 12])
    fun `skal ikke generere andel for barn som blir 13 måneder og som tidligere ikke har fått utbetalt i aug-des 2024`(måned: Int) {
        // Arrange
        val utbetalingsmåned = YearMonth.of(2024, måned)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato))
        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns emptyList()

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    @Test
    fun `skal ikke generere andel for barn som blir 13 måneder i juli 2024`() {
        // Arrange
        val utbetalingsmåned = jul(2024)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato))

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    @Test
    fun `skal ikke generere andel for barn som blir 13 måneder i januar 2025`() {
        // Arrange
        val utbetalingsmåned = jan(2025)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato))

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    @Test
    fun `skal generere to andeler for barn som blir 13 måneder i forskjellige måneder`() {
        // Arrange
        val utbetalingsmånedBarn1 = aug(2024)
        val (barn1, fødselsdatoBarn1) = lagBarn(utbetalingsmånedBarn1)

        val utbetalingsmånedBarn2 = okt(2024)
        val (barn2, fødselsdatoBarn2) = lagBarn(utbetalingsmånedBarn2)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn1, barn2))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn1 to fødselsdatoBarn1, barn2 to fødselsdatoBarn2))
        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns
            listOf(
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn1,
                    utbetalingsmåned = fødselsdatoBarn1.plusMonths(13).toYearMonth(),
                ),
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn2,
                    utbetalingsmåned = fødselsdatoBarn2.plusMonths(13).toYearMonth(),
                ),
            )

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024)
            .hasSize(2)
            .anySatisfy {
                assertThat(it.aktør).isEqualTo(barn1)
                assertThat(it.stønadFom).isEqualTo(utbetalingsmånedBarn1)
                assertThat(it.stønadTom).isEqualTo(utbetalingsmånedBarn1)
            }.anySatisfy {
                assertThat(it.aktør).isEqualTo(barn2)
                assertThat(it.stønadFom).isEqualTo(utbetalingsmånedBarn2)
                assertThat(it.stønadTom).isEqualTo(utbetalingsmånedBarn2)
            }.allSatisfy {
                assertThat(it.kalkulertUtbetalingsbeløp).isEqualTo(7500)
                assertThat(it.nasjonaltPeriodebeløp).isEqualTo(7500)
                assertThat(it.type).isEqualTo(YtelseType.PRAKSISENDRING_2024)
                assertThat(it.sats).isEqualTo(7500)
                assertThat(it.prosent).isEqualTo(BigDecimal(100))
            }
    }

    @Test
    fun `skal generere en andel dersom ett av to barn blir truffet av praksisendring`() {
        // Arrange
        val utbetalingsmånedBarn1 = jul(2024)
        val (barn1, fødselsdatoBarn1) = lagBarn(utbetalingsmånedBarn1)

        val utbetalingsmånedBarn2 = aug(2024)
        val (barn2, fødselsdatoBarn2) = lagBarn(utbetalingsmånedBarn2)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn1, barn2))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn1 to fødselsdatoBarn1, barn2 to fødselsdatoBarn2))
        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns
            listOf(
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn2,
                    utbetalingsmåned = fødselsdatoBarn2.plusMonths(13).toYearMonth(),
                ),
            )

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024)
            .hasSize(1)
            .anySatisfy {
                assertThat(it.aktør).isEqualTo(barn2)
                assertThat(it.stønadFom).isEqualTo(utbetalingsmånedBarn2)
                assertThat(it.stønadTom).isEqualTo(utbetalingsmånedBarn2)
                assertThat(it.kalkulertUtbetalingsbeløp).isEqualTo(7500)
                assertThat(it.nasjonaltPeriodebeløp).isEqualTo(7500)
                assertThat(it.type).isEqualTo(YtelseType.PRAKSISENDRING_2024)
                assertThat(it.sats).isEqualTo(7500)
                assertThat(it.prosent).isEqualTo(BigDecimal(100))
            }
    }

    @Test
    fun `skal generere en andel med 50 prosent utbetaling for barn med delt bosted`() {
        // Arrange
        val utbetalingsmånedBarn = aug(2024)
        val (barn, fødselsdatoBarn) = lagBarn(utbetalingsmånedBarn)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering =
            lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdatoBarn)).apply {
                personResultater.first().vilkårResultater.removeIf { it.vilkårType == Vilkår.BOR_MED_SØKER }
                personResultater.first().vilkårResultater.add(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = fødselsdatoBarn,
                        periodeTom = null,
                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                    ),
                )
            }

        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns
            listOf(
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn,
                    utbetalingsmåned = fødselsdatoBarn.plusMonths(13).toYearMonth(),
                ),
            )

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024)
            .hasSize(1)
            .anySatisfy {
                assertThat(it.aktør).isEqualTo(barn)
                assertThat(it.stønadFom).isEqualTo(utbetalingsmånedBarn)
                assertThat(it.stønadTom).isEqualTo(utbetalingsmånedBarn)
                assertThat(it.kalkulertUtbetalingsbeløp).isEqualTo(3750)
                assertThat(it.nasjonaltPeriodebeløp).isEqualTo(3750)
                assertThat(it.type).isEqualTo(YtelseType.PRAKSISENDRING_2024)
                assertThat(it.sats).isEqualTo(7500)
                assertThat(it.prosent).isEqualTo(BigDecimal(50))
            }
    }

    @Test
    fun `skal ikke generere andel dersom barn allerede har andel i måned`() {
        // Arrange
        val utbetalingsmåned = aug(2024)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse =
            lagInitiellTilkjentYtelse().apply {
                andelerTilkjentYtelse.add(
                    lagAndelTilkjentYtelse(
                        aktør = barn,
                        stønadFom = utbetalingsmåned,
                        stønadTom = utbetalingsmåned,
                    ),
                )
            }
        val vilkårsvurdering = lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato))

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    @Test
    fun `skal ikke generere andel dersom barn ikke starter i barnehage samme måned som det blir 13 måneder`() {
        // Arrange
        val utbetalingsmåned = aug(2024)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val fullBarnehageplassFraMåned15 = listOf(NullablePeriode(fødselsdato.plusMonths(15), null) to BigDecimal(33))
        val vilkårsvurdering =
            lagVilkårsvurdering { vilkårsvurdering ->
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn,
                        lagVilkårResultater = { personResultat ->
                            lagVilkårResultaterForBarn(
                                personResultat = personResultat,
                                barnFødselsdato = fødselsdato,
                                barnehageplassPerioder = fullBarnehageplassFraMåned15,
                                behandlingId = 0,
                            )
                        },
                    ),
                )
            }

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    @Test
    fun `skal generere andel dersom barn startet i delttidsbarnehage samme måned som det blir 13 måneder`() {
        // Arrange
        val utbetalingsmåned = aug(2024)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val delttidsBarnehageplassFraMåned13 = listOf(NullablePeriode(fødselsdato.plusMonths(13), null) to BigDecimal(20))
        val vilkårsvurdering =
            lagVilkårsvurdering { vilkårsvurdering ->
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn,
                        lagVilkårResultater = { personResultat ->
                            lagVilkårResultaterForBarn(
                                personResultat = personResultat,
                                barnFødselsdato = fødselsdato,
                                barnehageplassPerioder = delttidsBarnehageplassFraMåned13,
                                behandlingId = 0,
                            )
                        },
                    ),
                )
            }

        val fagsakId = vilkårsvurdering.behandling.fagsak.id

        every {
            mockPraksisendring2024Repository.finnPraksisendring2024ForFagsak(fagsakId)
        } returns
            listOf(
                Praksisendring2024(
                    id = 0,
                    fagsakId = fagsakId,
                    aktør = barn,
                    utbetalingsmåned = fødselsdato.plusMonths(13).toYearMonth(),
                ),
            )

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024)
            .hasSize(1)
            .anySatisfy {
                assertThat(it.aktør).isEqualTo(barn)
                assertThat(it.stønadFom).isEqualTo(utbetalingsmåned)
                assertThat(it.stønadTom).isEqualTo(utbetalingsmåned)
                assertThat(it.kalkulertUtbetalingsbeløp).isEqualTo(7500)
                assertThat(it.nasjonaltPeriodebeløp).isEqualTo(7500)
                assertThat(it.type).isEqualTo(YtelseType.PRAKSISENDRING_2024)
                assertThat(it.sats).isEqualTo(7500)
                assertThat(it.prosent).isEqualTo(BigDecimal(100))
            }
    }

    @ParameterizedTest
    @EnumSource(Vilkår::class, names = ["BOSATT_I_RIKET", "MEDLEMSKAP_ANNEN_FORELDER", "BOR_MED_SØKER", "BARNETS_ALDER"])
    fun `skal ikke generere andel dersom andre vilkår enn barnehageplass ikke er oppfylt i måned`(vilkår: Vilkår) {
        // Arrange
        val utbetalingsmåned = aug(2024)
        val (barn, fødselsdato) = lagBarn(utbetalingsmåned)

        val personopplysningGrunnlag = lagPersonopplysningGrunnlag(barn = listOf(barn))
        val tilkjentYtelse = lagInitiellTilkjentYtelse()
        val vilkårsvurdering =
            lagVilkårsvurderingMedBarnehageplassFomMåned13(setOf(barn to fødselsdato)).apply {
                personResultater.first().vilkårResultater.removeIf { it.vilkårType == vilkår }
                personResultater.first().vilkårResultater.add(
                    lagVilkårResultat(
                        vilkårType = vilkår,
                        resultat = Resultat.IKKE_OPPFYLT,
                        periodeFom = fødselsdato,
                        periodeTom = null,
                    ),
                )
            }

        // Act
        val andelerForPraksisendring2024 =
            praksisendring2024Service.genererAndelerForPraksisendring2024(
                personopplysningGrunnlag = personopplysningGrunnlag,
                vilkårsvurdering = vilkårsvurdering,
                tilkjentYtelse = tilkjentYtelse,
            )

        // Assert
        assertThat(andelerForPraksisendring2024).isEmpty()
    }

    private fun lagVilkårsvurderingMedBarnehageplassFomMåned13(
        barnMedFødselsdato: Collection<Pair<Aktør, LocalDate>>,
    ) = lagVilkårsvurdering { vilkårsvurdering ->
        barnMedFødselsdato
            .map { (barn, fødselsdato) ->
                lagPersonResultat(
                    vilkårsvurdering = vilkårsvurdering,
                    aktør = barn,
                    lagVilkårResultater = { personResultat ->
                        lagVilkårResultaterForBarn(
                            personResultat = personResultat,
                            barnFødselsdato = fødselsdato,
                            barnehageplassPerioder = listOf(NullablePeriode(fødselsdato.plusMonths(13), null) to BigDecimal(33)),
                            behandlingId = 0,
                        )
                    },
                )
            }.toSet()
    }

    private fun lagPersonopplysningGrunnlag(
        barn: List<Aktør>,
    ): PersonopplysningGrunnlag =
        lagPersonopplysningGrunnlag(
            barnasIdenter = barn.map { it.aktivFødselsnummer() },
            barnAktør = barn,
        )

    private fun lagBarn(månedBarnBlir13Måneder: YearMonth): Pair<Aktør, LocalDate> {
        val fødselsdato = månedBarnBlir13Måneder.atDay(1).minusMonths(13)
        val barn = randomAktør(randomFnr(fødselsdato))
        return barn to fødselsdato
    }
}
