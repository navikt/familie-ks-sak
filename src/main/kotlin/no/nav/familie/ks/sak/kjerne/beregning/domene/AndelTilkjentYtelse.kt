package no.nav.familie.ks.sak.kjerne.beregning.domene

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.integrasjon.økonomi.utbetalingsoppdrag.YtelsetypeKS
import no.nav.familie.ks.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.YearMonth
import java.util.Objects

@Entity(name = "AndelTilkjentYtelse")
@Table(name = "ANDEL_TILKJENT_YTELSE")
data class AndelTilkjentYtelse(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "andel_tilkjent_ytelse_seq_generator")
    @SequenceGenerator(
        name = "andel_tilkjent_ytelse_seq_generator",
        sequenceName = "andel_tilkjent_ytelse_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", nullable = false, updatable = false)
    val behandlingId: Long,
    @ManyToOne(cascade = [CascadeType.MERGE])
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    var tilkjentYtelse: TilkjentYtelse,
    @OneToOne(optional = false)
    @JoinColumn(name = "fk_aktoer_id", nullable = false, updatable = false)
    val aktør: Aktør,
    @Column(name = "kalkulert_utbetalingsbelop", nullable = false)
    val kalkulertUtbetalingsbeløp: Int,
    @Column(name = "stonad_fom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadFom: YearMonth,
    @Column(name = "stonad_tom", nullable = false, columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    val stønadTom: YearMonth,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: YtelseType,
    @Column(name = "sats", nullable = false)
    val sats: Int,
    @Column(name = "prosent", nullable = false)
    val prosent: BigDecimal,
    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.REMOVE], fetch = FetchType.EAGER)
    @JoinTable(
        name = "ANDEL_TIL_ENDRET_ANDEL",
        joinColumns = [JoinColumn(name = "fk_andel_tilkjent_ytelse_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_endret_utbetaling_andel_id")],
    )
    val endretUtbetalingAndeler: MutableList<EndretUtbetalingAndel> = mutableListOf(),
    // kildeBehandlingId, periodeOffset og forrigePeriodeOffset trengs kun i forbindelse med
    // iverksetting/konsistensavstemming, og settes først ved generering av selve oppdraget mot økonomi.
    // Samme informasjon finnes i utbetalingsoppdraget på hver enkelt sak, men for å gjøre operasjonene mer forståelig
    // og enklere å jobbe med har vi valgt å trekke det ut hit.
    // Brukes til å finne hvilke behandlinger som skal konsistensavstemmes
    @Column(name = "kilde_behandling_id")
    var kildeBehandlingId: Long? = null,
    // Brukes for å koble seg på tidligere kjeder sendt til økonomi
    @Column(name = "periode_offset")
    var periodeOffset: Long? = null,
    @Column(name = "forrige_periode_offset")
    var forrigePeriodeOffset: Long? = null,
    @Column(name = "nasjonalt_periodebelop")
    val nasjonaltPeriodebeløp: Int?,
    @Column(name = "differanseberegnet_periodebelop")
    val differanseberegnetPeriodebeløp: Int? = null,
) : BaseEntitet() {
    val periode get() = MånedPeriode(stønadFom, stønadTom)

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        } else if (this === other) {
            return true
        }

        val annen = other as AndelTilkjentYtelse
        return Objects.equals(behandlingId, annen.behandlingId) &&
            Objects.equals(type, annen.type) &&
            Objects.equals(kalkulertUtbetalingsbeløp, annen.kalkulertUtbetalingsbeløp) &&
            Objects.equals(stønadFom, annen.stønadFom) &&
            Objects.equals(stønadTom, annen.stønadTom) &&
            Objects.equals(aktør, annen.aktør) &&
            Objects.equals(nasjonaltPeriodebeløp, annen.nasjonaltPeriodebeløp) &&
            Objects.equals(differanseberegnetPeriodebeløp, annen.differanseberegnetPeriodebeløp)
    }

    override fun hashCode(): Int =
        Objects.hash(
            id,
            behandlingId,
            type,
            kalkulertUtbetalingsbeløp,
            stønadFom,
            stønadTom,
            aktør,
            nasjonaltPeriodebeløp,
            differanseberegnetPeriodebeløp,
        )

    override fun toString(): String =
        "AndelTilkjentYtelse(id = $id, behandling = $behandlingId, type = $type, prosent = $prosent," +
            "beløp = $kalkulertUtbetalingsbeløp, stønadFom = $stønadFom, stønadTom = $stønadTom, " +
            "periodeOffset = $periodeOffset, forrigePeriodeOffset = $forrigePeriodeOffset, " +
            "nasjonaltPeriodebeløp = $nasjonaltPeriodebeløp, differanseberegnetBeløp = $differanseberegnetPeriodebeløp)"

    fun stønadsPeriode() = MånedPeriode(this.stønadFom, this.stønadTom)

    fun erLøpende(nåÅrMåned: YearMonth): Boolean = this.stønadTom > nåÅrMåned

    fun overlapperMed(andelFraAnnenBehandling: AndelTilkjentYtelse): Boolean =
        this.type == andelFraAnnenBehandling.type &&
            this.overlapperPeriode(andelFraAnnenBehandling.periode)

    fun overlapperPeriode(måndePeriode: MånedPeriode): Boolean =
        this.stønadFom <= måndePeriode.tom &&
            this.stønadTom >= måndePeriode.fom

    fun erAndelSomSkalSendesTilOppdrag(): Boolean = this.kalkulertUtbetalingsbeløp != 0
}

fun List<AndelTilkjentYtelse>.slåSammenBack2BackAndelsperioderMedSammeBeløp(): List<AndelTilkjentYtelse> =
    this.fold(emptyList()) { acc, andelTilkjentYtelse ->
        val sisteElement = acc.lastOrNull()

        if (sisteElement?.stønadTom == andelTilkjentYtelse.stønadFom.plusMonths(1) &&
            sisteElement?.aktør == andelTilkjentYtelse.aktør &&
            sisteElement.kalkulertUtbetalingsbeløp == andelTilkjentYtelse.kalkulertUtbetalingsbeløp &&
            sisteElement.type == andelTilkjentYtelse.type
        ) {
            acc.dropLast(1) + sisteElement.copy(stønadTom = andelTilkjentYtelse.stønadTom)
        } else {
            acc + andelTilkjentYtelse
        }
    }

enum class YtelseType(
    val klassifisering: String,
) {
    ORDINÆR_KONTANTSTØTTE("KS"),
    OVERGANGSORDNING_2024("KO_2024"),
    ;

    fun tilYtelseType(): YtelsetypeKS =
        when (this) {
            ORDINÆR_KONTANTSTØTTE -> YtelsetypeKS.ORDINÆR_KONTANTSTØTTE
            OVERGANGSORDNING_2024 -> YtelsetypeKS.ORDINÆR_KONTANTSTØTTE
        }
}
