package no.nav.familie.ks.sak.kjerne.beregning.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.common.util.førsteDagIInneværendeMåned
import no.nav.familie.ks.sak.common.util.sisteDagIInneværendeMåned
import no.nav.familie.ks.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.slåSammen
import java.time.LocalDate
import java.time.YearMonth

@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
data class TilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "tilkjent_ytelse_seq_generator",
        sequenceName = "tilkjent_ytelse_seq",
        allocationSize = 50,
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
        orphanRemoval = true,
    )
    val andelerTilkjentYtelse: MutableSet<AndelTilkjentYtelse> = mutableSetOf(),
)

fun TilkjentYtelse.tilTidslinjeMedAndeler(): Tidslinje<Collection<AndelTilkjentYtelse>> {
    val tidslinjer =
        andelerTilkjentYtelse.map {
            listOf(
                Periode(
                    verdi = it,
                    fom = it.stønadFom.førsteDagIInneværendeMåned(),
                    tom = it.stønadTom.sisteDagIInneværendeMåned(),
                ),
            ).tilTidslinje()
        }
    return tidslinjer.slåSammen()
}

fun TilkjentYtelse.utbetalingsoppdrag(): Utbetalingsoppdrag? = objectMapper.readValue(this.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)

fun TilkjentYtelse.skalIverksettesMotOppdrag(): Boolean = this.utbetalingsoppdrag()?.utbetalingsperiode?.isNotEmpty() ?: false
