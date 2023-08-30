package no.nav.familie.ks.sak.kjerne.behandling.steg.behandlingsresultat

import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.time.Period
import java.time.YearMonth

data class BehandlingsresultatPerson(
    val aktør: Aktør,
    val søktForPerson: Boolean, // flagg som markerer om person er inkludert i utledning
    val eksplisittAvslag: Boolean = false,
    val forrigeAndeler: List<BehandlingsresultatAndelTilkjentYtelse> = emptyList(),
    val andeler: List<BehandlingsresultatAndelTilkjentYtelse>,
) {

    /**
     * Utleder krav for personer framstilt nå og/eller tidligere.
     * Disse populeres med behandlingens utfall for enkeltpersonene (YtelsePerson),
     * som igjen brukes for å utlede det totale Behandlingsresultat.
     *
     * @return Informasjon om hvordan person påvirkes i behandlingen (se YtelsePerson-doc)
     */
    fun utledYtelsePerson(): YtelsePerson {
        return YtelsePerson(
            aktør = aktør,
            ytelseType = YtelseType.ORDINÆR_KONTANTSTØTTE,
            kravOpprinnelse = utledKravOpprinnelser(),
        )
    }

    private fun utledKravOpprinnelser(): List<KravOpprinnelse> {
        return when {
            forrigeAndeler.isNotEmpty() && !søktForPerson -> listOf(KravOpprinnelse.TIDLIGERE)
            forrigeAndeler.isNotEmpty() && søktForPerson -> listOf(
                KravOpprinnelse.TIDLIGERE,
                KravOpprinnelse.INNEVÆRENDE,
            )
            else -> listOf(KravOpprinnelse.INNEVÆRENDE)
        }
    }

    override fun toString(): String {
        return "BehandlingsresultatPerson(" +
            "søktForPerson=$søktForPerson, " +
            "eksplisittAvslag=$eksplisittAvslag, " +
            "forrigeAndeler=$forrigeAndeler, " +
            "andeler=$andeler)"
    }
}

data class BehandlingsresultatAndelTilkjentYtelse(
    val stønadFom: YearMonth,
    val stønadTom: YearMonth,
    val kalkulertUtbetalingsbeløp: Int,
) {

    val periode get() = MånedPeriode(stønadFom, stønadTom)

    fun erLøpende(inneværendeMåned: YearMonth): Boolean = this.stønadTom > inneværendeMåned

    fun sumForPeriode(): Int {
        val between = Period.between(
            stønadFom.førsteDagIInneværendeMåned(),
            stønadTom.sisteDagIInneværendeMåned(),
        )
        val antallMåneder = (between.years * 12) + between.months

        return antallMåneder * kalkulertUtbetalingsbeløp
    }
}
