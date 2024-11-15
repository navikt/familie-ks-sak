package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.toYearMonth
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagPersonResultat
import no.nav.familie.ks.sak.data.lagUtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.lagVilkårsvurdering
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class OvergangsordningAndelValidatorTest {
    private val fødselsdato: LocalDate = LocalDate.now().minusYears(1)
    private val aktør = randomAktør(randomFnr(fødselsdato))
    private val person = lagPerson(aktør = aktør)
    private val andel =
        lagUtfyltOvergangsordningAndel(
            fom = YearMonth.now(),
            tom = YearMonth.now(),
            person = person,
        )

    @Test
    fun validerOvergangsordningAndeler() {
        val overgangsordningAndel =
            OvergangsordningAndel(
                behandlingId = 1,
                fom = person.fødselsdato.plusMonths(20).toYearMonth(),
                tom = person.fødselsdato.plusMonths(23).toYearMonth(),
                person = person,
            )

        val ordinæreAndel =
            lagAndelTilkjentYtelse(
                aktør = aktør,
                fom = person.fødselsdato.plusMonths(13).toYearMonth(),
                tom = person.fødselsdato.plusMonths(19).toYearMonth(),
                ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
            )

        val personResultaterForBarn =
            lagPersonResultat(
                vilkårsvurdering = lagVilkårsvurdering(),
                aktør = aktør,
                lagVilkårResultater = {
                    setOf(
                        lagVilkårResultat(
                            vilkårType = Vilkår.BARNEHAGEPLASS,
                            periodeFom = person.fødselsdato,
                            periodeTom = null,
                        ),
                    )
                },
            )

        assertDoesNotThrow {
            OvergangsordningAndelValidator.validerOvergangsordningAndeler(
                overgangsordningAndeler = listOf(overgangsordningAndel),
                andelerTilkjentYtelseNåværendeBehandling = setOf(ordinæreAndel),
                andelerTilkjentYtelseForrigeBehandling = setOf(ordinæreAndel),
                personResultaterForBarn = listOf(personResultaterForBarn),
                barna = listOf(person),
            )
        }
    }

    @Test
    fun validerIngenEndringIOrdinæreAndelerTilkjentYtelse() {
        val nåværendeAndeler =
            setOf(
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 5),
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
            )

        val forrigeAndeler =
            setOf(
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.of(2024, 1),
                    tom = YearMonth.of(2024, 3),
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
            )

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerIngenEndringIOrdinæreAndelerTilkjentYtelse(
                andelerNåværendeBehandling = nåværendeAndeler,
                andelerForrigeBehandling = forrigeAndeler,
                barna = listOf(person),
            )
        }
    }

    @Test
    fun validerAndelerErIPeriodenBarnetEr20Til23Måneder() {
        val andeler = listOf(andel)

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAndelerErIPeriodenBarnetEr20Til23Måneder(andeler)
        }
    }

    @Test
    fun validerAtAlleOpprettedeOvergangsordningAndelerErGyldigUtfylt() {
        val andeler = listOf(OvergangsordningAndel(behandlingId = 1))

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtAlleOpprettedeOvergangsordningAndelerErGyldigUtfylt(andeler)
        }
    }

    @Test
    fun validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler() {
        val andelerTilkjentYtelse =
            setOf(
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.now().plusMonths(1),
                    tom = YearMonth.now().plusMonths(2),
                    ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
                ),
                lagAndelTilkjentYtelse(
                    aktør = aktør,
                    fom = YearMonth.now().plusMonths(2),
                    tom = YearMonth.now().plusMonths(3),
                    ytelseType = YtelseType.OVERGANGSORDNING,
                ),
            )

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(andelerTilkjentYtelse)
        }
    }

    @Test
    fun validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder() {
        val barnehageVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.now().minusYears(1),
                periodeTom = LocalDate.now().minusMonths(1),
            )

        val barnehagevilkårPerAktør = mapOf(aktør to listOf(barnehageVilkår))

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(listOf(andel), barnehagevilkårPerAktør)
        }
    }

    @Test
    fun validerAtBarnehagevilkårMedTomLikNullErOppfyltForAlleOvergangsordningPerioder() {
        val barnehageVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.now().minusYears(1),
                periodeTom = null,
            )

        val barnehagevilkårPerAktør = mapOf(aktør to listOf(barnehageVilkår))

        assertDoesNotThrow {
            OvergangsordningAndelValidator.validerAtBarnehagevilkårErOppfyltForAlleOvergangsordningPerioder(listOf(andel), barnehagevilkårPerAktør)
        }
    }

    @Test
    fun validerIngenOverlappMedEksisterendeOvergangsordningAndeler() {
        val eksisterendeAndeler =
            listOf(
                OvergangsordningAndel(
                    behandlingId = 1,
                    person = person,
                    fom = YearMonth.now(),
                    tom = YearMonth.now(),
                ).tilUtfyltOvergangsordningAndel(),
            )

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerIngenOverlappMedEksisterendeOvergangsordningAndeler(andel, eksisterendeAndeler)
        }
    }

    @Test
    fun validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode() {
        val barnehageVilkår =
            lagVilkårResultat(
                vilkårType = Vilkår.BARNEHAGEPLASS,
                periodeFom = LocalDate.now().minusYears(1),
                periodeTom = LocalDate.now().minusMonths(1),
            )

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtBarnehagevilkårErOppfyltIOvergangsordningAndelPeriode(andel, listOf(barnehageVilkår))
        }
    }

    @Test
    fun validerFomDato() {
        val gyldigFom = YearMonth.now().plusMonths(2)

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerFomDato(andel, gyldigFom)
        }
    }

    @Test
    fun validerTomDato() {
        val gyldigTom = YearMonth.now().minusMonths(1)

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerTomDato(andel, gyldigTom)
        }
    }
}
