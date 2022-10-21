package no.nav.familie.ks.sak.kjerne.beregning.domene

import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.tidslinje.Tidslinje
import no.nav.familie.ks.sak.common.tidslinje.tilPeriodeVerdi
import no.nav.familie.ks.sak.common.tidslinje.tilTidslinje
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.kombinerMed
import no.nav.familie.ks.sak.common.tidslinje.utvidelser.tilPerioder
import no.nav.familie.ks.sak.common.util.TIDENES_MORGEN
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import java.time.LocalDate
import java.time.YearMonth
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
data class TilkjentYtelse(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "tilkjent_ytelse_seq_generator",
        sequenceName = "tilkjent_ytelse_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @OneToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandling: Behandling,

    @Column(name = "stonad_fom", nullable = true, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var stønadFom: YearMonth? = null,

    @Column(name = "stonad_tom", nullable = true, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var stønadTom: YearMonth? = null,

    @Column(name = "opphor_fom", nullable = true, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var opphørFom: YearMonth? = null,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: LocalDate,

    @Column(name = "endret_dato", nullable = false)
    var endretDato: LocalDate,

    @Column(name = "utbetalingsoppdrag", columnDefinition = "TEXT")
    var utbetalingsoppdrag: String? = null,

    @OneToMany(
        fetch = FetchType.EAGER,
        mappedBy = "tilkjentYtelse",
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE, CascadeType.REMOVE],
        orphanRemoval = true
    )
    val andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> = mutableSetOf()
)

fun TilkjentYtelse.tilTidslinjeMedAndeler(): Tidslinje<List<AndelTilkjentYtelse>> {
    val tidslinjer = andelerTilkjentYtelse.map {
        listOf(
            Periode(
                verdi = it,
                fom = it.stønadFom.førsteDagIInneværendeMåned(),
                tom = it.stønadTom.sisteDagIInneværendeMåned()
            )
        ).tilTidslinje()
    }
    return lagTidslinjeMedOverlappendePerioderForAndeler(tidslinjer)
}

fun lagTidslinjeMedOverlappendePerioderForAndeler(tidslinjer: List<Tidslinje<AndelTilkjentYtelse>>): Tidslinje<List<AndelTilkjentYtelse>> {
    if (tidslinjer.isEmpty()) return Tidslinje(startsTidspunkt = TIDENES_MORGEN, perioder = emptyList())

    val førstePeriode = tidslinjer.first().tilPerioder().first()
    val initiellSammenlagt = listOf(Periode(listOf(førstePeriode.verdi!!), førstePeriode.fom, førstePeriode.tom)).tilTidslinje()
    val resterende = tidslinjer.drop(1)
    return resterende.fold(initiellSammenlagt) { sammenlagt, neste ->
        sammenlagt.kombinerMed(neste) { elem1, elem2 ->
            if (elem1.isNullOrEmpty() || elem1.tilPeriodeVerdi().verdi == null) emptyList()
            else if (elem2 == null || elem2.tilPeriodeVerdi().verdi == null) elem1.tilPeriodeVerdi().verdi
            else elem1.tilPeriodeVerdi().verdi!! + elem2.tilPeriodeVerdi().verdi!!
        }
    }
}
