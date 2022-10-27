package no.nav.familie.ks.sak.kjerne.behandling.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.familie.ks.sak.common.entitet.BaseEntitet
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingSteg
import no.nav.familie.ks.sak.kjerne.behandling.steg.BehandlingStegStatus
import no.nav.familie.ks.sak.kjerne.behandling.steg.VenteÅrsak
import java.time.LocalDate
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity(name = "BehandlingStegTilstand")
@Table(name = "BEHANDLING_STEG_TILSTAND")
data class BehandlingStegTilstand(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "behandling_steg_tilstand_seq_generator")
    @SequenceGenerator(
        name = "behandling_steg_tilstand_seq_generator",
        sequenceName = "behandling_steg_tilstand_seq",
        allocationSize = 50
    )
    val id: Long = 0,

    @ManyToOne(optional = false)
    @JoinColumn(name = "fk_behandling_id", nullable = false, updatable = false)
    @JsonIgnore
    val behandling: Behandling,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg", nullable = false)
    val behandlingSteg: BehandlingSteg,

    @Enumerated(EnumType.STRING)
    @Column(name = "behandling_steg_status", nullable = false)
    var behandlingStegStatus: BehandlingStegStatus = BehandlingStegStatus.KLAR,

    @Enumerated(EnumType.STRING)
    @Column(name = "aarsak")
    var årsak: VenteÅrsak? = null,

    @Column(name = "frist")
    var frist: LocalDate? = null
) : BaseEntitet() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BehandlingStegTilstand

        return behandlingSteg == other.behandlingSteg
    }

    override fun hashCode(): Int {
        return Objects.hash(behandlingSteg)
    }

    override fun toString(): String {
        return "BehandlingStegTilstand(id=$id, behandling=${behandling.id}, behandlingSteg=$behandlingSteg, behandlingStegStatus=$behandlingStegStatus)"
    }
}
