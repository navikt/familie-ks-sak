package no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.statsborgerskap

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.common.entitet.DatoIntervallEntitet
import no.nav.familie.ks.sak.common.util.erInnenfor
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Medlemskap
import no.nav.familie.ks.sak.kjerne.personopplysninggrunnlag.domene.Person
import java.time.LocalDate

@Entity(name = "GrStatsborgerskap")
@Table(name = "PO_STATSBORGERSKAP")
data class GrStatsborgerskap(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "po_statsborgerskap_seq_generator")
    @SequenceGenerator(
        name = "po_statsborgerskap_seq_generator",
        sequenceName = "po_statsborgerskap_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @Embedded
    val gyldigPeriode: DatoIntervallEntitet? = null,

    @Column(name = "landkode", nullable = false)
    val landkode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "medlemskap", nullable = false)
    val medlemskap: Medlemskap = Medlemskap.UKJENT,

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_po_person_id", nullable = false, updatable = false)
    val person: Person
) : BaseEntitet() {

    fun gjeldendeNå(): Boolean = gyldigPeriode?.erInnenfor(LocalDate.now()) ?: true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrStatsborgerskap

        if (gyldigPeriode != other.gyldigPeriode) return false
        if (landkode != other.landkode) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * gyldigPeriode.hashCode() + landkode.hashCode()
    }

    companion object {
        fun fraStatsborgerskap(statsborgerskap: Statsborgerskap, medlemskap: Medlemskap, person: Person) =
            GrStatsborgerskap(
                gyldigPeriode = DatoIntervallEntitet(
                    fom = statsborgerskap.bekreftelsesdato ?: statsborgerskap.gyldigFraOgMed,
                    tom = statsborgerskap.gyldigTilOgMed
                ),
                landkode = statsborgerskap.land,
                medlemskap = medlemskap,
                person = person
            )
    }
}

fun List<GrStatsborgerskap>.filtrerGjeldendeNå(): List<GrStatsborgerskap> = this.filter { it.gjeldendeNå() }
