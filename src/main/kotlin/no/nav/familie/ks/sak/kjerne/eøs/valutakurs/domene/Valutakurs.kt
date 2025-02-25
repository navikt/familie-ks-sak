package no.nav.familie.ks.sak.kjerne.eøs.valutakurs.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.kjerne.eøs.felles.domene.EøsSkjemaEntitet
import no.nav.familie.ks.sak.kjerne.personident.Aktør
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Entity(name = "Valutakurs")
@Table(name = "VALUTAKURS")
data class Valutakurs(
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val fom: YearMonth?,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    override val tom: YearMonth?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "AKTOER_TIL_VALUTAKURS",
        joinColumns = [JoinColumn(name = "fk_valutakurs_id")],
        inverseJoinColumns = [JoinColumn(name = "fk_aktoer_id")],
    )
    override val barnAktører: Set<Aktør> = emptySet(),
    @Column(name = "valutakursdato", columnDefinition = "DATE")
    val valutakursdato: LocalDate? = null,
    @Column(name = "valutakode")
    val valutakode: String? = null,
    @Column(name = "kurs", nullable = false)
    val kurs: BigDecimal? = null,
) : EøsSkjemaEntitet<Valutakurs>() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "valutakurs_seq_generator")
    @SequenceGenerator(
        name = "valutakurs_seq_generator",
        sequenceName = "valutakurs_seq",
        allocationSize = 50,
    )
    override var id: Long = 0

    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    override var behandlingId: Long = 0

    // Valutakode skal alltid være satt (muligens til null), så den slettes ikke
    override fun utenInnhold() =
        copy(
            valutakursdato = null,
            kurs = null,
        )

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører,
    )

    companion object {
        val NULL = Valutakurs(null, null, emptySet())
    }
}
