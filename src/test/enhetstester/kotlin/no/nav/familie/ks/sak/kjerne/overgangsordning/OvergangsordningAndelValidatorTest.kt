package no.nav.familie.ks.sak.kjerne.overgangsordning

import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.data.lagAndelTilkjentYtelse
import no.nav.familie.ks.sak.data.lagInitiellTilkjentYtelse
import no.nav.familie.ks.sak.data.lagPerson
import no.nav.familie.ks.sak.data.lagUtfyltOvergangsordningAndel
import no.nav.familie.ks.sak.data.lagVilkårResultat
import no.nav.familie.ks.sak.data.randomAktør
import no.nav.familie.ks.sak.data.randomFnr
import no.nav.familie.ks.sak.kjerne.behandling.steg.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.overgangsordning.domene.OvergangsordningAndel
import org.junit.jupiter.api.Test
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
    fun validerAndelerErIPeriodenBarnetEr20Til23Måneder() {
        val andeler = listOf(andel)

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAndelerErIPeriodenBarnetEr20Til23Måneder(andeler)
        }
    }

    @Test
    fun validerAtAlleOpprettedeOvergangsordningAndelerErUtfylt() {
        val andeler = listOf(OvergangsordningAndel(behandlingId = 1))

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtAlleOpprettedeOvergangsordningAndelerErUtfylt(andeler)
        }
    }

    @Test
    fun validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler() {
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(
            listOf(
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
            ),
        )

        assertThrows<FunksjonellFeil> {
            OvergangsordningAndelValidator.validerAtOvergangsordningAndelerIkkeOverlapperMedOrdinæreAndeler(tilkjentYtelse)
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
    fun validerIngenOverlappMedEksisterendeOvergangsordningAndeler() {
        val eksisterendeAndeler =
            listOf(
                OvergangsordningAndel(
                    behandlingId = 1,
                    fom = YearMonth.now(),
                    tom = YearMonth.now(),
                ),
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
