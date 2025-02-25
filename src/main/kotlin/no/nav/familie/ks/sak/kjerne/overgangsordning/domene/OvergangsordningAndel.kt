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
import no.nav.familie.ks.sak.common.exception.FunksjonellFeil
import no.nav.familie.ks.sak.common.util.MånedPeriode
import no.nav.familie.ks.sak.common.util.YearMonthConverter
import no.nav.familie.ks.sak.common.util.overlapperHeltEllerDelvisMed
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import no.nav.familie.tidslinje.Periode
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
    @Column(name = "antallTimer")
    var antallTimer: BigDecimal = BigDecimal.ZERO,
    @Column(name = "deltBosted")
    var deltBosted: Boolean = false,
    @Column(name = "fom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var fom: YearMonth? = null,
    @Column(name = "tom", columnDefinition = "DATE")
    @Convert(converter = YearMonthConverter::class)
    var tom: YearMonth? = null,
) : BaseEntitet() {
    val periode
        get(): MånedPeriode {
            validerAtObligatoriskeFelterErGyldigUtfylt()
            return MånedPeriode(fom!!, tom!!)
        }

    override fun toString(): String =
        "OvergangsordningAndel(" +
            "id=$id, " +
            "behandling=$behandlingId, " +
            "person=${person?.aktør}, " +
            "antallTimer=$antallTimer, " +
            "deltBosted=$deltBosted, " +
            "fom=$fom, " +
            "tom=$tom)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OvergangsordningAndel

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    fun erObligatoriskeFelterUtfylt(): Boolean =
        this.person != null &&
            this.fom != null &&
            this.tom != null

    fun validerAtObligatoriskeFelterErUtfylt() {
        if (!erObligatoriskeFelterUtfylt()) {
            throw FunksjonellFeil("Person, fom og tom skal være utfylt: $this")
        }
    }

    fun validerAtObligatoriskeFelterErGyldigUtfylt() {
        validerAtObligatoriskeFelterErUtfylt()
        if (fom!! > tom!!) {
            throw FunksjonellFeil(
                melding = "T.o.m. dato kan ikke være før f.o.m. dato",
                frontendFeilmelding = "Du kan ikke sette en t.o.m. dato som er før f.o.m. dato",
            )
        }

        if (antallTimer < BigDecimal.ZERO) {
            throw FunksjonellFeil(
                melding = "Antall timer kan ikke være negativ",
                frontendFeilmelding = "Du kan ikke sette et negativt antall timer",
            )
        }
    }

    fun tilOvergangsordningAndelDto(): OvergangsordningAndelDto =
        OvergangsordningAndelDto(
            id = this.id,
            personIdent = this.person?.aktør?.aktivFødselsnummer(),
            antallTimer = this.antallTimer,
            deltBosted = this.deltBosted,
            fom = this.fom,
            tom = this.tom,
        )

    fun fraOvergangsordningAndelDto(
        overgangsordningAndelDto: OvergangsordningAndelDto,
        person: Person,
    ): OvergangsordningAndel {
        this.person = person
        this.antallTimer = overgangsordningAndelDto.antallTimer
        this.deltBosted = overgangsordningAndelDto.deltBosted
        this.fom = overgangsordningAndelDto.fom
        this.tom = overgangsordningAndelDto.tom

        return this
    }

    fun tilUtfyltOvergangsordningAndel(): UtfyltOvergangsordningAndel {
        validerAtObligatoriskeFelterErGyldigUtfylt()
        return UtfyltOvergangsordningAndel(
            id = id,
            behandlingId = behandlingId,
            person = person!!,
            antallTimer = antallTimer,
            deltBosted = deltBosted,
            fom = fom!!,
            tom = tom!!,
        )
    }
}

fun List<OvergangsordningAndel>.utfyltePerioder(): List<UtfyltOvergangsordningAndel> =
    filter { it.erObligatoriskeFelterUtfylt() }
        .map {
            UtfyltOvergangsordningAndel(
                id = it.id,
                behandlingId = it.behandlingId,
                person = it.person!!,
                antallTimer = it.antallTimer,
                deltBosted = it.deltBosted,
                fom = it.fom!!,
                tom = it.tom!!,
            )
        }

data class UtfyltOvergangsordningAndel(
    val id: Long,
    val behandlingId: Long,
    val person: Person,
    val antallTimer: BigDecimal,
    val deltBosted: Boolean,
    val fom: YearMonth,
    val tom: YearMonth,
) {
    val periode
        get(): MånedPeriode = MånedPeriode(fom, tom)

    fun tilPeriode(): Periode<OvergangsordningAndelPeriode> =
        Periode(
            verdi = OvergangsordningAndelPeriode(behandlingId, person, antallTimer, deltBosted),
            fom = fom.atDay(1),
            tom = tom.atEndOfMonth(),
        )

    fun overlapperMed(overgangsordningAndel: UtfyltOvergangsordningAndel) = overgangsordningAndel.periode.overlapperHeltEllerDelvisMed(this.periode)
}

fun List<UtfyltOvergangsordningAndel>.tilPerioder(): List<Periode<OvergangsordningAndelPeriode>> = map { it.tilPeriode() }

data class OvergangsordningAndelPeriode(
    val behandlingId: Long,
    val person: Person,
    val antallTimer: BigDecimal,
    val deltBosted: Boolean,
) {
    fun tilOvergangsordningAndel(
        fom: YearMonth,
        tom: YearMonth,
    ): OvergangsordningAndel =
        OvergangsordningAndel(
            behandlingId = behandlingId,
            person = person,
            antallTimer = antallTimer,
            deltBosted = deltBosted,
            fom = fom,
            tom = tom,
        )
}
