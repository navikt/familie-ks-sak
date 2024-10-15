package no.nav.familie.ks.sak.kjerne.overgangsordning.domene

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.ks.sak.api.dto.OvergangsordningAndelDto
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.tidslinje.Periode
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.math.BigDecimal
import java.time.YearMonth

@Entity(name = "OvergangsordningAndel")
@Table(name = "OVERGANGSORDNING_ANDEL")
data class OvergangsordningAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "overgangsordning_andel_seq_generator")
    @SequenceGenerator(
        name = "overgangsordning_andel_seq_generator",
        sequenceName = "overgangsordning_andel_seq",
        allocationSize = 50,
    )
    val id: Long = 0,
    @Column(name = "fk_behandling_id", updatable = false, nullable = false)
    val behandlingId: Long,
    @ManyToOne
    @JoinColumn(name = "fk_po_person_id")
    var person: Person? = null,
    @Column(name = "prosent")
    var prosent: BigDecimal? = null,
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,
) : BaseEntitet() {
    override fun toString(): String =
        "OvergangsordningAndel(" +
            "id=$id, " +
            "behandling=$behandlingId, " +
            "person=${person?.aktør}, " +
            "prosent=$prosent, " +
            "fom=$fom, " +
            "tom=$tom)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OvergangsordningAndel

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

interface IOvergangsordningAndel {
    val id: Long
    val behandlingId: Long
}

class UtfyltOvergangsordningAndel(
    override val id: Long,
    override val behandlingId: Long,
    val person: Person,
    val prosent: BigDecimal,
    val fom: YearMonth,
    val tom: YearMonth,
) : IOvergangsordningAndel

class TomOvergangsordningAndel(
    override val id: Long,
    override val behandlingId: Long,
) : IOvergangsordningAndel

fun OvergangsordningAndel.tilIOvergangsordningAndel(): IOvergangsordningAndel =
    if (erObligatoriskeFelterUtfylt()) {
        UtfyltOvergangsordningAndel(id, behandlingId, person!!, prosent!!, fom!!, tom!!)
    } else {
        TomOvergangsordningAndel(id, behandlingId)
    }

data class OvergangsordningAndelPeriode(
    val behandlingId: Long,
    val person: Person,
    val prosent: BigDecimal,
)

fun OvergangsordningAndelPeriode.tilOvergangsordningAndel(
    fom: YearMonth,
    tom: YearMonth,
): OvergangsordningAndel =
    OvergangsordningAndel(
        behandlingId = behandlingId,
        person = person,
        prosent = prosent,
        fom = fom,
        tom = tom,
    )

fun List<UtfyltOvergangsordningAndel>.tilPerioder(): List<Periode<OvergangsordningAndelPeriode>> = map { it.tilPeriode() }

private fun UtfyltOvergangsordningAndel.tilPeriode(): Periode<OvergangsordningAndelPeriode> = Periode(OvergangsordningAndelPeriode(behandlingId, person, prosent), fom.atDay(1), tom.atEndOfMonth())

fun OvergangsordningAndel.erObligatoriskeFelterUtfylt(): Boolean =
    this.person != null &&
        this.fom != null &&
        this.tom != null &&
        this.prosent != null

fun OvergangsordningAndel.tilOvergangsordningAndelDto(): OvergangsordningAndelDto =
    OvergangsordningAndelDto(
        id = this.id,
        personIdent = this.person?.aktør?.aktivFødselsnummer(),
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
    )

fun OvergangsordningAndel.fraOvergangsordningAndelDto(
    overgangsordningAndelDto: OvergangsordningAndelDto,
    person: Person,
): OvergangsordningAndel {
    this.person = person
    this.prosent = overgangsordningAndelDto.prosent
    this.fom = overgangsordningAndelDto.fom
    this.tom = overgangsordningAndelDto.tom

    return this
}
