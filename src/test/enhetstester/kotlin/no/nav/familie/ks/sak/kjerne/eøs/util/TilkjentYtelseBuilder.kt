package no.nav.familie.ks.sak.kjerne.eøs.util

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.mapVerdi
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.util.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.util.MAX_MÅNED
import no.nav.familie.ks.sak.common.util.MIN_MÅNED
import no.nav.familie.ks.sak.data.lagBehandling
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ks.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ks.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class TilkjentYtelseBuilder(
    private val startMåned: YearMonth,
    private val behandling: Behandling = lagBehandling(),
) {
    private val tilkjentYtelse =
        TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now(),
        )

    var gjeldendePersoner: List<Person> = emptyList()

    fun forPersoner(vararg personer: Person): TilkjentYtelseBuilder {
        gjeldendePersoner = personer.toList()
        return this
    }

    fun medOrdinær(
        s: String,
        prosent: Long = 100,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
    ) = medYtelse(
        s,
        YtelseType.ORDINÆR_KONTANTSTØTTE,
        prosent,
        nasjonalt,
        differanse,
        kalkulert,
    ) {
        listOf(Periode(7500, null, null)).tilTidslinje()
    }

    private fun medYtelse(
        s: String,
        type: YtelseType,
        prosent: Long = 100,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it },
        satsTidslinje: (Person) -> Tidslinje<Int>,
    ): TilkjentYtelseBuilder {
        val andeler =
            gjeldendePersoner
                .map { person ->
                    val andelTilkjentYtelseTidslinje =
                        s.tilTidslinje(startMåned) {
                            if (it == '$') true else null
                        }.mapVerdi {
                            it?.let {
                                AndelTilkjentYtelse(
                                    behandlingId = behandling.id,
                                    tilkjentYtelse = tilkjentYtelse,
                                    aktør = person.aktør,
                                    stønadFom = MIN_MÅNED,
                                    stønadTom = MAX_MÅNED,
                                    // Overskrives under
                                    kalkulertUtbetalingsbeløp = 0,
                                    // Overskrives under
                                    nasjonaltPeriodebeløp = 0,
                                    // Overskrives under
                                    differanseberegnetPeriodebeløp = null,
                                    prosent = BigDecimal.valueOf(prosent),
                                    // Overskrives under
                                    sats = 0,
                                    type = type,
                                )
                            }
                        }

                    andelTilkjentYtelseTidslinje.kombinerMed(satsTidslinje(person)) { aty, sats ->
                        if (aty == null || sats == null) {
                            null
                        } else {
                            aty.copy(
                                sats = nasjonalt(sats) ?: kalkulert(sats),
                                kalkulertUtbetalingsbeløp = kalkulert(sats),
                                nasjonaltPeriodebeløp = nasjonalt(sats) ?: kalkulert(sats),
                                differanseberegnetPeriodebeløp = differanse(sats),
                            )
                        }
                    }
                }.tilAndelerTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)
        return this
    }

    fun bygg(): TilkjentYtelse = tilkjentYtelse
}
