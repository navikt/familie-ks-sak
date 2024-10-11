package no.nav.familie.ks.sak.kjerne.kompensasjonsordning.domene

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
import no.nav.familie.ks.sak.api.dto.KompensasjonAndelDto
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.math.BigDecimal
import java.time.YearMonth

@Entity(name = "KompensasjonAndel")
@Table(name = "KOMPENSASJON_ANDEL")
data class KompensasjonAndel(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kompensasjon_andel_seq_generator")
    @SequenceGenerator(
        name = "kompensasjon_andel_seq_generator",
        sequenceName = "kompensasjon_andel_seq",
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
        "KompensasjonAndel(" +
            "id=$id, " +
            "behandling=$behandlingId, " +
            "person=${person?.aktør}, " +
            "prosent=$prosent, " +
            "fom=$fom, " +
            "tom=$tom)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KompensasjonAndel

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

fun KompensasjonAndel.tilKompensasjonAndelDto(): KompensasjonAndelDto =
    KompensasjonAndelDto(
        id = this.id,
        personIdent = this.person?.aktør?.aktivFødselsnummer(),
        prosent = this.prosent,
        fom = this.fom,
        tom = this.tom,
    )

fun KompensasjonAndel.fraKompenasjonAndelDto(
    kompensasjonAndelDto: KompensasjonAndelDto,
    person: Person,
): KompensasjonAndel {
    this.person = person
    this.prosent = kompensasjonAndelDto.prosent
    this.fom = kompensasjonAndelDto.fom
    this.tom = kompensasjonAndelDto.tom

    return this
}
