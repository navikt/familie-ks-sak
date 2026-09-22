package no.nav.familie.ks.sak.kjerne.eøs.util

import no.nav.familie.ks.sak.common.BehandlingId
import no.nav.familie.ks.sak.data.lagEndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.beregning.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ks.sak.kjerne.beregning.endretUtbetaling.AndelTilkjentYtelseMedEndretUtbetalingBehandler
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.eøs.utenlandskperiodebeløp.jan
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.YearMonth

class DeltBostedBuilder(
    startMåned: YearMonth = jan(2020),
    internal val tilkjentYtelse: TilkjentYtelse,
) : SkjemaBuilder<DeltBosted, DeltBostedBuilder>(startMåned, BehandlingId(tilkjentYtelse.behandling.id)) {
    fun medDeltBosted(
        k: String,
        vararg barn: Person,
    ) = medSkjema(k, barn.toList()) {
        when (it) {
            '0' -> DeltBosted(prosent = 0)
            '/' -> DeltBosted(prosent = 50)
            '1' -> DeltBosted(prosent = 100)
            else -> null
        }
    }
}

data class DeltBosted(
    override val fom: YearMonth? = null,
    override val tom: YearMonth? = null,
    override val barnAktører: Set<Aktør> = emptySet(),
    val prosent: Int?,
) : EøsSkjemaEntitet<DeltBosted>() {
    override fun utenInnhold() = copy(prosent = null)

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører.map { it.copy() }.toSet(),
    )

    override var id: Long = 0
    override var behandlingId: Long = 0
}

fun DeltBostedBuilder.oppdaterTilkjentYtelse(): TilkjentYtelse {
    val andelerTilkjentYtelserEtterEUA =
        AndelTilkjentYtelseMedEndretUtbetalingBehandler.lagAndelerMedEndretUtbetalingAndeler(
            tilkjentYtelse.andelerTilkjentYtelse.toList(),
            bygg().tilEndreteUtebetalingAndeler(),
            tilkjentYtelse,
        )

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelserEtterEUA.map { it.andel })
    return tilkjentYtelse
}

fun Iterable<DeltBosted>.tilEndreteUtebetalingAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> =
    this
        .filter { deltBosted -> deltBosted.fom != null && deltBosted.tom != null && deltBosted.prosent != null }
        .flatMap { deltBosted ->
            deltBosted.barnAktører.map { barnAktør ->
                val endretUtbetalingAndel: EndretUtbetalingAndel =
                    lagEndretUtbetalingAndel(
                        behandlingId = deltBosted.behandlingId,
                        aktører = setOf(barnAktør),
                        periodeFom = deltBosted.fom!!,
                        periodeTom = deltBosted.tom!!,
                        prosent = deltBosted.prosent!!.toBigDecimal(),
                        årsak = Årsak.ETTERBETALING_3MND,
                    )
                EndretUtbetalingAndelMedAndelerTilkjentYtelse(endretUtbetalingAndel, emptyList())
            }
        }
